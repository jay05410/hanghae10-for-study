package io.hhplus.ecommerce.product.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import jakarta.persistence.*

/**
 * 상품 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/Product에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, isActive, deletedAt는 ActiveJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "items",
    indexes = [
        Index(name = "idx_items_category_id", columnList = "category_id"),
        Index(name = "idx_items_status", columnList = "status"),
        Index(name = "idx_items_is_active", columnList = "is_active")
    ]
)
class ProductJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "category_id")
    val categoryId: Long,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, length = 20, name = "caffeine_type")
    val caffeineType: String,

    @Column(nullable = false, length = 100, name = "taste_profile")
    val tasteProfile: String,

    @Column(nullable = false, length = 100, name = "aroma_profile")
    val aromaProfile: String,

    @Column(nullable = false, length = 100, name = "color_profile")
    val colorProfile: String,

    @Column(nullable = false, name = "bag_per_weight")
    val bagPerWeight: Int,

    @Column(nullable = false, name = "price_per_100g")
    val pricePer100g: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val ingredients: String,

    @Column(nullable = false, length = 100)
    val origin: String,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: ProductStatus = ProductStatus.ACTIVE
) : ActiveJpaEntity()
