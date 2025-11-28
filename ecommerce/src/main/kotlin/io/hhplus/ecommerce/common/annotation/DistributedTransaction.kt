package io.hhplus.ecommerce.common.annotation

import org.springframework.transaction.annotation.Propagation

/**
 * 분산락과 함께 사용할 트랜잭션 어노테이션
 *
 * @param propagation 트랜잭션 전파 속성
 * @param readOnly 읽기 전용 여부
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedTransaction(
    val propagation: Propagation = Propagation.REQUIRES_NEW,
    val readOnly: Boolean = false
)