package io.hhplus.ecommerce.user.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.user.domain.constant.LoginType
import jakarta.persistence.*

/**
 * 사용자 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/User에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, isActive, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "uk_users_login_id", columnList = "login_id", unique = true),
        Index(name = "uk_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_phone", columnList = "phone"),
        Index(name = "idx_users_provider", columnList = "login_type, provider_id")
    ]
)
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 20, name = "login_type")
    @Enumerated(EnumType.STRING)
    val loginType: LoginType,

    @Column(nullable = false, unique = true, length = 100, name = "login_id")
    val loginId: String,

    @Column(length = 255)
    val password: String?,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, length = 20)
    val phone: String,

    @Column(length = 100, name = "provider_id")
    val providerId: String?
) : BaseJpaEntity()
