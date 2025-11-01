# 오늘 한 차 Mock API Server

JSON Server를 사용한 Mock API 서버입니다.

## 시작하기

### 설치
```bash
npm install
```

### 실행
```bash
# 개발 모드 (변경사항 자동 반영)
npm run dev

# 일반 모드
npm start
```

서버가 `http://localhost:3001`에서 실행됩니다.

## API 엔드포인트

### 상품
- `GET /products` - 상품 목록
- `GET /products/:id` - 상품 상세
- `GET /conditions` - 컨디션 옵션
- `GET /moods` - 기분 옵션
- `GET /scents` - 향 옵션
- `GET /combinations` - 조합 목록
- `GET /combinations/:id` - 조합 상세

### 장바구니
- `GET /cartItems?userId=1` - 장바구니 조회
- `POST /cartItems` - 장바구니 추가
- `DELETE /cartItems/:id` - 장바구니 삭제

### 주문
- `GET /orders?userId=1` - 주문 목록
- `GET /orders/:id` - 주문 상세
- `POST /orders` - 주문 생성
- `PATCH /orders/:id` - 주문 수정 (취소)

### 쿠폰
- `GET /coupons` - 쿠폰 목록
- `GET /userCoupons?userId=1` - 보유 쿠폰
- `POST /userCoupons` - 쿠폰 발급

### 포인트
- `GET /balanceHistory?userId=1` - 포인트 내역
- `POST /balanceHistory` - 포인트 충전/사용

## 예시 요청

### 상품 목록 조회
```bash
curl http://localhost:3001/products
```

### 장바구니 추가
```bash
curl -X POST http://localhost:3001/cartItems \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "combinationId": 1,
    "quantity": 1,
    "createdAt": "2025-10-31T10:00:00Z"
  }'
```

### 주문 생성
```bash
curl -X POST http://localhost:3001/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderNumber": "ORD-20251031-002",
    "userId": 1,
    "totalAmount": 29000,
    "finalAmount": 29000,
    "status": "PAID",
    "deliveryAddress": {
      "recipient": "김민지",
      "phone": "010-1234-5678",
      "zipCode": "06234",
      "address": "서울시 강남구 테헤란로 123",
      "addressDetail": "456호"
    },
    "orderedAt": "2025-10-31T15:00:00Z"
  }'
```

## 기능

### 필터링
```bash
# 특정 사용자의 주문
GET /orders?userId=1

# 상태별 필터
GET /orders?status=PAID

# 조합 조건
GET /orders?status=PAID&_sort=orderedAt&_order=desc
```

### 페이징
```bash
GET /orders?_page=1&_limit=10
```

### 정렬
```bash
GET /orders?_sort=orderedAt&_order=desc
```

### 검색
```bash
GET /products?name_like=프리미엄
```

## 주의사항

- 이 서버는 **개발/테스트 용도**입니다
- 데이터는 메모리에만 저장되며, 재시작 시 초기화됩니다
- 인증/권한 검증이 없습니다
- 비즈니스 로직이 없습니다 (재고 차감, 쿠폰 검증 등)

## 데이터 초기화

서버를 재시작하면 `db.json` 파일의 초기 데이터로 복원됩니다.