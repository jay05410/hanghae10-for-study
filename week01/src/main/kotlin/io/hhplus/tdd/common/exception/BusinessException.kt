package io.hhplus.tdd.common.exception

import io.hhplus.tdd.common.error.ErrorCode
import org.slf4j.event.Level

/**
 * 비즈니스 예외의 추상 클래스
 *
 * 모든 도메인별 비즈니스 예외는 이 클래스를 상속받아 구현
 * ErrorCode를 통해 일관된 에러 처리를 제공
 *
 * @property errorCode 에러 코드 정보
 * @property message 에러 메시지
 * @property logLevel 로그 레벨 (기본: WARN)
 * @property data 추가 데이터 (로깅 및 응답에 포함될 수 가능)
 */
abstract class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    val logLevel: Level = Level.WARN,
    val data: Map<String, Any> = emptyMap(),
) : RuntimeException(message)
