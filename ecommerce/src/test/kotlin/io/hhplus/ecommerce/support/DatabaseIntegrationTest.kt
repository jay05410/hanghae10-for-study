package io.hhplus.ecommerce.support

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.transaction.annotation.Transactional

/**
 * 데이터베이스 관련 통합 테스트를 위한 베이스 클래스
 *
 * JPA Repository와 관련된 테스트에 특화된 기능들을 제공합니다.
 */
@Transactional
abstract class DatabaseIntegrationTest : IntegrationTestBase() {

    /**
     * 테스트용 엔티티 매니저 (필요시 하위 클래스에서 주입받아 사용)
     */
    protected fun flushAndClear(entityManager: TestEntityManager) {
        entityManager.flush()
        entityManager.clear()
    }


    /**
     * 테스트 데이터 정리를 위한 헬퍼 메서드
     */
    protected fun clearAllTables() {
        // 필요시 구현: 모든 테이블 데이터 삭제
    }
}