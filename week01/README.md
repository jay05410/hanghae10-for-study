# 포인트 관리 시스템

TDD 방법론으로 구현한 포인트 충전/사용 시스템입니다.

## 📋 주요 기능
- ✅ 포인트 조회
- ✅ 포인트 충전
- ✅ 포인트 사용
- ✅ 포인트 내역 조회
- ✅ 동시성 제어

## 🏗️ 아키텍처

### 패키지 구조
```
io.hhplus.tdd
├── point/
│   ├── PointController.kt          # REST API 엔드포인트
│   ├── service/
│   │   ├── PointQueryService.kt    # 조회 서비스
│   │   ├── PointChargeService.kt   # 충전 서비스
│   │   ├── PointUseService.kt      # 사용 서비스
│   │   ├── UserLockManager.kt      # 동시성 제어
│   │   └── PointTransactionLogger.kt # 내역 기록
│   ├── vo/                         # Value Objects
│   ├── validator/                  # 검증 로직
│   └── extension/                  # 확장 함수
├── database/                       # 인메모리 DB
└── common/                         # 공통 모듈
```

### 주요 설계 원칙
- **단일 책임 원칙**: 각 서비스는 하나의 책임만 가짐
- **의존성 주입**: Spring의 DI 컨테이너 활용
- **Value Object 패턴**: 도메인 로직 캡슐화
- **책임 분리**: 내역 기록을 별도 컴포넌트로 분리

## 🧪 테스트 전략

### 테스트 구조
```
src/test/kotlin/io/hhplus/tdd/point/
├── service/                        # 단위 테스트
│   ├── PointChargeServiceTest.kt   # 충전 서비스 단위 테스트
│   ├── PointUseServiceTest.kt      # 사용 서비스 단위 테스트
│   └── PointQueryServiceTest.kt    # 조회 서비스 단위 테스트
├── PointIntegrationTest.kt         # 통합 테스트
└── PointConcurrencyIntegrationTest.kt # 동시성 통합 테스트
```

### 테스트 커버리지
- **총 24개 테스트** 모두 통과
- **단위 테스트**: 12개 (Mock 기반)
- **통합 테스트**: 7개 (전체 플로우)
- **동시성 테스트**: 5개 (멀티스레드)

## ⚡ 동시성 제어 분석 보고서

### 1. 문제 정의

#### 동시성 문제 시나리오
```
사용자A: 10,000원 → +5,000원 충전
사용자B: 10,000원 → +3,000원 충전 (동시 실행)

Thread1: selectById() → 10,000원
Thread2: selectById() → 10,000원 (같은 값!)
Thread1: newPoint = 15,000원
Thread2: newPoint = 13,000원
Thread1: insertOrUpdate(15,000원)
Thread2: insertOrUpdate(13,000원) ← 마지막 쓰기가 승리! 5,000원 손실!
```

#### 발생 가능한 문제
- **Race Condition**: 동시 읽기-쓰기로 인한 데이터 손실
- **Lost Update**: 나중에 수행된 작업이 이전 작업을 덮어씀
- **Inconsistent Data**: 포인트와 내역 간 불일치

### 2. 해결 방안 비교 분석

| 방식 | 장점 | 단점 | 적용 시나리오 |
|------|------|------|---------------|
| **사용자별 락** ⭐ | • 높은 동시성<br>• 데드락 방지<br>• 확장성 좋음 | • 메모리 사용량<br>• 구현 복잡도 | **선택된 방식**<br>사용자별 독립적 처리 |
| **전역 락** | • 구현 간단<br>• 확실한 보장 | • 성능 병목<br>• 확장성 나쁨 | 단순한 시스템 |
| **낙관적 락** | • 높은 동시성<br>• 충돌 시에만 재시도 | • 재시도 로직 필요<br>• 버전 관리 | 충돌이 적은 환경 |
| **비관적 락** | • 확실한 보장<br>• 로직 단순 | • 성능 저하<br>• 데드락 위험 | 충돌이 많은 환경 |

### 3. 구현된 동시성 제어

#### UserLockManager 설계
```kotlin
@Component
class UserLockManager {
    private val userLocks = ConcurrentHashMap<Long, ReentrantLock>()

    fun <T> executeWithLock(userId: Long, action: () -> T): T {
        val lock = getLock(userId)
        lock.lock()
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }
}
```

