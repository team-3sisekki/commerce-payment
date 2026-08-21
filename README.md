# commerce-payment

포트원(PortOne) V2 API를 연동하여 회원, 장바구니, 주문, 결제, 포인트 그리고 부분/전액 환불까지 커머스의 핵심 결제 사이클을 구현한 프로젝트입니다.

🚀 **서비스 배포 주소**: [https://zcookiez.click/](https://zcookiez.click/)

*(포트원 결제 웹훅(Webhook) 수신 등 외부 결제망과의 안전한 통신을 위해 HTTPS 보안 인증서가 적용되어 있습니다.)*

🔗 **[API 명세서 보러가기 (Notion)](https://app.notion.com/p/teamsparta/a662dc3ef5148347900601b45f23890a?v=8fa2dc3ef5148354a111080373492c15)**

---

## 🎯 핵심 비즈니스 도메인 (Core Domains)

| 도메인 (Domain) | 주요 기능 (Features) | 상세 설명 (Description) |
| :---: | :--- | :--- |
| 👤 **Member** | 회원가입, 로그인, 정보 조회 | JWT 기반 인증 및 사용자별 포인트 잔액 조회 관리 |
| 🛍️ **Product** | 상품 페이징 조회, 상세 확인 | 구매 가능한 상품 목록 및 상품별 재고/상세 정보 제공 |
| 🛒 **Cart** | 담기, 수량 수정, 삭제 | 사용자가 구매를 원하는 상품의 장바구니 목록 관리 |
| 📦 **Order** | 주문 생성, 내역 조회, 취소 | 주문서(Checkout) 생성 및 결제 대기(Pending) 상태 관리 |
| 💳 **Payment** | 결제 검증, 승인 및 실패 처리 | **포트원(PortOne)** 외부 PG 연동 및 결제 금액 정합성 2중 검증 |
| 💰 **Point** | 적립, 사용, 원장 기록 및 회수 | 비관적 락(Pessimistic Lock)을 통한 동시성 제어 및 포인트 거래 내역(원장) 관리 |
| 🔄 **Refund** | 부분/전액 환불, 결제 취소 | 포인트/PG 결제액 분할 비율 환불 처리 및 PG사 취소 통신 연동 |

---

## 🛠 주요 기술 스택

| 분류 (Category) | 사용 기술 (Tech Stack) | 설명 (Description) |
| :--- | :--- | :--- |
| **Backend** | Java 17, Spring Boot 4.1.0 | 메인 애플리케이션 프레임워크 |
| **Data Access** | Spring Data JPA | ORM 기반 데이터베이스 접근 및 조작 |
| **Security** | Spring Security, JWT, BCrypt | 사용자 인증/인가 및 패스워드 암호화 |
| **Template Engine**| Thymeleaf | 서버 사이드 HTML 렌더링 |
| **Build Tool** | Gradle | 프로젝트 빌드 및 라이브러리 의존성 관리 |
| **Database** | MySQL (AWS RDS) | 메인 관계형 데이터베이스 관리 시스템 |
| **Infra & CI/CD** | AWS EC2, GitHub Actions, Docker, HTTPS | 클라우드 서버 구축 및 HTTPS 보안, 컨테이너 기반 자동 배포 |
| **External API** | PortOne (포트원) API V2 | 외부 PG 연동을 통한 실시간 결제 및 환불 처리 |

---

## 📊 ERD (Entity Relationship Diagram)
![ERD](https://github.com/user-attachments/assets/90b1c202-880a-4c1a-8409-0de6d9bcb93e)

---

## 📁 디렉토리 구조
```text
    ├── 📁 domain         # 핵심 비즈니스 로직을 도메인(기능) 단위로 분리한 패키지
    │   ├── 📁 auth       # 인증/인가 도메인
    │   ├── 📁 cart       # 장바구니 도메인
    │   ├── 📁 member     # 회원 도메인
    │   ├── 📁 order      # 주문 도메인
    │   ├── 📁 payment    # 결제 도메인 (아래는 payment 도메인의 내부 구조 예시)
    │   │   ├── 📁 controller  # HTTP 요청을 받고 응답을 반환하는 프레젠테이션 계층
    │   │   ├── 📁 dto         # 계층 간 데이터 교환을 위한 객체 (Request, Response DTO 등)
    │   │   ├── 📁 entity      # 데이터베이스 테이블과 매핑되는 JPA 엔티티 객체
    │   │   ├── 📁 port        # 외부 시스템 또는 인프라와의 통신을 위한 인터페이스(포트) 모음
    │   │   ├── 📁 repository  # 데이터베이스에 접근하여 엔티티를 저장/조회하는 영속성 계층
    │   │   ├── 📁 service     # 핵심 비즈니스 로직을 수행하는 서비스 계층
    │   │   └── 📁 facade      # 다중 서비스간의 복잡한 로직을 조합하는 파사드 계층
    │   ├── 📁 refund     # 환불 도메인
    │   ├── 📁 product    # 상품 도메인
    │   └── 📁 point      # 포인트 도메인
    │
    ├── 📁 global         # 프로젝트 전역에서 공통으로 사용되는 인프라성/설정 코드 패키지
    │   ├── 📁 config     # Spring Security, WebMvc, Swagger 등 각종 설정 파일
    │   ├── 📁 entity     # 공통 엔티티 속성 (예: 생성일, 수정일을 담은 BaseEntity)
    │   ├── 📁 error      # 전역 예외 처리(Global Exception Handler) 및 커스텀 예외 정의
    │   ├── 📁 filter     # 서블릿 필터 (예: CORS 필터, 로깅 필터 등)
    │   ├── 📁 jwt        # JWT 토큰 생성 및 검증을 위한 유틸리티 및 프로바이더
    │   └── 📁 response   # API 공통 응답 포맷 (CommonResponse, ApiResponse 등)
    │
    ├── 📁 infra          # 외부 시스템 연동 및 인프라 구현체 패키지
    │   └── 📁 portone    # 외부 결제 PG사(PortOne) 연동 관련 구현 코드
    │
    └── 📁 web            # 프론트엔드 뷰(View) 렌더링용 컨트롤러 패키지
```

---

## 📌 개발 규칙 및 코드 컨벤션

### 1. 아키텍처 및 DTO
* **Facade Pattern**: Controller와 Service 사이에 Facade 계층을 두어, 여러 도메인 Service(예: OrderService + PaymentService)를 조합해야 하는 복잡한 트랜잭션과 로직의 흐름을 제어합니다.
* **DTO (Data Transfer Object)**: 최신 자바 문법인 `public record`를 사용하여 불변(Immutable) 객체로 선언하며 간결함을 유지합니다.
* **Entity 생성**: 객체 생성의 무분별한 사용을 막기 위해 `@Builder`를 활용하며, 최근 결제/환불 도메인부터 `private/public` 생성자와 빌더 패턴을 팀 표준으로 도입하여 일관성을 맞추고 있습니다.
* **DTO 매핑**: Entity를 DTO로 매핑할 때 Service 로직이 비대해지는 것을 방지하기 위해, DTO 내부에 `public static from(Entity)` 정적 팩토리 메서드를 만들어 변환 책임을 DTO에 위임합니다.

### 2. 공통 응답 및 예외 처리 (Common Response & Exception Handling)
* **공통 응답 (ApiResponse & ResponseEntity)**: 모든 API 응답은 `ApiResponse<T>` 객체로 래핑한 뒤,  `ResponseEntity`로 한 번 더 감싸서 반환합니다. 이를 통해 클라이언트는 명확한 HTTP 상태 코드(Status Code)와 함께 항상 일관된 JSON 구조로 데이터를 받아볼 수 있습니다.
* **전역 예외 처리 (GlobalExceptionHandler)**: `@RestControllerAdvice`를 활용해 애플리케이션 전역에서 발생하는 예외를 한 곳에서 중앙 집중식으로 캐치하고 처리합니다.
* **커스텀 비즈니스 예외 (BusinessException)**: 도메인 로직 처리 중 문제가 발생하면 `ErrorCode` enum에 정의된 에러 코드와 메시지를 담아 `throw new BusinessException(...)`을 발생시키며, 이는 자동으로 규격화된 에러 JSON 응답으로 클라이언트에게 전달됩니다.

### 3. 네이밍 규칙 (Naming Conventions)
* 클래스명: PascalCase (예: AdminService)
* 메서드 및 변수명: camelCase (예: getAdmins, adminId)
* 상수명: UPPER_SNAKE_CASE (예: MAX_PAGE_SIZE)
* [DB 테이블 및 컬럼](https://app.notion.com/p/teamsparta/3ba2dc3ef51480c8b635d36ff3f58c64): snake_case (예: admin_role, created_at)

### 4. RESTful API 설계
* URI 표기: 소문자와 하이픈(-) 위주로 사용하며, 자원(Resource)은 복수형 명사로 표현합니다. (예: /admins/{adminId})
* HTTP 메서드: 의미에 맞는 표준 메서드(GET, POST, PUT, PATCH, DELETE)를 엄격히 사용합니다.

### 5. Github 규칙 (Github Rules)
[🔗 Github 규칙 보러가기 (Notion)](https://app.notion.com/p/teamsparta/Github-Rules-f122dc3ef51483a9a9f281afd1836033)
* ✨ feat : 새로운 기능 추가
* 🐛 fix : 버그 수정
* 📄 docs : 문서 수정
* ♻️ style : 코드 포멧팅, 세미콜론 누락, 코드 변경이 없는 경우
* 🩹 refactor : 코드 리펙토링
* 🚚 test : 테스트 코드, 리펙토링 테스트 코드 추가
* 🔥 chore : 빌드 업무 수정, 패키지 매니저 수정
