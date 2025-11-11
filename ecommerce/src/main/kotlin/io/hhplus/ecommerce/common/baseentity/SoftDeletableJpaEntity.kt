package io.hhplus.ecommerce.common.baseentity

// import jakarta.persistence.MappedSuperclass
// import org.hibernate.annotations.FilterDef
import java.time.LocalDateTime

const val DELETED_FILTER = "deletedFilter"

// @FilterDef(
//     name = DELETED_FILTER,
//     defaultCondition = "deleted_at is null"
// )
// @MappedSuperclass
abstract class SoftDeletableJpaEntity(
    var deletedAt: LocalDateTime? = null
) {
    open fun delete() {
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = deletedAt != null

    open fun restore() {
        this.deletedAt = null
    }
}