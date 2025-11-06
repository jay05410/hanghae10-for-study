# 장바구니 추가 플로우

## 개요
- **목적**: 선택한 커스텀 박스를 장바구니에 추가
- **핵심 비즈니스 로직**: 재고 검증, 중복 방지, 수량 제한, 차 배합 비율 계산
- **주요 검증 사항**: 박스타입 유효성, 차 아이템 재고, 최대 수량, 배합 비율 합계

## API 엔드포인트

### Request
```http
POST /api/v1/cart
Authorization: Bearer {token}
Content-Type: application/json

{
  "boxTypeId": 2,
  "dailyServing": 2,
  "quantity": 1,
  "giftWrap": false,
  "giftMessage": null,
  "teaSelections": [
    {
      "itemId": 1,
      "selectionOrder": 1,
      "ratioPercent": 40
    },
    {
      "itemId": 3,
      "selectionOrder": 2,
      "ratioPercent": 35
    },
    {
      "itemId": 5,
      "selectionOrder": 3,
      "ratioPercent": 25
    }
  ]
}
```

### Response (성공)
```json
{
  "success": true,
  "data": {
    "cartItemId": 1,
    "boxType": {
      "id": 2,
      "name": "7일 박스",
      "days": 7
    },
    "dailyServing": 2,
    "quantity": 1,
    "totalTeaBags": 14,
    "estimatedPrice": 35000,
    "teaSelections": [
      {
        "item": {
          "id": 1,
          "name": "얼그레이",
          "category": "홍차"
        },
        "selectionOrder": 1,
        "ratioPercent": 40,
        "estimatedGrams": 168
      }
    ],
    "giftWrap": false,
    "message": "장바구니에 추가되었습니다"
  }
}
```

## 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant CartAPI
    participant CartService
    participant ProductService
    participant InventoryService
    participant Database
    participant Redis

    User->>Frontend: 커스텀 박스 구성 완료
    Frontend->>CartAPI: POST /api/v1/cart

    Note over CartAPI: 요청 검증
    CartAPI->>CartAPI: validateRequest(request)

    alt 잘못된 요청 데이터
        CartAPI-->>Frontend: 400 Bad Request
        Frontend-->>User: "입력 정보를 확인해주세요"
    end

    CartAPI->>CartService: addToCart(userId, request)

    Note over CartService: 비즈니스 로직 검증
    CartService->>CartService: validateBusinessRules(request)

    Note over CartService: 배합 비율 검증 (합계 100%)
    CartService->>CartService: validateTeaRatios(teaSelections)

    alt 배합 비율 오류
        CartService-->>CartAPI: InvalidTeaRatioException
        CartAPI-->>Frontend: 400 Bad Request
        Frontend-->>User: "차 배합 비율의 합이 100%가 되어야 합니다"
    end

    Note over CartService: 박스 타입 검증
    CartService->>ProductService: validateBoxType(boxTypeId)
    ProductService->>Database: SELECT * FROM box_type WHERE id = ? AND is_active = true

    alt 박스 타입 비활성화/없음
        Database-->>ProductService: 조회 결과 없음
        ProductService-->>CartService: BoxTypeNotFoundException
        CartService-->>CartAPI: 404 Not Found
        CartAPI-->>Frontend: "선택한 박스 타입을 찾을 수 없습니다"
    end

    Database-->>ProductService: 박스 타입 정보
    ProductService-->>CartService: BoxType 정보

    Note over CartService: 차 아이템 검증 및 재고 확인
    CartService->>InventoryService: validateItemsAndStock(teaSelections, boxType, dailyServing)

    Note over InventoryService: 분산 락 획득 (동시성 제어)
    InventoryService->>Redis: SET lock:inventory:check:{userId} EX 10 NX

    alt 락 획득 실패 (동시 요청)
        Redis-->>InventoryService: 락 획득 실패
        InventoryService-->>CartService: ConcurrentAccessException
        CartService-->>CartAPI: 409 Conflict
        CartAPI-->>Frontend: "잠시 후 다시 시도해주세요"
    end

    Redis-->>InventoryService: 락 획득 성공

    InventoryService->>Database: SELECT i.*, inv.quantity FROM item i JOIN inventory inv ON i.id = inv.item_id WHERE i.id IN (?) AND i.is_active = true

    Note over InventoryService: 필요 차량 계산
    InventoryService->>InventoryService: calculateRequiredTea(teaSelections, boxType, dailyServing)

    alt 재고 부족
        InventoryService->>Redis: DEL lock:inventory:check:{userId}
        InventoryService-->>CartService: InsufficientStockException
        CartService-->>CartAPI: 409 Conflict
        CartAPI-->>Frontend: "선택한 차의 재고가 부족합니다"
        Frontend-->>User: 재고 부족 메시지 + 대체 상품 제안
    end

    InventoryService->>Redis: DEL lock:inventory:check:{userId}
    InventoryService-->>CartService: 재고 검증 성공

    Note over CartService: 장바구니 중복/수량 검증
    CartService->>Database: SELECT COUNT(*) FROM cart_item ci JOIN cart c ON ci.cart_id = c.id WHERE c.user_id = ?

    alt 장바구니 최대 수량 초과
        Database-->>CartService: count >= 10
        CartService-->>CartAPI: 409 Conflict
        CartAPI-->>Frontend: "장바구니는 최대 10개까지 담을 수 있습니다"
    end

    CartService->>Database: SELECT * FROM cart_item ci JOIN cart c ON ci.cart_id = c.id WHERE c.user_id = ? AND ci.box_type_id = ?

    alt 동일 박스타입 존재
        Database-->>CartService: 기존 항목 존재
        CartService-->>CartAPI: 409 Conflict
        CartAPI-->>Frontend: "동일한 박스타입이 이미 장바구니에 있습니다"
        Frontend-->>User: "기존 항목을 수정하시겠습니까?"
    end

    Note over CartService: 가격 계산
    CartService->>CartService: calculatePrice(boxType, teaSelections, dailyServing, giftWrap)

    Note over CartService: 트랜잭션 시작
    CartService->>Database: BEGIN TRANSACTION

    Note over CartService: 사용자 장바구니 조회/생성
    CartService->>Database: SELECT id FROM cart WHERE user_id = ? AND is_active = true

    alt 장바구니 없음
        CartService->>Database: INSERT INTO cart (user_id, is_active, created_by) VALUES (?, true, ?)
    end

    CartService->>Database: INSERT INTO cart_item (cart_id, box_type_id, daily_serving, quantity, gift_wrap, gift_message, is_active, created_by)

    loop 차 선택 항목별
        CartService->>Database: INSERT INTO cart_item_tea (cart_item_id, item_id, selection_order, ratio_percent, is_active, created_by)
    end

    Note over CartService: 트랜잭션 커밋
    CartService->>Database: COMMIT TRANSACTION

    Note over CartService: 응답 데이터 구성
    CartService->>CartService: buildCartItemResponse(cartItem, teaSelections)

    CartService-->>CartAPI: 추가 성공 응답
    CartAPI-->>Frontend: 201 Created
    Frontend->>Frontend: 장바구니 아이콘 업데이트 (수량 증가)
    Frontend-->>User: "장바구니에 추가되었습니다"
