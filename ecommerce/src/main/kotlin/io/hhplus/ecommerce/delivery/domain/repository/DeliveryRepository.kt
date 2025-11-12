package io.hhplus.ecommerce.delivery.domain.repository

import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.entity.Delivery

/**
 * Delivery Repository Interface - 도메인 계층
 *
 * 책임:
 * - Delivery 도메인의 영속성 인터페이스 정의
 * - 도메인에 필요한 데이터 접근 메서드 선언
 * - 구현체는 infra 계층에서 담당
 *
 * 주요 특징:
 * - 주문 1건당 배송 1건 (1:1 관계) 보장
 * - 운송장 번호 기반 조회 지원
 * - 배송 상태별 조회 지원
 */
interface DeliveryRepository {
    /**
     * 배송 정보를 저장하거나 업데이트
     *
     * @param delivery 저장할 배송 엔티티
     * @return 저장된 배송 엔티티 (ID가 할당된 상태)
     */
    fun save(delivery: Delivery): Delivery

    /**
     * 배송 ID로 배송 정보 조회
     *
     * @param id 조회할 배송 ID
     * @return 배송 엔티티 (없으면 null)
     */
    fun findById(id: Long): Delivery?

    /**
     * 주문 ID로 배송 정보 조회 (1:1 관계)
     *
     * @param orderId 조회할 주문 ID
     * @return 배송 엔티티 (없으면 null)
     */
    fun findByOrderId(orderId: Long): Delivery?

    /**
     * 여러 주문 ID로 배송 정보 조회
     *
     * @param orderIds 조회할 주문 ID 목록
     * @return 배송 엔티티 목록
     */
    fun findByOrderIdIn(orderIds: List<Long>): List<Delivery>

    /**
     * 운송장 번호로 배송 정보 조회
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 엔티티 (없으면 null)
     */
    fun findByTrackingNumber(trackingNumber: String): Delivery?

    /**
     * 배송 상태로 배송 정보 조회
     *
     * @param status 배송 상태
     * @return 해당 상태의 배송 목록
     */
    fun findByStatus(status: DeliveryStatus): List<Delivery>

    /**
     * 택배사로 배송 정보 조회
     *
     * @param carrier 택배사
     * @return 해당 택배사의 배송 목록
     */
    fun findByCarrier(carrier: String): List<Delivery>
}
