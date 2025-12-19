package io.hhplus.ecommerce.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/**
 * Kafka 토픽 설정
 *
 * 도메인별 토픽 자동 생성:
 * - ecommerce.order: 주문 이벤트 (OrderCreated, OrderCancelled, OrderConfirmed)
 * - ecommerce.payment: 결제 이벤트 (PaymentCompleted, PaymentFailed)
 * - ecommerce.coupon: 쿠폰 이벤트 (CouponIssueRequest)
 * - ecommerce.inventory: 재고 이벤트
 * - ecommerce.data-platform: 외부 데이터 플랫폼 전송용
 */
@Configuration
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = false)
class KafkaTopicConfig {

    @Value("\${kafka.topics.order}")
    private lateinit var orderTopic: String

    @Value("\${kafka.topics.payment}")
    private lateinit var paymentTopic: String

    @Value("\${kafka.topics.coupon}")
    private lateinit var couponTopic: String

    @Value("\${kafka.topics.inventory}")
    private lateinit var inventoryTopic: String

    @Value("\${kafka.topics.data-platform}")
    private lateinit var dataPlatformTopic: String

    companion object {
        private const val DEFAULT_PARTITIONS = 3
        private const val DEFAULT_REPLICAS = 1
    }

    @Bean
    fun orderTopic(): NewTopic {
        return TopicBuilder.name(orderTopic)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
    }

    @Bean
    fun paymentTopic(): NewTopic {
        return TopicBuilder.name(paymentTopic)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
    }

    @Bean
    fun couponTopic(): NewTopic {
        return TopicBuilder.name(couponTopic)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
    }

    @Bean
    fun inventoryTopic(): NewTopic {
        return TopicBuilder.name(inventoryTopic)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
    }

    @Bean
    fun dataPlatformTopic(): NewTopic {
        return TopicBuilder.name(dataPlatformTopic)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
    }
}