package io.hhplus.ecommerce.common.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * List<Long>을 JSON 문자열로 변환하는 JPA Converter
 *
 * 사용 예:
 * @Convert(converter = LongListConverter::class)
 * val couponIds: List<Long> = emptyList()
 */
@Converter
class LongListConverter : AttributeConverter<List<Long>, String> {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Long.serializer())

    override fun convertToDatabaseColumn(attribute: List<Long>?): String {
        return if (attribute.isNullOrEmpty()) {
            "[]"
        } else {
            json.encodeToString(serializer, attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<Long> {
        return if (dbData.isNullOrBlank() || dbData == "[]") {
            emptyList()
        } else {
            try {
                json.decodeFromString(serializer, dbData)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
