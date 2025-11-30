package io.hhplus.ecommerce.common.aop

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.util.CustomSpringELParser
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.redisson.api.RedissonClient

/**
 * Redisson 기반 분산락 AOP
 *
 * 실행 순서: 분산락(Order 0) → 트랜잭션(Order 1)
 * - 락 획득 후 트랜잭션 시작
 * - 트랜잭션 커밋 후 락 해제
 */
@Aspect
@Component
@Order(0) // 트랜잭션보다 먼저 실행 (락을 먼저 획득)
class DistributedLockAop(
    private val redissonClient: RedissonClient
) {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val LOCK_PREFIX = "distributed:lock:"
    }


    @Around("@annotation(io.hhplus.ecommerce.common.annotation.DistributedLock)")
    fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
        val lockKey = generateLockKey(distributedLock.key, joinPoint)
        val rLock = redissonClient.getLock(lockKey)

        var retryCount = 0
        val maxRetries = 3

        while (retryCount <= maxRetries) {
            try {
                log.debug("분산락 획득 시도: {} (waitTime: {}s, leaseTime: {}s, retry: {})",
                    lockKey, distributedLock.waitTime, distributedLock.leaseTime, retryCount)

                val acquired = rLock.tryLock(
                    distributedLock.waitTime,
                    distributedLock.leaseTime,
                    distributedLock.timeUnit
                )

                if (!acquired) {
                    if (retryCount < maxRetries) {
                        retryCount++
                        log.warn("분산락 획득에 실패했습니다. 재시도합니다. key: {} (retry: {}/{})",
                            lockKey, retryCount, maxRetries)
                        Thread.sleep((100 * retryCount).toLong()) // 백오프
                        continue
                    } else {
                        log.error("분산락 획득에 최종 실패했습니다. key: {} (waitTime: {}s 초과)",
                            lockKey, distributedLock.waitTime)
                        throw IllegalStateException("분산락 획득에 실패했습니다. key: $lockKey")
                    }
                }

                log.debug("분산락 획득 성공: {} (retry: {})", lockKey, retryCount)
                return joinPoint.proceed()

            } finally {
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock()
                    log.debug("분산락 해제 성공: {}", lockKey)
                }
            }
        }

        throw IllegalStateException("분산락 획득에 실패했습니다. key: $lockKey")
    }

    private fun generateLockKey(keyExpression: String, joinPoint: ProceedingJoinPoint): String {
        val dynamicKey = if (keyExpression.contains("#")) {
            val methodSignature = joinPoint.signature as MethodSignature
            CustomSpringELParser.getDynamicValue(
                parameterNames = methodSignature.parameterNames,
                args = joinPoint.args,
                expression = keyExpression
            ).toString()
        } else {
            keyExpression
        }
        return "$LOCK_PREFIX$dynamicKey"
    }

}