package io.hhplus.ecommerce.pricing.domain.service

import io.hhplus.ecommerce.coupon.domain.constant.DiscountScope
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.service.CouponDomainService
import io.hhplus.ecommerce.pricing.domain.model.*
import io.hhplus.ecommerce.pricing.exception.PricingException
import io.hhplus.ecommerce.product.domain.repository.CategoryRepository
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 가격 계산 도메인 서비스
 *
 * 역할:
 * - 주문 아이템의 가격 조회 및 계산
 * - 쿠폰 할인 적용 및 계산 (TOTAL/CATEGORY/PRODUCT 스코프)
 * - 최종 결제 금액 산출
 *
 * 책임:
 * - Order 도메인이 Product/Coupon 도메인에 직접 의존하지 않도록 중재
 * - 모든 가격 관련 비즈니스 로직 캡슐화
 * - 할인 계산의 단일 책임자
 *
 * 의존성 방향:
 * Order → PricingDomainService → {Product, Category, Coupon}
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - 쿠폰 '사용 처리'(상태 변경)는 하지 않음 - 검증 및 계산만 수행
 */
@Component
class PricingDomainService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val couponDomainService: CouponDomainService
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        const val DEFAULT_GIFT_WRAP_PRICE = 2000L
    }

    /**
     * 주문 가격 계산 (쿠폰 미적용)
     *
     * @param itemRequests 주문 아이템 요청 목록
     * @return 가격 계산 결과
     */
    fun calculatePricing(itemRequests: List<PricingItemRequest>): PricingResult {
        validateItemRequests(itemRequests)

        val pricingItems = enrichItemsWithProductInfo(itemRequests)
        val totalAmount = pricingItems.sumOf { it.itemTotalPrice }

        val pricedItems = pricingItems.map { item ->
            PricedItem(
                productId = item.productId,
                productName = item.productName,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = item.giftWrapPrice,
                totalPrice = item.itemTotalPrice,
                itemDiscountAmount = 0L,
                requiresReservation = item.requiresReservation
            )
        }

        logger.debug { "[PricingDomainService] 가격 계산 완료 (쿠폰 미적용): totalAmount=$totalAmount" }

        return PricingResult(
            items = pricedItems,
            totalAmount = totalAmount,
            discountAmount = 0L,
            finalAmount = totalAmount,
            appliedCouponInfos = emptyList()
        )
    }

    /**
     * 주문 가격 계산 (다중 쿠폰 적용)
     *
     * 쿠폰 적용 순서:
     * 1. PRODUCT 스코프 (상품별 할인)
     * 2. CATEGORY 스코프 (카테고리별 할인)
     * 3. TOTAL 스코프 (전체 주문 할인)
     *
     * @param userId 사용자 ID
     * @param itemRequests 주문 아이템 요청 목록
     * @param userCouponIds 사용할 사용자 쿠폰 ID 목록
     * @return 가격 계산 결과 (모든 할인 포함)
     */
    fun calculatePricingWithCoupons(
        userId: Long,
        itemRequests: List<PricingItemRequest>,
        userCouponIds: List<Long>
    ): PricingResult {
        validateItemRequests(itemRequests)

        if (userCouponIds.isEmpty()) {
            return calculatePricing(itemRequests)
        }

        val pricingItems = enrichItemsWithProductInfo(itemRequests)
        val totalAmount = pricingItems.sumOf { it.itemTotalPrice }

        // 모든 쿠폰 검증 및 조회
        val validatedCoupons = userCouponIds.map { userCouponId ->
            val userCoupon = couponDomainService.getUserCoupon(userCouponId)
                ?: throw PricingException.UserCouponNotFound(userId, userCouponId)

            if (userCoupon.userId != userId) {
                throw PricingException.CouponNotApplicable("다른 사용자의 쿠폰입니다: $userCouponId")
            }

            if (!userCoupon.isUsable()) {
                throw PricingException.CouponNotApplicable("이미 사용되었거나 만료된 쿠폰입니다: $userCouponId")
            }

            val coupon = couponDomainService.getCoupon(userCoupon.couponId)
                ?: throw PricingException.CouponNotApplicable("쿠폰 정보를 찾을 수 없습니다: $userCouponId")

            Pair(userCouponId, coupon)
        }

        // 최소 주문 금액 검증 (모든 쿠폰에 대해)
        validatedCoupons.forEach { (userCouponId, coupon) ->
            if (!coupon.isValidForUse(totalAmount)) {
                throw PricingException.CouponNotApplicable(
                    "쿠폰 '${coupon.name}'의 최소 주문 금액(${coupon.minimumOrderAmount}원)을 충족하지 않습니다. 현재: ${totalAmount}원"
                )
            }
        }

        // 스코프별 정렬: PRODUCT → CATEGORY → TOTAL
        val sortedCoupons = validatedCoupons.sortedBy { (_, coupon) ->
            when (coupon.discountScope) {
                DiscountScope.PRODUCT -> 0
                DiscountScope.CATEGORY -> 1
                DiscountScope.TOTAL -> 2
            }
        }

        // 할인 적용 (누적)
        var currentItems = pricingItems
        var currentAmount = totalAmount
        var totalDiscount = 0L
        val appliedCouponInfos = mutableListOf<AppliedCouponInfo>()

        sortedCoupons.forEach { (userCouponId, coupon) ->
            val (discountAmount, newPricedItems) = calculateDiscountByScope(coupon, currentItems, currentAmount)
            totalDiscount += discountAmount
            currentAmount -= discountAmount

            appliedCouponInfos.add(
                AppliedCouponInfo(
                    userCouponId = userCouponId,
                    couponId = coupon.id,
                    couponName = coupon.name,
                    discountScope = coupon.discountScope,
                    calculatedDiscount = discountAmount
                )
            )

            // PricingItem -> PricedItem 변환 (다음 할인 계산을 위해)
            currentItems = newPricedItems.map { pricedItem ->
                PricingItem(
                    productId = pricedItem.productId,
                    productName = pricedItem.productName,
                    categoryId = pricedItem.categoryId,
                    categoryName = pricedItem.categoryName,
                    quantity = pricedItem.quantity,
                    unitPrice = pricedItem.unitPrice,
                    giftWrap = pricedItem.giftWrap,
                    giftMessage = pricedItem.giftMessage,
                    giftWrapPrice = pricedItem.giftWrapPrice,
                    requiresReservation = pricedItem.requiresReservation
                )
            }
        }

        val finalAmount = totalAmount - totalDiscount

        // 최종 PricedItem 생성
        val finalPricedItems = currentItems.map { item ->
            PricedItem(
                productId = item.productId,
                productName = item.productName,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = item.giftWrapPrice,
                totalPrice = item.itemTotalPrice,
                itemDiscountAmount = 0L,
                requiresReservation = item.requiresReservation
            )
        }

        logger.info {
            "[PricingDomainService] 가격 계산 완료 (쿠폰 ${appliedCouponInfos.size}개 적용): " +
                "totalAmount=$totalAmount, totalDiscount=$totalDiscount, finalAmount=$finalAmount"
        }

        return PricingResult(
            items = finalPricedItems,
            totalAmount = totalAmount,
            discountAmount = totalDiscount,
            finalAmount = finalAmount,
            appliedCouponInfos = appliedCouponInfos
        )
    }

    /**
     * 쿠폰 적용 가능 여부 검증
     *
     * @return 검증 결과 (적용 가능 여부 및 예상 할인 금액)
     */
    fun validateCouponApplicability(
        userId: Long,
        userCouponId: Long,
        itemRequests: List<PricingItemRequest>
    ): CouponValidationResult {
        return try {
            val pricingItems = enrichItemsWithProductInfo(itemRequests)
            val totalAmount = pricingItems.sumOf { it.itemTotalPrice }

            val userCoupon = couponDomainService.getUserCoupon(userCouponId)
                ?: return CouponValidationResult.invalid("쿠폰을 찾을 수 없습니다")

            if (userCoupon.userId != userId) {
                return CouponValidationResult.invalid("다른 사용자의 쿠폰입니다")
            }

            if (!userCoupon.isUsable()) {
                return CouponValidationResult.invalid("이미 사용되었거나 만료된 쿠폰입니다")
            }

            val coupon = couponDomainService.getCoupon(userCoupon.couponId)
                ?: return CouponValidationResult.invalid("쿠폰 정보를 찾을 수 없습니다")

            if (!coupon.isValidForUse(totalAmount)) {
                return CouponValidationResult.invalid(
                    "최소 주문 금액(${coupon.minimumOrderAmount}원)을 충족하지 않습니다"
                )
            }

            val (discountAmount, _) = calculateDiscountByScope(coupon, pricingItems, totalAmount)
            CouponValidationResult.valid(discountAmount)

        } catch (e: Exception) {
            logger.warn { "[PricingDomainService] 쿠폰 검증 실패: ${e.message}" }
            CouponValidationResult.invalid(e.message ?: "알 수 없는 오류")
        }
    }

    /**
     * 상품 정보 조회 및 PricingItem 변환
     */
    private fun enrichItemsWithProductInfo(requests: List<PricingItemRequest>): List<PricingItem> {
        return requests.map { request ->
            val product = productRepository.findById(request.productId)
                ?: throw PricingException.ProductNotFound(request.productId)

            val category = categoryRepository.findById(product.categoryId)
                ?: throw PricingException.CategoryNotFound(product.categoryId)

            PricingItem(
                productId = product.id,
                productName = product.name,
                categoryId = product.categoryId,
                categoryName = category.name,
                quantity = request.quantity,
                unitPrice = product.price,
                giftWrap = request.giftWrap,
                giftMessage = request.giftMessage,
                giftWrapPrice = if (request.giftWrap) DEFAULT_GIFT_WRAP_PRICE else 0L,
                requiresReservation = product.requiresReservation
            )
        }
    }

    /**
     * 스코프별 할인 금액 계산
     *
     * @return Pair(할인금액, PricedItem 목록)
     */
    private fun calculateDiscountByScope(
        coupon: Coupon,
        items: List<PricingItem>,
        totalAmount: Long
    ): Pair<Long, List<PricedItem>> {
        return when (coupon.discountScope) {
            DiscountScope.TOTAL -> calculateTotalScopeDiscount(coupon, items, totalAmount)
            DiscountScope.CATEGORY -> calculateCategoryScopeDiscount(coupon, items)
            DiscountScope.PRODUCT -> calculateProductScopeDiscount(coupon, items)
        }
    }

    /**
     * TOTAL 스코프: 전체 금액에 할인 적용
     */
    private fun calculateTotalScopeDiscount(
        coupon: Coupon,
        items: List<PricingItem>,
        totalAmount: Long
    ): Pair<Long, List<PricedItem>> {
        val discountAmount = coupon.calculateDiscountAmount(totalAmount)

        // 할인은 전체에 적용되므로 개별 아이템에는 할인 분배하지 않음
        val pricedItems = items.map { item ->
            PricedItem(
                productId = item.productId,
                productName = item.productName,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = item.giftWrapPrice,
                totalPrice = item.itemTotalPrice,
                itemDiscountAmount = 0L, // TOTAL 스코프에서는 개별 할인 없음
                requiresReservation = item.requiresReservation
            )
        }

        return Pair(discountAmount, pricedItems)
    }

    /**
     * CATEGORY 스코프: 대상 카테고리 상품에만 할인 적용
     */
    private fun calculateCategoryScopeDiscount(
        coupon: Coupon,
        items: List<PricingItem>
    ): Pair<Long, List<PricedItem>> {
        val targetCategoryIds = coupon.targetCategoryIds.toSet()

        // 대상 카테고리 아이템만 필터링
        val eligibleItems = items.filter { it.categoryId in targetCategoryIds }
        val eligibleAmount = eligibleItems.sumOf { it.itemTotalPrice }

        // 대상 금액에 대해 할인 계산
        val totalDiscount = if (eligibleAmount > 0) {
            coupon.calculateDiscountAmount(eligibleAmount)
        } else {
            0L
        }

        // 각 아이템별 할인 금액 분배 (비율 기준)
        val pricedItems = items.map { item ->
            val itemDiscount = if (item.categoryId in targetCategoryIds && eligibleAmount > 0) {
                (totalDiscount * item.itemTotalPrice / eligibleAmount)
            } else {
                0L
            }

            PricedItem(
                productId = item.productId,
                productName = item.productName,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = item.giftWrapPrice,
                totalPrice = item.itemTotalPrice,
                itemDiscountAmount = itemDiscount,
                requiresReservation = item.requiresReservation
            )
        }

        return Pair(totalDiscount, pricedItems)
    }

    /**
     * PRODUCT 스코프: 대상 상품에만 할인 적용
     */
    private fun calculateProductScopeDiscount(
        coupon: Coupon,
        items: List<PricingItem>
    ): Pair<Long, List<PricedItem>> {
        val targetProductIds = coupon.targetProductIds.toSet()

        // 대상 상품만 필터링
        val eligibleItems = items.filter { it.productId in targetProductIds }
        val eligibleAmount = eligibleItems.sumOf { it.itemTotalPrice }

        // 대상 금액에 대해 할인 계산
        val totalDiscount = if (eligibleAmount > 0) {
            coupon.calculateDiscountAmount(eligibleAmount)
        } else {
            0L
        }

        // 각 아이템별 할인 금액 분배 (비율 기준)
        val pricedItems = items.map { item ->
            val itemDiscount = if (item.productId in targetProductIds && eligibleAmount > 0) {
                (totalDiscount * item.itemTotalPrice / eligibleAmount)
            } else {
                0L
            }

            PricedItem(
                productId = item.productId,
                productName = item.productName,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = item.giftWrapPrice,
                totalPrice = item.itemTotalPrice,
                itemDiscountAmount = itemDiscount,
                requiresReservation = item.requiresReservation
            )
        }

        return Pair(totalDiscount, pricedItems)
    }

    /**
     * 요청 유효성 검증
     */
    private fun validateItemRequests(itemRequests: List<PricingItemRequest>) {
        if (itemRequests.isEmpty()) {
            throw PricingException.InvalidPricingRequest("주문 아이템이 비어있습니다")
        }

        itemRequests.forEach { request ->
            if (request.quantity <= 0) {
                throw PricingException.InvalidPricingRequest(
                    "수량은 0보다 커야 합니다: productId=${request.productId}, quantity=${request.quantity}"
                )
            }
        }
    }
}
