package com.closet.bff.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "content-service", url = "\${service.content.url}")
interface ContentServiceClient {

    // Magazines
    @PostMapping("/content/magazines")
    fun createMagazine(@RequestBody request: Any): Any

    @GetMapping("/content/magazines")
    fun getMagazines(
        @RequestParam(required = false) tag: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any

    @GetMapping("/content/magazines/{id}")
    fun getMagazine(@PathVariable id: Long): Any

    @PatchMapping("/content/magazines/{id}/publish")
    fun publishMagazine(@PathVariable id: Long): Any

    @PatchMapping("/content/magazines/{id}/archive")
    fun archiveMagazine(@PathVariable id: Long): Any

    // OOTD Snaps
    @PostMapping("/content/ootd-snaps")
    fun createOotdSnap(@RequestHeader("X-Member-Id") memberId: Long, @RequestBody request: Any): Any

    @GetMapping("/content/ootd-snaps")
    fun getOotdSnaps(
        @RequestParam(required = false) memberId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any

    @PostMapping("/content/ootd-snaps/{id}/like")
    fun likeOotdSnap(@PathVariable id: Long): Any

    @DeleteMapping("/content/ootd-snaps/{id}")
    fun deleteOotdSnap(@PathVariable id: Long)

    // Coordinations
    @PostMapping("/content/coordinations")
    fun createCoordination(@RequestBody request: Any): Any

    @GetMapping("/content/coordinations")
    fun getCoordinations(
        @RequestParam(required = false) style: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Any

    @GetMapping("/content/coordinations/{id}")
    fun getCoordination(@PathVariable id: Long): Any

    @PostMapping("/content/coordinations/{id}/products")
    fun addCoordinationProduct(@PathVariable id: Long, @RequestBody request: Any): Any
}