```

## 비즈니스 로직 상세

### 1. 차 배합 비율 검증
```kotlin
fun validateTeaRatios(teaSelections: List<TeaSelection>) {
    val totalRatio = teaSelections.sumOf { it.ratioPercent }
    if (totalRatio != 100) {
        throw InvalidTeaRatioException("차 배합 비율의 합이 100%가 되어야 합니다. 현재: ${totalRatio}%")
    }

    teaSelections.forEach { selection ->
        if (selection.ratioPercent < 5 || selection.ratioPercent > 80) {
            throw InvalidTeaRatioException("각 차의 배합 비율은 5% 이상 80% 이하여야 합니다")
        }
    }
}
```

### 2. 필요 차량 계산 로직
```kotlin
fun calculateRequiredTea(teaSelections: List<TeaSelection>, boxType: BoxType, dailyServing: Int): Map<Long, Int> {
    val totalTeaBags = boxType.days * dailyServing  // 예: 7일 * 2회 = 14개
    val avgBagWeight = 3  // 티백당 평균 3g
    val totalWeightNeeded = totalTeaBags * avgBagWeight  // 42g

    return teaSelections.associate { selection ->
        val weightForThisTea = (totalWeightNeeded * selection.ratioPercent / 100.0).toInt()
        selection.itemId to weightForThisTea  // 예: 얼그레이 40% = 16.8g ≈ 17g
    }
}
```

### 3. 가격 계산 로직
```kotlin
fun calculatePrice(boxType: BoxType, teaSelections: List<TeaSelection>, dailyServing: Int, giftWrap: Boolean): PriceBreakdown {
    val containerPrice = 5000  // 기본 용기 가격
    val teaPrice = calculateTeaPrice(teaSelections, boxType.days * dailyServing)
    val giftWrapPrice = if (giftWrap) 2000 else 0

    return PriceBreakdown(
        containerPrice = containerPrice,
        teaPrice = teaPrice,
        giftWrapPrice = giftWrapPrice,
        totalPrice = containerPrice + teaPrice + giftWrapPrice
    )
}

