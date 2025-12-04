package io.hhplus.ecommerce.coupon.application.port.out

/**
 * 선착순 쿠폰 발급 포트 - out port
 *
 * 역할:
 * - 선착순 쿠폰 발급의 Redis 기반 동시성 제어 인터페이스
 * - SADD + INCR + Soldout Flag 패턴으로 정확한 수량 제어
 * - 애플리케이션 계층이 인프라(Redis)에 의존하지 않도록 추상화
 *
 * 핵심 자료구조:
 * - SET: 발급된 유저 목록 (SADD 원자성으로 중복 방지)
 * - Counter: 발급 순번 (INCR 원자성으로 정확한 수량 제어)
 * - Soldout Flag: 매진 상태 (조기 종료로 불필요한 연산 방지)
 * - ZSET: 발급 대기열 (선착순 순서 보장)
 */
interface CouponIssuePort {

    /**
     * 선착순 쿠폰 발급 시도 (기본 방식)
     *
     * 처리 흐름:
     * 1. SADD로 원자적 중복 체크 + 등록
     * 2. SCARD로 현재 발급 수 확인
     * 3. 초과 시 SREM으로 롤백
     * 4. 성공 시 ZADD로 대기열 등록
     *
     * 시간복잡도: O(1) + O(1) + O(log N) = O(log N)
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @param maxQuantity 최대 발급 수량
     * @return 발급 결과
     */
    fun tryIssue(couponId: Long, userId: Long, maxQuantity: Int): CouponIssueResult

    /**
     * 대기열에서 다음 유저를 가져옴 (배치 처리용)
     *
     * Redis 명령어: ZPOPMIN (O(log N))
     *
     * @param couponId 쿠폰 ID
     * @param count 가져올 유저 수
     * @return 유저 ID 목록 (선착순)
     */
    fun popFromQueue(couponId: Long, count: Int): List<Long>

    /**
     * 현재 발급된 수량 조회
     *
     * Redis 명령어: SCARD (O(1))
     *
     * @param couponId 쿠폰 ID
     * @return 발급된 유저 수
     */
    fun getIssuedCount(couponId: Long): Long

    /**
     * 대기열 크기 조회
     *
     * Redis 명령어: ZCARD (O(1))
     *
     * @param couponId 쿠폰 ID
     * @return 대기열에 있는 유저 수
     */
    fun getQueueSize(couponId: Long): Long

    /**
     * 특정 유저가 이미 발급받았는지 확인
     *
     * Redis 명령어: SISMEMBER (O(1))
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 여부
     */
    fun isAlreadyIssued(couponId: Long, userId: Long): Boolean

    /**
     * 쿠폰 발급 데이터 초기화 (테스트용)
     *
     * @param couponId 쿠폰 ID
     */
    fun clearIssueData(couponId: Long)

    /**
     * 쿠폰 최대 발급 수량 설정
     *
     * Redis에 maxQuantity를 저장하여 캐시 의존성 제거.
     * 쿠폰 생성/수정 시 호출.
     *
     * @param couponId 쿠폰 ID
     * @param maxQuantity 최대 발급 수량
     */
    fun setMaxQuantity(couponId: Long, maxQuantity: Int)

    /**
     * 쿠폰 최대 발급 수량 조회
     *
     * @param couponId 쿠폰 ID
     * @return 최대 발급 수량 (없으면 null)
     */
    fun getMaxQuantity(couponId: Long): Int?

    /**
     * 선착순 쿠폰 발급 시도 (Redis에서 maxQuantity 조회)
     *
     * maxQuantity를 파라미터로 받지 않고 Redis에서 직접 조회.
     * 캐시 불일치 문제 해결.
     *
     * @param couponId 쿠폰 ID
     * @param userId 사용자 ID
     * @return 발급 결과
     */
    fun tryIssueWithStoredQuantity(couponId: Long, userId: Long): CouponIssueResult
}

/**
 * 쿠폰 발급 결과
 */
enum class CouponIssueResult {
    /** 발급 대기열 등록 성공 */
    QUEUED,

    /** 이미 발급받음 */
    ALREADY_ISSUED,

    /** 수량 소진 */
    SOLD_OUT
}
