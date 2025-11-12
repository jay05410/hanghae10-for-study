package io.hhplus.ecommerce.delivery.domain.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * DeliveryAddress를 JSON으로 변환하는 JPA AttributeConverter
 *
 * 역할:
 * - DeliveryAddress VO를 JSON 문자열로 직렬화하여 DB에 저장
 * - DB의 JSON 문자열을 DeliveryAddress VO로 역직렬화
 *
 * 사용:
 * - DeliveryJpaEntity의 deliveryAddress 필드에 @Convert 어노테이션으로 적용
 */
@Converter(autoApply = false)
class DeliveryAddressConverter : AttributeConverter<String, String> {

    private val objectMapper = ObjectMapper().findAndRegisterModules()

    /**
     * JSON 문자열을 그대로 DB에 저장 (이미 Mapper에서 직렬화됨)
     */
    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute
    }

    /**
     * DB의 JSON 문자열을 그대로 반환 (Mapper에서 역직렬화함)
     */
    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData
    }
}
