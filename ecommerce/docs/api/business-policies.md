# 이커머스 비즈니스 정책 정의서

## 필수 비즈니스 정책

### 1. 장바구니 관리 정책

#### 1.1 최대 장바구니 항목 제한
- **정책**: 사용자당 최대 10개의 박스까지만 장바구니에 담기 가능
- **규칙**:
  - 장바구니 항목 수 >= 10개 → `MaxCartItemsExceeded` 예외 발생
  - 장바구니 항목 수 < 10개 → 정상 처리
- **예외 메시지**: "장바구니는 최대 10개까지 담을 수 있습니다. 현재 항목: {currentCount}"

#### 1.2 동일 박스타입 중복 방지
- **정책**: 동일한 박스타입의 커스텀 박스는 장바구니에 1개만 허용
- **규칙**:
  - 동일 박스타입 존재 → `DuplicateBoxTypeInCart` 예외 발생
  - 동일 박스타입 없음 → 정상 처리
- **예외 메시지**: "동일한 박스타입이 이미 장바구니에 있습니다. 박스타입: {boxTypeName}"

#### 1.3 커스텀 박스 구성 검증
- **정책**: 박스타입의 일수와 선택한 차 개수가 일치해야 함
- **규칙**:
  - 박스 일수 != 선택한 차 개수 → `TeaCountMismatch` 예외 발생
  - 박스 일수 = 선택한 차 개수 → 정상 처리
- **예외 메시지**: "차 선택 개수가 올바르지 않습니다. 박스일수: {boxDays}, 선택개수: {selectedCount}"

### 2. 재고 관리 정책

#### 2.1 재고 부족 검증
- **정책**: 주문 가능한 재고량보다 많은 수량 주문 불가
- **규칙**:
  - 요청 수량 > 가용 재고 → `InsufficientStock` 예외 발생
  - 요청 수량 <= 가용 재고 → 정상 처리
- **예외 메시지**: "재고가 부족합니다. 아이템: {itemName}, 가용재고: {availableStock}, 요청수량: {requestedQuantity}"

#### 2.2 일일 생산 한도 제한
- **정책**: 박스타입별로 일일 생산 한도 설정
- **규칙**:
  - 일일 주문량 + 요청량 > 일일 한도 → `DailyProductionLimitExceeded` 예외 발생
  - 일일 주문량 + 요청량 <= 일일 한도 → 정상 처리
- **예외 메시지**: "일일 생산 한도를 초과했습니다. 박스타입: {boxTypeName}, 한도: {dailyLimit}, 오늘주문: {todayOrdered}"

### 3. 쿠폰 관리 정책

#### 3.1 선착순 쿠폰 수량 제한
- **정책**: 쿠폰별로 설정된 최대 발급 수량 초과 불가
- **규칙**:
  - 발급된 수량 >= 최대 발급 수량 → `CouponSoldOut` 예외 발생
  - 발급된 수량 < 최대 발급 수량 → 정상 처리
- **예외 메시지**: "쿠폰이 모두 소진되었습니다. 쿠폰: {couponName}"

#### 3.2 중복 발급 방지
- **정책**: 동일 사용자에게 동일 쿠폰 중복 발급 불가
- **규칙**:
  - 이미 발급받은 쿠폰 → `DuplicateCouponIssue` 예외 발생
  - 미발급 쿠폰 → 정상 처리
- **예외 메시지**: "이미 발급받은 쿠폰입니다. 쿠폰: {couponName}"

#### 3.3 쿠폰 유효성 검증
- **정책**: 만료된 쿠폰 또는 사용된 쿠폰 사용 불가
- **규칙**:
  - 만료일 < 현재일시 → `ExpiredCoupon` 예외 발생
  - 이미 사용된 쿠폰 → `AlreadyUsedCoupon` 예외 발생
  - 유효한 쿠폰 → 정상 처리
- **예외 메시지**:
  - 만료: "만료된 쿠폰입니다. 만료일: {expiredDate}"
  - 사용완료: "이미 사용된 쿠폰입니다. 사용일: {usedDate}"

