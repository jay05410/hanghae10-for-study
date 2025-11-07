package io.hhplus.ecommerce.user.domain.repository

import io.hhplus.ecommerce.user.domain.entity.User


interface UserRepository {
    fun save(user: User): User
    fun findById(id: Long): User?
    fun findByLoginId(loginId: String): User?
    fun findByEmail(email: String): User?
    fun existsByLoginId(loginId: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findActiveUsers(): List<User>
    fun findByIsActiveTrue(): List<User>
}