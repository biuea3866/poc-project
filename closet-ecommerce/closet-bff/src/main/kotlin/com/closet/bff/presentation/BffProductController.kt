package com.closet.bff.presentation

import com.closet.bff.facade.HomeBffFacade
import com.closet.bff.facade.ProductBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff")
class BffProductController(
    private val productFacade: ProductBffFacade,
    private val homeFacade: HomeBffFacade,
) {
    @GetMapping("/products/{id}")
    fun getProductDetail(@PathVariable id: Long) = ApiResponse.ok(productFacade.getProductDetail(id))

    @GetMapping("/home")
    fun getHome() = ApiResponse.ok(homeFacade.getHome())
}