#### 3.4 최소 주문 금액 검증
- **정책**: 쿠폰별로 설정된 최소 주문 금액 충족 필요
- **규칙**:
  - 주문 금액 < 최소 주문 금액 → `MinimumOrderAmountNotMet` 예외 발생
  - 주문 금액 >= 최소 주문 금액 → 정상 처리
- **예외 메시지**: "최소 주문 금액을 충족하지 못했습니다. 최소금액: {minAmount}, 주문금액: {orderAmount}"

### 4. 포인트 관리 정책

#### 4.1 포인트 적립 정책
- **정책**: 상품 구매 시 자동으로 일정 비율 적립
- **규칙**:
  - 기본 적립률: 구매 금액의 5%
  - 최소 적립 금액: 1원
  - 최대 누적 가능 잔액: 10,000,000원 (천만원)
- **예외 메시지**:
  - 최대 잔액 초과: "잔액은 10,000,000원을 초과할 수 없습니다: {balance}"

#### 4.2 포인트 사용 정책
- **정책**: 보유한 포인트로 다음 구매 시 할인 적용
- **규칙**:
  - 사용 금액 > 현재 잔액 → `InsufficientBalance` 예외 발생
  - 사용 금액 <= 현재 잔액 → 정상 처리
  - 최소 사용 단위: 100원
- **예외 메시지**: "잔고가 부족합니다. 현재 잔고: {currentBalance}, 사용 시도 금액: {useAmount}"

#### 4.3 포인트 소멸 정책
- **정책**: 적립일로부터 일정 기간 후 자동 소멸
- **규칙**:
  - 유효기간: 적립일로부터 1년
  - 소멸 예정 포인트 알림: 소멸 30일 전
  - 선입선출(FIFO): 가장 먼저 적립된 포인트부터 사용/소멸
- **예외 메시지**: "소멸 가능한 포인트가 부족합니다. 현재 잔고: {currentBalance}, 소멸 시도 금액: {expireAmount}"

### 5. 주문 관리 정책

#### 5.1 빈 장바구니 주문 방지
- **정책**: 장바구니가 비어있는 상태에서 주문 생성 불가
- **규칙**:
  - 장바구니 항목 수 = 0 → `EmptyCart` 예외 발생
  - 장바구니 항목 수 > 0 → 정상 처리
- **예외 메시지**: "장바구니가 비어있습니다. 상품을 추가한 후 주문해주세요."

#### 5.2 주문 취소 제한
- **정책**: 제조 시작 후에는 주문 취소 불가
- **규칙**:
  - 주문 상태 = PREPARING, SHIPPED, DELIVERED → `OrderCancellationNotAllowed` 예외 발생
  - 주문 상태 = PENDING, PAID → 정상 처리
- **예외 메시지**: "취소할 수 없는 주문 상태입니다. 현재상태: {currentStatus}"

#### 5.3 주문 번호 형식
- **정책**: 주문 번호는 ORD-YYYYMMDD-XXX 형식으로 생성
- **규칙**:
  - ORD-{날짜}-{일련번호} 형식으로 자동 생성
  - 일련번호는 일별로 001부터 시작

### 6. 동시성 제어 정책

#### 6.1 재고 차감 동시성
- **정책**: 동시에 같은 아이템을 주문할 때 재고 정합성 보장
- **규칙**:
  - Redis 분산 락 또는 DB 비관적 락 사용
  - 락 타임아웃: 3초
- **예외 메시지**: "재고 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."

#### 6.2 쿠폰 발급 동시성
- **정책**: 선착순 쿠폰 발급 시 정확한 수량 보장
- **규칙**:
  - Redis Lua Script 또는 DB 유니크 제약조건 활용
  - 중복 요청 방지를 위한 요청 제한
- **예외 메시지**: "중복된 요청입니다. 잠시 후 다시 시도해주세요."

### 7. 배송지 관리 정책

