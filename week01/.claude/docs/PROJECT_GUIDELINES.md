# 과제 상세 요구사항 가이드라인

## 과제 요구사항

### STEP1 - TDD 기본
- `/point` 패키지에 PointService 기본 기능 4개 구현:
  1. 포인트 조회 (`GET /point/{id}`)
  2. 포인트 충전 (`PATCH /point/{id}/charge`)
  3. 포인트 사용 (`PATCH /point/{id}/use`)
  4. 포인트 내역 조회 (`GET /point/{id}/histories`)
- 각 기능별 단위 테스트 작성
- 기존 database 테이블 클래스 수정 금지

### STEP2 - TDD 심화
- 포인트 충전/사용 정책 추가 (잔고 부족, 최대 잔고 등)
- 동일한 사용자에 대한 동시 요청이 정상적으로 처리될 수 있도록 개선
- 4가지 기능 통합 테스트 작성
- 동시성 제어 방식 분석 보고서 작성 (README.md)

### 필수 비즈니스 규칙
상세한 정책은 `BUSINESS_POLICIES.md` 참조
1. **최소 충전 금액**: 1,000원 이상
2. **사용 단위 제한**: 100원 단위만 가능
3. **일일 사용 한도**: 100,000원 까지

### Error Response 형식
```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "잔고가 부족합니다",
  "details": {
    "currentBalance": 5000,
    "requestAmount": 10000
  }
}
```

## 평가 기준

### STEP1 Pass/Fail
- [ ] 4가지 기능(포인트 조회 / 포인트 충전 / 포인트 사용 / 포인트 내역 조회) 구현 완료
- [ ] 각 기능들에 대해 단위 테스트 작성 완료
- [ ] 테스트 가능한 코드 구조
- [ ] AI 도구 활용하여 개발 프로세스 준수 여부

### STEP2 Pass/Fail
- [ ] 예외케이스 테스트 작성(정책 관련 실패 케이스)
- [ ] 각 기능별 통합테스트 작성
- [ ] 포인트 관련 기능에 대한 동시성 이슈 제어 및 검증하는 통합 테스트 작성 여부
- [ ] 동시성 제어 분석 보고서 필요

### 도전 항목
- Red-Green-Refactor 사이클 실천
- 테스트 가능한 코드 구조 설계 (의존성 주입, 인터페이스 활용)
- 모킹(Mocking)과 스텁(Stub) 활용의 적절성
- AI 프롬프트 엔지니어링을 통한 효율적인 테스트 코드 생성
- Custom Commands를 활용한 반복 작업 자동화
- 테스트 커버리지 80% 이상
- 동시성 제어 방식의 비교 분석
- TDD 방법론과 AI 활용 과정의 문서화

## Exception Classes
```kotlin
sealed class PointException(message: String) : RuntimeException(message) {
    class InsufficientBalance(currentBalance: Long, requestAmount: Long)
    class MinimumChargeAmount(amount: Long)
    class InvalidUseUnit(amount: Long)
    class DailyUseLimitExceeded(todayUsed: Long, requestAmount: Long)
}
```

## 요약

| 구분 | STEP1 | STEP2 |
|------|-------|-------|
| 핵심 | TDD 기본 | 통합테스트 & 동시성 |
| 결과물 | 코드 + 단위테스트 | 코드 + 통합테스트 + README |
| 커버리지 | 70% 이상 | 80% 이상 |