package io.hhplus.ecommerce.inventory.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 재고 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/Inventory에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "inventory",
    indexes = [
        Index(name = "uk_inventory_product", columnList = "product_id", unique = true),
        Index(name = "idx_inventory_quantity", columnList = "quantity, reserved_quantity")
    ]
)
class InventoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, name = "product_id")
    val productId: Long,

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(nullable = false, name = "reserved_quantity")
    var reservedQuantity: Int = 0,

    @Version
    @Column(nullable = false)
    var version: Int = 0
) : BaseJpaEntity()
