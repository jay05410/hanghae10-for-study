# CLAUDE.md

Claude Code (claude.ai/code) 작업을 위한 프로젝트 가이드입니다.

## 📋 필수 참고 파일 (순서대로)
모든 상세 문서는 `.claude/docs/` 폴더에 위치합니다:

1. **DEVELOPMENT_GUIDE.md** - 개발 진행 순서 및 커맨드 사용법
2. **BUSINESS_POLICIES.md** - 구현해야 할 비즈니스 정책 정의
3. **PROJECT_GUIDELINES.md** - 과제 요구사항 및 평가 기준

## 시작하기
**첫 작업 시 반드시 확인**: `.claude/docs/DEVELOPMENT_GUIDE.md`의 페이즈별 진행 순서를 따라 개발하세요.

## 프로젝트 구조

**week01** 디렉토리에 Spring Boot Kotlin 애플리케이션이 있습니다. 프로젝트명은 `hhplus-tdd-jvm`이며 포인트 관리 시스템을 TDD로 구현합니다.

## 명령어

모든 명령어는 `week01` 디렉토리에서 실행:

### 개발 명령어
- **애플리케이션 실행**: `./gradlew bootRun`
- **프로젝트 빌드**: `./gradlew build`
- **테스트 실행**: `./gradlew test`
- **빌드 정리**: `./gradlew clean`

### 테스트 명령어
- **전체 테스트**: `./gradlew test`
- **커버리지 생성**: `./gradlew jacocoTestReport`

## 아키텍처

### 핵심 컴포넌트
- **애플리케이션**: `TddApplication.kt`
- **컨트롤러**: `PointController.kt` - TODO 주석이 있는 REST API
- **데이터 모델**:
  - `UserPoint.kt` - 사용자 포인트 데이터
  - `PointHistory.kt` - 거래 내역 (CHARGE/USE)
- **데이터베이스**:
  - `UserPointTable.kt` - 포인트 저장 (지연시간 포함)
  - `PointHistoryTable.kt` - 거래 내역 저장
- **예외 처리**: `ApiControllerAdvice.kt` - 글로벌 예외 핸들러

### 주요 구현 사항
- **패키지 구조**: `io.hhplus.tdd.*`
- **데이터베이스**: 인메모리 구현체 (100-300ms 랜덤 지연)
- **API 엔드포인트**:
  - `GET /point/{id}` - 포인트 조회
  - `GET /point/{id}/histories` - 내역 조회
  - `PATCH /point/{id}/charge` - 포인트 충전
  - `PATCH /point/{id}/use` - 포인트 사용
- **프레임워크**: Spring Boot 3.2.0 + Kotlin 1.9.21

### 개발 노트
- **TDD 중심**: 컨트롤러 메서드에 TODO 주석으로 구현할 기능 표시
- **동시성 테스트**: 데이터베이스 테이블에 의도적 지연시간 포함
- **제약사항**: 기존 데이터베이스 테이블 클래스 수정 금지

## TDD 과제 컨텍스트

### 과제 초점
- TDD 방법론 학습을 위한 과제
- 포인트 관리 시스템을 TDD 사이클로 개발
- 상세한 요구사항은 PROJECT_GUIDELINES.md 참조

### 핵심 구현 노트
- TODO 주석이 있는 기능들을 우선 구현
- 기존 database 테이블 클래스는 수정 금지
- Red-Green-Refactor 사이클 엄격 준수

## 코딩 컨벤션

### Kotlin 스타일
- **네이밍**: 함수/변수는 camelCase, 클래스는 PascalCase
- **문서화**: 모든 public 함수에 KDoc 주석 필수
- **예외 처리**: 비즈니스 예외는 custom exception 클래스 사용
- **검증**: 입력값 검증은 controller 레이어에서 수행

### 테스트 코드 기준
- **테스트 클래스**: `{ClassName}Test` 형식
- **테스트 메서드**: `should_{예상결과}_when_{조건}` 형식
- **테스트 데이터**: given-when-then 패턴 사용
- **Mock 사용**: 외부 의존성만 mock, 비즈니스 로직은 실제 객체

### 커밋 메시지 형식
```
<type>(<scope>): <description>

[optional body]
```
- Types: feat, fix, test, refactor, docs
- Scope: point, user, history, test
- 예시: `feat(point): implement charge validation with minimum amount`

## Custom Commands

### 사용 가능한 명령어
- `/tdd-cycle` - TDD 사이클 자동화 (Red-Green-Refactor)
- `/point-feature` - 포인트 기능 구현 자동화
- `/test-setup` - 테스트 데이터 및 환경 셋업
- `/create-pr` - PR 템플릿 기반 자동 PR 생성

### 워크플로우 통합
이 명령어들을 활용하여 TDD 개발 워크플로우를 자동화하고 일관성 있는 코드 품질을 유지하세요.