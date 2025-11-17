package io.hhplus.ecommerce.common.aop

import org.springframework.transaction.annotation.Transactional

/**
 * Soft Delete 필터 어노테이션
 *
 * 역할:
 * - 메서드 실행 시 자동으로 soft delete 필터 적용
 * - deletedAt이 null인 데이터만 조회되도록 Hibernate Filter 활성화
 *
 * 사용법:
 * ```
 * @SoftDeletedFilter
 * fun findActiveUsers(): List<User>
 * ```
 */
@Transactional
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SoftDeletedFilter