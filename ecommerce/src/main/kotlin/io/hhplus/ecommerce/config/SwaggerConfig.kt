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

@OpenAPIDefinition(
    servers = [
        Server(
            url = "https://api.hhplus-ecommerce.org",
            description = "Production server",
        ),
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