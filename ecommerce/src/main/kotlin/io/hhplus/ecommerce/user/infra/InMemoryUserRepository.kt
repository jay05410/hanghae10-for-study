package io.hhplus.ecommerce.user.infra

import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<Long, User>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val user1 = User.create(
            loginType = LoginType.LOCAL,
            loginId = "user1",
            password = "password123",
            email = "user1@example.com",
            name = "홍길동",
            phone = "010-1234-5678",
            providerId = null,
            createdBy = 1L
        )
        val user2 = User.create(
            loginType = LoginType.LOCAL,
            loginId = "user2",
            password = "password123",
            email = "user2@example.com",
            name = "김철수",
            phone = "010-9876-5432",
            providerId = null,
            createdBy = 1L
        )
        val user3 = User.create(
            loginType = LoginType.KAKAO,
            loginId = "kakao_user",
            password = null,
            email = "kakao@example.com",
            name = "이영희",
            phone = "010-5555-6666",
            providerId = "kakao_12345",
            createdBy = 1L
        )

        users[1L] = User(
            id = 1L,
            loginType = user1.loginType,
            loginId = user1.loginId,
            password = user1.password,
            email = user1.email,
            name = user1.name,
            phone = user1.phone,
            providerId = user1.providerId
        )
        users[2L] = User(
            id = 2L,
            loginType = user2.loginType,
            loginId = user2.loginId,
            password = user2.password,
            email = user2.email,
            name = user2.name,
            phone = user2.phone,
            providerId = user2.providerId
        )
        users[3L] = User(
            id = 3L,
            loginType = user3.loginType,
            loginId = user3.loginId,
            password = user3.password,
            email = user3.email,
            name = user3.name,
            phone = user3.phone,
            providerId = user3.providerId
        )

        idGenerator.set(4L)
    }

    override fun save(user: User): User {
        // For in-memory repository, we simulate ID assignment
        // In a real JPA implementation, this would be handled by the persistence layer
        users[user.id] = user
        return user
    }

    override fun findById(id: Long): User? {
        return users[id]
    }

    override fun findByEmail(email: String): User? {
        return users.values.find { it.email == email }
    }

    override fun findByIsActiveTrue(): List<User> {
        return users.values.filter { it.isActive }
    }

    override fun findByLoginId(loginId: String): User? {
        return users.values.find { it.loginId == loginId }
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return users.values.any { it.loginId == loginId }
    }

    override fun existsByEmail(email: String): Boolean {
        return users.values.any { it.email == email }
    }

    override fun findActiveUsers(): List<User> {
        return findByIsActiveTrue()
    }

    fun clear() {
        users.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}