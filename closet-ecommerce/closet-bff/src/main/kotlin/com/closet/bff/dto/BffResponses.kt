package com.closet.bff.dto

// === BFF Aggregated Responses ===

/** 상품 상세 (상품 + 리뷰 요약 + 재고 상태) */
data class ProductDetailBffResponse(
    val product: ProductResponse,
    val reviewSummary: ReviewSummaryResponse?, // Phase 2
    val relatedProducts: List<ProductResponse>?, // 같은 카테고리 상품
)

/** 주문 상세 (주문 + 결제 + 배송) */
data class OrderDetailBffResponse(
    val order: OrderResponse,
    val payment: PaymentResponse?,
    val shipment: ShipmentResponse?, // Phase 2
)

/** 마이페이지 (회원 + 최근 주문 + 포인트) */
data class MyPageBffResponse(
    val member: MemberResponse,
    val recentOrders: List<OrderResponse>,
    val addresses: List<ShippingAddressResponse>,
)

/** 체크아웃 (장바구니 + 배송지 + 쿠폰) */
data class CheckoutBffResponse(
    val cart: CartResponse,
    val addresses: List<ShippingAddressResponse>,
    val defaultAddress: ShippingAddressResponse?,
    val availableCoupons: List<CouponResponse>?, // Phase 3
)

/** 메인 페이지 (배너 + 랭킹 + 신상품 + 기획전) */
data class HomeBffResponse(
    val banners: List<BannerResponse>?, // Phase 3
    val rankings: List<ProductResponse>,
    val newArrivals: List<ProductResponse>,
    val exhibitions: List<ExhibitionResponse>?, // Phase 3
)

// === Downstream Service Response DTOs ===

data class ProductResponse(
    val id: Long,
    val name: String,
    val brandName: String?,
    val basePrice: Long,
    val salePrice: Long,
    val discountRate: Int,
    val status: String,
    val imageUrl: String?,
)

data class MemberResponse(
    val id: Long,
    val email: String,
    val name: String,
    val grade: String,
    val pointBalance: Int,
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val totalAmount: Long,
    val paymentAmount: Long,
    val status: String,
    val orderedAt: String,
    val items: List<OrderItemResponse>?,
)

data class OrderItemResponse(
    val id: Long,
    val productName: String,
    val optionName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long,
    val status: String,
)

data class PaymentResponse(
    val id: Long,
    val orderId: Long,
    val paymentKey: String?,
    val method: String?,
    val finalAmount: Long,
    val status: String,
)

data class CartResponse(
    val id: Long,
    val items: List<CartItemResponse>,
)

data class CartItemResponse(
    val id: Long,
    val productId: Long,
    val productName: String?,
    val optionName: String?,
    val quantity: Int,
    val unitPrice: Long,
)

data class ShippingAddressResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
    val isDefault: Boolean,
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val depth: Int,
    val children: List<CategoryResponse>?,
)

data class BrandResponse(
    val id: Long,
    val name: String,
    val logoUrl: String?,
)

data class ReviewSummaryResponse(
    val averageRating: Double,
    val totalCount: Int,
) // Phase 2

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

// === BFF Request DTOs ===

data class CreateOrderBffRequest(
    val items: List<OrderItemRequest>,
    val shippingAddressId: Long,
    val couponId: Long? = null,
    val pointAmount: Int = 0,
)

data class OrderItemRequest(
    val productId: Long,
    val productOptionId: Long,
    val quantity: Int,
)

data class ConfirmPaymentBffRequest(
    val paymentKey: String,
    val orderId: Long,
    val amount: Long,
)

data class ConfirmPaymentRequest(
    val paymentKey: String,
    val orderId: Long,
    val amount: Long,
)

data class CancelRequest(
    val reason: String,
)

data class AddCartItemRequest(
    val productId: Long,
    val productOptionId: Long,
    val quantity: Int,
)

data class UpdateQuantityRequest(
    val quantity: Int,
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String? = null,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
)

data class AddAddressRequest(
    val name: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
)

data class UpdateAddressRequest(
    val name: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
)

// Phase 2 shipping DTOs
data class ShipmentResponse(
    val id: Long,
    val trackingNumber: String?,
    val status: String,
)

data class ShipmentBffResponse(
    val id: Long,
    val orderId: Long,
    val carrier: String?,
    val trackingNumber: String?,
    val status: String,
    val receiverName: String?,
    val shippedAt: String?,
    val deliveredAt: String?,
)

data class TrackingLogBffResponse(
    val id: Long,
    val shippingId: Long,
    val carrierStatus: String,
    val mappedStatus: String,
    val location: String?,
    val description: String?,
    val trackedAt: String?,
)

data class ReturnRequestBffResponse(
    val id: Long,
    val orderId: Long,
    val status: String,
    val reason: String,
    val refundAmount: Long,
    val shippingFee: Long,
)

data class ExchangeRequestResponse(
    val id: Long,
    val orderId: Long,
    val status: String,
    val reason: String,
    val originalProductOptionId: Long,
    val newProductOptionId: Long,
    val quantity: Int,
)

// Phase 2 inventory DTOs
data class InventoryBffResponse(
    val id: Long,
    val productId: Long,
    val productOptionId: Long,
    val sku: String,
    val availableQuantity: Int,
    val outOfStock: Boolean,
)

// Phase 2 search DTOs
data class SearchProductBffResponse(
    val productId: Long,
    val name: String,
    val brandName: String?,
    val salePrice: Long,
    val imageUrl: String?,
    val reviewCount: Int,
    val avgRating: Double,
)

data class AutocompleteBffResponse(
    val productId: Long,
    val name: String,
    val brandName: String?,
)

data class PopularKeywordBffResponse(
    val rank: Int,
    val keyword: String,
    val score: Long,
)

data class SearchPageBffResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

// Phase 2 review DTOs
data class ReviewBffResponse(
    val id: Long,
    val productId: Long,
    val memberId: Long,
    val rating: Int,
    val content: String,
    val hasImage: Boolean,
    val createdAt: String?,
)

data class ReviewSummaryBffResponse(
    val productId: Long,
    val totalCount: Int,
    val avgRating: Double,
    val ratingDistribution: Map<Int, Int>?,
    val photoReviewCount: Int,
)

// Phase 2/3 placeholders
data class CouponResponse(
    val id: Long,
    val name: String,
    val discountValue: Long,
    val couponType: String,
)

data class BannerResponse(
    val id: Long,
    val title: String,
    val imageUrl: String,
    val linkUrl: String,
)

data class ExhibitionResponse(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
)
