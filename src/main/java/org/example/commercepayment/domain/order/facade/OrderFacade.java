package org.example.commercepayment.domain.order.facade;

import lombok.RequiredArgsConstructor;
import org.example.commercepayment.domain.cart.entity.CartItem;
import org.example.commercepayment.domain.cart.service.CartService;
import org.example.commercepayment.domain.member.entity.Member;
import org.example.commercepayment.domain.member.service.MemberService;
import org.example.commercepayment.domain.order.dto.*;
import org.example.commercepayment.domain.order.entity.Order;
import org.example.commercepayment.domain.order.entity.OrderItem;
import org.example.commercepayment.domain.order.entity.OrderStatus;
import org.example.commercepayment.domain.order.service.OrderService;
import org.example.commercepayment.domain.payment.entity.FailReason;
import org.example.commercepayment.domain.payment.entity.Payment;
import org.example.commercepayment.domain.payment.service.PaymentService;
import org.example.commercepayment.domain.product.entity.Product;
import org.example.commercepayment.domain.product.repository.ProductRepository;
import org.example.commercepayment.global.error.BusinessException;
import org.example.commercepayment.global.error.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class OrderFacade {

    private final CartService cartService;
    private final MemberService memberService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ProductRepository productRepository;

    // 주문서 미리보기_읽기 전용 + DB 변경 X
    public CheckoutResponse getCheckout(Long memberId, List<Long> cartItemIds) {

        Member member = memberService.findById(memberId);
        // cartItemIds가 null/비어있으면 "전체 장바구니", 값이 있으면 "선택도니 아이템만"
        List<CartItem> cartItems = getValidateCartItems(
                memberId, cartItemIds != null ? cartItemIds : List.of());

        // 스냅샷 전단계_상품 현재가 반영
        List<CheckoutResponse.CheckoutItemResponse> items = cartItems.stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();
                    int price = product.getPrice();
                    int quantity = cartItem.getQuantity();

                    return new CheckoutResponse.CheckoutItemResponse(
                            product.getId(),
                            product.getName(),
                            price,
                            quantity,
                            price * quantity,
                            product.getStock(),
                            product.getStock() >= quantity // 결제 화면에서 품절을 미리 안내
                    );
                })
                .toList();

        // 장바구니 총액
        int totalPrice = items.stream()
                .mapToInt(CheckoutResponse.CheckoutItemResponse::subtotal)
                .sum();

        return new CheckoutResponse(items, totalPrice, member.getPoint());
    }

    // 주문 생성
    // 재고 검증 > 선차감 > 주문생성 > 결제생성 + PaymentId 채번
    @Transactional
    public OrderCheckoutResponse createOrder(Long memberId, OrderCheckoutRequest request) {
        List<Long> cartItemIds = (request != null) ? request.cartItemIds() : List.of();
        int usePoint = (request != null) ? request.usePoint() : 0;

        // 0. 회원 조회
        Member member = memberService.findById(memberId);

        // 1. 장바구니 조회 (선택된 아이템만)
        List<CartItem> cartItems = getValidateCartItems(memberId, cartItemIds);
        // 상품에 비관적 락을 걸고 일괄 조회
        // 순서대로 잡아야 데드락이 없으니 정렬비
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = productRepository.findAllByIdForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 2. 재고 선차감 + 스냅샷 OrderItem 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProductId());
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            // 3. 재고가 하나라도 부족하면 예외 > 트랜잭션 롤백 > 재고 원복
            product.deductStock(cartItem.getQuantity());

            orderItems.add(new OrderItem(product, product.getPrice(), cartItem.getQuantity()));
        }

        int totalPrice = orderItems.stream().mapToInt(OrderItem::getSubtotal).sum();

        // 포인트 검증_당장 차감 X > 차감은 결제 확정 시
        if (usePoint > totalPrice) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        if (member.getPoint() < usePoint) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT_BALANCE);
        }
        // 4. 주문 저장
        Order order = orderService.createOrder(member, orderItems, usePoint);

        // 5. 결제 정보 생성 (IN_PROGRESS 상태)
        Payment payment = paymentService.createPayment(order, totalPrice, usePoint);

        // 6. 응답
        return new OrderCheckoutResponse(
                order.getId(),
                order.getOrderNumber(),
                payment.getPortonePaymentId(),
                totalPrice,
                usePoint,
                payment.getPgAmount(),   // 0이면 클라이언트가 결제창을 건너뛴다
                order.getOrderName(),
                order.getStatus().name()
        );
    }

    // 주문 취소
    @Transactional
    public void cancelOrder(Long memberId, Long orderId) {
        Order order = orderService.findOrderEntity(orderId);
        validateOwner(order, memberId);

        // 결제 대기 아닌거 거르기
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
        }

        // 상태 전이_취소된 주문은 막힘
        order.transitTo(OrderStatus.CANCELED);
        Payment payment = paymentService.findByOrderId(orderId); 
        paymentService.failPayment(payment, FailReason.USER_CANCELLED); // 메서드명 변경 필요

        // 선차감 재고 복구
        order.getOrderItems().forEach(item ->
                item.getProduct().restoreStock(item.getQuantity()));
    }

    // 조회
    public List<OrderResponse> getOrders(Long memberId) {
        List<Order> orders = orderService.findOrderEntities(memberId);
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        //IN 쿼리 한번으로 결제를 모아 N+1 피하기
        Map<Long, Payment> paymentMap = paymentService.findPaymentMapByOrderIds(orderIds);

        return orders.stream()
                .map(order -> orderService.toResponse(order, paymentMap.get(order.getId())))
                .toList();
    }

    public OrderResponse getOrder(Long memberId, Long orderId) {
        Order order = orderService.findOrderEntity(orderId);
        validateOwner(order, memberId);

        Payment payment = paymentService.findByOrderId(orderId);

        return orderService.toResponse(order, payment);
    }
    // 소유권 검증 메서드
    private void validateOwner(Order order, Long memberId) {
        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }
    // cartItem 리스트가
    private List<CartItem> getValidateCartItems(Long memberId, List<Long> cartItemIds) {

        List<CartItem> cartItems = cartItemIds.isEmpty()
                ? cartService.findCartEntities(memberId)
                : cartService.findCartEntitiesByIds(memberId, cartItemIds);

        // 1차 검증: 장바구니에 아무것도 없다?
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        // 2차 검증: 선택한 상품 개수가 다르다?(일부가 잘못된 조회)
        if (!cartItemIds.isEmpty() && cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cartItems;
    }
}
