package io.hhplus.ecommerce.domain.user.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val loginType: LoginType,

    @Column(nullable = false, unique = true, length = 100)
    val loginId: String,

    @Column(length = 255)
    val password: String?,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, length = 20)
    val phone: String,

    @Column(length = 100)
    val providerId: String?,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedBy: Long
) {
    fun validatePhoneFormat() {
        val phoneRegex = Regex("^01[0-9]-\\d{4}-\\d{4}$")
        if (!phone.matches(phoneRegex)) {
            throw IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다: $phone")
        }
    }

    fun deactivate(): User {
        return User(
            id = this.id,
            loginType = this.loginType,
            loginId = this.loginId,
            password = this.password,
            email = this.email,
            name = this.name,
            phone = this.phone,
            providerId = this.providerId,
            isActive = false,
            createdAt = this.createdAt,
            createdBy = this.createdBy,
            updatedAt = LocalDateTime.now(),
            updatedBy = this.updatedBy
        )
    }

    companion object {
        fun create(
            loginType: LoginType,
            loginId: String,
            password: String?,
            email: String,
            name: String,
            phone: String,
            providerId: String?,
            createdBy: Long
        ): User {
            return User(
                loginType = loginType,
                loginId = loginId,
                password = password,
                email = email,
                name = name,
                phone = phone,
                providerId = providerId,
                createdBy = createdBy,
                updatedBy = createdBy
            ).also { it.validatePhoneFormat() }
        }
    }
}

enum class LoginType {
    LOCAL, KAKAO, NAVER, GOOGLE
}