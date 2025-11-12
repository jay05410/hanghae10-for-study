package io.hhplus.ecommerce.common.baseentity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

/**
 * 활성화 상태 관리 JPA 엔티티
 *
 * 역할:
 * - 엔티티의 활성화/비활성화 상태 관리
 *
 * 계층 구조:
 * - SoftDeletableJpaEntity (deletedAt)
 *   └── BaseJpaEntity (audit fields)
 *       └── ActiveJpaEntity (isActive) ← 이 클래스
 */
@MappedSuperclass
abstract class ActiveJpaEntity(
    @Column(nullable = false, name = "is_active")
    open var isActive: Boolean = true
) : BaseJpaEntity() {

    open fun activate() {
        this.isActive = true
    }

    open fun deactivate() {
        this.isActive = false
    }

    fun isDeactivated(): Boolean = !isActive
}