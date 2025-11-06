package io.hhplus.ecommerce.domain.product.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "box_types")
class BoxType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val name: String,

    @Column(nullable = false)
    val teaCount: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedBy: Long
) {
    fun isAvailable(): Boolean = isActive

    fun validateTeaCount(actualTeaCount: Int) {
        if (actualTeaCount != teaCount) {
            throw IllegalArgumentException("박스 타입과 차 개수가 일치하지 않습니다. 예상: $teaCount, 실제: $actualTeaCount")
        }
    }

    companion object {
        fun create(
            name: String,
            teaCount: Int,
            description: String,
            createdBy: Long
        ): BoxType {
            require(name.isNotBlank()) { "박스 타입명은 필수입니다" }
            require(teaCount > 0) { "차 개수는 0보다 커야 합니다" }
            require(description.isNotBlank()) { "박스 타입 설명은 필수입니다" }

            return BoxType(
                name = name,
                teaCount = teaCount,
                description = description,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}