package io.hhplus.ecommerce.order.application

import io.hhplus.ecommerce.common.queue.QueueProcessor
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderQueueRequest
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.usecase.OrderCommandUseCase
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 주문 생성 Queue Processor
 */
@Component
class OrderQueueProcessor(
    private val orderQueueService: OrderQueueService,
    private val orderCommandUseCase: OrderCommandUseCase
) : QueueProcessor<OrderQueueRequest, Order> {

    private val logger = KotlinLogging.logger {}

    override fun dequeue(): OrderQueueRequest? {
        return orderQueueService.dequeue()
    }

    override fun process(item: OrderQueueRequest): Order {
        logger.info(
            "주문 생성 Queue 처리 시작 - queueId: {}, userId: {}, itemCount: {}, position: {}",
            item.queueId, item.userId, item.items.size, item.queuePosition
        )

        // OrderQueueRequest를 CreateOrderRequest로 변환
        val createOrderRequest = CreateOrderRequest(
            userId = item.userId,
            items = item.items,
            deliveryAddress = item.deliveryAddress,
            usedCouponId = item.usedCouponId
        )

        return orderCommandUseCase.processOrderDirectly(createOrderRequest)
    }

    override fun onSuccess(item: OrderQueueRequest, result: Order) {
        orderQueueService.completeQueue(
            queueId = item.queueId,
            orderId = result.id
        )

        logger.info(
            "주문 생성 성공 - queueId: {}, userId: {}, orderId: {}",
            item.queueId, item.userId, result.id
        )
    }

    override fun onFailure(item: OrderQueueRequest, error: Exception) {
        val failureReason = error.message ?: "알 수 없는 오류"

        orderQueueService.failQueue(
            queueId = item.queueId,
            reason = failureReason
        )

        logger.warn(
            "주문 생성 실패 - queueId: {}, userId: {}, reason: {}",
            item.queueId, item.userId, failureReason, error
        )
    }
}