#### 7.1 배송지 개수 제한
- **정책**: 사용자당 최대 10개의 배송지까지 저장 가능
- **규칙**:
  - 배송지 개수 >= 10개 → `MaxDeliveryAddressExceeded` 예외 발생
  - 배송지 개수 < 10개 → 정상 처리
- **예외 메시지**: "배송지는 최대 10개까지 저장할 수 있습니다. 현재 개수: {currentCount}"

#### 7.2 기본 배송지 설정
- **정책**: 사용자는 반드시 하나의 기본 배송지를 가져야 함
- **규칙**:
  - 첫 번째 배송지는 자동으로 기본 배송지로 설정
  - 기본 배송지 변경 시 이전 기본 배송지는 자동으로 해제
  - 기본 배송지 삭제 시 다른 배송지가 있으면 그 중 하나를 기본으로 설정

### 8. 데이터 유효성 정책

#### 8.1 전화번호 형식 검증
- **정책**: 한국 휴대폰 번호 형식만 허용
- **규칙**:
  - 형식: 010-XXXX-XXXX 또는 01X-XXXX-XXXX
  - 유효하지 않은 형식 → `InvalidPhoneFormat` 예외 발생
- **예외 메시지**: "올바른 휴대폰 번호 형식이 아닙니다. 입력값: {phoneNumber}"

#### 8.2 이메일 중복 검증
- **정책**: 동일한 이메일로 중복 가입 불가
- **규칙**:
  - 기존 이메일 존재 → `DuplicateEmail` 예외 발생
  - 신규 이메일 → 정상 처리
- **예외 메시지**: "이미 사용 중인 이메일입니다. 이메일: {email}"

### 9. 외부 시스템 연동 정책

#### 9.1 Outbox 패턴 적용
- **정책**: 외부 제조사 API 연동 시 트랜잭션 보장
- **규칙**:
  - 주문 생성과 동시에 outbox 이벤트 저장
  - 외부 API 호출 실패 시에도 주문은 성공 처리
  - 최대 3회 재시도 후 실패 시 관리자 알림

#### 9.2 외부 시스템 장애 격리
- **정책**: 외부 시스템 장애가 주문 프로세스에 영향을 주지 않음
- **규칙**:
  - Circuit Breaker 패턴 적용
  - 타임아웃: 5초
  - 실패율 임계값: 50%

## Exception Classes 구조

```kotlin
sealed class ECommerceException(message: String) : RuntimeException(message) {

    // 장바구니 관련
    class MaxCartItemsExceeded(currentCount: Int) :
        ECommerceException("장바구니는 최대 10개까지 담을 수 있습니다. 현재 항목: $currentCount")

    class DuplicateBoxTypeInCart(boxTypeName: String) :
        ECommerceException("동일한 박스타입이 이미 장바구니에 있습니다. 박스타입: $boxTypeName")

    class TeaCountMismatch(boxDays: Int, selectedCount: Int) :
        ECommerceException("차 선택 개수가 올바르지 않습니다. 박스일수: $boxDays, 선택개수: $selectedCount")

    // 재고 관련
    class InsufficientStock(itemName: String, availableStock: Int, requestedQuantity: Int) :
        ECommerceException("재고가 부족합니다. 아이템: $itemName, 가용재고: $availableStock, 요청수량: $requestedQuantity")

    class DailyProductionLimitExceeded(boxTypeName: String, dailyLimit: Int, todayOrdered: Int) :
        ECommerceException("일일 생산 한도를 초과했습니다. 박스타입: $boxTypeName, 한도: $dailyLimit, 오늘주문: $todayOrdered")

    // 쿠폰 관련
    class CouponSoldOut(couponName: String) :
        ECommerceException("쿠폰이 모두 소진되었습니다. 쿠폰: $couponName")

    class DuplicateCouponIssue(couponName: String) :
        ECommerceException("이미 발급받은 쿠폰입니다. 쿠폰: $couponName")

    class ExpiredCoupon(expiredDate: String) :
        ECommerceException("만료된 쿠폰입니다. 만료일: $expiredDate")

    class AlreadyUsedCoupon(usedDate: String) :
        ECommerceException("이미 사용된 쿠폰입니다. 사용일: $usedDate")

    class MinimumOrderAmountNotMet(minAmount: Long, orderAmount: Long) :
        ECommerceException("최소 주문 금액을 충족하지 못했습니다. 최소금액: $minAmount, 주문금액: $orderAmount")

    // 결제 관련
    class InsufficientBalance(currentBalance: Long, paymentAmount: Long) :
        ECommerceException("잔액이 부족합니다. 현재잔액: $currentBalance, 결제금액: $paymentAmount")

    class MinimumChargeAmount(amount: Long) :
        ECommerceException("최소 충전 금액은 1,000원입니다. 요청금액: $amount")

    class MaximumChargeAmount(amount: Long) :
        ECommerceException("최대 충전 금액은 100,000원입니다. 요청금액: $amount")

    class InvalidChargeUnit(amount: Long) :
        ECommerceException("포인트 충전은 100원 단위로만 가능합니다. 요청금액: $amount")

    // 주문 관련
    class EmptyCart :
        ECommerceException("장바구니가 비어있습니다. 상품을 추가한 후 주문해주세요.")

    class OrderCancellationNotAllowed(currentStatus: String) :
        ECommerceException("취소할 수 없는 주문 상태입니다. 현재상태: $currentStatus")

    // 동시성 관련
    class ConcurrencyException(operation: String) :
        ECommerceException("$operation 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")

    class DuplicateRequestException :
        ECommerceException("중복된 요청입니다. 잠시 후 다시 시도해주세요.")
}
```

