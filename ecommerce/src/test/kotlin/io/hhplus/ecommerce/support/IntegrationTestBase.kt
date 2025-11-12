package io.hhplus.ecommerce.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 통합 테스트를 위한 공통 베이스 클래스
 *
 * TestContainers를 사용하여 실제 MySQL 데이터베이스와 통합 테스트를 수행합니다.
 * 향후 Redis, Kafka 등 다른 인프라 컴포넌트 추가 시 확장 가능하도록 설계되었습니다.
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
abstract class IntegrationTestBase {

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

            // TestContainers 최적화
            registry.add("logging.level.org.testcontainers") { "INFO" }
            registry.add("logging.level.com.github.dockerjava") { "WARN" }
        }
    }

    /**
     * 테스트 데이터 초기화를 위한 헬퍼 메서드들을 하위 클래스에서 사용할 수 있습니다.
     * 예: 공통 테스트 데이터 생성, 트랜잭션 롤백 등
     */
}