package io.hhplus.ecommerce.common.aop

import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * 분산 트랜잭션 AOP
 *
 * @DistributedTransaction 어노테이션 처리
 * 기본 @Transactional과 분리하여 분산락과 함께 사용
 *
 */
@Aspect
@Component
@Order(1) // 분산락 이후에 실행 (락 안에서 트랜잭션 수행)
class AopForTransaction(
    transactionManager: PlatformTransactionManager
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    private val readOnlyTransactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
        isReadOnly = true
    }

    @Around("@annotation(distributedTransaction)")
    fun around(joinPoint: ProceedingJoinPoint, distributedTransaction: DistributedTransaction): Any? {
        return if (distributedTransaction.readOnly) {
            readOnlyTransactionTemplate.execute { joinPoint.proceed() }
        } else {
            transactionTemplate.execute { joinPoint.proceed() }
        }
    }
}