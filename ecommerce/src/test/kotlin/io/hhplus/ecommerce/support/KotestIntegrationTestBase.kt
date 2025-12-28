package io.hhplus.ecommerce.support

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Kotest 통합 테스트를 위한 공통 베이스 클래스
 *
 * DescribeSpec 스타일의 Kotest 테스트에서 TestContainers MySQL을 사용합니다.
 */
@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
    ]
)
@ActiveProfiles("test")
@Testcontainers
abstract class KotestIntegrationTestBase(body: DescribeSpec.() -> Unit = {}) : DescribeSpec(body) {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        // 각 테스트 전에 기본 데이터 설정
        beforeTest {
            try {
                // 기본 카테고리가 없으면 생성 (ID=1)
                val categoryExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM categories WHERE id = 1",
                    Int::class.java
                ) ?: 0

                if (categoryExists == 0) {
                    jdbcTemplate.execute("""
                        INSERT INTO categories (id, name, description, display_order, created_at, updated_at)
                        VALUES (1, '기본 카테고리', '테스트용 기본 카테고리', 0, NOW(), NOW())
                    """.trimIndent())
                }
            } catch (e: Exception) {
                // 테이블이 아직 생성되지 않은 경우 무시
            }
        }

        // 각 테스트 후 데이터 정리 (테스트 간 격리 보장)
        afterTest {
            try {
                // Foreign key 제약 조건 비활성화
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")

                // 모든 테이블 데이터 삭제
                val tables = listOf(
                    "cart_item_tea", "cart_items", "carts",
                    "point_history", "user_point",
                    "inventory", "coupon_issue_history", "user_coupons", "coupons",
                    "order_item_tea", "order_item", "orders",
                    "payment_history", "payments",
                    "product_statistics", "stock_reservations",
                    "outbox_event", "users", "delivery"
                )

                tables.forEach { table ->
                    try {
                        jdbcTemplate.execute("TRUNCATE TABLE $table")
                    } catch (e: Exception) {
                        // 테이블이 없으면 무시
                    }
                }

                // Foreign key 제약 조건 재활성화
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
            } catch (e: Exception) {
                // 에러 무시 (테스트 실패하지 않도록)
            }
        }
    }

    companion object {
        /**
         * MySQL TestContainer
         * 테스트 클래스별로 공유되어 성능을 최적화합니다.
         */
        @Container
        @JvmStatic
        protected val mysqlContainer = MySQLContainer("mysql:8.0.43")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
            .apply {
                start() // 명시적으로 시작
            }

        @Container
        @JvmStatic
        protected val redisContainer = GenericContainer(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379)
            .apply {
                start() // 명시적으로 시작
            }

        /**
         * Kafka TestContainer
         * Singleton 패턴: 모든 테스트에서 동일 컨테이너 재사용
         * - 컨테이너 시작 시간 절약 (~10초 → 1회만)
         * - 이벤트 유실 없이 테스트 간 연속성 유지
         * - 7.5.0 사용 (8.x는 KRaft 모드 기본으로 호환성 문제)
         */
        @Container
        @JvmStatic
        protected val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .apply {
                start() // 명시적으로 시작
            }

        /**
         * Spring Boot의 DataSource 설정을 TestContainer로 오버라이드
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

            // JPA 설정
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.jpa.show-sql") { "true" }
            registry.add("spring.jpa.properties.hibernate.format_sql") { "true" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.MySQLDialect" }
            registry.add("spring.jpa.defer-datasource-initialization") { "false" }

            // MySQL 호환성 설정 (AUTO_INCREMENT 사용)
            registry.add("spring.jpa.properties.hibernate.dialect.storage_engine") { "innodb" }
            registry.add("spring.jpa.properties.hibernate.id.new_generator_mappings") { "false" }
            registry.add("spring.jpa.properties.hibernate.id.optimizer.pooled.prefer_lo") { "true" }

            // Redis TestContainer 설정
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }

            // MySQL 특화 JPA 설정
            registry.add("spring.jpa.properties.hibernate.connection.characterEncoding") { "utf8mb4" }
            registry.add("spring.jpa.properties.hibernate.connection.CharSet") { "utf8mb4" }
            registry.add("spring.jpa.properties.hibernate.connection.useUnicode") { "true" }

            // MySQL 특화 설정
            registry.add("spring.datasource.hikari.connection-test-query") { "SELECT 1" }
            registry.add("spring.datasource.hikari.minimum-idle") { "1" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "5" }

            // SQL 초기화 비활성화 (DDL 생성과 충돌 방지)
            registry.add("spring.sql.init.mode") { "never" }

            // Redis 설정
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }

            // Kafka TestContainer 설정
            registry.add("kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }

            // TestContainers 최적화
            registry.add("logging.level.org.testcontainers") { "INFO" }
            registry.add("logging.level.com.github.dockerjava") { "WARN" }
        }
    }
}
