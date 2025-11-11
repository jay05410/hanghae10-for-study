package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.entity.BoxType
import io.hhplus.ecommerce.product.domain.repository.BoxTypeRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Repository
class InMemoryBoxTypeRepository : BoxTypeRepository {
    private val boxTypes = ConcurrentHashMap<Long, BoxType>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val weeklyBox = BoxType.create(
            code = "WEEKLY_7",
            name = "주간 티 박스",
            days = 7,
            teaCount = 7,
            description = "일주일 동안 즐길 수 있는 다양한 차 7종 구성",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val monthlyBox = BoxType.create(
            code = "MONTHLY_30",
            name = "월간 티 박스",
            days = 30,
            teaCount = 30,
            description = "한 달 동안 매일 다른 차를 즐길 수 있는 30종 구성",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val premiumBox = BoxType.create(
            code = "PREMIUM_14",
            name = "프리미엄 티 박스",
            days = 14,
            teaCount = 14,
            description = "2주간의 프리미엄 티 경험을 제공하는 고급 차 14종 구성",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        val sampleBox = BoxType.create(
            code = "SAMPLE_3",
            name = "샘플 티 박스",
            days = 3,
            teaCount = 3,
            description = "처음 시작하는 분들을 위한 3일 체험 구성",
            createdBy = 1L
        ).copy(id = idGenerator.getAndIncrement())

        boxTypes[weeklyBox.id] = weeklyBox
        boxTypes[monthlyBox.id] = monthlyBox
        boxTypes[premiumBox.id] = premiumBox
        boxTypes[sampleBox.id] = sampleBox
    }

    override fun save(boxType: BoxType): BoxType {
        simulateLatency()

        val savedBoxType = if (boxType.id == 0L) {
            BoxType(
                id = idGenerator.getAndIncrement(),
                code = boxType.code,
                name = boxType.name,
                days = boxType.days,
                teaCount = boxType.teaCount,
                description = boxType.description
            )
        } else {
            boxType
        }

        boxTypes[savedBoxType.id] = savedBoxType
        return savedBoxType
    }

    override fun findById(id: Long): BoxType? {
        simulateLatency()
        return boxTypes[id]
    }

    override fun findByCode(code: String): BoxType? {
        simulateLatency()
        return boxTypes.values.find { it.code == code }
    }

    override fun findByName(name: String): BoxType? {
        simulateLatency()
        return boxTypes.values.find { it.name == name }
    }

    override fun findAll(): List<BoxType> {
        simulateLatency()
        return boxTypes.values.toList().sortedBy { it.days }
    }

    override fun findActiveBoxTypes(): List<BoxType> {
        simulateLatency()
        return boxTypes.values
            .filter { it.isAvailable() }
            .sortedBy { it.days }
    }

    override fun findByDays(days: Int): List<BoxType> {
        simulateLatency()
        return boxTypes.values.filter { it.days == days }
    }

    override fun findByTeaCount(teaCount: Int): List<BoxType> {
        simulateLatency()
        return boxTypes.values.filter { it.teaCount == teaCount }
    }

    override fun existsByCode(code: String): Boolean {
        simulateLatency()
        return boxTypes.values.any { it.code == code }
    }

    override fun existsByName(name: String): Boolean {
        simulateLatency()
        return boxTypes.values.any { it.name == name }
    }

    override fun deleteById(id: Long) {
        simulateLatency()
        boxTypes.remove(id)
    }

    private fun simulateLatency() {
        Thread.sleep(Random.nextLong(50, 200))
    }

    fun clear() {
        boxTypes.clear()
        idGenerator.set(1)
        initializeSampleData()
    }

    private fun BoxType.copy(id: Long = this.id): BoxType {
        return BoxType(
            id = id,
            code = this.code,
            name = this.name,
            days = this.days,
            teaCount = this.teaCount,
            description = this.description
        )
    }
}