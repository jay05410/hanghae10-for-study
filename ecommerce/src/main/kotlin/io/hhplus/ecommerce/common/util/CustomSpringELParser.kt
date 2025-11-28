package io.hhplus.ecommerce.common.util

import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

/**
 * Spring EL 표현식 파서
 *
 * 메서드 파라미터를 기반으로 동적 키 생성
 */
object CustomSpringELParser {

    private val parser: ExpressionParser = SpelExpressionParser()

    /**
     * SpEL 표현식을 파싱하여 동적 값 반환
     *
     * @param parameterNames 메서드 파라미터 이름들
     * @param args 메서드 실제 인자들
     * @param expression SpEL 표현식
     * @return 파싱된 값
     */
    fun getDynamicValue(
        parameterNames: Array<String>,
        args: Array<Any?>,
        expression: String
    ): Any? {
        val context = StandardEvaluationContext()

        // 파라미터를 컨텍스트에 등록
        parameterNames.forEachIndexed { index, paramName ->
            if (index < args.size) {
                context.setVariable(paramName, args[index])
            }
        }

        return parser.parseExpression(expression).getValue(context)
    }
}