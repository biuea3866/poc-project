package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "search-service", url = "\${service.search.url}")
interface SearchServiceClient {

    @GetMapping("/search/products")
    fun searchProducts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(required = false) gender: String?,
        @RequestParam(required = false) season: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any

    @GetMapping("/search/autocomplete")
    fun autocomplete(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "5") limit: Int,
    ): Any

    @PostMapping("/search/index")
    fun reindex(): Any

    @PostMapping("/search/index/sync")
    fun syncFromProductService(): Any
}
