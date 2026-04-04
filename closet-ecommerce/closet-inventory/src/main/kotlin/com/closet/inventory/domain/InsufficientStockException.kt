package com.closet.inventory.domain

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode

class InsufficientStockException(
    val productOptionId: Long,
    val sku: String,
    val requested: Int,
    val available: Int,
) : BusinessException(
    ErrorCode.INVALID_INPUT,
    "재고가 부족합니다. sku=$sku, requested=$requested, available=$available"
)
