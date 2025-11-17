package io.hhplus.ecommerce.common.aop

import io.hhplus.ecommerce.common.baseentity.DELETED_FILTER
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.hibernate.Session
import org.springframework.stereotype.Component

/**
 * Hibernate Filter AOP 컴포넌트
 *
 * 역할:
 * - @SoftDeletedFilter 어노테이션이 붙은 메서드 실행 시 Hibernate Filter 활성화
 * - deletedAt이 null인 엔티티만 조회되도록 필터 적용
 */
@Component
@Aspect
class HibernateFilterAspect(
    @PersistenceContext private val entityManager: EntityManager
) {

    /**
     * @SoftDeletedFilter 어노테이션이 적용된 메서드 실행 전 필터 활성화
     */
    @Before("@annotation(io.hhplus.ecommerce.common.aop.SoftDeletedFilter)")
    fun enableSoftDeleteFilter() {
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter(DELETED_FILTER)
    }
}