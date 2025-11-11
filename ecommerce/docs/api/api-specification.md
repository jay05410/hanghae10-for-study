# API 명세서 (API Specification)

## 도메인별 API 문서

본 이커머스 서비스는 도메인별로 분리된 REST API를 제공합니다. 각 도메인별 상세한 API 명세는 아래 링크를 참고하세요.

### 📋 도메인별 API 문서 목록

| 도메인 | 설명 | API 문서 |
|--------|------|----------|
| **Order** | 주문 생성, 조회, 상태 변경 | [Order API](sequences/order/order-api.md) |
| **Product** | 상품 조회, 생성, 수정, 인기상품 | [Product API](sequences/product/product-api.md) |
| **Cart** | 장바구니 관리 (추가/수정/삭제) | [Cart API](sequences/cart/cart-api.md) |
| **User** | 사용자 관리 및 계정 운영 | [User API](sequences/user/user-api.md) |
| **Point** | 포인트 충전, 차감, 내역 조회 | [Point API](sequences/point/point-api.md) |
| **Coupon** | 쿠폰 발급, 사용, 검증 | [Coupon API](sequences/coupon/coupon-api.md) |
| **Payment** | 결제 처리 및 결제 내역 | [Payment API](sequences/payment/process-payment.md) |

### 🔄 시퀀스 다이어그램

각 도메인의 핵심 유스케이스별 시퀀스 다이어그램이 포함되어 있습니다:
- 주문 생성 및 취소 플로우
- 장바구니 상품 추가 및 관리 플로우
- 선착순 쿠폰 발급 플로우
- 인기 상품 조회 및 통계 플로우
- 포인트 충전 및 결제 플로우

### 🏗️ API 공통 사양

### Base URL
```
로컬 개발: http://localhost:8080
```

### API 버전
```
현재 버전: v1
URL 형식: /api/v1/{domain}/{resource}
```

### 공통 응답 구조

#### 성공 응답
```json
{
  "success": true,
  "data": {
    // 실제 응답 데이터
  }
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ORDER001",
    "message": "존재하지 않는 주문입니다"
  }
}
```

### HTTP 상태 코드

| 코드 | 의미 | 사용 케이스 |
|------|------|-------------|
| 200 | OK | 조회/수정/삭제 성공 |
| 201 | Created | 생성 성공 |
| 400 | Bad Request | 잘못된 요청 |
| 401 | Unauthorized | 인증 실패 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (재고 부족 등) |
| 429 | Too Many Requests | 요청 제한 초과 |
| 500 | Internal Server Error | 서버 내부 오류 |


## 📚 기타 문서

- [데이터 모델 설계](data-models.md)
- [비즈니스 정책](business-policies.md)
- [사용자 스토리](user-stories.md)
- [요구사항 명세](requirements.md)
