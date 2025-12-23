package io.hhplus.ecommerce.config

import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.config.properties.KafkaProperties
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Kafka 통합 설정
 *
 * Producer, Consumer, Topic 설정을 한 곳에서 관리
 * 설정값은 KafkaProperties에서 중앙 관리
 *
 * @see io.hhplus.ecommerce.config.properties.KafkaProperties
 * @see io.hhplus.ecommerce.common.messaging.Topics
 */
@Configuration
@EnableConfigurationProperties(KafkaProperties::class)
class KafkaConfig(
    private val kafkaProperties: KafkaProperties
) {

    // ===========================================
    // Producer
    // ===========================================

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to kafkaProperties.producer.acks,
            ProducerConfig.RETRIES_CONFIG to kafkaProperties.producer.retries,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.LINGER_MS_CONFIG to 5,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy"
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> = KafkaTemplate(producerFactory())

    // ===========================================
    // Consumer
    // ===========================================

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaProperties.consumer.autoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 100
        )
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setConcurrency(3)
        return factory
    }

    // ===========================================
    // Topics
    // ===========================================

    @Bean
    fun orderTopic(): NewTopic = buildTopic(Topics.ORDER)

    @Bean
    fun paymentTopic(): NewTopic = buildTopic(Topics.PAYMENT)

    @Bean
    fun couponTopic(): NewTopic = buildTopic(Topics.COUPON)

    @Bean
    fun inventoryTopic(): NewTopic = buildTopic(Topics.INVENTORY)

    @Bean
    fun dataPlatformTopic(): NewTopic = buildTopic(Topics.DATA_PLATFORM)

    private fun buildTopic(name: String): NewTopic {
        return TopicBuilder.name(name)
            .partitions(kafkaProperties.topic.partitions)
            .replicas(kafkaProperties.topic.replicas)
            .build()
    }
}