## 정책 검증 테스트 케이스

### 장바구니 정책 테스트
- [ ] 10개 초과 장바구니 추가 시도 → MaxCartItemsExceeded 예외
- [ ] 동일 박스타입 중복 추가 시도 → DuplicateBoxTypeInCart 예외
- [ ] 박스 일수와 차 개수 불일치 → TeaCountMismatch 예외

### 재고 정책 테스트
- [ ] 재고 부족 상황에서 주문 시도 → InsufficientStock 예외
- [ ] 일일 생산 한도 초과 시도 → DailyProductionLimitExceeded 예외

### 쿠폰 정책 테스트
- [ ] 소진된 쿠폰 발급 시도 → CouponSoldOut 예외
- [ ] 중복 쿠폰 발급 시도 → DuplicateCouponIssue 예외
- [ ] 만료된 쿠폰 사용 시도 → ExpiredCoupon 예외
- [ ] 최소 주문 금액 미달 시 쿠폰 사용 → MinimumOrderAmountNotMet 예외

### 결제 정책 테스트
- [ ] 잔액 부족 상황에서 결제 시도 → InsufficientBalance 예외
- [ ] 잘못된 충전 금액 시도 → 각종 충전 관련 예외

### 주문 정책 테스트
- [ ] 빈 장바구니로 주문 시도 → EmptyCart 예외
- [ ] 제조 중인 주문 취소 시도 → OrderCancellationNotAllowed 예외

### 동시성 테스트
- [ ] 동시 재고 차감 처리 (정합성 보장)
- [ ] 선착순 쿠폰 동시 발급 (정확한 수량 보장)

## 구현 참고사항

### 정책 검증 순서
1. 입력값 기본 검증 (null, 음수, 형식 등)
2. 비즈니스 규칙 검증 (재고, 중복, 한도 등)
3. 동시성 제어 (락 획득)
4. 실제 비즈니스 로직 수행
5. 외부 시스템 연동 (비동기)

### 에러 응답 구조
```json
{
  "success": false,
  "error": {
    "code": "CART003",
    "message": "장바구니는 최대 10개까지 담을 수 있습니다. 현재 항목: 10",
    "details": {
      "currentCount": 10,
      "maxCount": 10
    }
  },
  "timestamp": "2025-10-31T14:30:00Z"
}
```

### 모니터링 지표
- 정책 위반 발생 빈도
- 동시성 제어 락 대기 시간
- 외부 시스템 연동 성공률
- 재고 부족으로 인한 주문 실패율