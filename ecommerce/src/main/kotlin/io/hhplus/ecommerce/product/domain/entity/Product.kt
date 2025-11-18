package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import java.time.LocalDateTime

/**
 * 상품 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 상품 정보 관리
 * - 상품 상태 전환 및 검증
 * - 상품 정보 업데이트 및 재고 관리
 *
 * 비즈니스 규칙:
 * - 상품명은 필수이며 빈 값일 수 없음
 * - 가격은 0보다 커야 함
 * - 티백 용량은 0보다 커야 함
 * - 상품 정보는 가변 객체로 관리 (직접 필드 수정)
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/ProductJpaEntity에서 처리됩니다.
 */
data class Product(
    val id: Long = 0,
    val categoryId: Long,
    var name: String,
    var description: String,
    val caffeineType: String,
    val tasteProfile: String,
    val aromaProfile: String,
    val colorProfile: String,
    val bagPerWeight: Int,
    var pricePer100g: Int,
    val ingredients: String,
    val origin: String,
    var status: ProductStatus = ProductStatus.ACTIVE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0,
    val deletedAt: LocalDateTime? = null
) {
    val price: Long get() = pricePer100g.toLong()

    /**
     * 상품 사용 가능 여부 확인
     */
    fun isAvailable(): Boolean = status == ProductStatus.ACTIVE

    /**
     * 품절 상태로 변경
     *
     * @param updatedBy 변경자 ID
     */
    fun markOutOfStock(updatedBy: Long) {
        this.status = ProductStatus.OUT_OF_STOCK
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 단종 상태로 변경
     *
     * @param updatedBy 변경자 ID
     */
    fun markDiscontinued(updatedBy: Long) {
        this.status = ProductStatus.DISCONTINUED
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 숨김 상태로 변경
     *
     * @param updatedBy 변경자 ID
     */
    fun hide(updatedBy: Long) {
        this.status = ProductStatus.HIDDEN
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 활성 상태로 복구
     *
     * @param updatedBy 변경자 ID
     */
    fun restore(updatedBy: Long) {
        this.status = ProductStatus.ACTIVE
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 상품 정보 업데이트
     *
     * @param name 변경할 상품명
     * @param description 변경할 상품 설명
     * @param price 변경할 가격
     * @param updatedBy 변경자 ID
     * @throws IllegalArgumentException 필수 정보 누락 시
     */
    fun updateInfo(
        name: String,
        description: String,
        price: Long,
        updatedBy: Long
    ) {
        require(name.isNotBlank()) { "상품명은 필수입니다" }
        require(price > 0) { "가격은 0보다 커야 합니다" }

        this.name = name
        this.description = description
        this.pricePer100g = price.toInt()
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }


    companion object {
        /**
         * 기본 상품 생성
         */
        fun create(
            name: String,
            description: String,
            price: Long,
            categoryId: Long,
            createdBy: Long
        ): Product {
            require(name.isNotBlank()) { "상품명은 필수입니다" }
            require(price > 0) { "가격은 0보다 커야 합니다" }

            val now = LocalDateTime.now()
            return Product(
                categoryId = categoryId,
                name = name,
                description = description,
                caffeineType = "MEDIUM", // 기본값
                tasteProfile = "MILD",    // 기본값
                aromaProfile = "FRESH",   // 기본값
                colorProfile = "GOLDEN",  // 기본값
                bagPerWeight = 3,        // 기본값
                pricePer100g = price.toInt(),
                ingredients = "차 잎 100%", // 기본값
                origin = "한국",          // 기본값
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }

        /**
         * 상세 정보를 포함한 상품 생성
         */
        fun createDetailed(
            categoryId: Long,
            name: String,
            description: String,
            caffeineType: String,
            tasteProfile: String,
            aromaProfile: String,
            colorProfile: String,
            bagPerWeight: Int,
            pricePer100g: Int,
            ingredients: String,
            origin: String,
            createdBy: Long
        ): Product {
            require(name.isNotBlank()) { "상품명은 필수입니다" }
            require(pricePer100g > 0) { "가격은 0보다 커야 합니다" }
            require(bagPerWeight > 0) { "티백 용량은 0보다 커야 합니다" }

            val now = LocalDateTime.now()
            return Product(
                categoryId = categoryId,
                name = name,
                description = description,
                caffeineType = caffeineType,
                tasteProfile = tasteProfile,
                aromaProfile = aromaProfile,
                colorProfile = colorProfile,
                bagPerWeight = bagPerWeight,
                pricePer100g = pricePer100g,
                ingredients = ingredients,
                origin = origin,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

