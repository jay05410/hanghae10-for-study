package io.hhplus.ecommerce.point.presentation.dto

import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 사용자 포인트 정보 응답 DTO - Presentation Layer
 *
 * 역할:
 * - UserPoint 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출
 * - 도메인 객체와 API 스펙 간의 격리
 */
@Schema(description = "사용자 포인트 정보")
data class UserPointResponse(
    @Schema(description = "포인트 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "현재 포인트 잔액", example = "5000")
    val balance: Long,

    @Schema(description = "낙관적 락 버전", example = "3")
    val version: Int
)

/**
 * 포인트 이력 정보 응답 DTO - Presentation Layer
 */
@Schema(description = "포인트 이력 정보")
data class PointHistoryResponse(
    @Schema(description = "이력 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 ID", example = "100")
    val userId: Long,

    @Schema(description = "거래 금액", example = "1000")
    val amount: Long,

    @Schema(description = "거래 유형", example = "CHARGE", allowableValues = ["CHARGE", "DEDUCT"])
    val transactionType: PointTransactionType,

    @Schema(description = "거래 전 잔액", example = "4000")
    val balanceBefore: Long,

    @Schema(description = "거래 후 잔액", example = "5000")
    val balanceAfter: Long,

    @Schema(description = "연관된 주문 ID (선택)", example = "12345")
    val orderId: Long?,

    @Schema(description = "거래 설명 (선택)", example = "상품 구매")
    val description: String?
)

/**
 * UserPoint -> UserPointResponse 변환
 */
fun UserPoint.toResponse(): UserPointResponse = UserPointResponse(
    id = this.id,
    userId = this.userId,
    balance = this.balance.value,
    version = this.version
)

/**
 * PointHistory -> PointHistoryResponse 변환
 */
fun PointHistory.toResponse(): PointHistoryResponse = PointHistoryResponse(
    id = this.id,
    userId = this.userId,
    amount = this.amount,
    transactionType = this.transactionType,
    balanceBefore = this.balanceBefore,
    balanceAfter = this.balanceAfter,
    orderId = this.orderId,
    description = this.description
)
