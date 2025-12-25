package io.hhplus.ecommerce.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis 설정 (Redis 연결 및 직렬화 설정)
 */
@Configuration
class RedisConfig {

    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var redisHost: String

    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 6379

    @Value("\${spring.data.redis.password:}")
    private var redisPassword: String = ""

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        val address = "redis://$redisHost:$redisPort"
        config.useSingleServer()
            .setAddress(address)
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10)
            .setIdleConnectionTimeout(30000)
            .setConnectTimeout(10000)
            .setTimeout(10000)
            .setRetryAttempts(3)
            .setRetryInterval(1500)

        // 패스워드가 설정된 경우만 적용
        if (redisPassword.isNotBlank()) {
            config.useSingleServer().password = redisPassword
        }

        return Redisson.create(config)
    }

    /**
     * Redis 전용 ObjectMapper (Kotlin data class + 타입 정보 포함)
     *
     * RedisTemplate, RedisCacheManager 등에서 공유 사용
     *
     * 기본 ObjectMapper(@Bean)와 별도로 생성하는 이유:
     * 1. activateDefaultTyping: Redis에서 다형성 역직렬화를 위해 타입 정보 포함
     * 2. PolymorphicTypeValidator: io.hhplus.ecommerce 패키지만 허용 (보안)
     * 3. REST API 응답에 타입 정보 노출 방지
     */
    @Bean("redisObjectMapper")
    fun redisObjectMapper(): ObjectMapper {
        val typeValidator: PolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Any::class.java)
            .allowIfSubType("io.hhplus.ecommerce")
            .build()

        return ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
        }
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        @Qualifier("redisObjectMapper") redisObjectMapper: ObjectMapper
    ): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
            valueSerializer = jsonSerializer
            hashValueSerializer = jsonSerializer
        }
    }
}
