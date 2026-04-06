package com.closet.product.application.dto

import com.closet.product.domain.entity.Brand
import com.closet.product.domain.entity.Category
import com.closet.product.domain.entity.Product
import com.closet.product.domain.entity.ProductImage
import com.closet.product.domain.entity.ProductOption
import com.closet.product.domain.enums.FitType
import com.closet.product.domain.enums.Gender
import com.closet.product.domain.enums.ImageType
import com.closet.product.domain.enums.ProductStatus
import com.closet.product.domain.enums.Season
import com.closet.product.domain.enums.Size
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

// ===== Product =====

data class ProductCreateRequest(
    @field:NotBlank(message = "상품명은 필수입니다")
    val name: String,
    @field:NotBlank(message = "상품 설명은 필수입니다")
    val description: String,
    @field:NotNull(message = "브랜드 ID는 필수입니다")
    val brandId: Long,
    @field:NotNull(message = "카테고리 ID는 필수입니다")
    val categoryId: Long,
    @field:Min(0, message = "정가는 0 이상이어야 합니다")
    val basePrice: BigDecimal,
    @field:Min(0, message = "판매가는 0 이상이어야 합니다")
    val salePrice: BigDecimal,
    val discountRate: Int = 0,
    val season: Season? = null,
    val fitType: FitType? = null,
    val gender: Gender? = null,
)

data class ProductUpdateRequest(
    @field:NotBlank(message = "상품명은 필수입니다")
    val name: String,
    @field:NotBlank(message = "상품 설명은 필수입니다")
    val description: String,
    @field:NotNull(message = "브랜드 ID는 필수입니다")
    val brandId: Long,
    @field:NotNull(message = "카테고리 ID는 필수입니다")
    val categoryId: Long,
    @field:Min(0, message = "정가는 0 이상이어야 합니다")
    val basePrice: BigDecimal,
    @field:Min(0, message = "판매가는 0 이상이어야 합니다")
    val salePrice: BigDecimal,
    val discountRate: Int = 0,
    val season: Season? = null,
    val fitType: FitType? = null,
    val gender: Gender? = null,
)

data class ProductStatusChangeRequest(
    @field:NotNull(message = "상태값은 필수입니다")
    val status: ProductStatus,
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String,
    val brandId: Long,
    val categoryId: Long,
    val basePrice: BigDecimal,
    val salePrice: BigDecimal,
    val discountRate: Int,
    val status: ProductStatus,
    val season: Season?,
    val fitType: FitType?,
    val gender: Gender?,
    val options: List<ProductOptionResponse>,
    val images: List<ProductImageResponse>,
) {
    companion object {
        fun from(product: Product): ProductResponse =
            ProductResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                brandId = product.brandId,
                categoryId = product.categoryId,
                basePrice = product.basePrice.amount,
                salePrice = product.salePrice.amount,
                discountRate = product.discountRate,
                status = product.status,
                season = product.season,
                fitType = product.fitType,
                gender = product.gender,
                options = product.options.map { ProductOptionResponse.from(it) },
                images = product.images.map { ProductImageResponse.from(it) },
            )
    }
}

data class ProductListResponse(
    val id: Long,
    val name: String,
    val brandId: Long,
    val categoryId: Long,
    val basePrice: BigDecimal,
    val salePrice: BigDecimal,
    val discountRate: Int,
    val status: ProductStatus,
    val season: Season?,
    val fitType: FitType?,
    val gender: Gender?,
) {
    companion object {
        fun from(product: Product): ProductListResponse =
            ProductListResponse(
                id = product.id,
                name = product.name,
                brandId = product.brandId,
                categoryId = product.categoryId,
                basePrice = product.basePrice.amount,
                salePrice = product.salePrice.amount,
                discountRate = product.discountRate,
                status = product.status,
                season = product.season,
                fitType = product.fitType,
                gender = product.gender,
            )
    }
}

// ===== ProductOption =====

data class ProductOptionCreateRequest(
    @field:NotNull(message = "사이즈는 필수입니다")
    val size: Size,
    @field:NotBlank(message = "색상명은 필수입니다")
    val colorName: String,
    @field:NotBlank(message = "색상 HEX 코드는 필수입니다")
    val colorHex: String,
    @field:NotBlank(message = "SKU 코드는 필수입니다")
    val skuCode: String,
    val additionalPrice: BigDecimal = BigDecimal.ZERO,
)

data class ProductOptionResponse(
    val id: Long,
    val size: Size,
    val colorName: String,
    val colorHex: String,
    val skuCode: String,
    val additionalPrice: BigDecimal,
) {
    companion object {
        fun from(option: ProductOption): ProductOptionResponse =
            ProductOptionResponse(
                id = option.id,
                size = option.size,
                colorName = option.colorName,
                colorHex = option.colorHex,
                skuCode = option.skuCode,
                additionalPrice = option.additionalPrice.amount,
            )
    }
}

// ===== ProductImage =====

data class ProductImageResponse(
    val id: Long,
    val imageUrl: String,
    val type: ImageType,
    val sortOrder: Int,
) {
    companion object {
        fun from(image: ProductImage): ProductImageResponse =
            ProductImageResponse(
                id = image.id,
                imageUrl = image.imageUrl,
                type = image.type,
                sortOrder = image.sortOrder,
            )
    }
}

// ===== Category =====

data class CategoryCreateRequest(
    @field:NotBlank(message = "카테고리명은 필수입니다")
    val name: String,
    val parentId: Long? = null,
    val depth: Int = 1,
    val sortOrder: Int = 0,
)

data class CategoryResponse(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val depth: Int,
    val sortOrder: Int,
    val status: Boolean,
    val children: List<CategoryResponse> = emptyList(),
) {
    companion object {
        fun from(
            category: Category,
            children: List<CategoryResponse> = emptyList(),
        ): CategoryResponse =
            CategoryResponse(
                id = category.id,
                parentId = category.parentId,
                name = category.name,
                depth = category.depth,
                sortOrder = category.sortOrder,
                status = category.status,
                children = children,
            )
    }
}

// ===== Brand =====

data class BrandCreateRequest(
    @field:NotBlank(message = "브랜드명은 필수입니다")
    val name: String,
    val logoUrl: String? = null,
    val description: String? = null,
    @field:NotNull(message = "셀러 ID는 필수입니다")
    val sellerId: Long,
)

data class BrandResponse(
    val id: Long,
    val name: String,
    val logoUrl: String?,
    val description: String?,
    val sellerId: Long,
    val status: Boolean,
) {
    companion object {
        fun from(brand: Brand): BrandResponse =
            BrandResponse(
                id = brand.id,
                name = brand.name,
                logoUrl = brand.logoUrl,
                description = brand.description,
                sellerId = brand.sellerId,
                status = brand.status,
            )
    }
}
