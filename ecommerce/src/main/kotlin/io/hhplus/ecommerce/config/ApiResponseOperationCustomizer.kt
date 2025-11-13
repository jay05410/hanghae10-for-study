package io.hhplus.ecommerce.config

import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.responses.ApiResponse as OasApiResponse
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import java.lang.reflect.ParameterizedType

/**
 * API 응답 구조를 자동으로 Swagger 문서화하는 커스터마이저
 *
 * 리턴 타입이 `ApiResponse<T>`인 경우 자동으로 T를 추출하여
 * 일관된 API 문서를 생성함
 *
 * 동작 방식:
 * 1. 메서드 리턴 타입이 `ApiResponse<T>`인지 확인
 * 2. 제네릭 타입 T를 리플렉션으로 추출
 * 3. T가 List<X>인 경우 Array 스키마로 처리
 * 4. 자동으로 성공/에러 응답 문서 생성
 *
 * 설정:
 * - application.yml의 `swagger.auto-wrap-response: false`로 비활성화 가능
 */
@Component
@ConditionalOnProperty(
    name = ["swagger.auto-wrap-response"],
    havingValue = "true",
    matchIfMissing = true  // 설정이 없으면 기본적으로 활성화
)
class ApiResponseOperationCustomizer : OperationCustomizer {

    override fun customize(operation: Operation, handlerMethod: HandlerMethod): Operation {
        // 리턴 타입 확인
        val returnType = handlerMethod.returnType.parameterType

        // ApiResponse<T>인 경우에만 처리
        if (!isApiResponse(returnType)) {
            return operation
        }

        // 리턴 타입에서 자동으로 제네릭 타입 추출하여 성공 응답 추가
        extractAndAddSuccessResponse(operation, handlerMethod)

        // 기본 에러 응답 추가
        addDefaultErrorResponse(operation)

        return operation
    }

    /**
     * 리턴 타입이 ApiResponse인지 확인
     */
    private fun isApiResponse(returnType: Class<*>): Boolean {
        return ApiResponse::class.java.isAssignableFrom(returnType)
    }

    /**
     * 리턴 타입에서 제네릭 타입을 추출하여 성공 응답 추가
     */
    private fun extractAndAddSuccessResponse(operation: Operation, handlerMethod: HandlerMethod) {
        val genericReturnType = handlerMethod.returnType.genericParameterType

        if (genericReturnType is ParameterizedType) {
            val typeArguments = genericReturnType.actualTypeArguments
            if (typeArguments.isNotEmpty()) {
                val dataType = typeArguments[0]

                // List<T> 타입인지 확인
                val (isArray, actualType) = when (dataType) {
                    is ParameterizedType -> {
                        val rawType = dataType.rawType as Class<*>
                        if (List::class.java.isAssignableFrom(rawType) && dataType.actualTypeArguments.isNotEmpty()) {
                            true to (dataType.actualTypeArguments[0] as? Class<*>)
                        } else {
                            false to (dataType as? Class<*>)
                        }
                    }
                    is Class<*> -> false to dataType
                    else -> false to null
                }

                actualType?.let { type ->
                    addSuccessResponseFromType(operation, type, isArray)
                }
            }
        }
    }

    /**
     * 타입 정보로부터 성공 응답 추가
     */
    private fun addSuccessResponseFromType(operation: Operation, dataType: Class<*>, isArray: Boolean) {
        val responses = operation.responses ?: ApiResponses()

        // ModelConverters를 사용하여 실제 스키마 생성
        val resolvedSchemas = ModelConverters.getInstance().readAll(dataType)
        val actualSchema = resolvedSchemas.values.firstOrNull()

        val successSchema = ObjectSchema().apply {
            description = "API 성공 응답"
            addProperty("success", Schema<Boolean>().apply {
                type = "boolean"
                example = true
                description = "요청 성공 여부"
            })

            // 실제 스키마를 직접 사용 (참조가 아닌 인라인)
            val dataSchema = if (isArray) {
                ArraySchema().apply {
                    items = actualSchema ?: Schema<Any>().apply {
                        type = "object"
                        description = dataType.simpleName
                    }
                }
            } else {
                actualSchema ?: Schema<Any>().apply {
                    type = "object"
                    description = dataType.simpleName
                }
            }

            addProperty("data", dataSchema.apply {
                description = "응답 데이터"
            })
        }

        responses.addApiResponse("200", OasApiResponse().apply {
            description = "요청 성공"
            content = Content().addMediaType(
                "application/json",
                MediaType().schema(successSchema)
            )
        })

        operation.responses = responses
    }

    /**
     * 기본 에러 응답 추가 (400, 500)
     */
    private fun addDefaultErrorResponse(operation: Operation) {
        val responses = operation.responses ?: ApiResponses()

        // 400 에러
        responses.addApiResponse("400", OasApiResponse().apply {
            description = "잘못된 요청"
            content = Content().addMediaType(
                "application/json",
                MediaType().schema(createErrorSchema("E001"))
            )
        })

        // 500 에러
        responses.addApiResponse("500", OasApiResponse().apply {
            description = "서버 내부 오류"
            content = Content().addMediaType(
                "application/json",
                MediaType().schema(createErrorSchema("E999"))
            )
        })

        operation.responses = responses
    }

    /**
     * ApiResponse.Error 스키마 생성
     */
    private fun createErrorSchema(errorCodeExample: String): Schema<*> {
        return ObjectSchema().apply {
            description = "API 에러 응답"
            addProperty("success", Schema<Boolean>().apply {
                type = "boolean"
                example = false
                description = "요청 성공 여부"
            })
            addProperty("errorCode", Schema<String>().apply {
                type = "string"
                example = errorCodeExample
                description = "에러 코드"
            })
            addProperty("errorMessage", Schema<String>().apply {
                type = "string"
                example = "에러 메시지"
                description = "에러 상세 메시지"
            })
            addProperty("data", Schema<Any>().apply {
                type = "object"
                nullable = true
                description = "추가 에러 정보 (선택)"
            })
        }
    }
}
