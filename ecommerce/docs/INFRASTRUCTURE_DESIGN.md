# 인프라 설계 문서

## 개요

AWS 인프라 설계를 정리한 문서입니다.

---

## 설계 목표

1. **최소 비용**: 학습/테스트 목적에 맞는 비용 최적화
2. **서비스 분리**: 각 컴포넌트별 독립적인 리소스 할당
3. **정확한 성능 측정**: 부하 테스트 시 병목 지점 명확히 파악
4. **학습 가치**: 직접 운영 경험을 통한 인프라 이해

---

## 최종 인프라 구성

```mermaid
flowchart TB
    subgraph VPC["AWS VPC"]
        subgraph Public["Public Subnet"]
            APP["🖥️ App Server<br/>t4g.micro (On-Demand)<br/>Spring Boot"]
            REDIS["📦 Redis Server<br/>t4g.micro (Spot)<br/>Redis"]
            KAFKA["📨 Kafka Server<br/>t4g.small (Spot)<br/>Kafka"]
        end

        subgraph Private["Private Subnet"]
            RDS["🗄️ MySQL<br/>db.t4g.micro (Free Tier)<br/>RDS"]
        end
    end

    USER["👤 User"] --> APP
    APP --> REDIS
    APP --> RDS
    APP --> KAFKA
```

> **보안 참고**: Redis/Kafka가 Public Subnet에 있지만, Security Group에서 App SG로부터만 접근을 허용합니다. 인터넷에서 직접 접근은 차단되어 있습니다. Private Subnet 사용 시 NAT Gateway 비용(~$32/월)이 발생하므로, 학습/테스트 목적에서는 현재 구성이 비용 효율적입니다.

---

## 비용 구조

```mermaid
pie title 월별 예상 비용 (테스트 시)
    "App EC2 (On-Demand)" : 6
    "Redis EC2 (Spot)" : 2
    "Kafka EC2 (Spot)" : 4
    "RDS MySQL (Free Tier)" : 0
```

| 서비스 | 인스턴스 | 타입 | 월 비용 |
|-------|---------|------|--------|
| App | t4g.micro (2 vCPU, 1GB) | On-Demand | ~$6 |
| Redis | t4g.micro (2 vCPU, 1GB) | Spot | ~$2 |
| MySQL | db.t4g.micro (2 vCPU, 1GB) | RDS Free Tier | 무료 |
| Kafka | t4g.small (2 vCPU, 2GB) | Spot | ~$4 |

### 상황별 비용

| 상황 | 구성 | 월 비용 |
|-----|------|--------|
| 평상시 | App + Redis + RDS | ~$8 |
| 부하 테스트 | App + Redis + RDS + Kafka | ~$12 |

---

## 의사결정 과정

### 1. 서비스 분리 vs 통합

```mermaid
flowchart LR
    subgraph Before["❌ 단일 서버 (기각)"]
        ALL["EC2 t3.xlarge<br/>App + MySQL + Redis + Kafka<br/>~$36/월"]
    end

    subgraph After["✅ 서비스 분리 (채택)"]
        A1["App<br/>$6"]
        A2["Redis<br/>$6"]
        A3["RDS<br/>무료"]
        A4["Kafka<br/>$4"]
    end
```

**기각 사유 (단일 서버)**:
- 리소스 경쟁으로 성능 측정 부정확
- MySQL 쿼리 폭주 시 App도 영향
- 병목 지점 파악 어려움

**채택 사유 (서비스 분리)**:
- 각 컴포넌트 독립적 리소스 사용
- 부하 테스트 시 정확한 병목 파악 가능
- 실무와 유사한 아키텍처 경험

### 2. Spot vs On-Demand 인스턴스

```mermaid
flowchart TD
    Q1{"서비스 중단 시<br/>영향도?"}
    Q1 -->|"높음"| OD["On-Demand<br/>(App)"]
    Q1 -->|"중간/낮음"| SPOT["Spot Instance<br/>(Redis, Kafka)"]

    OD --> R1["안정성 우선<br/>비용 ~$6/월"]
    SPOT --> R2["70% 비용 절감<br/>종료 위험 감수"]
```

| 서비스 | 선택 | 이유 |
|-------|------|------|
| App | On-Demand | 사용자 요청 처리, 중단 불가 |
| Redis | Spot | 테스트 환경이므로 캐시 유실 허용, 비용 절감 |
| Kafka | Spot | 테스트 용도, 종료되면 재시작하면 됨 |

> **참고**: 프로덕션 환경에서는 Redis를 On-Demand로 전환하거나 ElastiCache 사용을 권장합니다. Spot Instance는 stateful 서비스에 적합하지 않지만, 학습/테스트 환경에서는 비용 절감 효과가 큽니다.

### 3. RDS vs EC2 MySQL

```mermaid
flowchart LR
    subgraph EC2MySQL["EC2 + MySQL"]
        E1["직접 설치/관리"]
        E2["백업 직접 구성"]
        E3["패치 직접 적용"]
    end

    subgraph RDS["RDS MySQL ✅"]
        R1["관리형 서비스"]
        R2["자동 백업"]
        R3["자동 패치"]
        R4["12개월 무료"]
    end

    EC2MySQL -->|"비용 동일, 관리 부담"| X["기각"]
    RDS -->|"무료 + 자동 관리"| O["채택"]
```

