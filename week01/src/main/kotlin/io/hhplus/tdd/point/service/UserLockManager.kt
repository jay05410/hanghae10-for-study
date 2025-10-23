package io.hhplus.tdd.point.service

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * 동시성 제어 - 사용자 ID별 락
 */
@Component
class UserLockManager {
    private val userLocks = ConcurrentHashMap<Long, ReentrantLock>()

    /**
     * 사용자 ID에 해당하는 락을 반환
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 락 인스턴스
     */
    fun getLock(userId: Long): ReentrantLock {
        return userLocks.computeIfAbsent(userId) { ReentrantLock() }
    }

    /**
     * 사용자 작업을 락과 실행
     *
     * @param userId 사용자 ID
     * @param action 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> executeWithLock(userId: Long, action: () -> T): T {
        val lock = getLock(userId)
        lock.lock()
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }
}