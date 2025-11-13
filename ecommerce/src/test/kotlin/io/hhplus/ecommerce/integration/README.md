# 통합 테스트 가이드

## 📋 개요

이 디렉토리는 ecommerce 프로젝트의 **도메인별 통합 테스트**를 포함합니다.
TestContainers MySQL을 사용하여 실제 데이터베이스 환경에서 전체 플로우를 검증합니다.

## 🏗️ 구조

```
integration/
├── config/                           # 공통 설정
│   └── IntegrationTestFixtures.kt    # 테스트 픽스처 (테스트 데이터 생성 헬퍼)
│
├── point/                            # ⭐ Point 도메인 (26 tests - COMPLETED)
│   ├── PointChargeIntegrationTest.kt          # 포인트 충전 (5 tests)
│   ├── PointUseIntegrationTest.kt             # 포인트 사용 (8 tests)
│   ├── PointExpireIntegrationTest.kt          # 포인트 소멸 (8 tests)
│   └── PointConcurrencyIntegrationTest.kt     # 동시성 테스트 (5 tests)
│
├── inventory/                        # ⭐ Inventory 도메인 (15 tests - COMPLETED)
│   ├── InventoryDeductIntegrationTest.kt      # 재고 차감 (9 tests)
│   └── InventoryConcurrencyIntegrationTest.kt # 동시성 테스트 (6 tests)
│
├── cart/                             # ⭐ Cart 도메인 (11 tests - COMPLETED)
│   └── CartAddIntegrationTest.kt              # 장바구니 추가/수정/삭제
│
├── coupon/                           # ⭐ Coupon 도메인 (동시성 1 test - COMPLETED)
│   └── CouponConcurrencyIntegrationTest.kt    # 선착순 쿠폰 발급 동시성
│
└── [order, payment, delivery, product, user, e2e]/  # 📝 Templates (TODO)
```

## ✅ 완료된 도메인 테스트

### 1. Point 도메인 (26 tests)

**PointChargeIntegrationTest** - 포인트 충전
- 정상 충전 + 이력 기록
- 연속 충전
- 최대 잔액(10,000,000원) 초과 검증
- 5% 적립 정책 검증

**PointUseIntegrationTest** - 포인트 사용
- 정상 사용 + 이력 기록
- 잔액 부족 검증
- 연속 사용
- 주문 연결 사용

**PointExpireIntegrationTest** - 포인트 소멸
- 정상 소멸 + 이력 기록
- FIFO 방식 소멸 (가장 오래된 포인트부터)

**PointConcurrencyIntegrationTest** - 동시성
- 동시 사용 정합성 (10 threads)
- 동시 적립 정합성 (20 threads)
- 높은 동시성 (100 threads)

---

### 2. Inventory 도메인 (15 tests)

**InventoryDeductIntegrationTest** - 재고 차감
- 정상 차감
- 재고 부족 검증
- 예약된 재고 고려한 가용 재고 차감
- 재고 보충 후 차감

**InventoryConcurrencyIntegrationTest** - 동시성
- **비관적 락** 기반 동시 차감 정합성
- 동시 예약 정합성
- 예약 확정 동시 처리

---

### 3. Cart 도메인 (11 tests)

**CartAddIntegrationTest** - 장바구니 CRUD
- 아이템 추가
- 여러 아이템 추가
- 동일 박스타입 덮어쓰기
- 선물 포장 + 메시지
- 차 구성 커스텀 박스
- 수량 업데이트
- 아이템 삭제
- 전체 비우기

---

### 4. Coupon 도메인 (1 test)

**CouponConcurrencyIntegrationTest** - 선착순 동시성
- 20명이 10개 한정 쿠폰 동시 발급 → 정확히 10개만 발급

---

## 🎯 비즈니스 정책 검증

각 테스트는 `/docs/api/business-policies.md`의 정책을 기반으로 작성되었습니다:

