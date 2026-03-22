package com.closet.bff.presentation

import com.closet.bff.client.ContentServiceClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/content")
class ContentProxyController(
    private val contentClient: ContentServiceClient,
) {

    // Magazines
    @PostMapping("/magazines")
    fun createMagazine(@RequestBody request: Any) =
        contentClient.createMagazine(request)

    @GetMapping("/magazines")
    fun getMagazines(
        @RequestParam(required = false) tag: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = contentClient.getMagazines(tag, page, size)

    @GetMapping("/magazines/{id}")
    fun getMagazine(@PathVariable id: Long) =
        contentClient.getMagazine(id)

    @PatchMapping("/magazines/{id}/publish")
    fun publishMagazine(@PathVariable id: Long) =
        contentClient.publishMagazine(id)

    @PatchMapping("/magazines/{id}/archive")
    fun archiveMagazine(@PathVariable id: Long) =
        contentClient.archiveMagazine(id)

    // OOTD Snaps
    @PostMapping("/ootd-snaps")
    fun createOotdSnap(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: Any,
    ) = contentClient.createOotdSnap(memberId, request)

    @GetMapping("/ootd-snaps")
    fun getOotdSnaps(
        @RequestParam(required = false) memberId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = contentClient.getOotdSnaps(memberId, page, size)

    @PostMapping("/ootd-snaps/{id}/like")
    fun likeOotdSnap(@PathVariable id: Long) =
        contentClient.likeOotdSnap(id)

    @DeleteMapping("/ootd-snaps/{id}")
    fun deleteOotdSnap(@PathVariable id: Long): ResponseEntity<Void> {
        contentClient.deleteOotdSnap(id)
        return ResponseEntity.noContent().build()
    }

    // Coordinations
    @PostMapping("/coordinations")
    fun createCoordination(@RequestBody request: Any) =
        contentClient.createCoordination(request)

    @GetMapping("/coordinations")
    fun getCoordinations(
        @RequestParam(required = false) style: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = contentClient.getCoordinations(style, page, size)

    @GetMapping("/coordinations/{id}")
    fun getCoordination(@PathVariable id: Long) =
        contentClient.getCoordination(id)

    @PostMapping("/coordinations/{id}/products")
    fun addCoordinationProduct(@PathVariable id: Long, @RequestBody request: Any) =
        contentClient.addCoordinationProduct(id, request)
}
