package com.closet.promotion.presentation.discount

import com.closet.promotion.application.DiscountPolicyService
import com.closet.promotion.presentation.dto.ApplyDiscountRequest
import com.closet.promotion.presentation.dto.CreateDiscountPolicyRequest
import com.closet.promotion.presentation.dto.DiscountPolicyResponse
import com.closet.promotion.presentation.dto.DiscountResult
import com.closet.promotion.presentation.dto.StackedDiscountResult
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/discount-policies")
class DiscountPolicyController(
    private val discountPolicyService: DiscountPolicyService,
) {
    @PostMapping
    fun createPolicy(
        @Valid @RequestBody request: CreateDiscountPolicyRequest,
    ): ResponseEntity<DiscountPolicyResponse> {
        val response = discountPolicyService.createPolicy(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{id}")
    fun getPolicy(
        @PathVariable id: Long,
    ): ResponseEntity<DiscountPolicyResponse> = ResponseEntity.ok(discountPolicyService.getPolicy(id))

    @GetMapping("/applicable")
    fun getApplicablePolicies(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) orderAmount: BigDecimal?,
    ): ResponseEntity<List<DiscountPolicyResponse>> =
        ResponseEntity.ok(discountPolicyService.findApplicablePolicies(categoryId, brandId, orderAmount))

    @PostMapping("/apply/best")
    fun applyBestDiscount(
        @Valid @RequestBody request: ApplyDiscountRequest,
    ): ResponseEntity<DiscountResult> = ResponseEntity.ok(discountPolicyService.applyBestDiscount(request))

    @PostMapping("/apply/stacked")
    fun applyStackedDiscounts(
        @Valid @RequestBody request: ApplyDiscountRequest,
    ): ResponseEntity<StackedDiscountResult> = ResponseEntity.ok(discountPolicyService.applyStackedDiscounts(request))

    @PutMapping("/{id}/deactivate")
    fun deactivatePolicy(
        @PathVariable id: Long,
    ): ResponseEntity<DiscountPolicyResponse> = ResponseEntity.ok(discountPolicyService.deactivatePolicy(id))
}