| 정책 | 검증 테스트 |
|------|-------------|
| 포인트 최대 잔액 (10,000,000원) | PointChargeIntegrationTest |
| 포인트 FIFO 소멸 | PointExpireIntegrationTest |
| 재고 동시 차감 정합성 | InventoryConcurrencyIntegrationTest |
| 선착순 쿠폰 수량 제한 | CouponConcurrencyIntegrationTest |
| 장바구니 박스타입 중복 방지 | CartAddIntegrationTest |

## 🧪 테스트 실행

### 전체 통합 테스트 실행
```bash
./gradlew test --tests "io.hhplus.ecommerce.integration.*"
```

### 도메인별 실행
```bash
# Point 도메인
./gradlew test --tests "io.hhplus.ecommerce.integration.point.*"

# Inventory 도메인
./gradlew test --tests "io.hhplus.ecommerce.integration.inventory.*"

# Cart 도메인
./gradlew test --tests "io.hhplus.ecommerce.integration.cart.*"

# Coupon 도메인
./gradlew test --tests "io.hhplus.ecommerce.integration.coupon.*"
```

### 특정 테스트 클래스 실행
```bash
./gradlew test --tests "io.hhplus.ecommerce.integration.point.PointConcurrencyIntegrationTest"
```

## 🔧 기술 스택

- **TestContainers**: MySQL 8.0.43 컨테이너 기반 테스트
- **Spring Boot Test**: @SpringBootTest, @Transactional
- **Kotest**: Kotlin 친화적 assertion
- **JUnit 5**: 테스트 프레임워크

## 📝 TODO: 나머지 도메인

다음 도메인의 통합 테스트가 템플릿으로 생성되어 있습니다:

- [ ] **Order** - 주문 생성, 취소, 주문번호 생성
- [ ] **Payment** - 결제 처리, PaymentHistory 기록, 포인트 연동
- [ ] **Delivery** - 배송 상태 전환, 배송지 변경 제한
- [ ] **Product** - 인기 상품 집계, 조회수 증가
- [ ] **User** - 이메일 중복, 전화번호 형식 검증
- [ ] **E2E** - 전체 주문 플로우 (Cart → Order → Payment → Delivery → Point 적립)

각 템플릿 파일에 TODO 주석으로 필요한 테스트 케이스가 명시되어 있습니다.

## 💡 작성 가이드

### 1. 템플릿 활용
```kotlin
// 기존 템플릿 파일에 TODO 주석이 포함되어 있습니다
// integration/order/OrderCreateIntegrationTest.kt 참고
```

### 2. Point/Inventory 참고
완성된 Point와 Inventory 테스트를 참고하여 패턴을 따라 작성하세요.

### 3. 비즈니스 정책 확인
`/docs/api/business-policies.md` 파일에서 해당 도메인의 정책을 확인하고 테스트 케이스를 작성하세요.

### 4. 동시성 테스트
동시성 제어가 필요한 도메인(Order, Coupon 등)은 별도 `*ConcurrencyIntegrationTest.kt` 파일로 분리하세요.

## 📊 현재 통합 테스트 커버리지

| 도메인 | 상태 | 테스트 수 | 비고 |
|--------|------|-----------|------|
| Point | ✅ 완료 | 26개 | 동시성 포함 |
| Inventory | ✅ 완료 | 15개 | 동시성 포함 |
| Cart | ✅ 완료 | 11개 | CRUD 전체 |
| Coupon | ✅ 일부 완료 | 1개 | 동시성만 |
| Order | 📝 템플릿 | 0개 | TODO |
| Payment | 📝 템플릿 | 0개 | TODO |
| Delivery | 📝 템플릿 | 0개 | TODO |
| Product | 📝 템플릿 | 0개 | TODO |
| User | 📝 템플릿 | 0개 | TODO |
| E2E | 📝 템플릿 | 0개 | TODO |

**총 완료: 53개 통합 테스트**

## 🚀 다음 단계

1. Order 도메인 통합 테스트 작성
2. Payment 도메인 통합 테스트 작성 (PaymentHistory 검증 포함)
3. E2E 통합 테스트 작성 (전체 주문 플로우)
4. 나머지 도메인 테스트 작성