# 비즈니스 정책 정의서

## 필수 비즈니스 정책

### 1. 포인트 충전 정책

#### 1.1 최소 충전 금액 제한
- **정책**: 포인트 충전은 1,000원 이상만 가능
- **규칙**:
  - 충전 요청 금액 < 1,000원 → `MinimumChargeAmount` 예외 발생
  - 충전 요청 금액 ≥ 1,000원 → 정상 처리
- **예외 메시지**: "최소 충전 금액은 1,000원입니다. 요청금액: {amount}"

#### 1.2 최대 충전 금액 제한
- **정책**: 1회 최대 충전 금액은 1,000,000원
- **규칙**:
  - 충전 요청 금액 > 1,000,000원 → `MaximumChargeAmount` 예외 발생
  - 충전 요청 금액 ≤ 1,000,000원 → 정상 처리
- **예외 메시지**: "최대 충전 금액은 1,000,000원입니다. 요청금액: {amount}"

#### 1.3 충전 단위 제한
- **정책**: 포인트 충전은 100원 단위만 가능
- **규칙**:
  - 충전 요청 금액 % 100 ≠ 0 → `InvalidChargeUnit` 예외 발생
  - 충전 요청 금액 % 100 = 0 → 정상 처리
- **예외 메시지**: "포인트 충전은 100원 단위로만 가능합니다. 요청금액: {amount}"

### 2. 포인트 사용 정책

#### 2.1 잔고 부족 검증 (기본 요구사항)
- **정책**: 현재 잔고보다 많은 포인트 사용 불가
- **규칙**:
  - 사용 요청 금액 > 현재 잔고 → `InsufficientBalance` 예외 발생
  - 사용 요청 금액 ≤ 현재 잔고 → 정상 처리
- **예외 메시지**: "잔고가 부족합니다. 현재잔고: {currentBalance}, 요청금액: {requestAmount}"

#### 2.2 사용 단위 제한
- **정책**: 포인트 사용은 100원 단위만 가능
- **규칙**:
  - 사용 요청 금액 % 100 ≠ 0 → `InvalidUseUnit` 예외 발생
  - 사용 요청 금액 % 100 = 0 → 정상 처리
- **예외 메시지**: "포인트 사용은 100원 단위로만 가능합니다. 요청금액: {amount}"

#### 2.3 일일 사용 한도 제한
- **정책**: 하루 최대 100,000원까지만 사용 가능
- **규칙**:
  - 오늘 사용 누적 금액 + 사용 요청 금액 > 100,000원 → `DailyUseLimitExceeded` 예외 발생
  - 오늘 사용 누적 금액 + 사용 요청 금액 ≤ 100,000원 → 정상 처리
- **예외 메시지**: "일일 사용 한도를 초과했습니다. 오늘사용: {todayUsed}, 요청금액: {requestAmount}"

#### 2.4 최소 사용 금액 제한
- **정책**: 포인트 사용은 100원 이상만 가능
- **규칙**:
  - 사용 요청 금액 < 100원 → `MinimumUseAmount` 예외 발생
  - 사용 요청 금액 ≥ 100원 → 정상 처리
- **예외 메시지**: "최소 사용 금액은 100원입니다. 요청금액: {amount}"

### 3. 포인트 조회 정책

#### 3.1 사용자 존재 검증
- **정책**: 존재하지 않는 사용자의 포인트 조회시 기본값 반환
- **규칙**:
  - 사용자 ID가 존재하지 않을 경우 → 0포인트로 새 UserPoint 생성
  - 사용자 ID가 존재할 경우 → 실제 포인트 정보 반환

### 4. 포인트 내역 조회 정책

#### 4.1 내역 제한
- **정책**: 최근 100건까지만 조회 가능
- **규칙**:
  - 전체 내역 중 최신 100건만 반환
  - 시간 역순 정렬 (최신순)

#### 4.2 사용자별 내역 격리
- **정책**: 각 사용자는 본인의 내역만 조회 가능
- **규칙**:
  - 요청한 사용자 ID와 일치하는 내역만 반환

## Exception Classes 구조

```kotlin
sealed class PointException(message: String) : RuntimeException(message) {
    // 기본 요구사항
    class InsufficientBalance(currentBalance: Long, requestAmount: Long) :
        PointException("잔고가 부족합니다. 현재잔고: $currentBalance, 요청금액: $requestAmount")

    // 충전 정책
    class MinimumChargeAmount(amount: Long) :
        PointException("최소 충전 금액은 1,000원입니다. 요청금액: $amount")

    class MaximumChargeAmount(amount: Long) :
        PointException("최대 충전 금액은 1,000,000원입니다. 요청금액: $amount")

    class InvalidChargeUnit(amount: Long) :
        PointException("포인트 충전은 100원 단위로만 가능합니다. 요청금액: $amount")

    // 사용 정책
    class InvalidUseUnit(amount: Long) :
        PointException("포인트 사용은 100원 단위로만 가능합니다. 요청금액: $amount")

    class DailyUseLimitExceeded(todayUsed: Long, requestAmount: Long) :
        PointException("일일 사용 한도를 초과했습니다. 오늘사용: $todayUsed, 요청금액: $requestAmount")

    class MinimumUseAmount(amount: Long) :
        PointException("최소 사용 금액은 100원입니다. 요청금액: $amount")
}
```

## 정책 검증 테스트 케이스

### 충전 정책 테스트
- [ ] 999원 충전 시도 → MinimumChargeAmount 예외
- [ ] 1,000원 충전 시도 → 정상 처리
- [ ] 1,000,001원 충전 시도 → MaximumChargeAmount 예외
- [ ] 1,050원 충전 시도 → InvalidChargeUnit 예외
- [ ] 1,100원 충전 시도 → 정상 처리

### 사용 정책 테스트
- [ ] 잔고 5,000원에서 6,000원 사용 시도 → InsufficientBalance 예외
- [ ] 1,050원 사용 시도 → InvalidUseUnit 예외
- [ ] 일일 사용 한도 초과 시도 → DailyUseLimitExceeded 예외
- [ ] 50원 사용 시도 → MinimumUseAmount 예외
- [ ] 정상 범위 사용 시도 → 정상 처리

### 동시성 테스트
- [ ] 동일 사용자 동시 충전 요청 처리
- [ ] 동일 사용자 동시 사용 요청 처리 (잔고 부족 상황)
- [ ] 동일 사용자 동시 조회 요청 처리

## 구현 참고사항

### 일일 사용 한도 추적 방법
- 메모리 기반 일일 사용 금액 추적기 구현 필요
- 사용자별 + 날짜별 사용 금액 관리
- PointHistory에서 당일 USE 타입 거래 합계 계산

### 정책 검증 순서
1. 입력값 기본 검증 (null, 음수 등)
2. 단위 검증 (100원 단위)
3. 금액 범위 검증 (최소/최대)
4. 비즈니스 규칙 검증 (잔고, 일일 한도 등)
5. 실제 처리 수행