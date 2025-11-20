package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.product.domain.constant.ProductStatus

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
    val requiresReservation: Boolean = false  // 선착순/한정판 여부
) {
    val price: Long get() = pricePer100g.toLong()

    /**
     * 상품 사용 가능 여부 확인
     */
    fun isAvailable(): Boolean = status == ProductStatus.ACTIVE

    /**
     * 재고 예약이 필요한 상품인지 확인
     * (선착순, 한정판 등)
     */
    fun requiresStockReservation(): Boolean = requiresReservation


    /**
     * 품절 상태로 변경
     */
    fun markOutOfStock() {
        this.status = ProductStatus.OUT_OF_STOCK
    }

    /**
     * 단종 상태로 변경
     */
    fun markDiscontinued() {
        this.status = ProductStatus.DISCONTINUED
    }

    /**
     * 숨김 상태로 변경
     */
    fun hide() {
        this.status = ProductStatus.HIDDEN
    }

    /**
     * 활성 상태로 복구
     */
    fun restore() {
        this.status = ProductStatus.ACTIVE
    }

    /**
     * 상품 정보 업데이트
     *
     * @param name 변경할 상품명
     * @param description 변경할 상품 설명
     * @param price 변경할 가격
     * @throws IllegalArgumentException 필수 정보 누락 시
     */
    fun updateInfo(
        name: String,
        description: String,
        price: Long
    ) {
        require(name.isNotBlank()) { "상품명은 필수입니다" }
        require(price > 0) { "가격은 0보다 커야 합니다" }

        this.name = name
        this.description = description
        this.pricePer100g = price.toInt()
    }


    companion object {
        /**
         * 기본 상품 생성
         */
        fun create(
            name: String,
            description: String,
            price: Long,
            categoryId: Long
        ): Product {
            require(name.isNotBlank()) { "상품명은 필수입니다" }
            require(price > 0) { "가격은 0보다 커야 합니다" }

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
                origin = "한국"          // 기본값
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
            origin: String
        ): Product {
            require(name.isNotBlank()) { "상품명은 필수입니다" }
            require(pricePer100g > 0) { "가격은 0보다 커야 합니다" }
            require(bagPerWeight > 0) { "티백 용량은 0보다 커야 합니다" }

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
                origin = origin
            )
        }
    }
}

