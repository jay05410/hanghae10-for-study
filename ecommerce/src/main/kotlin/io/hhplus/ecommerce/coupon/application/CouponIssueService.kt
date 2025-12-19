package io.hhplus.ecommerce.coupon.application

import io.hhplus.ecommerce.common.cache.CacheInvalidationPublisher
import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.common.messaging.MessagePublisher
import io.hhplus.ecommerce.common.outbox.payload.CouponIssueRequestPayload
import io.hhplus.ecommerce.coupon.application.port.out.CouponIssuePort
import io.hhplus.ecommerce.coupon.application.port.out.CouponIssueResult
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.vo.CouponCacheInfo
import io.hhplus.ecommerce.coupon.exception.CouponException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 선착순 쿠폰 발급 서비스
 *
 * 역할:
 * - 선착순 쿠폰 발급 요청 처리
 * - CouponIssuePort를 통한 Redis 기반 동시성 제어
 *
 * 동시성 제어 패턴: SADD + INCR + Soldout Flag
 * - SET: 발급된 유저 목록 (SADD 원자성으로 중복 방지)
 * - Counter: 발급 순번 (INCR 원자성으로 정확한 수량 제어)
 * - Soldout Flag: 매진 상태 (조기 종료로 불필요한 연산 방지)
 * - ZSET: 발급 대기열 (순번 score로 선착순 보장)
 */
@Service
class CouponIssueService(
    private val couponIssuePort: CouponIssuePort,
    private val couponRepository: CouponRepository,
    private val cacheInvalidationPublisher: CacheInvalidationPublisher,
    private val messagePublisher: MessagePublisher,
    @Value("\${kafka.topics.coupon-issue:ecommerce.coupon.issue}")
    private val couponIssueTopic: String
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 쿠폰 발급 요청
     *
     * 캐시 전략:
     * - 유효기간 검증: CouponCacheInfo (호출자에서 검증 완료)
     * - 수량 제어: Redis maxQuantity (동적 데이터)
     *
     * maxQuantity 초기화:
     * - Redis에 없으면 DB에서 조회하여 설정 (최초 발급 시)
     * - 이후에는 Redis에서만 조회 (캐시 불일치 문제 해결)
     *
     * @param userId 사용자 ID
     * @param couponInfo 캐시된 쿠폰 정보 (totalQuantity 미포함)
     * @throws CouponException.AlreadyRequested 이미 발급 요청한 경우
     * @throws CouponException.IssueSoldOut 수량이 소진된 경우
     */
    fun requestIssue(userId: Long, couponInfo: CouponCacheInfo) {
        // Redis에 maxQuantity가 없으면 DB에서 조회하여 설정
        if (couponIssuePort.getMaxQuantity(couponInfo.id) == null) {
            val coupon = couponRepository.findById(couponInfo.id)
                ?: throw CouponException.CouponNotFound(couponInfo.id)
            couponIssuePort.setMaxQuantity(couponInfo.id, coupon.totalQuantity)
        }

        // Redis에서 maxQuantity를 직접 조회하여 발급 처리
        val result = couponIssuePort.tryIssueWithStoredQuantity(
            couponId = couponInfo.id,
            userId = userId
        )

        when (result) {
            CouponIssueResult.QUEUED -> {
                // Kafka로 발급 요청 발행
                val payload = CouponIssueRequestPayload(
                    couponId = couponInfo.id,
                    userId = userId,
                    couponName = couponInfo.name,
                    requestedAt = System.currentTimeMillis()
                )

                messagePublisher.publish(
                    topic = couponIssueTopic,
                    key = couponInfo.id.toString(),
                    payload = json.encodeToString(payload)
                )

                logger.info(
                    "[CouponIssueService] 발급 요청 Kafka 발행: couponId={}, userId={}",
                    couponInfo.id, userId
                )
            }
            CouponIssueResult.ALREADY_ISSUED -> throw CouponException.AlreadyRequested(userId, couponInfo.name)
            CouponIssueResult.SOLD_OUT -> throw CouponException.IssueSoldOut(couponInfo.name)
        }
    }

    /**
     * 쿠폰 최대 발급 수량 업데이트
     *
     * 쿠폰 생성/수정 시 호출하여:
     * 1. Redis에 maxQuantity 저장
     * 2. Pub/Sub으로 모든 서버의 로컬 캐시 무효화
     * 3. soldout 플래그 초기화 (수량 증가 시 재발급 가능하도록)
     *
     * @param couponId 쿠폰 ID
     * @param maxQuantity 최대 발급 수량
     */
    fun updateMaxQuantity(couponId: Long, maxQuantity: Int) {
        // 1. Redis에 maxQuantity 저장
        couponIssuePort.setMaxQuantity(couponId, maxQuantity)

        // 2. Pub/Sub으로 모든 서버의 COUPON_INFO 로컬 캐시 무효화
        cacheInvalidationPublisher.publishInvalidation(CacheNames.COUPON_INFO, couponId)
    }

    /**
     * 대기열에서 다음 유저들을 가져옴 (Worker용)
     *
     * ZPOPMIN으로 선착순(낮은 timestamp) 순서대로 꺼냄.
     *
     * @param couponId 쿠폰 ID
     * @param count 가져올 유저 수
     * @return 유저 ID 목록
     */
    fun popPendingUsers(couponId: Long, count: Int = 1): List<Long> {
        return couponIssuePort.popFromQueue(couponId, count)
    }

    /**
     * 특정 유저가 이미 발급받았는지 확인
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 여부
     */
    fun isAlreadyIssued(couponId: Long, userId: Long): Boolean {
        return couponIssuePort.isAlreadyIssued(couponId, userId)
    }

    /**
     * 현재 발급된 수량 조회
     *
     * @param couponId 쿠폰 ID
     * @return 발급된 유저 수
     */
    fun getIssuedCount(couponId: Long): Long {
        return couponIssuePort.getIssuedCount(couponId)
    }

    /**
     * 대기열 크기 조회
     *
     * @param couponId 쿠폰 ID
     * @return 대기열에 있는 유저 수
     */
    fun getPendingCount(couponId: Long): Long {
        return couponIssuePort.getQueueSize(couponId)
    }

    /**
     * 쿠폰 발급 데이터 초기화 (테스트용)
     *
     * @param couponId 쿠폰 ID
     */
    fun clearIssueData(couponId: Long) {
        couponIssuePort.clearIssueData(couponId)
    }
}
