# 통합 테스트 전략 및 커버리지

## 📌 개요

이 문서는 프로젝트의 통합테스트에 대해 정리한 문서입니다.

---

## 🎯 통합 테스트 기준

### ✅ 반드시 테스트하는 경우

1. **돈이 오가는 API**
   - 결제, 환불, 포인트 충전/사용
   - 쿠폰 발급/사용
   - 이유: 데이터 정합성이 매우 중요

2. **동시성 문제가 있는 API**
   - 재고 차감
   - 쿠폰 발급 (선착순)
   - 포인트 사용
   - 이유: Race Condition 방지

3. **복잡한 비즈니스 로직**
   - 주문 생성 (재고 차감 + 포인트 사용 + 쿠폰 적용)
   - 주문 취소 (재고 복구 + 포인트 환불)
   - 이유: 여러 도메인이 연계되어 통합 테스트 필수

4. **여러 테이블을 JOIN하는 복잡한 조회**
   - 주문 상세 조회 (Order + OrderItem + Product)
   - 사용자 주문 목록 (N+1 문제 해결됨 - FETCH JOIN 적용)
   - 장바구니와 아이템 조회 (N+1 문제 해결됨 - FETCH JOIN 적용)
   - 포인트와 히스토리 조회 (N+1 문제 해결됨 - FETCH JOIN 적용)
   - 결제와 결제이력 조회 (N+1 문제 해결됨 - FETCH JOIN 적용)
   - 이유: 성능 및 데이터 정합성 검증

### ❌ 테스트하지 않는 경우

1. **단순 CRUD**
   - GET /users/{id} (단순 조회)
   - GET /products (단순 목록)
   - 이유: 비즈니스 로직 없음, Repository 테스트로 충분

2. **비즈니스 로직이 없는 API**
   - 단순 조회
   - 단순 저장
   - 이유: Unit 테스트로 충분

3. **Unit 테스트로 충분한 경우**
   - Service 레이어에서 이미 검증됨
   - 이유: 통합 테스트 비용 대비 효과 낮음

---

## 📊 API 통합 테스트 커버리지

### 1. Order (주문)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /orders | 주문 생성 | ✅ 완료 | 핵심 비즈니스 로직, 복잡한 트랜잭션 |
| GET /orders/{orderId} | 주문 조회 | ❌ 불필요 | 단순 조회 |
| GET /orders?userId={userId} | 사용자 주문 목록 | ✅ 완료 | JOIN 많음, N+1 검증 |
| POST /orders/{orderId}/confirm | 주문 확정 | ✅ 완료 | 상태 변경 로직 |
| POST /orders/{orderId}/cancel | 주문 취소 | ✅ 완료 | 재고 복구, 포인트 환불 |
| 동시성 테스트 | 주문 동시 처리 | ✅ 완료 | 동시 주문 생성, 같은 사용자 동시 주문, 읽기/쓰기 동시성 |

### 2. Payment (결제)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /payments | 결제 생성 | ✅ 완료 | 돈이 오가는 API |
| GET /payments/{paymentId} | 결제 조회 | ❌ 불필요 | 단순 조회 |

### 3. Point (포인트)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /point/charge | 포인트 충전 | ✅ 완료 | 돈이 오가는 API |
| POST /point/use | 포인트 사용 | ✅ 완료 | 동시성 문제, 잔액 검증 |
| POST /point/expire | 포인트 만료 | ✅ 완료 | 비즈니스 로직 |
| GET /point/{userId} | 포인트 조회 | ❌ 불필요 | 단순 조회 |
| GET /point/{userId}/history | 포인트 히스토리 | ✅ 완료 | 페이징, 정렬 검증 |
| 동시성 테스트 | 포인트 동시 처리 | ✅ 완료 | 포인트 동시 사용 시나리오 검증 |

### 4. Coupon (쿠폰)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /coupon/issue | 쿠폰 발급 | ✅ 완료 | 선착순, 동시성 문제 |
| GET /coupon/{userId} | 사용자 쿠폰 조회 | ❌ 불필요 | 단순 조회 |
| 동시성 테스트 | 쿠폰 동시 발급 | ✅ 완료 | 선착순 쿠폰 발급 동시성 검증 |

### 5. Inventory (재고)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /inventory/deduct | 재고 차감 | ✅ 완료 | 동시성 문제, 재고 검증 |
| GET /inventory/{productId} | 재고 조회 | ❌ 불필요 | 단순 조회 |
| 동시성 테스트 | 재고 동시 차감 | ✅ 완료 | 재고 동시 차감 시나리오 검증 |

