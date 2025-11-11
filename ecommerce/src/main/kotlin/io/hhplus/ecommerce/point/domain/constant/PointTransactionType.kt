package io.hhplus.ecommerce.point.domain.constant

/**
 * 포인트 거래 타입
 *
 * 포인트는 결제 수단이 아닌 적립 혜택 시스템:
 * - EARN: 상품 구매 시 일정 비율 적립
 * - USE: 다음 구매 시 포인트로 할인
 * - REFUND: 주문 취소 시 포인트 환불
 * - EXPIRE: 기간 만료로 인한 포인트 소멸
 */
enum class PointTransactionType {
    EARN,    // 적립
    USE,     // 사용
    REFUND,  // 환불
    EXPIRE   // 소멸
}