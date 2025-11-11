package io.hhplus.ecommerce.inventory.domain.constant

enum class ReservationStatus {
    RESERVED,   // 예약됨
    CONFIRMED,  // 결제 완료로 확정됨
    CANCELLED,  // 사용자가 취소함
    EXPIRED     // 시간 만료로 자동 해제됨
}