package io.hhplus.ecommerce.point.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 사용자 포인트 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/UserPoint에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, isActive, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "user_point",
    indexes = [
        Index(name = "uk_user_point_user", columnList = "user_id", unique = true),
        Index(name = "idx_user_point_active", columnList = "is_active, deleted_at")
    ]
)
class UserPointJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, name = "user_id")
    val userId: Long,

    @Column(nullable = false)
    var balance: Long,

    @Version
    var version: Int = 0
) : BaseJpaEntity()
