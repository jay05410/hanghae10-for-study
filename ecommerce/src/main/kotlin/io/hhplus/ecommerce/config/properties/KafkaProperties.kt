package io.hhplus.ecommerce.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

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
@Component("kafkaProperties")
@ConfigurationProperties(prefix = "kafka")
class KafkaProperties(
    var bootstrapServers: String = "localhost:9092",
    var consumer: ConsumerProperties = ConsumerProperties(),
    var producer: ProducerProperties = ProducerProperties(),
    var topic: TopicProperties = TopicProperties()
)

/**
 * Consumer 설정
 */
class ConsumerProperties(
    var couponGroupId: String = "ecommerce-coupon-consumer-group",
    var dataPlatformGroupId: String = "ecommerce-data-platform-consumer-group",
    var autoOffsetReset: String = "latest"
)

/**
 * Producer 설정
 */
class ProducerProperties(
    var acks: String = "all",
    var retries: Int = 3
)

/**
 * Topic 설정
 */
class TopicProperties(
    var partitions: Int = 3,
    var replicas: Int = 1
)
