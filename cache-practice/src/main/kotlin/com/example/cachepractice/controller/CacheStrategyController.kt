package com.example.cachepractice.controller

import com.example.cachepractice.domain.Product
import com.example.cachepractice.service.problem.BloomFilterService
import com.example.cachepractice.service.problem.EarlyRefreshService
import com.example.cachepractice.service.problem.ProbabilisticEarlyRecomputationService
import com.example.cachepractice.service.strategy.LookAsideWriteAroundService
import com.example.cachepractice.service.strategy.ReadThroughWriteAroundService
import com.example.cachepractice.service.strategy.ReadThroughWriteThroughService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * 캐시 전략 및 문제 해결 REST API
 *
 * 엔드포인트 구성:
 * - /api/strategy/look-aside-write-around: Look Aside + Write Around 전략
 * - /api/strategy/read-through-write-around: Read Through + Write Around 전략
 * - /api/strategy/read-through-write-through: Read Through + Write Through 전략
 * - /api/problem/early-refresh: 조기 갱신 (캐시 스템피드 해결)
 * - /api/problem/per: PER (캐시 스템피드 해결)
 * - /api/problem/bloom-filter: 블룸 필터 (캐시 관통 해결)
 */
@RestController
@RequestMapping("/api")
class CacheStrategyController(
    private val lookAsideWriteAroundService: LookAsideWriteAroundService,
    private val readThroughWriteAroundService: ReadThroughWriteAroundService,
    private val readThroughWriteThroughService: ReadThroughWriteThroughService,
    private val earlyRefreshService: EarlyRefreshService,
    private val perService: ProbabilisticEarlyRecomputationService,
    private val bloomFilterService: BloomFilterService
) {

    // ========== Look Aside + Write Around ==========

    @GetMapping("/strategy/look-aside-write-around/{id}")
    fun lookAsideGet(@PathVariable id: Long): ResponseEntity<Product> {
        val product = lookAsideWriteAroundService.getProduct(id)
        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    @PostMapping("/strategy/look-aside-write-around")
    fun lookAsideCreate(@RequestBody request: ProductRequest): ResponseEntity<Product> {
        val product = Product(
            id = request.id ?: 0L,
            name = request.name,
            price = request.price
        )
        val saved = lookAsideWriteAroundService.createOrUpdateProduct(product)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/strategy/look-aside-write-around/{id}")
    fun lookAsideDelete(@PathVariable id: Long): ResponseEntity<Void> {
        lookAsideWriteAroundService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/strategy/look-aside-write-around/cache")
    fun lookAsideClearCache(): ResponseEntity<String> {
        lookAsideWriteAroundService.clearCache()
        return ResponseEntity.ok("Look Aside + Write Around 캐시 초기화 완료")
    }

    // ========== Read Through + Write Around ==========

    @GetMapping("/strategy/read-through-write-around/{id}")
    fun readThroughWriteAroundGet(@PathVariable id: Long): ResponseEntity<Product> {
        val product = readThroughWriteAroundService.getProduct(id)
        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    @PostMapping("/strategy/read-through-write-around")
    fun readThroughWriteAroundCreate(@RequestBody request: ProductRequest): ResponseEntity<Product> {
        val product = Product(
            id = request.id ?: 0L,
            name = request.name,
            price = request.price
        )
        val saved = readThroughWriteAroundService.createOrUpdateProduct(product)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/strategy/read-through-write-around/{id}")
    fun readThroughWriteAroundDelete(@PathVariable id: Long): ResponseEntity<Void> {
        readThroughWriteAroundService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/strategy/read-through-write-around/stats")
    fun readThroughWriteAroundStats(): ResponseEntity<String> {
        return ResponseEntity.ok(readThroughWriteAroundService.getCacheStats())
    }

    @DeleteMapping("/strategy/read-through-write-around/cache")
    fun readThroughWriteAroundClearCache(): ResponseEntity<String> {
        readThroughWriteAroundService.clearCache()
        return ResponseEntity.ok("Read Through + Write Around 캐시 초기화 완료")
    }

    // ========== Read Through + Write Through ==========

    @GetMapping("/strategy/read-through-write-through/{id}")
    fun readThroughWriteThroughGet(@PathVariable id: Long): ResponseEntity<Product> {
        val product = readThroughWriteThroughService.getProduct(id)
        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    @PostMapping("/strategy/read-through-write-through")
    fun readThroughWriteThroughCreate(@RequestBody request: ProductRequest): ResponseEntity<Product> {
        val product = Product(
            id = request.id ?: 0L,
            name = request.name,
            price = request.price
        )
        val saved = readThroughWriteThroughService.createOrUpdateProduct(product)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/strategy/read-through-write-through/{id}")
    fun readThroughWriteThroughDelete(@PathVariable id: Long): ResponseEntity<Void> {
        readThroughWriteThroughService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/strategy/read-through-write-through/stats")
    fun readThroughWriteThroughStats(): ResponseEntity<String> {
        return ResponseEntity.ok(readThroughWriteThroughService.getCacheStats())
    }

    @DeleteMapping("/strategy/read-through-write-through/cache")
    fun readThroughWriteThroughClearCache(): ResponseEntity<String> {
        readThroughWriteThroughService.clearCache()
        return ResponseEntity.ok("Read Through + Write Through 캐시 초기화 완료")
    }

    // ========== Early Refresh (조기 갱신) ==========

    @GetMapping("/problem/early-refresh/{id}")
    fun earlyRefreshGet(@PathVariable id: Long): ResponseEntity<Product> {
        val product = earlyRefreshService.getProduct(id)
        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    @PostMapping("/problem/early-refresh/hotkey/{id}")
    fun earlyRefreshAddHotKey(@PathVariable id: Long): ResponseEntity<String> {
        earlyRefreshService.addHotKey(id)
        return ResponseEntity.ok("핫키 추가: $id")
    }

    @DeleteMapping("/problem/early-refresh/hotkey/{id}")
    fun earlyRefreshRemoveHotKey(@PathVariable id: Long): ResponseEntity<String> {
        earlyRefreshService.removeHotKey(id)
        return ResponseEntity.ok("핫키 제거: $id")
    }

    @PostMapping("/problem/early-refresh/refresh")
    fun earlyRefreshTrigger(): ResponseEntity<String> {
        earlyRefreshService.refreshAllCache()
        return ResponseEntity.ok("전체 캐시 강제 갱신 완료")
    }

    @GetMapping("/problem/early-refresh/stats")
    fun earlyRefreshStats(): ResponseEntity<String> {
        return ResponseEntity.ok(earlyRefreshService.getCacheStats())
    }

    @DeleteMapping("/problem/early-refresh/cache")
    fun earlyRefreshClearCache(): ResponseEntity<String> {
        earlyRefreshService.clearCache()
        return ResponseEntity.ok("Early Refresh 캐시 초기화 완료")
    }

    // ========== PER (Probabilistic Early Recomputation) ==========

    @GetMapping("/problem/per/{id}")
    fun perGet(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "1.0") beta: Double
    ): ResponseEntity<Product> {
        val product = perService.getProduct(id, beta)
        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    @PostMapping("/problem/per")
    fun perCreate(@RequestBody request: ProductRequest): ResponseEntity<Product> {
        val product = Product(
            id = request.id ?: 0L,
            name = request.name,
            price = request.price
        )
        val saved = perService.createOrUpdateProduct(product)
        return ResponseEntity.ok(saved)
    }

    @DeleteMapping("/problem/per/{id}")
    fun perDelete(@PathVariable id: Long): ResponseEntity<Void> {
        perService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/problem/per/stats")
    fun perStats(): ResponseEntity<String> {
        return ResponseEntity.ok(perService.getCacheStats())
    }

    @PostMapping("/problem/per/test-beta/{id}")
    fun perTestBeta(@PathVariable id: Long): ResponseEntity<String> {
        val betaValues = listOf(0.5, 1.0, 2.0, 5.0)
        perService.testBetaImpact(id, betaValues)
        return ResponseEntity.ok("Beta 테스트 완료 (로그 확인)")
    }

    @DeleteMapping("/problem/per/cache")
    fun perClearCache(): ResponseEntity<String> {
        perService.clearCache()
        return ResponseEntity.ok("PER 캐시 초기화 완료")
    }

    // ========== Bloom Filter ==========

    @GetMapping("/problem/bloom-filter/{id}")
    fun bloomFilterGet(@PathVariable id: Long): ResponseEntity<Product> {
        val product = bloomFilterService.getProduct(id)
        return if (product.isNotFound()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(product)
        }
    }

    @PostMapping("/problem/bloom-filter")
    fun bloomFilterCreate(@RequestBody request: ProductRequest): ResponseEntity<Product> {
        val product = Product(
            id = request.id ?: 0L,
            name = request.name,
            price = request.price
        )
        val saved = bloomFilterService.createProduct(product)
        return ResponseEntity.ok(saved)
    }

    @PutMapping("/problem/bloom-filter/{id}")
    fun bloomFilterUpdate(
        @PathVariable id: Long,
        @RequestBody request: ProductRequest
    ): ResponseEntity<Product> {
        val product = Product(
            id = id,
            name = request.name,
            price = request.price
        )
        val updated = bloomFilterService.updateProduct(id, product)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/problem/bloom-filter/{id}")
    fun bloomFilterDelete(@PathVariable id: Long): ResponseEntity<Void> {
        bloomFilterService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/problem/bloom-filter/stats")
    fun bloomFilterStats(): ResponseEntity<String> {
        return ResponseEntity.ok(bloomFilterService.getStats())
    }

    @DeleteMapping("/problem/bloom-filter/cache")
    fun bloomFilterClearAll(): ResponseEntity<String> {
        bloomFilterService.clearAll()
        return ResponseEntity.ok("Bloom Filter 캐시 및 필터 초기화 완료")
    }
}

/**
 * 제품 요청 DTO
 */
data class ProductRequest(
    val id: Long? = null,
    val name: String,
    val price: BigDecimal
)
