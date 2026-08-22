package org.example.commercepayment.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 화면 컨트롤러 : SPA 방식의 HTML 쉘 반환
 * 모든 데이터는 JavaScript가 /api/** 엔드포인트를 호출하여 가져옵니다.
 * PortOne 설정(storeId, channelKey)도 /api/config/portone API로 조회합니다.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/products/{id}")
    public String productDetail() {
        return "product/detail";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart/list";
    }

    @GetMapping("/orders")
    public String orders() {
        return "order/list";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetail() {
        return "order/detail";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "order/checkout";
    }

    @GetMapping("/orders/complete")
    public String orderComplete() {
        return "order/complete";
    }

    @GetMapping("/webhooks")
    public String webhooks() {
        return "webhook/list";
    }

}
