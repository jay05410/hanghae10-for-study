package io.hhplus.ecommerce.order.application

import io.hhplus.ecommerce.order.domain.entity.OrderItemTea
import io.hhplus.ecommerce.order.domain.repository.OrderItemTeaRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 아이템 차 구성 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 주문 아이템의 차 구성 관리 전담
 * - 차 구성 데이터의 생명주기 관리
 * - 주문용 차 구성 유효성 검증
 *
 * 책임:
 * - 주문 아이템별 차 구성 저장, 조회, 삭제
 * - 차 구성 입력 데이터 검증 및 계산
 * - 주문에 특화된 차 구성 비즈니스 로직
 */
@Service
class OrderItemTeaService(
    private val orderItemTeaRepository: OrderItemTeaRepository
) {

    @Transactional
    fun saveOrderItemTeas(orderItemId: Long, teaItems: List<TeaItemRequest>): List<OrderItemTea> {
        return teaItems.map { teaItem ->
            val orderItemTea = OrderItemTea.create(
                orderItemId = orderItemId,
                productId = teaItem.productId,
                productName = "Product ${teaItem.productId}", // TODO: 실제 상품명으로 교체
                categoryName = "Category", // TODO: 실제 카테고리명으로 교체
                selectionOrder = teaItem.selectionOrder,
                ratioPercent = teaItem.ratioPercent,
                unitPrice = 1000 // TODO: 실제 단가로 교체
            )
            orderItemTeaRepository.save(orderItemTea)
        }
    }

    fun getOrderItemTeas(orderItemId: Long): List<OrderItemTea> {
        return orderItemTeaRepository.findByOrderItemId(orderItemId)
    }

    @Transactional
    fun deleteOrderItemTeas(orderItemId: Long) {
        orderItemTeaRepository.deleteByOrderItemId(orderItemId)
    }

    fun validateTeaItemsForOrder(teaItems: List<TeaItemRequest>) {
        require(teaItems.isNotEmpty()) { "주문 차 구성은 최소 1개 이상이어야 합니다" }
        require(teaItems.size <= 3) { "주문 차 구성은 최대 3개까지 가능합니다" }

        val totalRatio = teaItems.sumOf { it.ratioPercent }
        require(totalRatio == 100) { "총 배합 비율은 100%가 되어야 합니다. 현재: ${totalRatio}%" }

        // 중복 상품 체크
        val productIds = teaItems.map { it.productId }
        require(productIds.size == productIds.distinct().size) { "중복된 차 상품이 있습니다" }

        // 선택 순서 검증
        val orders = teaItems.map { it.selectionOrder }.sorted()
        require(orders == (1..teaItems.size).toList()) { "선택 순서는 1부터 연속적이어야 합니다" }
    }

    fun calculateTeaTotalRatio(teaItems: List<TeaItemRequest>): Int {
        return teaItems.sumOf { it.ratioPercent }
    }

    fun getTeaProductIds(orderItemId: Long): List<Long> {
        return getOrderItemTeas(orderItemId).map { it.productId }
    }
}