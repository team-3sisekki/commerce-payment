-- ============================================================
-- 테스트용 더미 데이터 (main 병합 후 구조 기준)
--
-- 병합으로 바뀐 점
--   members.password        (password_hash 아님)
--   products.stock_quantity (stock 아님)
--   products.category / sales_status 추가 (NOT NULL)
--   carts 테이블 신설 → cart_items 는 cart_id 를 참조
--
-- 실행 조건 (application.yml)
--   spring.jpa.hibernate.ddl-auto: create
--   spring.jpa.defer-datasource-initialization: true
--   spring.sql.init.mode: always
--
-- 모든 회원의 비밀번호는 12345678 입니다.
-- ============================================================


-- ── 회원 ─────────────────────────────────────────────────────
-- point 는 스냅샷이므로 아래 point_transactions 와 값이 반드시 맞아야 한다.
-- (불변식: members.point == SUM(point_transactions.amount))

-- id=1 : 기본 테스트 계정 (50,000P)
INSERT INTO members (email, password, name, phone_number, point, created_at, updated_at)
VALUES ('yoshi@test.com',
        '$2b$10$Ep.t/i1/Ni8NTbzpezsmie4K7XjgfswtEkHREyRYuKz9bYGK70ZZq',
        '요시', '010-1111-1111', 50000, NOW(), NOW());

-- id=2 : 타인 자원 접근 테스트용 (403 검증)
INSERT INTO members (email, password, name, phone_number, point, created_at, updated_at)
VALUES ('other@test.com',
        '$2b$10$Ep.t/i1/Ni8NTbzpezsmie4K7XjgfswtEkHREyRYuKz9bYGK70ZZq',
        '남의계정', '010-2222-2222', 0, NOW(), NOW());

-- id=3 : 포인트 부족 케이스 테스트용 (1,000P)
INSERT INTO members (email, password, name, phone_number, point, created_at, updated_at)
VALUES ('poor@test.com',
        '$2b$10$Ep.t/i1/Ni8NTbzpezsmie4K7XjgfswtEkHREyRYuKz9bYGK70ZZq',
        '빈지갑', '010-3333-3333', 1000, NOW(), NOW());


-- ── 포인트 원장 ───────────────────────────────────────────────
-- 잔액만 넣고 원장을 비워두면 정합성 검증이 시작부터 실패한다.
-- payment_id 는 결제와 무관한 초기 지급이므로 NULL.

INSERT INTO points (member_id, payment_id, transaction_type, amount, created_at, updated_at)
VALUES (1, NULL, 'EARN', 50000, NOW(), NOW());

INSERT INTO points (member_id, payment_id, transaction_type, amount, created_at, updated_at)
VALUES (3, NULL, 'EARN', 1000, NOW(), NOW());
-- id=2 는 잔액 0 이라 원장도 없음 (0 == 0 으로 불변식 성립)


-- ── 상품 ─────────────────────────────────────────────────────
-- 컬럼명 주의: stock 이 아니라 stock_quantity
-- category, sales_status 는 NOT NULL 이라 반드시 값이 필요하다
INSERT INTO products (name, price, stock_quantity, description, category, sales_status, created_at, updated_at) VALUES
    ('사과',        300, 100, '맛있는 사과입니다',                   '식품', 'ON_SALE',      NOW(), NOW()),  -- id=1
    ('바나나',      500,  50, '노란 바나나입니다',                   '식품', 'ON_SALE',      NOW(), NOW()),  -- id=2
    ('한정판 굿즈', 2000,   3, '재고 부족 롤백 테스트용 (재고 3개)',  '잡화', 'ON_SALE',      NOW(), NOW()),  -- id=3
    ('품절상품',    10000,   0, '품절 노출 정책 테스트용 (재고 0)',    '잡화', 'ON_SALE',      NOW(), NOW()),  -- id=4
    ('9천원상품',    9000, 100, '포인트 전액결제 테스트용',            '식품', 'ON_SALE',      NOW(), NOW()),  -- id=5
    ('단종상품',     7000,  10, '단종 필터 테스트용',                 '잡화', 'DISCONTINUED', NOW(), NOW());  -- id=6


-- ── 장바구니 ─────────────────────────────────────────────────
-- 병합 후 구조: 회원 1명당 carts 1건 → cart_items 가 cart_id 를 참조
INSERT INTO carts (member_id, created_at, updated_at) VALUES
    (1, NOW(), NOW()),   -- cart id=1 : 요시
    (2, NOW(), NOW()),   -- cart id=2 : 남의계정
    (3, NOW(), NOW());   -- cart id=3 : 빈지갑

-- 요시의 장바구니 (주문서 미리보기/주문 생성을 바로 테스트할 수 있게 미리 담아둠)
INSERT INTO cart_items (cart_id, product_id, quantity, created_at, updated_at) VALUES
    (1, 1, 3, NOW(), NOW()),   -- cart_item id=1 : 사과 3개   = 9,000원
    (1, 2, 2, NOW(), NOW());   -- cart_item id=2 : 바나나 2개 = 10,000원
                               -- 합계 19,000원

-- 남의계정 장바구니 (타인 자원 접근 테스트용)
INSERT INTO cart_items (cart_id, product_id, quantity, created_at, updated_at) VALUES
    (2, 1, 1, NOW(), NOW());   -- cart_item id=3

-- 빈지갑 장바구니 (포인트 부족 테스트용, 9,000원짜리)
INSERT INTO cart_items (cart_id, product_id, quantity, created_at, updated_at) VALUES
    (3, 5, 1, NOW(), NOW());   -- cart_item id=4


-- ============================================================
-- 테스트 시나리오 빠른 참조
--
-- [카드 단독]    yoshi  cartItemIds:[1,2] usePoint:0     -> pgAmount 19000
-- [포인트 일부]  yoshi  cartItemIds:[1,2] usePoint:5000  -> pgAmount 14000
-- [포인트 전액]  yoshi  cartItemIds:[1]   usePoint:9000  -> pgAmount 0
-- [잔액 부족]    poor   cartItemIds:[4]   usePoint:5000  -> 409 POINT_001
-- [포인트 초과]  yoshi  cartItemIds:[1]   usePoint:20000 -> 400 POINT_002
-- [재고 부족]    한정판 굿즈(id=3) 5개 담고 주문          -> 409 PRODUCT_002 + 전체 롤백
-- [타인 주문]    other 토큰으로 요시 주문 조회            -> 403 ORDER_003
--
-- 정합성 검증 쿼리:
--   SELECT m.id, m.point, COALESCE(SUM(pt.amount),0) AS ledger,
--          m.point = COALESCE(SUM(pt.amount),0) AS ok
--   FROM members m LEFT JOIN point_transactions pt ON pt.member_id = m.id
--   GROUP BY m.id, m.point;
--
-- 재고 확인:
--   SELECT id, name, stock_quantity FROM products;
-- ============================================================
