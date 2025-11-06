package io.hhplus.ecommerce.common.lock

import io.hhplus.ecommerce.common.errorcode.CommonErrorCode
import io.hhplus.ecommerce.common.exception.BusinessException
import org.slf4j.event.Level
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 동시성 제어 - 키별 락 매니저
 */
@Component
class LockManager {

    private val logger = LoggerFactory.getLogger(LockManager::class.java)
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    /**
     * 키에 해당하는 락을 사용하여 작업 실행
     *
     * @param key 락 키 (예: "user:123", "product:456")
     * @param timeout 락 획득 대기 시간 (기본: 3초)
     * @param action 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> withLock(
        key: String,
        timeout: Duration = Duration.ofSeconds(3),
        action: () -> T
    ): T {
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }

        try {
            val acquired = lock.tryLock(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!acquired) {
                logger.warn("Failed to acquire lock for key: {}", key)
                throw LockAcquisitionException(key)
            }

            try {
                logger.debug("Lock acquired for key: {}", key)
                return action()
            } finally {
                lock.unlock()
                logger.debug("Lock released for key: {}", key)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw LockAcquisitionException(key)
        }
    }

    /**
     * 사용하지 않는 락 정리
     */
    fun cleanupUnusedLocks() {
        locks.entries.removeIf { (key, lock) ->
            if (!lock.isLocked && lock.queueLength == 0) {
                logger.debug("Removing unused lock for key: {}", key)
                true
            } else {
                false
            }
        }
    }
}

/**
 * 락 획득 실패 예외
 */
class LockAcquisitionException(lockKey: String) : BusinessException(
    errorCode = CommonErrorCode.CONCURRENCY_ERROR,
    message = CommonErrorCode.CONCURRENCY_ERROR.withParams("lockKey" to lockKey),
    logLevel = Level.WARN,
    data = mapOf("lockKey" to lockKey)
)