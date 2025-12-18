package io.hhplus.ecommerce.product.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 카테고리 JPA 엔티티 (영속성 모델)
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/Category에 있습니다.
 */
@Entity
@Table(
    name = "categories",
    indexes = [
        Index(name = "idx_categories_display_order", columnList = "display_order")
    ]
)
class CategoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, name = "display_order")
    val displayOrder: Int = 0
) : BaseJpaEntity()