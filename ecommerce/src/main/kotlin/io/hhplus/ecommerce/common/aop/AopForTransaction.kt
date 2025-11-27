package io.hhplus.ecommerce.common.aop

import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 분산 트랜잭션 AOP
 *
 * @DistributedTransaction 어노테이션 처리
 * 기본 @Transactional과 분리하여 분산락과 함께 사용
 */
@Aspect
@Component
@Order(1) // 분산락 이후에 실행 (락 안에서 트랜잭션 수행)
class AopForTransaction {

    @Around("@annotation(distributedTransaction)")
    fun around(joinPoint: ProceedingJoinPoint, distributedTransaction: DistributedTransaction): Any? {
        return if (distributedTransaction.readOnly) {
            proceedReadOnly { joinPoint.proceed() }
        } else {
            proceed(distributedTransaction.propagation) { joinPoint.proceed() }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> proceed(propagation: Propagation = Propagation.REQUIRES_NEW, supplier: () -> T): T {
        return supplier()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun <T> proceedReadOnly(supplier: () -> T): T {
        return supplier()
    }
}