# 오늘한차 (今日漢茶) - 티 큐레이션 박스 이커머스

## 프로젝트 개요

**서비스명**: 오늘한차 (Today's Tea / DAYTEA)  
**도메인**: 일반차(허브티, 블렌딩티, 웰니스티) 큐레이션 박스 이커머스

### 핵심 가치
- **개인화**: 컨디션/기분/향 기반 맞춤 구성
- **트렌디**: MZ세대 감성의 브랜딩

## 프로젝트 구조
```
project-root/
├── docs/
│   ├── api/
│   │   ├── requirements.md          # 비즈니스 요구사항
│   │   ├── user-stories.md          # 사용자 스토리
│   │   ├── api-specification.md     # API 명세
│   │   └── data-models.md           # 데이터 모델
│   └── README.md                    # [현재 문서]
├── modules/                         
│   ├── api/                         # API 모듈 (추후 예정)
│   ├── domain/                      # 도메인 모듈 (추후 예정)
│   ├── infrastructure/              # 인프라 모듈 (추후 예정)
│   └── batch/                       # 배치 모듈 (추후 예정)
├── mock-server/
│   ├── db.json                      # 목업 데이터
│   ├── routes.json                  # 커스텀 라우팅 
│   ├── package.json                 # mock-server 설정
│   └── README.md                    # mock-server 사용안내
└── openapi.yaml
```

## 기술 스택 (예정)

### Backend
- **언어**: Kotlin 2.1.20
- **프레임워크**: Spring Boot (미정)
- **빌드 도구**: Gradle 

### Database
- **Primary DB**: PostgreSQL
- **Cache**: Redis
- **Message Queue**: Kafka (미정)

### 아키텍처 패턴
- Clean Architecture
- Hexagonal Architecture (Ports & Adapters)
- Event-Driven Architecture (Outbox Pattern)

## 핵심 기능 (MVP)

### 1. 상품 관리
- 박스 상품 조회 (기본/프리미엄)
- 선택 옵션 조회 (컨디션/기분/향)
- 조합별 재고 실시간 확인
- 인기 조합 통계 (최근 3일 TOP 5)

### 2. 재고 관리
- 조합별 재고 차감 (동시성 제어)
- 일일 생산 한도 관리
- 재고 복원 (주문 취소 시)

### 3. 장바구니
- 장바구니 추가/조회/삭제
- 여러 조합 담기
- 재고 검증

### 4. 주문/결제
- 주문 생성 및 재고 차감
- 포인트 기반 결제
- 쿠폰 할인 적용
- 주문 취소 및 환불

### 5. 쿠폰 시스템
- 선착순 쿠폰 발급 (한정 수량)
- 중복 발급 방지 (1인 1매)
- 쿠폰 유효성 검증

### 6. 외부 시스템 연동
- 제조사에 주문 정보 전송 (비동기)
- Outbox 패턴
- 재시도 메커니즘 (최대 3회)

## 비기능적 요구사항

### 성능
- 상품 조회: 평균 100ms 이하
- 재고 확인: 평균 200ms 이하
- 주문 처리: 평균 2초 이하

### 동시성
- 동시 주문 처리: 초당 100건
- 재고 정합성: 100% 보장
- 쿠폰 발급 정합성: 100% 보장

### 가용성
- 서비스 가동률: 99% 이상
- 외부 연동 장애 격리

## 동시성 제어 전략

### 재고 관리
- **비관적 락 (FOR UPDATE)**: 재고 차감 시
- **버전 관리**: 낙관적 락 보완
- **Redis 분산 락**: 데드락 방지

### 쿠폰 발급
- **Redis Lua Script**: 원자적 카운터 증가
- **DB 유니크 제약**: 중복 발급 방지

### 일일 생산 한도
- **Redis INCR**: 원자적 카운터

## 문서 가이드

### requirements.md
비즈니스 요구사항과 제약사항을 정의합니다.
- 스테이크홀더 요구사항
- 기능적/비기능적 요구사항
- 비즈니스 규칙

### user-stories.md
사용자 관점의 기능 명세와 시나리오입니다.
- 사용자 스토리
- 인수 테스트 시나리오
- 예외 케이스

### api-specification.md
개발팀을 위한 기술적 API 명세입니다.
- 엔드포인트 정의
- 요청/응답 구조
- 에러 코드

### data-models.md
비즈니스 로직을 반영한 데이터 모델입니다.
- ERD
- 엔티티 상세
- 관계 정의

## 참고 자료

- [과제 요구사항](./api/requirements.md)
- [사용자 스토리](./api/user-stories.md)
- [API 명세](./api/api-specification.md)
- [데이터 모델](./api/data-models.md)
