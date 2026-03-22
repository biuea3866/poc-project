package com.closet.bff.presentation

import com.closet.bff.client.SearchServiceClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchProxyController(
    private val searchClient: SearchServiceClient,
) {

    @GetMapping("/products")
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
    ) = searchClient.searchProducts(keyword, categoryId, brandId, minPrice, maxPrice, gender, season, sort, page, size)

    @GetMapping("/autocomplete")
    fun autocomplete(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "5") limit: Int,
    ) = searchClient.autocomplete(query, limit)

    @PostMapping("/index")
    fun reindex() = searchClient.reindex()

    @PostMapping("/index/sync")
    fun syncFromProductService() = searchClient.syncFromProductService()
}
