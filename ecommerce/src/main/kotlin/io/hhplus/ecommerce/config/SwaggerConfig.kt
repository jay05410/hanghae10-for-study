package io.hhplus.ecommerce.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger/OpenAPI 설정
 *
 * API 문서화를 위한 Swagger UI 설정
 *
 * 자동화된 기능:
 * - ApiResponseOperationCustomizer: 리턴 타입만으로 ApiResponse<T> 구조 자동 문서화
 * - 제네릭 타입 자동 추출 및 스키마 생성
 * - List<T> 자동 감지 및 배열 스키마 생성
 */
@OpenAPIDefinition(
    servers = [
//        Server(
//            url = "https://api.hhplus-ecommerce.org",
//            description = "Production server",
//        ),
        Server(
            url = "http://localhost:8080",
            description = "Local development server",
        ),
    ],
)
@Configuration
@SecurityScheme(
    name = "securityCookie",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = "access_token"
)
class SwaggerConfig(
    @Value("\${swagger.title:HH Plus E-Commerce API}") private val title: String,
    @Value("\${swagger.description:HH Plus E-Commerce Platform API Documentation}") private val description: String,
    @Value("\${swagger.version:1.0.0}") private val version: String
) {
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title(title)
                    .description(description)
                    .version(version)
            )
    }
}