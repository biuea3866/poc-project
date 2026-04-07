package com.closet.order.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.common.vo.Money
import com.closet.order.domain.cart.Cart
import com.closet.order.domain.cart.CartItem
import com.closet.order.presentation.dto.AddCartItemRequest
import com.closet.order.presentation.dto.CartResponse
import com.closet.order.repository.CartItemRepository
import com.closet.order.repository.CartRepository
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
) {
    @Transactional
    fun addItem(request: AddCartItemRequest): CartResponse {
        val cart = cartRepository.findByMemberId(request.memberId) ?: cartRepository.save(Cart.create(request.memberId))

        val cartItem =
            CartItem.create(
                cartId = cart.id,
                productId = request.productId,
                productOptionId = request.productOptionId,
                quantity = request.quantity,
                unitPrice = Money(request.unitPrice),
            )
        cartItemRepository.save(cartItem)

        val items = cartItemRepository.findByCartId(cart.id)
        logger.info { "장바구니 항목 추가: cartId=${cart.id}, productId=${request.productId}" }
        return CartResponse.from(cart, items)
    }

    @Transactional
    fun updateQuantity(
        itemId: Long,
        quantity: Int,
    ): CartResponse {
        val cartItem =
            cartItemRepository.findByIdOrNull(itemId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "장바구니 항목을 찾을 수 없습니다. id=$itemId")

        cartItem.updateQuantity(quantity)

        val cart =
            cartRepository.findByIdOrNull(cartItem.cartId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "장바구니를 찾을 수 없습니다")
        val items = cartItemRepository.findByCartId(cart.id)
        return CartResponse.from(cart, items)
    }

    @Transactional
    fun removeItem(itemId: Long) {
        if (!cartItemRepository.existsById(itemId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "장바구니 항목을 찾을 수 없습니다. id=$itemId")
        }
        cartItemRepository.deleteById(itemId)
        logger.info { "장바구니 항목 삭제: itemId=$itemId" }
    }

    fun getCart(memberId: Long): CartResponse {
        val cart = cartRepository.findByMemberId(memberId) ?: return CartResponse(id = 0, memberId = memberId, items = emptyList())

        val items = cartItemRepository.findByCartId(cart.id)
        return CartResponse.from(cart, items)
    }

    @Transactional
    fun clearCart(memberId: Long) {
        val cart = cartRepository.findByMemberId(memberId) ?: return
        cartItemRepository.deleteByCartId(cart.id)
        logger.info { "장바구니 비우기: memberId=$memberId" }
    }
}
