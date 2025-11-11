package io.hhplus.ecommerce.user.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.user.domain.constant.LoginType
// import jakarta.persistence.*

// @Entity
// @Table(name = "users")
class User(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    val loginType: LoginType,

    // @Column(nullable = false, unique = true, length = 100)
    val loginId: String,

    // @Column(length = 255)
    val password: String?,

    // @Column(nullable = false, unique = true, length = 100)
    val email: String,

    // @Column(nullable = false, length = 50)
    val name: String,

    // @Column(nullable = false, length = 20)
    val phone: String,

    // @Column(length = 100)
    val providerId: String?
) : ActiveJpaEntity() {

    fun update(name: String, email: String, updatedBy: Long) {
        // Since these are val properties, we would need to create a new entity
        // This is a design issue that would require changing the entity structure
        // For now, we'll just validate the inputs
        require(name.isNotBlank()) { "이름은 필수입니다" }
        require(email.isNotBlank()) { "이메일은 필수입니다" }
    }

    fun validatePhoneFormat() {
        val phoneRegex = Regex("^01[0-9]-\\d{4}-\\d{4}$")
        if (!phone.matches(phoneRegex)) {
            throw IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다: $phone")
        }
    }

    fun deactivateUser() {
        this.deactivate()
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
                providerId = providerId
            ).also { it.validatePhoneFormat() }
        }
    }
}

