package io.hhplus.ecommerce.cart.application

import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import io.hhplus.ecommerce.cart.domain.repository.CartItemTeaRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장바구니 아이템 차 구성 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 장바구니 아이템의 차 구성 관리 전담
 * - 차 구성 데이터의 생명주기 관리
 * - 차 구성 유효성 검증 로직 제공
 *
 * 책임:
 * - 장바구니 아이템별 차 구성 저장, 수정, 삭제
 * - 차 구성 데이터 조회 및 검색
 * - 차 구성 입력 데이터 검증
 */
@Service
class CartItemTeaService(
    private val cartItemTeaRepository: CartItemTeaRepository
) {

    /**
     * 장바구니 아이템의 차 구성을 저장한다
     *
     * @param cartItemId 장바구니 아이템 ID
     * @param teaItems 저장할 차 구성 목록
     * @return 저장된 차 구성 엔티티 목록
     */
    @Transactional
    fun saveCartItemTeas(cartItemId: Long, teaItems: List<TeaItemRequest>): List<CartItemTea> {
        return teaItems.map { teaItem ->
            val cartItemTea = CartItemTea.create(
                cartItemId = cartItemId,
                productId = teaItem.productId,
                quantity = teaItem.quantity
            )
            cartItemTeaRepository.save(cartItemTea)
        }
    }

    /**
     * 장바구니 아이템의 차 구성을 업데이트한다
     *
     * @param cartItemId 장바구니 아이템 ID
     * @param teaItems 새로운 차 구성 목록
     * @return 업데이트된 차 구성 엔티티 목록
     */
    @Transactional
    fun updateCartItemTeas(cartItemId: Long, teaItems: List<TeaItemRequest>): List<CartItemTea> {
        // 기존 차 구성 삭제
        deleteCartItemTeas(cartItemId)

        // 새로운 차 구성 저장
        return saveCartItemTeas(cartItemId, teaItems)
    }

    /**
     * 장바구니 아이템의 모든 차 구성을 삭제한다
     *
     * @param cartItemId 차 구성을 삭제할 장바구니 아이템 ID
     */
    @Transactional
    fun deleteCartItemTeas(cartItemId: Long) {
        cartItemTeaRepository.deleteByCartItemId(cartItemId)
    }

    /**
     * 장바구니 아이템의 차 구성 목록을 조회한다
     *
     * @param cartItemId 조회할 장바구니 아이템 ID
     * @return 차 구성 엔티티 목록
     */
    fun getCartItemTeas(cartItemId: Long): List<CartItemTea> {
        return cartItemTeaRepository.findByCartItemId(cartItemId)
    }

    /**
     * ID로 차 구성을 단건 조회한다
     *
     * @param id 조회할 차 구성 ID
     * @return 차 구성 엔티티 (존재하지 않을 경우 null)
     */
    fun getCartItemTea(id: Long): CartItemTea? {
        return cartItemTeaRepository.findById(id)
    }

    /**
     * 차 구성 데이터의 유효성을 검증한다
     *
     * @param teaItems 검증할 차 구성 목록
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    fun validateTeaItems(teaItems: List<TeaItemRequest>) {
        require(teaItems.isNotEmpty()) { "차 구성은 최소 1개 이상이어야 합니다" }

        val totalQuantity = teaItems.sumOf { it.quantity }
        require(totalQuantity > 0) { "총 차 수량은 0보다 커야 합니다" }

        // 중복 상품 체크
        val productIds = teaItems.map { it.productId }
        require(productIds.size == productIds.distinct().size) { "중복된 차 상품이 있습니다" }
    }
}