### 6. Cart (장바구니)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /cart | 장바구니 추가 | ✅ 완료 | 비즈니스 로직 (티 선택) |
| GET /cart/{userId} | 장바구니 조회 | ❌ 불필요 | 단순 조회 |
| DELETE /cart/{cartId} | 장바구니 삭제 | ❌ 불필요 | 단순 삭제 |

### 7. Product (상품)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| GET /products | 상품 목록 | ❌ 불필요 | 단순 조회 |
| GET /products/{productId} | 상품 조회 | ❌ 불필요 | 단순 조회 |
| GET /products/{productId}/statistics | 상품 통계 | ✅ 완료 | 복잡한 집계 로직 |

### 8. User (사용자)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| POST /users | 사용자 생성 | ✅ 완료 | 유효성 검증 |
| GET /users/{userId} | 사용자 조회 | ❌ 불필요 | 단순 조회 |

### 9. Delivery (배송)

| API | Method | 테스트 여부 | 이유 |
|-----|--------|------------|------|
| GET /delivery/{orderId}/status | 배송 상태 조회 | ✅ 완료 | 외부 API 연동 시뮬레이션 |

---

## 📈 통합 테스트 통계

### 현재 상태
- **총 통합 테스트:** 19개
- **핵심 기능 테스트:** 13개 (비즈니스 로직)
- **동시성 테스트:** 4개 (포인트, 쿠폰, 재고, 주문)
- **단순 기능 테스트:** 2개 (사용자 검증, 배송 상태)

### 도메인별 분포
- **Order (주문):** 5개 - 생성, 확정, 취소, 목록 조회, 동시성
- **Point (포인트):** 5개 - 충전, 사용, 만료, 히스토리, 동시성
- **Coupon (쿠폰):** 2개 - 발급, 동시성
- **Inventory (재고):** 2개 - 차감, 동시성
- **Payment (결제):** 1개 - 생성
- **Cart (장바구니):** 1개 - 추가
- **Product (상품):** 1개 - 통계
- **User (사용자):** 1개 - 검증
- **Delivery (배송):** 1개 - 상태 조회

---

## 🎓 통합 테스트 아키텍처

### 테스트 베이스 클래스
- **KotestIntegrationTestBase**: 모든 통합 테스트의 공통 베이스
- **TestContainers MySQL**: 실제 데이터베이스 환경 시뮬레이션
- **생성자 주입**: Spring의 의존성 주입 활용

### 테스트 패턴
```kotlin
class XxxIntegrationTest(
    private val xxxUseCase: XxxUseCase,
    private val xxxRepository: XxxRepository
) : KotestIntegrationTestBase({

    describe("기능 설명") {
        context("조건") {
            it("예상 결과") {
                // Given (준비)
                // When (실행)
                // Then (검증)
            }
        }
    }
})
```

### 동시성 테스트 전략
동시성 문제가 있는 API에 대해 별도의 동시성 테스트 구현:
1. **PointConcurrencyIntegrationTest** - 포인트 동시 사용
2. **CouponConcurrencyIntegrationTest** - 쿠폰 선착순 발급
3. **InventoryConcurrencyIntegrationTest** - 재고 동시 차감
4. **OrderServiceConcurrencyIntegrationTest** - 주문 동시 생성

---

## 📝 통합 테스트 작성 가이드

### 1. 네이밍 컨벤션
- **기능별**: `{Domain}{Action}IntegrationTest` (예: OrderCreateIntegrationTest)
- **동시성**: `{Domain}ConcurrencyIntegrationTest` (예: PointConcurrencyIntegrationTest)

### 2. 테스트 구조
- **describe**: 도메인/기능 설명
- **context**: 테스트 조건/시나리오
- **it**: 예상 결과/검증 항목

### 3. 검증 항목
- ✅ 데이터 정합성 (저장된 값이 정확한지)
- ✅ 예외 처리 (잘못된 요청 시 올바른 예외)
- ✅ 상태 변경 (주문 상태, 재고 등)
- ✅ 연관 데이터 (주문 생성 시 OrderItem도 함께)
- ✅ 동시성 (Race Condition 방지)
- ✅ N+1 문제 해결 (FETCH JOIN 적용)

---

## 🔧 N+1 문제 해결

### 문제 식별 및 해결 현황

**2025년 1월 업데이트**: 강하게 결합된 엔티티들에 직접 참조와 FETCH JOIN을 적용하여 N+1 문제를 해결했습니다.

