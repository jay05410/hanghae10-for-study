# Domain Refactoring Guide (임시 문서 - 모든 리팩터링 완료 후 삭제)

## 개요
기존 Application Service 중심 구조를 **헥사고날 + 클린 아키텍처**로 전환합니다.

### 핵심 변경사항
1. **Application Service** → **Domain Service** (도메인 내부로 이동)
2. **UseCase**가 트랜잭션, 분산락, 오케스트레이션 담당
3. **Repository** → **Port/Adapter** 패턴으로 명확히 분리
4. **도메인 계층은 인프라 어노테이션(@Transactional 등) 의존 없음**

---

## 리팩터링 완료 현황

| 도메인       | 상태 | 비고 |
|-----------|------|------|
| Payment   | ✅ 완료 | 참조 구현 |
| Point     | ✅ 완료 | |
| Order     | ✅ 완료 | |
| Product   | ✅ 완료 | |
| Coupon    | ✅ 완료 | CQRS: CouponCommandUseCase, GetCouponQueryUseCase |
| Cart      | ✅ 완료 | |
| Inventory | ✅ 완료 | CQRS: InventoryCommandUseCase, GetInventoryQueryUseCase, StockReservationCommandUseCase |
| Delivery  | ✅ 완료 | |
| User      | ✅ 완료 | |

---

## 목표 디렉터리 구조 (Payment 기준)

```
{domain}/
├── application/              # 애플리케이션 계층
│   ├── port/
│   │   └── out/             # 아웃바운드 포트 (인프라로 나가는 인터페이스)
│   │       └── {Domain}ExecutorPort.kt  # 필요시 (결제 실행기 등)
│   └── usecase/             # 유스케이스 (오케스트레이션, 트랜잭션, 분산락)
│       ├── Process{Domain}UseCase.kt
│       ├── Get{Domain}QueryUseCase.kt
│       └── ...
│
├── domain/                   # 도메인 계층 (순수 비즈니스 로직)
│   ├── constant/            # 상수, Enum
│   │   └── {Domain}Status.kt
│   ├── entity/              # 도메인 엔티티
│   │   └── {Domain}.kt
│   ├── model/               # Value Object, DTO
│   │   └── {Domain}Context.kt
│   ├── repository/          # Repository 인터페이스 (인바운드 포트)
│   │   └── {Domain}Repository.kt
│   ├── service/             # 도메인 서비스 (@Transactional 없음!)
│   │   └── {Domain}DomainService.kt
│   └── gateway/             # 외부 시스템 게이트웨이 인터페이스 (필요시)
│
├── exception/               # 도메인 예외
│   ├── {Domain}ErrorCode.kt
│   └── {Domain}Exception.kt
│
├── infra/                   # 인프라 계층
│   ├── executor/            # 포트 구현체 (결제 실행기 등)
│   │   └── Balance{Domain}Executor.kt
│   └── persistence/
│       ├── adapter/         # Repository 구현체 (어댑터)
│       │   └── {Domain}RepositoryImpl.kt
│       ├── entity/          # JPA 엔티티
│       │   └── {Domain}JpaEntity.kt
│       ├── mapper/          # 도메인 <-> JPA 매퍼
│       │   └── {Domain}Mapper.kt
│       └── repository/      # JPA Repository
│           └── {Domain}JpaRepository.kt
│
└── presentation/            # 프레젠테이션 계층
    ├── controller/
    │   └── {Domain}Controller.kt
    └── dto/
        ├── {Domain}Request.kt
        └── {Domain}Response.kt
```

---

## 계층별 책임

### 1. Application Layer (UseCase)
**위치**: `application/usecase/`

**책임**:
- 트랜잭션 경계 관리 (`@Transactional`, `@DistributedTransaction`)
- 분산락 적용 (`@DistributedLock`)
- 비즈니스 흐름 오케스트레이션
- 여러 도메인 서비스 협력 조정

**예시** (ProcessPaymentUseCase):
```kotlin
@Component
class ProcessPaymentUseCase(
    private val paymentDomainService: PaymentDomainService,
    executors: List<PaymentExecutorPort>
) {
    @DistributedLock(key = "...")
    @DistributedTransaction
    fun execute(request: ProcessPaymentRequest): Payment {
        // 1. 검증 (도메인 서비스)
        paymentDomainService.validateNoDuplicatePayment(request.orderId)

        // 2. 생성 (도메인 서비스)
        var payment = paymentDomainService.createPayment(...)

        // 3. 실행 (인프라 포트)
        val result = executor.execute(context)

        // 4. 결과 처리 (도메인 서비스)
        return paymentDomainService.handlePaymentResult(payment, result)
    }
}
```

