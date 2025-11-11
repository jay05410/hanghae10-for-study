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
                quantity = teaItem.quantity
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

        val totalQuantity = teaItems.sumOf { it.quantity }
        require(totalQuantity > 0) { "총 차 수량은 0보다 커야 합니다" }

        // 중복 상품 체크
        val productIds = teaItems.map { it.productId }
        require(productIds.size == productIds.distinct().size) { "중복된 차 상품이 있습니다" }
    }

    fun calculateTeaTotalQuantity(teaItems: List<TeaItemRequest>): Int {
        return teaItems.sumOf { it.quantity }
    }

    fun getTeaProductIds(orderItemId: Long): List<Long> {
        return getOrderItemTeas(orderItemId).map { it.productId }
    }
}