package io.hhplus.ecommerce.common.baseentity

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Column
import java.time.LocalDateTime

const val DELETED_FILTER = "deletedFilter"

/**
 * Soft Delete JPA 엔티티
 *
 * 역할:
 * - 논리 삭제(Soft Delete) 기능 제공
 *
 * 주의:
 * - 모든 JPA 엔티티의 최상위 베이스
 */
@MappedSuperclass
abstract class SoftDeletableJpaEntity(
    @Column(name = "deleted_at")
    open var deletedAt: LocalDateTime? = null
) {
    open fun delete() {
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = deletedAt != null

    open fun restore() {
        this.deletedAt = null
    }
}