### 2. Domain Layer (Service)
**위치**: `domain/service/`

**책임**:
- 순수 도메인 비즈니스 로직
- 도메인 불변식 보장
- 엔티티 생성 및 상태 전환
- Repository(Port)를 통한 영속성 위임

**주의사항**:
- `@Transactional` 사용 금지 (UseCase에서 관리)
- 외부 시스템 직접 연동 금지
- `@Component`만 사용

**예시** (PaymentDomainService):
```kotlin
@Component  // NOT @Service - 트랜잭션 없음을 명시
class PaymentDomainService(
    private val paymentRepository: PaymentRepository,
    private val snowflakeGenerator: SnowflakeGenerator
) {
    fun validateNoDuplicatePayment(orderId: Long) {
        val existing = paymentRepository.findByOrderId(orderId).firstOrNull()
        if (existing != null) {
            throw PaymentException.DuplicatePayment("...")
        }
    }

    fun createPayment(...): Payment {
        val payment = Payment.create(...)
        return paymentRepository.save(payment)
    }
}
```

### 3. Infrastructure Layer (Adapter)
**위치**: `infra/persistence/adapter/`

**책임**:
- Repository 인터페이스 구현
- 도메인 엔티티 ↔ JPA 엔티티 변환
- JPA Repository 위임

**예시** (PaymentRepositoryImpl):
```kotlin
@Repository
class PaymentRepositoryImpl(
    private val jpaRepository: PaymentJpaRepository,
    private val mapper: PaymentMapper
) : PaymentRepository {

    override fun save(payment: Payment): Payment =
        jpaRepository.save(payment.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): Payment? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)
}
```

### 4. Port 인터페이스
**Repository Port (domain/repository/)**:
```kotlin
interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Payment?
    fun findByOrderId(orderId: Long): List<Payment>
    // ...
}
```

**Executor Port (application/port/out/)** - 필요시:
```kotlin
interface PaymentExecutorPort {
    fun supportedMethod(): PaymentMethod
    fun canExecute(context: PaymentContext): Boolean
    fun execute(context: PaymentContext): PaymentResult
}
```

---

## 리팩터링 단계별 체크리스트

### Phase 1: 디렉터리 구조 생성
- [ ] `application/usecase/` 디렉터리 생성
- [ ] `application/port/out/` 디렉터리 생성 (필요시)
- [ ] `domain/service/` 디렉터리 생성
- [ ] `domain/model/` 디렉터리 생성 (필요시)
- [ ] `infra/persistence/adapter/` 디렉터리 생성
- [ ] `infra/persistence/mapper/` 디렉터리 생성
- [ ] `presentation/controller/` 디렉터리 생성
- [ ] `presentation/dto/` 디렉터리 생성

### Phase 2: Domain Service 생성
- [ ] 기존 Service에서 순수 도메인 로직 추출
- [ ] `{Domain}DomainService.kt` 생성
- [ ] `@Transactional` 어노테이션 제거
- [ ] `@Component`로 변경

### Phase 3: UseCase 리팩터링
- [ ] 기존 UseCase를 `application/usecase/`로 이동
- [ ] 트랜잭션/분산락 어노테이션 UseCase에 부여
- [ ] DomainService 의존성 주입으로 변경
- [ ] 오케스트레이션 로직만 남기기

### Phase 4: Repository/Adapter 분리
- [ ] `domain/repository/{Domain}Repository.kt` 인터페이스 확인/생성
- [ ] `infra/persistence/adapter/{Domain}RepositoryImpl.kt` 구현체 이동
- [ ] `infra/persistence/mapper/{Domain}Mapper.kt` 매퍼 이동/생성
- [ ] `infra/persistence/entity/{Domain}JpaEntity.kt` 확인

### Phase 5: Presentation 정리
- [ ] Controller를 `presentation/controller/`로 이동
- [ ] DTO를 `presentation/dto/`로 이동
- [ ] 응답 변환 로직 정리 (toResponse())

### Phase 6: 기존 파일 정리
- [ ] 기존 `application/` 디렉터리의 Service 삭제
- [ ] 기존 `infra/` 디렉터리의 중복 파일 삭제
- [ ] 기존 Controller/DTO 삭제