#### 핵심 특징
- **사용자별 독립 락**: 다른 사용자 간 블로킹 없음
- **ConcurrentHashMap**: Thread-Safe한 락 저장소
- **ReentrantLock**: 재진입 가능한 락
- **Template Method**: 일관된 락 관리

### 4. 동시성 테스트 검증

#### 테스트 시나리오 및 결과

**1) 대량 동시 충전 테스트**
```kotlin
// 100개 스레드가 동시에 1,000원씩 충전
val futures = (1..100).map {
    CompletableFuture.supplyAsync {
        chargeService.charge(userId, 1000L)
    }
}
```
- **결과**: 정확히 100,000원 ✅
- **내역**: 100건 모두 기록 ✅

**2) 사용자별 독립성 테스트**
```kotlin
// 사용자1: 50번 충전, 사용자2: 30번 충전 동시 실행
```
- **결과**: 사용자1=50,000원, 사용자2=30,000원 ✅
- **독립성**: 사용자 간 간섭 없음 ✅

**3) 충전/사용 혼합 테스트**
```kotlin
// 30개 충전(+1000원) + 20개 사용(-500원) 동시 실행
```
- **결과**: 50,000 + 30,000 - 10,000 = 70,000원 ✅
- **정확성**: 모든 연산 정확히 반영 ✅

### 5. 성능 분석

#### 처리량 측정
- **동시 요청**: 100개 스레드
- **평균 처리 시간**: ~200ms (DB 지연 포함)
- **성공률**: 100% (데이터 손실 없음)

#### 확장성 고려사항
- **메모리 사용**: 사용자당 하나의 락 객체
- **가비지 컬렉션**: 오래된 락 정리 메커니즘 고려 필요
- **분산 환경**: Redis 기반 분산 락으로 확장 가능

### 6. 결론 및 개선 방향

#### 현재 구현의 장점
✅ **정확성**: 모든 동시성 테스트 통과
✅ **성능**: 사용자별 독립적 처리로 높은 동시성
✅ **확장성**: 새로운 사용자 추가 시 자동 락 생성
✅ **안정성**: 데드락 위험 최소화

#### 향후 개선 방안
- **락 메모리 관리**: LRU 기반 락 정리 메커니즘
- **분산 락**: Redis/Zookeeper 기반 분산 환경 지원
- **모니터링**: 락 경합 상황 모니터링 및 알림
- **성능 최적화**: 락 범위 최소화 및 비동기 처리

## 🚀 실행 방법

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 단위 테스트만
./gradlew test --tests "*Service*Test"

# 통합 테스트만
./gradlew test --tests "*Integration*Test"

# 동시성 테스트만
./gradlew test --tests "*Concurrency*Test"
```

### API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/point/{id}` | 포인트 조회 |
| GET | `/point/{id}/histories` | 내역 조회 |
| PATCH | `/point/{id}/charge` | 포인트 충전 |
| PATCH | `/point/{id}/use` | 포인트 사용 |

## 📊 비즈니스 정책

### 충전 정책
- **최소 금액**: 1,000원
- **최대 금액**: 1,000,000원
- **충전 단위**: 100원 단위

### 사용 정책
- **최소 금액**: 100원
- **사용 단위**: 100원 단위
- **일일 한도**: 100,000원
- **잔고 검증**: 사용 금액 ≤ 현재 잔고

### 내역 정책
- **정렬**: 최신순 (timeMillis 내림차순)
- **최대 조회**: 100건
- **타입**: CHARGE, USE

## 🛠️ 기술 스택

- **언어**: Kotlin 1.9.21
- **프레임워크**: Spring Boot 3.2.0
- **테스트**: Kotest 5.8.0
- **모킹**: MockK 1.13.8
- **빌드**: Gradle 8.5
- **JDK**: OpenJDK 21

## 📝 개발 과정

이 프로젝트는 **TDD(Test-Driven Development)** 방법론을 따라 개발되었습니다:

1. **Red**: 실패하는 테스트 작성
2. **Green**: 테스트를 통과하는 최소 코드 구현
3. **Refactor**: 코드 품질 개선 및 리팩토링

### 주요 리팩토링 내용
- 책임 분리: `PointTransactionLogger` 추출
- 동시성 제어: `UserLockManager` 도입
- 테스트 구조: 단위/통합/동시성 테스트 분리