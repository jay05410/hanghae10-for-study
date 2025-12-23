package io.hhplus.ecommerce.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Kafka 설정 (중앙화)
 *
 * 모든 Kafka 관련 설정을 한 곳에서 관리:
 * - 연결: bootstrapServers
 * - Consumer: groupId, autoOffsetReset
 * - Producer: acks, retries
 * - Topic: partitions, replicas
 *
 * 토픽명은 Topics object에서 상수로 관리
 * @see io.hhplus.ecommerce.common.messaging.Topics
 */
@ConfigurationProperties(prefix = "kafka")
data class KafkaProperties(
    val bootstrapServers: String = "localhost:9092",
    val consumer: ConsumerProperties = ConsumerProperties(),
    val producer: ProducerProperties = ProducerProperties(),
    val topic: TopicProperties = TopicProperties()
)

/**
 * Consumer 설정
 */
data class ConsumerProperties(
    val couponGroupId: String = "ecommerce-coupon-consumer-group",
    val dataPlatformGroupId: String = "ecommerce-data-platform-consumer-group",
    val autoOffsetReset: String = "latest"
)

/**
 * Producer 설정
 */
data class ProducerProperties(
    val acks: String = "all",
    val retries: Int = 3
)

/**
 * Topic 설정
 */
data class TopicProperties(
    val partitions: Int = 3,
    val replicas: Int = 1
)
