package io.hhplus.ecommerce.integration.coupon

import io.hhplus.ecommerce.coupon.application.CouponQueueService
import io.hhplus.ecommerce.coupon.application.CouponQueueWorker
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import io.hhplus.ecommerce.coupon.domain.constant.QueueStatus
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * 쿠폰 발급 Queue 시스템 통합 테스트
 *
 * 검증 사항:
 * 1. Queue 등록 (enqueue)
 * 2. Worker 처리 (dequeue + 쿠폰 발급)
 * 3. 상태 업데이트 (WAITING → PROCESSING → COMPLETED)
 * 4. UserCoupon 생성 확인
 */
@SpringBootTest
@ActiveProfiles("test")
class CouponQueueIntegrationTest(
    private val couponQueueService: CouponQueueService,
    private val couponQueueWorker: CouponQueueWorker,
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) : KotestIntegrationTestBase() {

    init {
        beforeEach {
            // Redis 데이터 초기화
            redisTemplate.execute { connection ->
                connection.serverCommands().flushAll()
                null
            }
        }

        describe("쿠폰 발급 Queue 시스템") {
            context("사용자가 쿠폰 발급을 요청하면") {
                it("Queue에 등록되고 Worker가 처리하여 쿠폰이 발급된다") {
                    // Given: 쿠폰 생성
                    val coupon = Coupon.create(
                        name = "테스트 쿠폰",
                        code = "TEST100",
                        discountType = DiscountType.FIXED,
                        discountValue = 1000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now().minusDays(1),
                        validTo = LocalDateTime.now().plusDays(30)
                    )
                    val savedCoupon = couponRepository.save(coupon)
                    val userId = 1L

                    // When: Queue에 등록
                    val queueRequest = couponQueueService.enqueue(
                        userId = userId,
                        couponId = savedCoupon.id,
                        couponName = savedCoupon.name
                    )

                    // Then: Queue 등록 확인
                    queueRequest.status shouldBe QueueStatus.WAITING
                    queueRequest.userId shouldBe userId
                    queueRequest.couponId shouldBe savedCoupon.id
                    queueRequest.queuePosition shouldBe 1

                    // When: Worker가 처리
                    couponQueueWorker.processQueue()

                    // 처리 대기 (비동기 처리를 위한 짧은 대기)
                    Thread.sleep(500)

                    // Then: Queue 상태 확인
                    val processedRequest = couponQueueService.getQueueRequest(queueRequest.queueId)
                    processedRequest shouldNotBe null
                    processedRequest!!.status shouldBe QueueStatus.COMPLETED
                    processedRequest.userCouponId shouldNotBe null

                    // Then: UserCoupon 생성 확인
                    val userCoupon = userCouponRepository.findById(processedRequest.userCouponId!!)
                    userCoupon shouldNotBe null
                    userCoupon!!.userId shouldBe userId
                    userCoupon.couponId shouldBe savedCoupon.id

                    // Then: 쿠폰 발급 수량 증가 확인
                    val updatedCoupon = couponRepository.findById(savedCoupon.id)
                    updatedCoupon shouldNotBe null
                    updatedCoupon!!.issuedQuantity shouldBe 1
                }
            }

            context("쿠폰이 품절되면") {
                it("Queue 처리 시 FAILED 상태로 변경된다") {
                    // Given: 품절된 쿠폰 생성
                    val coupon = Coupon.create(
                        name = "품절 쿠폰",
                        code = "SOLDOUT",
                        discountType = DiscountType.FIXED,
                        discountValue = 1000L,
                        totalQuantity = 1,
                        validFrom = LocalDateTime.now().minusDays(1),
                        validTo = LocalDateTime.now().plusDays(30)
                    )
                    coupon.issue() // 1개 발급 (품절)
                    val savedCoupon = couponRepository.save(coupon)

                    // When: Queue에 등록
                    val queueRequest = couponQueueService.enqueue(
                        userId = 2L,
                        couponId = savedCoupon.id,
                        couponName = savedCoupon.name
                    )

                    // When: Worker가 처리
                    couponQueueWorker.processQueue()

                    // 처리 대기
                    Thread.sleep(500)

                    // Then: Queue 상태가 FAILED
                    val failedRequest = couponQueueService.getQueueRequest(queueRequest.queueId)
                    failedRequest shouldNotBe null
                    failedRequest!!.status shouldBe QueueStatus.FAILED
                    failedRequest.failureReason shouldNotBe null
                }
            }

            context("동일한 사용자가 같은 쿠폰을 중복 요청하면") {
                it("AlreadyInQueue 예외가 발생한다") {
                    // Given: 쿠폰 생성
                    val coupon = Coupon.create(
                        name = "중복 테스트 쿠폰",
                        code = "DUP100",
                        discountType = DiscountType.FIXED,
                        discountValue = 1000L,
                        totalQuantity = 100,
                        validFrom = LocalDateTime.now().minusDays(1),
                        validTo = LocalDateTime.now().plusDays(30)
                    )
                    val savedCoupon = couponRepository.save(coupon)
                    val userId = 3L

                    // When: 첫 번째 Queue 등록
                    val firstRequest = couponQueueService.enqueue(
                        userId = userId,
                        couponId = savedCoupon.id,
                        couponName = savedCoupon.name
                    )

                    firstRequest.status shouldBe QueueStatus.WAITING

                    // Then: 두 번째 Queue 등록 시도 시 예외 발생
                    val exception = runCatching {
                        couponQueueService.enqueue(
                            userId = userId,
                            couponId = savedCoupon.id,
                            couponName = savedCoupon.name
                        )
                    }.exceptionOrNull()

                    exception shouldNotBe null
                    exception!!.message shouldNotBe null
                }
            }
        }
    }
}
