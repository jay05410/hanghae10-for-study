package io.hhplus.ecommerce.product.application.port.out

import io.hhplus.ecommerce.product.domain.vo.ProductStatsVO

/**
 * 상품 통계 포트 - 출력 포트 (Hexagonal Architecture)
 *
 * 역할:
 * - 상품 통계 데이터 수집 및 조회 인터페이스 정의
 * - 애플리케이션 계층이 인프라에 의존하지 않도록 추상화
 *
 * 구현체:
 * - RedisProductStatisticsAdapter: Redis 기반 실시간 통계 저장/조회
 */
interface ProductStatisticsPort {

    /**
     * 상품 조회 이벤트를 기록하고 최근 10분 조회수를 반환
     *
     * @param productId 조회된 상품 ID
     * @param userId 조회한 사용자 ID (비회원인 경우 null)
     * @return 최근 10분간 조회수
     */
    fun recordViewEvent(productId: Long, userId: Long?): Long

    /**
     * 상품 판매 이벤트를 기록하고 통계 정보를 반환
     *
     * @param productId 판매된 상품 ID
     * @param quantity 판매 수량
     * @param orderId 주문 ID
     * @return 갱신된 상품 통계 정보
     */
    fun recordSalesEvent(productId: Long, quantity: Int, orderId: Long): ProductStatsVO

    /**
     * 상품 찜하기 이벤트를 기록하고 현재 찜 개수를 반환
     *
     * @param productId 찜한 상품 ID
     * @param userId 찜한 사용자 ID
     * @return 현재 찜 개수
     */
    fun recordWishEvent(productId: Long, userId: Long): Long

    /**
     * 상품 찜 취소 이벤트를 기록하고 현재 찜 개수를 반환
     *
     * @param productId 찜 취소한 상품 ID
     * @param userId 찜 취소한 사용자 ID
     * @return 현재 찜 개수
     */
    fun recordUnwishEvent(productId: Long, userId: Long): Long

    /**
     * 최근 10분간 조회수를 조회
     *
     * @param productId 상품 ID
     * @return 최근 10분간 조회수
     */
    fun getLast10MinuteViews(productId: Long): Long

    /**
     * 실시간 통계를 조회 (조회수, 판매량, 찜 개수)
     *
     * @param productId 상품 ID
     * @return Triple(조회수, 판매량, 찜 개수)
     */
    fun getRealTimeStats(productId: Long): Triple<Long, Long, Long>

    /**
     * 실시간 인기 상품 목록을 조회
     *
     * @param limit 조회할 상품 수
     * @return 상품 ID와 조회수 쌍의 리스트
     */
    fun getRealTimePopularProducts(limit: Int): List<Pair<Long, Long>>
}
