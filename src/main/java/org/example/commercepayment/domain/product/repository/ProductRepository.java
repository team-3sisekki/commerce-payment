package org.example.commercepayment.domain.product.repository;

import jakarta.persistence.LockModeType;
import org.example.commercepayment.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // JPQL을 사용하여 동적 쿼리를 처리
    // 파라미터가 null일 경우 해당 조건을 무시하는 패턴을 사용
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:salesStatus IS NULL OR p.salesStatus = :salesStatus) AND " +
            "(:isSoldOut IS NULL OR (:isSoldOut = true AND p.stock = 0) OR (:isSoldOut = false AND p.stock > 0))")
    Page<Product> findProductsByConditions(
            @Param("category") String category,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("salesStatus") String salesStatus,
            @Param("isSoldOut") Boolean isSoldOut,
            Pageable pageable
    );

    // 재고 차감 전용 조회
    // 락이 없으면 동시에 여러개를 주문 시 재고가 부족해서 통과가 된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}