### Phase 7: 테스트 수정
- [ ] Unit Test import 경로 수정
- [ ] Integration Test import 경로 수정
- [ ] 테스트 컴파일 확인
- [ ] 테스트 실행 확인

---

## 네이밍 컨벤션

| 구분 | 기존 | 리팩터링 후 |
|------|------|------------|
| 도메인 서비스 | `PaymentService` | `PaymentDomainService` |
| 유스케이스 | `ProcessPaymentUseCase` | `ProcessPaymentUseCase` (동일) |
| Repository 인터페이스 | `PaymentRepository` | `PaymentRepository` (동일) |
| Repository 구현체 | `PaymentRepositoryImpl` | `PaymentRepositoryImpl` (동일) |
| JPA Repository | `PaymentJpaRepository` | `PaymentJpaRepository` (동일) |
| 매퍼 | - | `PaymentMapper` |
| JPA 엔티티 | `PaymentJpaEntity` | `PaymentJpaEntity` (동일) |

---

## 주의사항

### 1. 도메인 서비스의 의존성
도메인 서비스는 다음만 의존해야 함:
- Repository 인터페이스 (Port)
- 순수 유틸리티 (SnowflakeGenerator 등)
- 다른 도메인 서비스 (필요시)

### 2. 트랜잭션 경계
- UseCase에서만 `@Transactional` 사용
- DomainService에서는 절대 사용 금지
- 분산 트랜잭션 필요시 `@DistributedTransaction` 사용

### 3. 분산락 적용
- 동시성 제어가 필요한 UseCase에 `@DistributedLock` 적용
- Key는 `DistributedLockKeys`에 상수로 정의

### 4. 매퍼 패턴
```kotlin
// 확장 함수 스타일 권장
fun Payment.toEntity(mapper: PaymentMapper): PaymentJpaEntity = ...
fun PaymentJpaEntity?.toDomain(mapper: PaymentMapper): Payment? = ...
fun List<PaymentJpaEntity>.toDomain(mapper: PaymentMapper): List<Payment> = ...
```

### 5. 수정 후 코드
- 수정 후 기존 코드와의 호환성을 위해 `xxxLegacy`와 같은 형식으로 기존 코드(호환성을 위한 메서드 등) 남기는 것 금지
- 무조건 리팩터링 후에는 리팩터링 후의 코드를 기준으로 컴파일 및 테스트 수정
- 수정 후 테스트 컴파일 시 테스트 수행되지 않는다고 임의로 `.bak`, `.disabled` 등으로 변경하거나 주석 처리해서 통과 시키는 것 금지.
- 필요한 테스트는 수정된 서비스 및 애플리케이션 코드에 따라 무조건 수정 후 테스트를 통과해야 하며, 리팩토링 과정에서 서비스 로직이 잘못되어 변경한 경우 테스트 로직 변경이 필요한 경우에는 테스트 로직 변경도 진행되어야 함.

---

## Payment 도메인 참조 파일 목록

### Application Layer
- `applic뭊ation/usecase/ProcessPaymentUseCase.kt` - 결제 처리
- `application/usecase/GetPaymentQueryUseCase.kt` - 결제 조회
- `application/usecase/RefundPaymentUseCase.kt` - 환불 처리
- `application/port/out/PaymentExecutorPort.kt` - 결제 실행 포트

### Domain Layer
- `domain/service/PaymentDomainService.kt` - 도메인 서비스
- `domain/repository/PaymentRepository.kt` - Repository 포트
- `domain/entity/Payment.kt` - 도메인 엔티티
- `domain/model/PaymentContext.kt` - 컨텍스트 VO
- `domain/model/PaymentResult.kt` - 결과 VO

### Infrastructure Layer
- `infra/persistence/adapter/PaymentRepositoryImpl.kt` - 어댑터
- `infra/persistence/mapper/PaymentMapper.kt` - 매퍼
- `infra/persistence/entity/PaymentJpaEntity.kt` - JPA 엔티티
- `infra/persistence/repository/PaymentJpaRepository.kt` - JPA Repository
- `infra/executor/BalancePaymentExecutor.kt` - 포인트 결제 실행기

### Presentation Layer
- `presentation/controller/PaymentController.kt` - API 컨트롤러
- `presentation/dto/PaymentRequestDto.kt` - 요청 DTO
- `presentation/dto/PaymentResponse.kt` - 응답 DTO

---

*이 문서는 모든 도메인 리팩터링이 완료되면 삭제합니다.*