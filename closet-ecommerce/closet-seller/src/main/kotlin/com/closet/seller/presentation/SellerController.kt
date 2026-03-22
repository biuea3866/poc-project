package com.closet.seller.presentation

import com.closet.common.response.ApiResponse
import com.closet.seller.application.SellerApplicationService
import com.closet.seller.application.SellerService
import com.closet.seller.application.SellerSettlementAccountService
import com.closet.seller.domain.ApplicationStatus
import com.closet.seller.presentation.dto.ApproveApplicationRequest
import com.closet.seller.presentation.dto.RegisterSettlementAccountRequest
import com.closet.seller.presentation.dto.RejectApplicationRequest
import com.closet.seller.presentation.dto.SellerApplicationResponse
import com.closet.seller.presentation.dto.SellerApplyRequest
import com.closet.seller.presentation.dto.SellerResponse
import com.closet.seller.presentation.dto.SettlementAccountResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sellers")
class SellerController(
    private val sellerService: SellerService,
    private val sellerApplicationService: SellerApplicationService,
    private val settlementAccountService: SellerSettlementAccountService,
) {
    /** 입점 신청 */
    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    fun apply(@Valid @RequestBody request: SellerApplyRequest): ApiResponse<SellerApplicationResponse> {
        return ApiResponse.created(sellerApplicationService.apply(request))
    }

    /** 신청 목록 조회 */
    @GetMapping("/applications")
    fun getApplications(
        @RequestParam(required = false, defaultValue = "SUBMITTED") status: ApplicationStatus,
    ): ApiResponse<List<SellerApplicationResponse>> {
        return ApiResponse.ok(sellerApplicationService.findByStatus(status))
    }

    /** 심사 시작 */
    @PatchMapping("/applications/{id}/review")
    fun startReview(@PathVariable id: Long): ApiResponse<SellerApplicationResponse> {
        return ApiResponse.ok(sellerApplicationService.startReview(id))
    }

    /** 승인 */
    @PatchMapping("/applications/{id}/approve")
    fun approve(
        @PathVariable id: Long,
        @Valid @RequestBody request: ApproveApplicationRequest,
    ): ApiResponse<SellerResponse> {
        return ApiResponse.ok(sellerApplicationService.approve(id, request.commissionRate))
    }

    /** 반려 */
    @PatchMapping("/applications/{id}/reject")
    fun reject(
        @PathVariable id: Long,
        @Valid @RequestBody request: RejectApplicationRequest,
    ): ApiResponse<SellerApplicationResponse> {
        return ApiResponse.ok(sellerApplicationService.reject(id, request.reason))
    }

    /** 셀러 정보 조회 */
    @GetMapping("/{id}")
    fun getSeller(@PathVariable id: Long): ApiResponse<SellerResponse> {
        return ApiResponse.ok(sellerService.findById(id))
    }

    /** 셀러 목록 조회 */
    @GetMapping
    fun getSellers(): ApiResponse<List<SellerResponse>> {
        return ApiResponse.ok(sellerService.findAll())
    }

    /** 정산 계좌 등록 */
    @PostMapping("/{id}/settlement-account")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerSettlementAccount(
        @PathVariable id: Long,
        @Valid @RequestBody request: RegisterSettlementAccountRequest,
    ): ApiResponse<SettlementAccountResponse> {
        return ApiResponse.created(settlementAccountService.register(id, request))
    }
}
