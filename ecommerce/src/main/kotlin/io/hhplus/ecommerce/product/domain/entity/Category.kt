package io.hhplus.ecommerce.product.domain.entity

/**
 * 카테고리 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 카테고리 정보 관리
 * - 카테고리 상태 검증
 *
 * 비즈니스 규칙:
 * - 카테고리명은 필수이며 빈 값일 수 없음
 * - 카테고리 설명은 필수
 * - 표시 순서는 0 이상이어야 함
 */
data class Category(
    val id: Long = 0,
    var name: String,
    var description: String,
    var displayOrder: Int = 0
) {

    /**
     * 카테고리 정보 업데이트
     *
     * @param name 변경할 카테고리명
     * @param description 변경할 설명
     * @param displayOrder 변경할 표시 순서
     */
    fun updateInfo(
        name: String,
        description: String,
        displayOrder: Int
    ) {
        require(name.isNotBlank()) { "카테고리명은 필수입니다" }
        require(description.isNotBlank()) { "카테고리 설명은 필수입니다" }
        require(displayOrder >= 0) { "표시 순서는 0 이상이어야 합니다" }

        this.name = name
        this.description = description
        this.displayOrder = displayOrder
    }

    companion object {
        /**
         * 카테고리 생성
         */
        fun create(
            name: String,
            description: String,
            displayOrder: Int = 0
        ): Category {
            require(name.isNotBlank()) { "카테고리명은 필수입니다" }
            require(description.isNotBlank()) { "카테고리 설명은 필수입니다" }
            require(displayOrder >= 0) { "표시 순서는 0 이상이어야 합니다" }

            return Category(
                name = name,
                description = description,
                displayOrder = displayOrder
            )
        }
    }
}