**RDS 선택 이유**:
- 12개월 Free Tier (db.t4g.micro)
- 자동 백업, 패치, 장애 복구
- 추후 확장 용이 (버튼 클릭으로 스케일업)

### 4. ElastiCache vs EC2 Redis

```mermaid
flowchart LR
    subgraph Elasti["ElastiCache"]
        EL1["관리형 서비스"]
        EL2["~$12/월"]
        EL3["Free Tier 없음"]
    end

    subgraph EC2Redis["EC2 + Redis (Spot) ✅"]
        ER1["직접 운영"]
        ER2["~$2/월"]
        ER3["학습 가치 높음"]
    end

    Elasti -->|"비용 높음"| X["기각"]
    EC2Redis -->|"저렴 + 직접 경험"| O["채택"]
```

**EC2 + Redis (Spot) 선택 이유**:
- 학습 목적: 직접 운영해봐야 이해됨
- ElastiCache 대비 ~85% 비용 절감 (Spot 활용)
- Redis 설정/튜닝 직접 경험
- 테스트 환경이므로 Spot 종료 시 재시작하면 됨

### 5. Kafka 노드 수 결정

```mermaid
flowchart TD
    Q["테스트 목적?"]
    Q -->|"앱 성능/부하 테스트"| ONE["1노드 충분 ✅"]
    Q -->|"Kafka 장애 복구 테스트"| THREE["3노드 필요"]
    Q -->|"운영 환경 시뮬레이션"| THREE

    ONE --> COST1["~$4/월 (Spot)"]
    THREE --> COST3["~$12/월+ (3x Spot)"]
```

**1노드 선택 이유**:
- 테스트 목적: 앱이 트래픽을 얼마나 버티는지
- Kafka 장애 복구 테스트는 현재 범위 외

---

## 성능 테스트 아키텍처

```mermaid
sequenceDiagram
    participant K6 as 🔫 K6 (부하 생성)
    participant App as 🖥️ App Server
    participant Redis as 📦 Redis
    participant MySQL as 🗄️ MySQL
    participant Kafka as 📨 Kafka

    K6->>App: HTTP 요청 (동시 1000명)
    App->>Redis: 캐시 조회
    Redis-->>App: 캐시 Hit/Miss
    App->>MySQL: DB 쿼리
    MySQL-->>App: 결과
    App->>Kafka: 이벤트 발행
    App-->>K6: HTTP 응답

    Note over App: CPU/Memory 모니터링
    Note over Redis: Memory 사용량
    Note over MySQL: Query 성능
    Note over Kafka: 처리량 (TPS)
```

### 분리 구성의 장점

서비스가 분리되어 있어 부하 테스트 시 **병목 지점을 명확히 파악** 가능:

```mermaid
flowchart LR
    subgraph Metrics["모니터링 지표"]
        M1["App CPU 100%?<br/>→ App 스케일업"]
        M2["Redis Memory 부족?<br/>→ Redis 스케일업"]
        M3["MySQL Slow Query?<br/>→ 쿼리 최적화"]
        M4["Kafka Lag 증가?<br/>→ Consumer 확장"]
    end
```

---

## 환경별 설정

### 로컬 개발 환경

```mermaid
flowchart LR
    subgraph Local["로컬 (docker-compose.dev.yml)"]
        L1["Spring Boot"]
        L2["MySQL Container"]
        L3["Redis Container"]
        L4["Kafka Container"]
    end

    ENV[".env 파일"] --> Local
```

### AWS 운영 환경

```mermaid
flowchart LR
    subgraph AWS["AWS (docker-compose.prod.yml)"]
        A1["EC2: App"]
        A2["EC2: Redis"]
        A3["RDS: MySQL"]
        A4["EC2: Kafka (Spot)"]
    end

    GH["GitHub Secrets"] --> AWS
```

---

## 확장 계획

현재 구성에서 트래픽 증가 시 확장 경로:

```mermaid
flowchart TD
    subgraph Current["현재 (MVP)"]
        C1["App: t4g.micro"]
        C2["Redis: t4g.micro"]
        C3["MySQL: db.t4g.micro"]
    end

    subgraph Scale1["1단계 확장"]
        S1["App: t4g.small"]
        S2["Redis: t4g.small"]
        S3["MySQL: db.t4g.small"]
    end

    subgraph Scale2["2단계 확장"]
        S4["App: Auto Scaling Group"]
        S5["Redis: ElastiCache"]
        S6["MySQL: db.t4g.medium + Read Replica"]
    end

    Current --> Scale1
    Scale1 --> Scale2
```

---

## 참고

### ARM 인스턴스 (t4g) 선택 이유

- x86 (t3) 대비 **20% 저렴**
- 동일 스펙에서 **성능 더 좋음**
- Kafka, MySQL, Redis 모두 ARM 지원

### 비용 최적화

1. **Kafka는 테스트할 때만 켜기**: 평상시 ~$8/월
2. **RDS Free Tier 활용**: 12개월 무료
3. **Spot Instance**: Redis, Kafka에 적용하여 ~70% 비용 절감
4. **같은 AZ 배치**: 네트워크 비용 절감
5. **Public Subnet 활용**: NAT Gateway 비용(~$32/월) 절약, Security Group으로 보안 확보
