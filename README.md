# 항해플러스 10기 - 스터디용 repo

항해플러스 백엔드 10기 과제를 위한 프로젝트 저장소

## 📋 과제 목록

### Week01 - TDD 포인트 관리 시스템 ✅
- **주제**: Test-Driven Development를 활용한 포인트 충전/사용 시스템
- **기술 스택**: Spring Boot 3.2.0, Kotlin 1.9.21, Kotest, MockK
- **핵심 기능**: 포인트 조회/충전/사용/내역, 동시성 제어
- **상세 문서**: [week01/README.md](./week01/README.md)

#### 주요 성과
- ✅ **TDD 방법론 적용**: Red-Green-Refactor 사이클
- ✅ **동시성 제어**: 사용자별 락을 통한 Race Condition 해결
- ✅ **완벽한 테스트 구조**: 단위/통합/동시성 테스트 (24개 모두 통과)
- ✅ **아키텍처 설계**: SRP 기반 서비스 분리, Value Object 패턴

## 🛠 공통 기술 스택

### Backend
- **Language**: Kotlin 1.9.21
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle
- **JDK**: OpenJDK 17+

### Testing
- **Framework**: Kotest
- **Mocking**: MockK

### Development
- **IDE**: IntelliJ IDEA
- **VCS**: Git
- **Code Style**: Kotlin Coding Conventions

## 🚀 실행 방법

각 주차별 프로젝트는 독립적으로 실행할 수 있습니다.

```bash
# 특정 주차로 이동
cd week01

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew build
```

## 📁 프로젝트 구조

```
hanghae10-for-study/
├── week01/                     # TDD 포인트 관리 시스템
│   ├── src/main/kotlin/       # 메인 소스 코드
│   ├── src/test/kotlin/       # 테스트 코드
│   ├── build.gradle.kts       # 빌드 설정
│   └── README.md              # 주차별 상세 문서
├── week02/                     # (예정)
├── week03/                     # (예정)
├── .claude/                    # Claude Code 설정
│   └── docs/                   # 프로젝트 가이드 문서
├── CLAUDE.md                   # Claude Code 가이드
└── README.md                   # 전체 프로젝트 개요 (현재 파일)
```

## 📚 학습 목표

### 주요 학습 포인트
- **TDD 방법론**: 테스트 주도 개발을 통한 설계 개선
- **Clean Architecture**: 계층 분리와 의존성 관리
- **동시성 처리**: 멀티스레드 환경에서의 데이터 무결성
- **테스트 전략**: 단위/통합/동시성 테스트 구분
- **코드 품질**: SOLID 원칙, 디자인 패턴 적용

### 기술적 성장
- Kotlin 언어 숙련도 향상
- Spring Boot 생태계 이해
- 테스트 프레임워크 활용
- 동시성 제어 기법 학습

## 🎯 프로젝트 철학

> **"코드는 사람이 읽기 위해 작성되는 것이며, 기계가 실행하는 것은 부차적이다."**

각 프로젝트는 다음 원칙을 따릅니다:

1. **가독성**: 명확하고 이해하기 쉬운 코드
2. **유지보수성**: 변경에 유연한 구조
3. **테스트 가능성**: 검증 가능한 설계
4. **확장성**: 요구사항 변화에 대응
