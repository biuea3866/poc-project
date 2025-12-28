package com.example.cachepractice.redis.caching

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*

/**
 * Redis 데이터 캐싱 예제 API
 */
@RestController
@RequestMapping("/api/redis/caching")
@Profile("redis")
class RedisCachingController(
    private val cachingService: RedisCachingService
) {

    /**
     * Look-Aside 패턴 테스트
     * GET /api/redis/caching/look-aside/{userId}
     */
    @GetMapping("/look-aside/{userId}")
    fun testLookAside(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "60") ttl: Long
    ): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val user = cachingService.getWithLookAside("user:$userId", ttl)
        val duration = System.currentTimeMillis() - startTime

        return mapOf(
            "pattern" to "Look-Aside",
            "user" to (user ?: "Not found"),
            "durationMs" to duration,
            "description" to "캐시 우선 조회, 미스 시 DB 조회 후 캐싱"
        )
    }

    /**
     * Write-Through 패턴 테스트
     * POST /api/redis/caching/write-through
     */
    @PostMapping("/write-through")
    fun testWriteThrough(
        @RequestBody user: User,
        @RequestParam(defaultValue = "60") ttl: Long
    ): Map<String, Any> {
        val savedUser = cachingService.saveWithWriteThrough(user, ttl)

        return mapOf(
            "pattern" to "Write-Through",
            "user" to savedUser,
            "description" to "캐시와 DB를 동시에 업데이트"
        )
    }

    /**
     * Write-Behind 패턴 테스트
     * POST /api/redis/caching/write-behind
     */
    @PostMapping("/write-behind")
    fun testWriteBehind(
        @RequestBody user: User,
        @RequestParam(defaultValue = "60") ttl: Long
    ): Map<String, Any> {
        val savedUser = cachingService.saveWithWriteBehind(user, ttl)

        return mapOf(
            "pattern" to "Write-Behind",
            "user" to savedUser,
            "description" to "캐시에 먼저 쓰고, 나중에 비동기로 DB 업데이트"
        )
    }

    /**
     * Cache Invalidation
     * DELETE /api/redis/caching/{userId}
     */
    @DeleteMapping("/{userId}")
    fun invalidateCache(@PathVariable userId: Long): Map<String, Any> {
        val deleted = cachingService.invalidateCache("user:$userId")

        return mapOf(
            "pattern" to "Cache Invalidation",
            "userId" to userId,
            "deleted" to deleted,
            "description" to "캐시 무효화"
        )
    }

    /**
     * Hash 자료구조 테스트
     * POST /api/redis/caching/hash
     */
    @PostMapping("/hash")
    fun testHash(
        @RequestBody user: User,
        @RequestParam(defaultValue = "60") ttl: Long
    ): Map<String, Any> {
        cachingService.saveUserWithHash(user, ttl)

        return mapOf(
            "dataStructure" to "Hash",
            "user" to user,
            "description" to "Hash 자료구조로 여러 필드를 하나의 키에 저장"
        )
    }

    @GetMapping("/hash/{userId}")
    fun getFromHash(@PathVariable userId: Long): Map<String, Any> {
        val user = cachingService.getUserFromHash(userId)

        return mapOf(
            "dataStructure" to "Hash",
            "user" to (user ?: "Not found"),
            "description" to "Hash 자료구조에서 조회"
        )
    }

    /**
     * List 자료구조 테스트 - 최근 조회 항목
     * POST /api/redis/caching/recent-view
     */
    @PostMapping("/recent-view")
    fun addRecentView(@RequestBody request: RecentViewRequest): Map<String, Any> {
        cachingService.addToRecentViewList(request.userId, request.productId, request.maxSize)
        val recentViews = cachingService.getRecentViewList(request.userId)

        return mapOf(
            "dataStructure" to "List",
            "userId" to request.userId,
            "recentViews" to recentViews,
            "description" to "List 자료구조로 최근 조회 항목 관리"
        )
    }

    @GetMapping("/recent-view/{userId}")
    fun getRecentViews(@PathVariable userId: Long): Map<String, Any> {
        val recentViews = cachingService.getRecentViewList(userId)

        return mapOf(
            "dataStructure" to "List",
            "userId" to userId,
            "recentViews" to recentViews,
            "description" to "최근 조회한 상품 리스트"
        )
    }

    /**
     * Sorted Set 자료구조 테스트 - 랭킹
     * POST /api/redis/caching/ranking
     */
    @PostMapping("/ranking")
    fun updateRanking(@RequestBody request: RankingRequest): Map<String, Any> {
        cachingService.updateRanking(request.productId, request.score)

        return mapOf(
            "dataStructure" to "Sorted Set",
            "productId" to request.productId,
            "score" to request.score,
            "description" to "Sorted Set으로 랭킹 업데이트"
        )
    }

    @GetMapping("/ranking/top/{topN}")
    fun getTopRankings(@PathVariable topN: Int): Map<String, Any> {
        val rankings = cachingService.getTopRankings(topN)

        return mapOf(
            "dataStructure" to "Sorted Set",
            "topN" to topN,
            "rankings" to rankings.map { (productId, score) ->
                mapOf("productId" to productId, "score" to score)
            },
            "description" to "Sorted Set으로 상위 랭킹 조회"
        )
    }
}

data class RecentViewRequest(
    val userId: Long,
    val productId: Long,
    val maxSize: Int = 10
)

data class RankingRequest(
    val productId: Long,
    val score: Double
)
