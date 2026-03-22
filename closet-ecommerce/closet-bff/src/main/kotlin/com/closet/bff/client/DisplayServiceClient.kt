package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "display-service", url = "\${service.display.url}")
interface DisplayServiceClient {

    // Banners
    @PostMapping("/display/banners")
    fun createBanner(@RequestBody request: Any): Any

    @GetMapping("/display/banners")
    fun findActiveBanners(@RequestParam position: String): Any

    @PatchMapping("/display/banners/{id}/visibility")
    fun toggleBannerVisibility(@PathVariable id: Long): Any

    // Rankings
    @GetMapping("/display/rankings")
    fun getRanking(
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "DAILY") period: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ): Any

    // Exhibitions
    @PostMapping("/display/exhibitions")
    fun createExhibition(@RequestBody request: Any): Any

    @GetMapping("/display/exhibitions/active")
    fun findActiveExhibitions(): Any

    @PatchMapping("/display/exhibitions/{id}/activate")
    fun activateExhibition(@PathVariable id: Long): Any

    @PatchMapping("/display/exhibitions/{id}/end")
    fun endExhibition(@PathVariable id: Long): Any

    @GetMapping("/display/exhibitions/{id}/products")
    fun getExhibitionProducts(@PathVariable id: Long): Any

    @PostMapping("/display/exhibitions/{id}/products")
    fun addExhibitionProduct(@PathVariable id: Long, @RequestBody request: Any): Any

    @DeleteMapping("/display/exhibitions/{exhibitionId}/products/{productId}")
    fun removeExhibitionProduct(@PathVariable exhibitionId: Long, @PathVariable productId: Long)
}