### ✅ 해결된 N+1 문제 영역

#### 1. **Order-OrderItem 관계**
```kotlin
// 기존 문제 (N+1 발생)
fun getOrdersByUser(userId: Long): List<Order> {
    val orders = orderRepository.findByUserIdAndIsActive(userId, true)
    orders.forEach { order ->
        order.orderItems // 각 Order마다 별도 쿼리 실행
    }
}

// 해결 후 (FETCH JOIN 적용)
fun getOrdersByUser(userId: Long): List<Order> {
    return orderRepository.findOrdersWithItemsByUserId(userId) // 한 번의 쿼리
}
```

**적용된 최적화**:
- `@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)` 직접 참조 추가
- `findOrdersWithItemsByUserId()` FETCH JOIN 쿼리 메서드 추가
- `findOrderWithItemsById()` 주문 확정 시 FETCH JOIN 활용

#### 2. **Cart-CartItem 관계**
```kotlin
// 기존 문제 (N+1 발생)
fun getCartByUser(userId: Long): Cart? {
    val cart = cartRepository.findByUserId(userId)
    cart?.items // CartItem 별도 조회
}

// 해결 후 (FETCH JOIN 적용)
fun getCartByUser(userId: Long): Cart? {
    return cartRepository.findByUserIdWithItems(userId) // 한 번의 쿼리
}
```

**적용된 최적화**:
- `@OneToMany(mappedBy = "cart", fetch = FetchType.LAZY)` 직접 참조 추가
- 모든 Cart 관련 메서드에서 `findByUserIdWithItems()` 활용

#### 3. **UserPoint-PointHistory 관계**
```kotlin
// 새로 추가된 최적화 메서드
fun getUserPointWithHistories(userId: Long): UserPoint? {
    return userPointRepository.findUserPointWithHistoriesByUserId(userId)
}
```

**적용된 최적화**:
- `@OneToMany(mappedBy = "userPoint", fetch = FetchType.LAZY)` 직접 참조 추가
- `findUserPointWithHistoriesByUserId()` FETCH JOIN 메서드 추가

#### 4. **Payment-PaymentHistory 관계**
```kotlin
// 새로 추가된 최적화 메서드들
fun getPaymentWithHistories(paymentId: Long): Payment? {
    return paymentRepository.findPaymentWithHistoriesById(paymentId)
}

fun getPaymentsWithHistoriesByOrderId(orderId: Long): List<Payment> {
    return paymentRepository.findPaymentsWithHistoriesByOrderId(orderId)
}
```

**적용된 최적화**:
- `@OneToMany(mappedBy = "payment", fetch = FetchType.LAZY)` 직접 참조 추가
- 다양한 조회 패턴에 FETCH JOIN 메서드 추가

### 🎯 성능 최적화 효과

#### Before (N+1 문제)
```sql
-- 사용자 주문 목록 조회 시
SELECT * FROM orders WHERE user_id = 1;           -- 1회
SELECT * FROM order_item WHERE order_id = 101;    -- N회 (주문 수만큼)
SELECT * FROM order_item WHERE order_id = 102;    -- N회
SELECT * FROM order_item WHERE order_id = 103;    -- N회
-- 총 1 + N개의 쿼리
```

#### After (FETCH JOIN 적용)
```sql
-- 한 번의 쿼리로 해결
SELECT o.*, oi.*
FROM orders o
LEFT JOIN order_item oi ON o.id = oi.order_id
WHERE o.user_id = 1
ORDER BY o.created_at DESC;
-- 총 1개의 쿼리
```

### 📏 도메인 경계 고려사항

**✅ 직접 참조 적용 대상** (같은 마이크로서비스):
- Order ↔ OrderItem (주문 서비스)
- Cart ↔ CartItem (장바구니 서비스)
- UserPoint ↔ PointHistory (포인트 서비스)
- Payment ↔ PaymentHistory (결제 서비스)

**❌ 간접 참조 유지 대상** (MSA 경계):
- Order → User (Order 서비스 → User 서비스)
- Order → Product (Order 서비스 → Product 서비스)
- Payment → Order (Payment 서비스 → Order 서비스)

### 🚀 사용 가이드

**상황별 메서드 선택**:
```kotlin
// 연관 데이터가 필요 없는 경우
orderRepository.findByUserId(userId)

// 연관 데이터가 필요한 경우 (성능 최적화)
orderRepository.findOrdersWithItemsByUserId(userId)

// 특정 주문과 아이템을 함께 조회
orderRepository.findOrderWithItemsById(orderId)
```