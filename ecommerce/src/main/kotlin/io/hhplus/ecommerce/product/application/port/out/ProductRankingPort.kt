package io.hhplus.ecommerce.product.application.port.out

/**
 * 상품 판매 랭킹 포트 - 출력 포트 (Hexagonal Architecture)
 *
 * 역할:
 * - 상품 판매량 기반 랭킹 데이터 수집 및 조회 인터페이스 정의
 * - 애플리케이션 계층이 인프라(Redis)에 의존하지 않도록 추상화
 *
 * Redis 자료구조:
 * - Sorted Set 사용 (ZINCRBY, ZREVRANGE, ZRANK, ZSCORE)
 * - 일별/주별/누적 랭킹 분리 관리
 *
 * 구현체:
 * - RedisProductRankingAdapter: Redis Sorted Set 기반 랭킹 저장/조회
 *
 */
interface ProductRankingPort {

    /**
     * 상품 판매량을 랭킹에 반영
     *
     * Redis 명령어: ZINCRBY (Atomic, O(log N))
     * - 일별, 주별, 누적 랭킹 모두 업데이트
     *
     * @param productId 판매된 상품 ID
     * @param quantity 판매 수량
     * @return 해당 상품의 일별 누적 판매량
     */
    fun incrementSalesCount(productId: Long, quantity: Int): Long

    /**
     * 일별 판매 랭킹 Top N 상품 조회
     *
     * Redis 명령어: ZREVRANGE (O(log N + M))
     *
     * @param date 조회할 날짜 (yyyyMMdd 형식)
     * @param limit 조회할 상품 수
     * @return 상품 ID와 판매량 쌍의 리스트 (판매량 내림차순)
     */
    fun getDailyTopProducts(date: String, limit: Int): List<Pair<Long, Long>>

    /**
     * 주간 판매 랭킹 Top N 상품 조회
     *
     * Redis 명령어: ZREVRANGE (O(log N + M))
     *
     * @param yearWeek 조회할 주차 (yyyyWW 형식)
     * @param limit 조회할 상품 수
     * @return 상품 ID와 판매량 쌍의 리스트 (판매량 내림차순)
     */
    fun getWeeklyTopProducts(yearWeek: String, limit: Int): List<Pair<Long, Long>>

    /**
     * 누적 판매 랭킹 Top N 상품 조회
     *
     * Redis 명령어: ZREVRANGE (O(log N + M))
     *
     * @param limit 조회할 상품 수
     * @return 상품 ID와 판매량 쌍의 리스트 (판매량 내림차순)
     */
    fun getTotalTopProducts(limit: Int): List<Pair<Long, Long>>

    /**
     * 특정 상품의 일별 판매량 조회
     *
     * Redis 명령어: ZSCORE (O(1))
     *
     * @param productId 상품 ID
     * @param date 조회할 날짜 (yyyyMMdd 형식)
     * @return 해당 날짜의 판매량 (없으면 0)
     */
    fun getDailySalesCount(productId: Long, date: String): Long

    /**
     * 특정 상품의 일별 랭킹 순위 조회
     *
     * Redis 명령어: ZREVRANK (O(log N))
     *
     * @param productId 상품 ID
     * @param date 조회할 날짜 (yyyyMMdd 형식)
     * @return 랭킹 순위 (0-based, 없으면 null)
     */
    fun getDailyRank(productId: Long, date: String): Long?

    /**
     * 특정 상품의 누적 판매량 조회
     *
     * Redis 명령어: ZSCORE (O(1))
     *
     * @param productId 상품 ID
     * @return 누적 판매량 (없으면 0)
     */
    fun getTotalSalesCount(productId: Long): Long

    /**
     * 일별 랭킹 데이터 조회 (배치 동기화용)
     *
     * Redis → DB 동기화 시 사용
     *
     * @param date 조회할 날짜 (yyyyMMdd 형식)
     * @return 모든 상품의 ID와 판매량 맵
     */
    fun getAllDailySales(date: String): Map<Long, Long>

    /**
     * 일별 랭킹 키 만료 설정
     *
     * @param date 날짜 (yyyyMMdd 형식)
     * @param ttlDays TTL (일 단위)
     */
    fun setDailyKeyExpire(date: String, ttlDays: Long)

    /**
     * 주간 랭킹 키 만료 설정
     *
     * @param yearWeek 주차 (yyyyWW 형식)
     * @param ttlDays TTL (일 단위)
     */
    fun setWeeklyKeyExpire(yearWeek: String, ttlDays: Long)
}
