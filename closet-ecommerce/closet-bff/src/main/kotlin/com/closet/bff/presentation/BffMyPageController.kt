package com.closet.bff.presentation

import com.closet.bff.facade.MyPageBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff")
class BffMyPageController(
    private val myPageFacade: MyPageBffFacade,
) {
    @GetMapping("/mypage")
    fun getMyPage(@RequestHeader("X-Member-Id") memberId: Long) = ApiResponse.ok(myPageFacade.getMyPage(memberId))
}
