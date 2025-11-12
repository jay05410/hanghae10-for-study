package io.hhplus.ecommerce.config

import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiCustomConfig {

    @Bean
    fun ignoreCommonParameters(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            openApi.paths.values.forEach { pathItem ->
                pathItem.readOperations().forEach { operation ->
                    operation.parameters = operation.parameters
                        ?.filterNot { param ->
                            // userId나 다른 공통 파라미터들을 숨기고 싶을 때 사용
                            (param.`in` == "header" && param.name == "X-User-Id") ||
                            (param.`in` == "query" && param.name == "debug")
                        }
                }
            }
        }
    }
}