private fun calculateTeaPrice(teaSelections: List<TeaSelection>, totalBags: Int): Int {
    val totalWeightGrams = totalBags * 3  // 총 필요 차량 (g)

    return teaSelections.sumOf { selection ->
        val itemPrice = getItemPrice(selection.itemId)  // 100g당 가격
        val weightForThisItem = (totalWeightGrams * selection.ratioPercent / 100.0)
        val priceForThisItem = (itemPrice * weightForThisItem / 100.0).toInt()
        priceForThisItem
    }
}
```

## 비즈니스 정책 반영

### 장바구니 관리 정책 (BP-CART-001, BP-CART-002)
- **수량 제한**: 사용자당 최대 10개 항목
- **중복 방지**: 동일 박스타입의 커스텀 박스는 1개만 허용
- **유효 기간**: 장바구니 항목은 7일 후 자동 삭제 (배치 작업)

### 재고 관리 정책 (BP-INVENTORY-001, BP-INVENTORY-002)
- **실시간 재고 확인**: 장바구니 추가 시점에 실제 재고 검증
- **안전 재고 고려**: 필요량이 (현재재고 - 안전재고)를 초과하면 거부
- **동시성 제어**: Redis 분산 락으로 동시 재고 확인 방지

### 상품 구성 정책 (BP-PRODUCT-002, BP-PRODUCT-003)
- **배합 비율 제한**: 각 차는 5% 이상 80% 이하로 제한
- **최소/최대 차 종류**: 박스당 최소 2종, 최대 5종의 차 선택 가능

## 에러 처리

| 에러 코드 | HTTP 상태 | 시나리오 | 메시지 |
|----------|----------|----------|--------|
| CART001 | 400 | 잘못된 배합 비율 | "차 배합 비율의 합이 100%가 되어야 합니다" |
| CART002 | 400 | 배합 비율 범위 초과 | "각 차의 배합 비율은 5% 이상 80% 이하여야 합니다" |
| CART003 | 409 | 장바구니 최대 수량 초과 | "장바구니는 최대 10개까지 담을 수 있습니다" |
| CART004 | 409 | 동일 박스타입 중복 | "동일한 박스타입이 이미 장바구니에 있습니다" |
| INVENTORY001 | 409 | 재고 부족 | "선택한 차의 재고가 부족합니다" |
| PRODUCT001 | 404 | 박스타입 없음 | "선택한 박스 타입을 찾을 수 없습니다" |
| PRODUCT002 | 404 | 차 아이템 없음 | "선택한 차를 찾을 수 없습니다" |
| SYSTEM003 | 409 | 동시 요청 충돌 | "잠시 후 다시 시도해주세요" |

상세한 에러 코드는 [../api-specification.md#8-에러-코드](../api-specification.md#8-에러-코드) 참조

## 성능 고려사항

### 1. 데이터베이스 최적화
```sql
-- 장바구니 조회 최적화 인덱스
CREATE INDEX idx_cart_user_active ON cart(user_id, is_active);
CREATE INDEX idx_cart_item_cart_box ON cart_item(cart_id, box_type_id);

-- 재고 조회 최적화 인덱스
CREATE INDEX idx_inventory_item ON inventory(item_id);
```

### 2. 동시성 제어
- **Redis 분산 락**: 사용자별 재고 확인 시 10초 타임아웃
- **데이터베이스 트랜잭션**: 장바구니 항목 생성만 트랜잭션 처리
- **낙관적 락**: 재고 테이블의 version 컬럼 활용

### 3. 캐싱 전략
- **상품 정보**: 1시간 캐시 (변경 빈도 낮음)
- **재고 정보**: 실시간 조회 (정확성 중요)
- **사용자 장바구니**: 세션 기반 임시 캐시

## 테스트 시나리오

### 기능 테스트
1. **정상 케이스**
   - 올바른 배합 비율(100%)로 커스텀 박스 추가
   - 선물 포장 옵션 포함 추가
   - 다양한 하루 섭취량(1/2/3회) 설정

2. **예외 케이스**
   - 배합 비율 오류 (합계 ≠ 100%)
   - 재고 부족한 차 선택
   - 장바구니 최대 수량 초과
   - 동일 박스타입 중복 추가

### 성능 테스트
1. **동시성 테스트**: 동일 사용자의 동시 장바구니 추가 요청
2. **부하 테스트**: 1000명 동시 장바구니 추가
3. **트랜잭션 테스트**: 데이터베이스 락 및 롤백 확인

### 통합 테스트
1. **재고 연동 테스트**: 실제 재고 차감과 장바구니 추가 연계
2. **가격 계산 테스트**: 복잡한 배합 비율의 정확한 가격 산정
3. **에러 복구 테스트**: 중간 실패 시 데이터 정합성 확인