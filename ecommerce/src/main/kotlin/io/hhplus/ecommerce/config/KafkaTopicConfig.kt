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
 * 애플리케이션 시작 시 토픽 자동 생성
 */
@Configuration
@ConditionalOnProperty(name = ["kafka.enabled"], havingValue = "true", matchIfMissing = false)
class KafkaTopicConfig {

    @Value("\${kafka.topics.order-events}")
    private lateinit var orderEventsTopic: String

    @Value("\${kafka.topics.payment-events}")
    private lateinit var paymentEventsTopic: String

    @Value("\${kafka.topics.data-platform}")
    private lateinit var dataPlatformTopic: String

    companion object {
        private const val DEFAULT_PARTITIONS = 3
        private const val DEFAULT_REPLICAS = 1
    }

    @Bean
    fun orderEventsTopic(): NewTopic {
        return TopicBuilder.name(orderEventsTopic)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build()
    }

    @Bean
    fun paymentEventsTopic(): NewTopic {
        return TopicBuilder.name(paymentEventsTopic)
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
