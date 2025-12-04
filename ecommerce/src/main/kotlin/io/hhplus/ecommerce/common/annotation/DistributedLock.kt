package io.hhplus.ecommerce.common.annotation

/**
 * 분산락 적용을 위한 어노테이션
 *
 * @param key 락 키 (SpEL 표현식 지원)
 * @param waitTime 락 획득 대기 시간 (초)
 * @param leaseTime 락 보유 시간 (초)
 * @param timeUnit 시간 단위
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTime: Long = 10L,
    val leaseTime: Long = 30L,
    val timeUnit: java.util.concurrent.TimeUnit = java.util.concurrent.TimeUnit.SECONDS
)