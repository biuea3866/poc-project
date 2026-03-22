package com.closet.bff.presentation

import com.closet.bff.client.DisplayServiceClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/display")
class DisplayProxyController(
    private val displayClient: DisplayServiceClient,
) {

    // Banners
    @PostMapping("/banners")
    fun createBanner(@RequestBody request: Any) =
        displayClient.createBanner(request)

    @GetMapping("/banners")
    fun findActiveBanners(@RequestParam position: String) =
        displayClient.findActiveBanners(position)

    @PatchMapping("/banners/{id}/visibility")
    fun toggleBannerVisibility(@PathVariable id: Long) =
        displayClient.toggleBannerVisibility(id)

    // Rankings
    @GetMapping("/rankings")
    fun getRanking(
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "DAILY") period: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ) = displayClient.getRanking(categoryId, period, limit)

    // Exhibitions
    @PostMapping("/exhibitions")
    fun createExhibition(@RequestBody request: Any) =
        displayClient.createExhibition(request)

    @GetMapping("/exhibitions/active")
    fun findActiveExhibitions() =
        displayClient.findActiveExhibitions()

    @PatchMapping("/exhibitions/{id}/activate")
    fun activateExhibition(@PathVariable id: Long) =
        displayClient.activateExhibition(id)

    @PatchMapping("/exhibitions/{id}/end")
    fun endExhibition(@PathVariable id: Long) =
        displayClient.endExhibition(id)

    @GetMapping("/exhibitions/{id}/products")
    fun getExhibitionProducts(@PathVariable id: Long) =
        displayClient.getExhibitionProducts(id)

    @PostMapping("/exhibitions/{id}/products")
    fun addExhibitionProduct(@PathVariable id: Long, @RequestBody request: Any) =
        displayClient.addExhibitionProduct(id, request)

    @DeleteMapping("/exhibitions/{exhibitionId}/products/{productId}")
    fun removeExhibitionProduct(
        @PathVariable exhibitionId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        displayClient.removeExhibitionProduct(exhibitionId, productId)
        return ResponseEntity.noContent().build()
    }
}
