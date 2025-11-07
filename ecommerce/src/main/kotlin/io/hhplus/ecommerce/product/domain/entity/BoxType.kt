package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*

// @Entity
// @Table(name = "box_types")
class BoxType(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true, length = 50)
    val code: String,

    // @Column(nullable = false, unique = true, length = 50)
    val name: String,

    // @Column(nullable = false)
    val days: Int,

    // @Column(nullable = false)
    val teaCount: Int,

    // @Column(nullable = false, columnDefinition = "TEXT")
    val description: String
) : ActiveJpaEntity() {
    fun isAvailable(): Boolean = isActive

    fun validateTeaCount(actualTeaCount: Int) {
        if (actualTeaCount != teaCount) {
            throw IllegalArgumentException("박스 타입과 차 개수가 일치하지 않습니다. 예상: $teaCount, 실제: $actualTeaCount")
        }
    }

    companion object {
        fun create(
            code: String,
            name: String,
            days: Int,
            teaCount: Int,
            description: String,
            createdBy: Long
        ): BoxType {
            require(code.isNotBlank()) { "박스 타입 코드는 필수입니다" }
            require(name.isNotBlank()) { "박스 타입명은 필수입니다" }
            require(days > 0) { "일수는 0보다 커야 합니다" }
            require(teaCount > 0) { "차 개수는 0보다 커야 합니다" }
            require(description.isNotBlank()) { "박스 타입 설명은 필수입니다" }

            return BoxType(
                code = code,
                name = name,
                days = days,
                teaCount = teaCount,
                description = description
            )
        }
    }
}