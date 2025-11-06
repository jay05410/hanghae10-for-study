# API 명세서 (API Specification)

## 목차
1. [API 개요](#1-api-개요)
2. [공통 사양](#2-공통-사양)
3. [상품 API](#3-상품-api)
4. [장바구니 API](#4-장바구니-api)
5. [주문 API](#5-주문-api)
6. [쿠폰 API](#6-쿠폰-api)
7. [포인트 API](#7-포인트-api)
8. [에러 코드](#8-에러-코드)

---

## 1. API 개요

### 1.1 Base URL
```
개발: https://dev-api.oneulhancha.com
운영: https://api.oneulhancha.com
```

### 1.2 API 버전
```
현재 버전: v1
URL 형식: /api/v1/{resource}
```

### 1.3 프로토콜
- **HTTPS**: 모든 API는 HTTPS를 통해서만 접근 가능
- **HTTP Methods**: GET, POST, PUT, DELETE

---

## 2. 공통 사양

### 2.1 요청 헤더

| 헤더명 | 필수 | 설명 | 예시 |
|--------|------|------|------|
| Content-Type | Yes | 요청 데이터 타입 | application/json |
| Authorization | Conditional | JWT 토큰 (인증 필요 시) | Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... |
| X-Request-ID | No | 요청 추적 ID | uuid-v4 |

### 2.2 응답 헤더

| 헤더명 | 설명 |
|--------|------|
| Content-Type | application/json; charset=utf-8 |
| X-Request-ID | 요청 추적 ID (요청 시 제공된 경우) |
| X-Response-Time | 응답 시간 (ms) |

### 2.3 공통 응답 구조

#### 성공 응답
```json
{
  "success": true,
  "data": {
    // 실제 응답 데이터
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "PRODUCT001",
    "message": "존재하지 않는 상품입니다",
    "details": {
      "productId": 999
    }
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

### 2.4 페이징 구조
```json
{
  "content": [],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10
  }
}
```

### 2.5 HTTP 상태 코드

| 코드 | 의미 | 사용 케이스 |
|------|------|-------------|
| 200 | OK | 조회/수정/삭제 성공 |
| 201 | Created | 생성 성공 |
| 400 | Bad Request | 잘못된 요청 (유효성 검증 실패) |
| 401 | Unauthorized | 인증 실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (재고 부족, 중복 발급 등) |
| 410 | Gone | 리소스 소진 (쿠폰 소진) |
| 429 | Too Many Requests | 요청 제한 초과 |
| 500 | Internal Server Error | 서버 내부 오류 |
| 503 | Service Unavailable | 서비스 일시 중단 |

---

## 3. 상품 API

### 3.1 박스 타입 목록 조회

#### Request
```http
GET /api/v1/box-types
```

#### Response
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "THREE_DAYS",
      "name": "3일 박스",
      "days": 3,
      "description": "맛보기 세트",
      "isActive": true
    },
    {
      "id": 2,
      "code": "SEVEN_DAYS",
      "name": "7일 박스",
      "days": 7,
      "description": "일주일 티 큐레이션",
      "isActive": true
    },
    {
      "id": 3,
      "code": "FOURTEEN_DAYS",
      "name": "14일 박스",
      "days": 14,
      "description": "2주간의 다양한 티 경험",
      "isActive": true
    }
  ],
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Response Fields**:
- `id` (number): 박스타입 ID
- `code` (string): 박스타입 코드
- `name` (string): 박스명
- `days` (number): 일수
- `description` (string): 설명
- `isActive` (boolean): 활성화 여부

**Status Codes**:
- `200 OK`: 조회 성공

---

### 3.2 카테고리별 차 목록 조회

#### Request
```http
GET /api/v1/items?categoryId=1&status=ACTIVE
```

**Query Parameters**:
- `categoryId` (number, optional): 카테고리 ID
- `status` (string, optional): 상품 상태

#### Response
```json
{
  "success": true,
  "data": {
    "categories": [
      {
        "id": 1,
        "name": "허브티",
        "description": "천연 허브로 만든 건강한 차",
        "items": [
          {
            "id": 1,
            "name": "라벤더 차",
            "description": "편안한 휴식을 위한 프랑스산 라벤더",
            "caffeineType": "카페인프리",
            "tasteProfile": "플로럴, 부드러운",
            "aromaProfile": "라벤더 향",
            "colorProfile": "연보라빛",
            "bagPerWeight": 3,
            "pricePer100g": 15000,
            "ingredients": "라벤더, 레몬밤",
            "origin": "프랑스",
            "status": "ACTIVE"
          },
          {
            "id": 2,
            "name": "카모마일 차",
            "description": "숙면에 도움을 주는 독일산 카모마일",
            "caffeineType": "카페인프리",
            "tasteProfile": "달콤한, 부드러운",
            "aromaProfile": "사과향",
            "colorProfile": "황금빛",
            "bagPerWeight": 2,
            "pricePer100g": 12000,
            "ingredients": "카모마일",
            "origin": "독일",
            "status": "ACTIVE"
          }
        ]
      }
    ]
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
        "name": "CALM",
        "displayName": "평온",
        "description": "편안하고 차분한 기분이 좋아요",
        "sortOrder": 2
      },
      {
        "id": 3,
        "name": "FOCUS",
        "displayName": "집중",
        "description": "집중력과 명료함이 필요해요",
        "sortOrder": 3
      }
    ],
    "scents": [
      {
        "id": 1,
        "name": "FLORAL",
        "displayName": "플로럴",
        "description": "꽃향기가 나는 부드러운 향",
        "sortOrder": 1
      },
      {
        "id": 2,
        "name": "CITRUS",
        "displayName": "시트러스",
        "description": "상큼하고 청량한 과일 향",
        "sortOrder": 2
      },
      {
        "id": 3,
        "name": "HERBAL",
        "displayName": "허브",
        "description": "허브의 깊고 진한 향",
        "sortOrder": 3
      }
    ]
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공

---

### 3.3 차 아이템 재고 조회

#### Request
```http
GET /api/v1/items/{itemId}/inventory
```

**Path Parameters**:
- `itemId` (number): 차 아이템 ID

#### Response
```json
{
  "success": true,
  "data": {
    "itemId": 1,
    "item": {
      "id": 1,
      "name": "라벤더 릴랙스",
      "description": "편안한 수면을 도와주는 프랑스산 라벤더",
      "pricePer100g": 12000,
      "categoryId": 1
    },
    "stockQuantity": 15,
    "reservedQuantity": 3,
    "availableQuantity": 12,
    "isAvailable": true,
    "lastUpdated": "2025-10-31T10:30:00Z"
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Response Fields**:
- `stockQuantity` (number): 전체 재고 수량
- `reservedQuantity` (number): 예약된 수량
- `availableQuantity` (number): 주문 가능 수량
- `isAvailable` (boolean): 구매 가능 여부

**Status Codes**:
- `200 OK`: 조회 성공
- `404 Not Found`: 존재하지 않는 아이템

---

### 3.4 인기 조합 통계 조회

#### Request
```http
GET /api/v1/products/popular-combinations?days=3
```

**Query Parameters**:
- `days` (number, optional, default=3): 통계 기간 (일)

#### Response
```json
{
  "success": true,
  "data": {
    "period": {
      "days": 3,
      "from": "2025-10-28T00:00:00Z",
      "to": "2025-10-31T23:59:59Z"
    },
    "combinations": [
      {
        "rank": 1,
        "combination": {
          "id": 1,
          "productName": "기본 7일 박스",
          "condition": "피로",
          "mood": "활력",
          "scent": "시트러스"
        },
        "orderCount": 143,
        "percentageChange": "+15%",
        "tagline": "에너지가 필요한 직장인들의 선택"
      },
      {
        "rank": 2,
        "combination": {
          "id": 5,
          "productName": "기본 7일 박스",
          "condition": "스트레스",
          "mood": "평온",
          "scent": "플로럴"
        },
        "orderCount": 112,
        "percentageChange": "+8%",
        "tagline": "마음의 안정을 찾고 싶을 때"
      },
      {
        "rank": 3,
        "combination": {
          "id": 12,
          "productName": "기본 7일 박스",
          "condition": "소화불편",
          "mood": "집중",
          "scent": "허브"
        },
        "orderCount": 87,
        "percentageChange": "-3%",
        "tagline": "편안한 소화와 집중력을 함께"
      },
      {
        "rank": 4,
        "combination": {
          "id": 3,
          "productName": "기본 7일 박스",
          "condition": "피로",
          "mood": "평온",
          "scent": "허브"
        },
        "orderCount": 71,
        "percentageChange": "+12%",
        "tagline": "피곤한 하루를 마무리하는 릴렉싱 블렌드"
      },
      {
        "rank": 5,
        "combination": {
          "id": 7,
          "productName": "기본 7일 박스",
          "condition": "스트레스",
          "mood": "활력",
          "scent": "시트러스"
        },
        "orderCount": 64,
        "percentageChange": "0%",
        "tagline": "스트레스를 날려버리고 싶을 때"
      }
    ]
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공

---

### 3.5 박스 조합 미리보기

#### Request
```http
GET /api/v1/products/combinations/{combinationId}/preview
```

**Path Parameters**:
- `combinationId` (number): 조합 ID

#### Response
```json
{
  "success": true,
  "data": {
    "combinationId": 1,
    "combination": {
      "productId": 1,
      "productName": "기본 7일 박스",
      "condition": "피로",
      "mood": "활력",
      "scent": "시트러스"
    },
    "weeklyTeas": [
      {
        "dayNumber": 1,
        "dayOfWeek": "월요일",
        "teaName": "레몬그라스 진저 티",
        "mainIngredients": "레몬그라스, 생강, 레몬밤",
        "expectedEffects": "활력 증진, 기분 전환",
        "brewingGuide": "80도 물 200ml에 3분"
      },
      {
        "dayNumber": 2,
        "dayOfWeek": "화요일",
        "teaName": "얼그레이 베르가못",
        "mainIngredients": "홍차, 베르가못",
        "expectedEffects": "에너지 증진, 기분 상승",
        "brewingGuide": "95도 물 200ml에 3-4분"
      },
      {
        "dayNumber": 3,
        "dayOfWeek": "수요일",
        "teaName": "페퍼민트 그린티",
        "mainIngredients": "녹차, 페퍼민트",
        "expectedEffects": "정신 맑게, 상쾌함",
        "brewingGuide": "70도 물 200ml에 2-3분"
      },
      {
        "dayNumber": 4,
        "dayOfWeek": "목요일",
        "teaName": "로즈마리 레몬티",
        "mainIngredients": "로즈마리, 레몬필",
        "expectedEffects": "집중력 향상, 활력",
        "brewingGuide": "90도 물 200ml에 5분"
      },
      {
        "dayNumber": 5,
        "dayOfWeek": "금요일",
        "teaName": "유자 루이보스",
        "mainIngredients": "루이보스, 유자",
        "expectedEffects": "비타민 보충, 활력",
        "brewingGuide": "100도 물 200ml에 5분"
      },
      {
        "dayNumber": 6,
        "dayOfWeek": "토요일",
        "teaName": "오렌지 카모마일",
        "mainIngredients": "카모마일, 오렌지필",
        "expectedEffects": "편안한 휴식, 기분 전환",
        "brewingGuide": "95도 물 200ml에 5분"
      },
      {
        "dayNumber": 7,
        "dayOfWeek": "일요일",
        "teaName": "자스민 화이트티",
        "mainIngredients": "백차, 자스민",
        "expectedEffects": "마음 진정, 평온",
        "brewingGuide": "75도 물 200ml에 3분"
      }
    ]
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공
- `404 Not Found`: 존재하지 않는 조합

---

## 4. 장바구니 API

### 4.1 장바구니 추가

#### Request
```http
POST /api/v1/cart
Authorization: Bearer {token}
Content-Type: application/json

{
  "boxTypeId": 1,
  "teaSelections": [
    {
      "dayNumber": 1,
      "itemId": 1
    },
    {
      "dayNumber": 2,
      "itemId": 3
    }
  ],
  "quantity": 1
}
```

**Request Body**:
- `boxTypeId` (number, required): 박스 타입 ID
- `teaSelections` (array, required): 차 선택 목록
  - `dayNumber` (number): 일차 (1-7 또는 1-30)
  - `itemId` (number): 차 아이템 ID
- `quantity` (number, required): 수량 (현재는 1만 가능)

#### Response
```json
{
  "success": true,
  "data": {
    "cartItemId": 1,
    "boxType": {
      "id": 1,
      "name": "7일 커스텀 박스",
      "description": "7일간의 개인 맞춤 차 박스",
      "dayCount": 7,
      "price": 29000
    },
    "teaSelections": [
      {
        "dayNumber": 1,
        "item": {
          "id": 1,
          "name": "라벤더 릴랙스",
          "price": 4200
        }
      },
      {
        "dayNumber": 2,
        "item": {
          "id": 3,
          "name": "카모마일 차",
          "price": 3800
        }
      }
    ],
    "message": "장바구니에 추가되었습니다"
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Status Codes**:
- `201 Created`: 추가 성공
- `400 Bad Request`: 잘못된 요청 (차 개수 불일치 등)
- `401 Unauthorized`: 인증 실패
- `404 Not Found`: 존재하지 않는 박스타입/차 아이템
- `409 Conflict`: 장바구니 최대 개수 초과 (10개)

---

### 4.2 장바구니 조회

#### Request
```http
GET /api/v1/cart
Authorization: Bearer {token}
```

#### Response
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "cartItemId": 1,
        "boxType": {
          "id": 1,
          "name": "7일 커스텀 박스",
          "description": "7일간의 개인 맞춤 차 박스",
          "dayCount": 7,
          "price": 29000
        },
        "teaSelections": [
          {
            "dayNumber": 1,
            "item": {
              "id": 1,
              "name": "라벤더 릴랙스",
              "price": 4200,
              "stockQuantity": 15
            }
          },
          {
            "dayNumber": 2,
            "item": {
              "id": 3,
              "name": "카모마일 차",
              "price": 3800,
              "stockQuantity": 22
            }
          }
        ],
        "quantity": 1,
        "subtotal": 29000,
        "isAvailable": true,
        "createdAt": "2025-10-31T09:15:00Z"
      },
      {
        "cartItemId": 2,
        "boxType": {
          "id": 2,
          "name": "30일 프리미엄 박스",
          "description": "30일간의 프리미엄 차 박스",
          "dayCount": 30,
          "price": 89000
        },
        "teaSelections": [
          {
            "dayNumber": 1,
            "item": {
              "id": 5,
              "name": "에나지 그린티",
              "price": 4500,
              "stockQuantity": 8
            }
          }
        ],
        "quantity": 1,
        "subtotal": 89000,
        "isAvailable": true,
        "createdAt": "2025-10-31T10:20:00Z"
      }
    ],
    "summary": {
      "totalItems": 2,
      "totalAmount": 118000,
      "maxItems": 10
    }
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Response Fields**:
- `isAvailable` (boolean): 현재 구매 가능 여부
- `stockRemaining` (number): 남은 재고

**Status Codes**:
- `200 OK`: 조회 성공
- `401 Unauthorized`: 인증 실패

---

### 4.3 장바구니 항목 삭제

#### Request
```http
DELETE /api/v1/cart/items/{cartItemId}
Authorization: Bearer {token}
```

**Path Parameters**:
- `cartItemId` (number): 장바구니 항목 ID

#### Response
```json
{
  "success": true,
  "data": {
    "message": "장바구니 항목이 삭제되었습니다"
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 삭제 성공
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음 (다른 사용자의 항목)
- `404 Not Found`: 존재하지 않는 항목

---

### 4.4 장바구니 비우기

#### Request
```http
DELETE /api/v1/cart
Authorization: Bearer {token}
```

#### Response
```json
{
  "success": true,
  "data": {
    "message": "장바구니가 비워졌습니다"
  },
  "timestamp": "2025-10-31T10:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 삭제 성공
- `401 Unauthorized`: 인증 실패

---

## 5. 주문 API

### 5.1 주문 생성

#### Request
```http
POST /api/v1/orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "couponId": 1,
  "deliveryAddress": {
    "recipient": "김민지",
    "phone": "010-1234-5678",
    "zipCode": "06234",
    "address": "서울시 강남구 테헤란로 123",
    "addressDetail": "456호"
  }
}
```

**Request Body**:
- `couponId` (number, optional): 사용할 쿠폰 ID
- `deliveryAddress` (object, required): 배송지 정보
    - `recipient` (string): 수령인
    - `phone` (string): 전화번호
    - `zipCode` (string): 우편번호
    - `address` (string): 주소
    - `addressDetail` (string): 상세 주소

#### Response
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20251031-001",
    "items": [
      {
        "orderItemId": 1,
        "boxType": {
          "id": 1,
          "name": "7일 커스텀 박스",
          "dayCount": 7,
          "price": 29000
        },
        "teaSelections": [
          {
            "dayNumber": 1,
            "item": {
              "id": 1,
              "name": "라벤더 릴랙스",
              "price": 4200
            }
          },
          {
            "dayNumber": 2,
            "item": {
              "id": 3,
              "name": "카모마일 차",
              "price": 3800
            }
          }
        ],
        "quantity": 1,
        "subtotal": 29000
      }
    ],
    "payment": {
      "totalAmount": 29000,
      "discountAmount": 14500,
      "finalAmount": 14500,
      "method": "BALANCE",
      "paidAt": "2025-10-31T14:30:00Z"
    },
    "delivery": {
      "recipient": "김민지",
      "phone": "010-1234-5678",
      "address": "서울시 강남구 테헤란로 123 456호",
      "estimatedDeliveryDate": "2025-11-03"
    },
    "status": "PAID",
    "orderedAt": "2025-10-31T14:30:00Z"
  },
  "timestamp": "2025-10-31T14:30:00Z"
}
```

**Status Codes**:
- `201 Created`: 주문 생성 성공
- `400 Bad Request`: 잘못된 요청 (빈 장바구니, 쿠폰 무효 등)
- `401 Unauthorized`: 인증 실패
- `409 Conflict`: 재고 부족

---

### 5.2 주문 내역 조회

#### Request
```http
GET /api/v1/orders?page=0&size=10&status=ALL
Authorization: Bearer {token}
```

**Query Parameters**:
- `page` (number, optional, default=0): 페이지 번호
- `size` (number, optional, default=10): 페이지 크기
- `status` (string, optional, default=ALL): 주문 상태 필터
    - `ALL`, `PENDING`, `PAID`, `PREPARING`, `SHIPPED`, `DELIVERED`, `CANCELLED`

#### Response
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": 1,
        "orderNumber": "ORD-20251031-001",
        "totalItems": 2,
        "finalAmount": 29000,
        "status": "PAID",
        "orderedAt": "2025-10-31T14:30:00Z"
      },
      {
        "orderId": 2,
        "orderNumber": "ORD-20251030-045",
        "totalItems": 1,
        "finalAmount": 49000,
        "status": "SHIPPED",
        "orderedAt": "2025-10-30T10:15:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 10,
      "totalElements": 25,
      "totalPages": 3
    }
  },
  "timestamp": "2025-10-31T14:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공
- `401 Unauthorized`: 인증 실패

---

### 5.3 주문 상세 조회

#### Request
```http
GET /api/v1/orders/{orderId}
Authorization: Bearer {token}
```

**Path Parameters**:
- `orderId` (number): 주문 ID

#### Response
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20251031-001",
    "status": "PAID",
    "items": [
      {
        "product": {
          "name": "기본 7일 박스",
          "price": 29000
        },
        "combination": {
          "condition": "피로",
          "mood": "활력",
          "scent": "시트러스"
        },
        "quantity": 1,
        "weeklyTeas": [
          {
            "dayNumber": 1,
            "dayOfWeek": "월요일",
            "teaName": "레몬그라스 진저 티",
            "mainIngredients": "레몬그라스, 생강, 레몬밤",
            "expectedEffects": "활력 증진, 기분 전환",
            "brewingGuide": "80도 물 200ml에 3분"
          },
          {
            "dayNumber": 2,
            "dayOfWeek": "화요일",
            "teaName": "얼그레이 베르가못",
            "mainIngredients": "홍차, 베르가못",
            "expectedEffects": "에너지 증진, 기분 상승",
            "brewingGuide": "95도 물 200ml에 3-4분"
          }
          // ... 나머지 5일
        ]
      }
    ],
    "payment": {
      "totalAmount": 29000,
      "discountAmount": 14500,
      "finalAmount": 14500,
      "paidAt": "2025-10-31T14:30:15Z"
    },
    "delivery": {
      "recipient": "김민지",
      "phone": "010-1234-5678",
      "address": "서울시 강남구 테헤란로 123 456호",
      "estimatedDeliveryDate": "2025-11-03",
      "trackingNumber": null
    },
    "orderedAt": "2025-10-31T14:30:00Z"
  },
  "timestamp": "2025-10-31T14:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음 (다른 사용자의 주문)
- `404 Not Found`: 존재하지 않는 주문

---

### 5.4 주문 취소

#### Request
```http
POST /api/v1/orders/{orderId}/cancel
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "주문 실수"
}
```

**Path Parameters**:
- `orderId` (number): 주문 ID

**Request Body**:
- `reason` (string, required): 취소 사유

#### Response
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "status": "CANCELLED",
    "refund": {
      "amount": 14500,
      "method": "BALANCE",
      "refundedAt": "2025-10-31T15:00:00Z"
    },
    "message": "주문이 취소되었습니다"
  },
  "timestamp": "2025-10-31T15:00:00Z"
}
```

**Status Codes**:
- `200 OK`: 취소 성공
- `400 Bad Request`: 취소 불가능한 상태 (제조 시작 후)
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 존재하지 않는 주문

---

## 6. 쿠폰 API

### 6.1 선착순 쿠폰 발급

#### Request
```http
POST /api/v1/coupons/{couponId}/issue
Authorization: Bearer {token}
```

**Path Parameters**:
- `couponId` (number): 쿠폰 ID

#### Response
```json
{
  "success": true,
  "data": {
    "userCouponId": 1,
    "coupon": {
      "id": 1,
      "name": "첫구매 50% 할인",
      "discountType": "PERCENTAGE",
      "discountValue": 50,
      "minOrderAmount": 20000
    },
    "issuedAt": "2025-10-31T14:00:00Z",
    "expiredAt": "2025-11-07T23:59:59Z",
    "message": "쿠폰이 발급되었습니다"
  },
  "timestamp": "2025-10-31T14:00:00Z"
}
```

**Status Codes**:
- `201 Created`: 발급 성공
- `401 Unauthorized`: 인증 실패
- `409 Conflict`: 이미 발급받음
- `410 Gone`: 쿠폰 소진
- `429 Too Many Requests`: 중복 요청

---

### 6.2 발급 가능 쿠폰 목록 조회

#### Request
```http
GET /api/v1/coupons
```

#### Response
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "첫구매 50% 할인",
      "code": "FIRST50",
      "discountType": "PERCENTAGE",
      "discountValue": 50,
      "minOrderAmount": 20000,
      "maxIssueCount": 100,
      "issuedCount": 78,
      "remainingCount": 22,
      "issueStartAt": "2025-10-25T00:00:00Z",
      "issueEndAt": "2025-11-30T23:59:59Z",
      "validDays": 7,
      "isActive": true,
      "isIssuable": true
    },
    {
      "id": 2,
      "name": "얼리버드 10,000원 할인",
      "code": "EARLYBIRD",
      "discountType": "FIXED",
      "discountValue": 10000,
      "minOrderAmount": 29000,
      "maxIssueCount": 200,
      "issuedCount": 200,
      "remainingCount": 0,
      "issueStartAt": "2025-10-20T00:00:00Z",
      "issueEndAt": "2025-10-31T23:59:59Z",
      "validDays": 14,
      "isActive": true,
      "isIssuable": false
    }
  ],
  "timestamp": "2025-10-31T14:00:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공

---

### 6.3 보유 쿠폰 조회

#### Request
```http
GET /api/v1/coupons/my?status=AVAILABLE
Authorization: Bearer {token}
```

**Query Parameters**:
- `status` (string, optional, default=ALL): 쿠폰 상태 필터
    - `ALL`, `AVAILABLE`, `USED`, `EXPIRED`

#### Response
```json
{
  "success": true,
  "data": [
    {
      "userCouponId": 1,
      "coupon": {
        "id": 1,
        "name": "첫구매 50% 할인",
        "discountType": "PERCENTAGE",
        "discountValue": 50,
        "minOrderAmount": 20000
      },
      "status": "AVAILABLE",
      "issuedAt": "2025-10-31T14:00:00Z",
      "expiredAt": "2025-11-07T23:59:59Z",
      "usedAt": null,
      "daysUntilExpiry": 7
    }
  ],
  "timestamp": "2025-10-31T14:00:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공
- `401 Unauthorized`: 인증 실패

---

## 7. 포인트 API

### 7.1 포인트 충전

#### Request
```http
POST /api/v1/users/balance/charge
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 50000
}
```

**Request Body**:
- `amount` (number, required): 충전 금액 (원, 양수)

#### Response
```json
{
  "success": true,
  "data": {
    "transactionId": 1,
    "amount": 50000,
    "balanceAfter": 50000,
    "chargedAt": "2025-10-31T10:00:00Z"
  },
  "timestamp": "2025-10-31T10:00:00Z"
}
```

**Status Codes**:
- `200 OK`: 충전 성공
- `400 Bad Request`: 잘못된 금액 (음수, 0원, 100원 단위 아님)
- `401 Unauthorized`: 인증 실패

---

### 7.2 포인트 내역 조회

#### Request
```http
GET /api/v1/users/balance/history?page=0&size=20
Authorization: Bearer {token}
```

**Query Parameters**:
- `page` (number, optional, default=0): 페이지 번호
- `size` (number, optional, default=20): 페이지 크기

#### Response
```json
{
  "success": true,
  "data": {
    "currentBalance": 50000,
    "history": [
      {
        "transactionId": 3,
        "transactionType": "USE",
        "amount": -29000,
        "balanceAfter": 21000,
        "description": "주문 결제 (ORD-20251031-001)",
        "createdAt": "2025-10-31T14:30:00Z"
      },
      {
        "transactionId": 2,
        "transactionType": "REFUND",
        "amount": 49000,
        "balanceAfter": 50000,
        "description": "주문 취소 환불 (ORD-20251030-045)",
        "createdAt": "2025-10-31T11:00:00Z"
      },
      {
        "transactionId": 1,
        "transactionType": "CHARGE",
        "amount": 50000,
        "balanceAfter": 50000,
        "description": "포인트 충전",
        "createdAt": "2025-10-31T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 3,
      "totalPages": 1
    }
  },
  "timestamp": "2025-10-31T14:30:00Z"
}
```

**Status Codes**:
- `200 OK`: 조회 성공
- `401 Unauthorized`: 인증 실패

---

## 8. 에러 코드

### 8.1 상품 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| PRODUCT001 | 404 | 존재하지 않는 상품입니다 | 상품 ID가 유효하지 않음 |
| PRODUCT002 | 400 | 판매가 중단된 상품입니다 | 비활성화된 상품 |
| PRODUCT003 | 404 | 존재하지 않는 조합입니다 | 조합 ID가 유효하지 않음 |
| PRODUCT004 | 400 | 유효하지 않은 옵션 조합입니다 | 컨디션/기분/향 조합이 잘못됨 |

### 8.2 재고 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| INVENTORY001 | 409 | 재고가 부족합니다 | 주문 수량 > 재고 |
| INVENTORY002 | 409 | 재고가 부족하여 주문할 수 없습니다 | 재고 0개 |
| INVENTORY003 | 409 | 일일 생산 한도를 초과했습니다 | 일일 한도 초과 |
| INVENTORY004 | 503 | 재고 처리 중 오류가 발생했습니다 | 락 타임아웃 |

### 8.3 쿠폰 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| COUPON001 | 410 | 쿠폰이 모두 소진되었습니다 | 선착순 쿠폰 소진 |
| COUPON002 | 409 | 이미 발급받은 쿠폰입니다 | 1인 1매 제한 |
| COUPON003 | 400 | 만료된 쿠폰입니다 | 유효기간 만료 |
| COUPON004 | 429 | 중복된 요청입니다 | 동시 발급 시도 |
| COUPON005 | 404 | 존재하지 않는 쿠폰입니다 | 쿠폰 ID 유효하지 않음 |
| COUPON006 | 400 | 사용할 수 없는 쿠폰입니다 | 비활성화된 쿠폰 |
| COUPON007 | 400 | 최소 주문 금액을 충족하지 못했습니다 | 최소 주문 금액 미달 |
| COUPON008 | 400 | 이미 사용된 쿠폰입니다 | 사용 완료된 쿠폰 |

### 8.4 주문 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| ORDER001 | 404 | 존재하지 않는 주문입니다 | 주문 ID 유효하지 않음 |
| ORDER002 | 403 | 주문에 접근할 권한이 없습니다 | 다른 사용자의 주문 |
| ORDER003 | 400 | 취소할 수 없는 주문 상태입니다 | 제조 시작 후 |
| ORDER004 | 400 | 장바구니가 비어있습니다 | 빈 장바구니로 주문 시도 |
| ORDER005 | 400 | 유효하지 않은 주문 상태입니다 | 잘못된 상태 전이 |

### 8.5 결제 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| PAYMENT001 | 400 | 잔액이 부족합니다 | 포인트 부족 |
| PAYMENT002 | 500 | 결제 처리 중 오류가 발생했습니다 | 결제 시스템 오료 |
| PAYMENT003 | 409 | 이미 처리된 결제입니다 | 중복 결제 시도 |
| PAYMENT004 | 400 | 유효하지 않은 결제 금액입니다 | 음수 또는 0원 |
| PAYMENT005 | 400 | 최소 충전 금액은 1,000원입니다 | 최소 충전 금액 미달 |
| PAYMENT006 | 400 | 최대 충전 금액은 100,000원입니다 | 최대 충전 금액 초과 |
| PAYMENT007 | 400 | 포인트 충전은 100원 단위로만 가능합니다 | 충전 단위 불일치 |

### 8.6 장바구니 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| CART001 | 404 | 장바구니 항목을 찾을 수 없습니다 | 항목 ID 유효하지 않음 |
| CART002 | 403 | 장바구니 항목에 접근할 권한이 없습니다 | 다른 사용자의 항목 |
| CART003 | 400 | 유효하지 않은 수량입니다 | 0 이하 또는 과도한 수량 |
| CART004 | 409 | 장바구니는 최대 10개까지 담을 수 있습니다 | 최대 항목 수 초과 |
| CART005 | 409 | 동일한 박스타입이 이미 장바구니에 있습니다 | 중복 박스타입 추가 |
| CART006 | 400 | 차 선택 개수가 올바르지 않습니다 | 박스 일수와 차 개수 불일치 |

### 8.7 동시성 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| CONCURRENCY001 | 503 | 재고 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요 | 재고 락 타임아웃 |
| CONCURRENCY002 | 429 | 중복된 요청입니다. 잠시 후 다시 시도해주세요 | 쿠폰 발급 중복 요청 |
| CONCURRENCY003 | 503 | 시스템이 혼잡합니다. 잠시 후 다시 시도해주세요 | 동시성 제어 실패 |

### 8.8 외부 연동 관련

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| EXTERNAL001 | 504 | 외부 시스템 응답 시간 초과 | 타임아웃 |
| EXTERNAL002 | 500 | 외부 시스템 연동 오류 | API 오류 |
| EXTERNAL003 | 503 | 외부 시스템을 사용할 수 없습니다 | 시스템 다운 |

---

## 9. Rate Limiting

### 9.1 요청 제한

| 엔드포인트 | 제한 | 단위 |
|-----------|------|------|
| 전체 API | 100 | 요청/분/사용자 |
| 쿠폰 발급 | 5 | 요청/분/사용자 |
| 주문 생성 | 10 | 요청/분/사용자 |

### 9.2 제한 초과 시 응답
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "요청 횟수 제한을 초과했습니다",
    "details": {
      "retryAfter": 60
    }
  },
  "timestamp": "2025-10-31T14:30:00Z"
}
```

**Response Headers**:
- `X-RateLimit-Limit`: 제한 수
- `X-RateLimit-Remaining`: 남은 요청 수
- `X-RateLimit-Reset`: 리셋 시각 (Unix timestamp)
- `Retry-After`: 재시도 가능 시간 (초)

---

## 변경 이력
| 버전 | 날짜 | 변경 내용 | 작성자  |
|-----|------|----------|------|
| 1.0 | 2025-10-31 | 초안 작성 | jay  |