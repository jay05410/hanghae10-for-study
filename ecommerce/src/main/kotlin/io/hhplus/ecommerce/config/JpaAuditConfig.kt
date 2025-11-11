//package io.hhplus.ecommerce.config
//
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.domain.AuditorAware
//import org.springframework.data.jpa.repository.config.EnableJpaAuditing
//import java.util.*
//
//@Configuration
//@EnableJpaAuditing
//class JpaAuditConfig {
//
//    @Bean
//    fun auditorProvider(): AuditorAware<Long> {
//        return AuditorAware<Long> {
//            // TODO: 실제 환경에서는 SecurityContext에서 현재 사용자 ID를 가져와야 함
//            // 현재는 시스템 사용자(1L)로 설정
//            Optional.of(1L)
//        }
//    }
//}