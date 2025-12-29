package com.example.cachepractice.redis.caching

import com.example.cachepractice.redis.RedisTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

@DisplayName("Redis 데이터 캐싱 E2E 테스트")
class RedisCachingE2ETest : RedisTestBase() {

    @Autowired
    private lateinit var cachingService: RedisCachingService

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Autowired(required = false)
    private var userRepository: UserRepository? = null

    @AfterEach
    fun cleanup() {
        // Redis 데이터 정리
        redisTemplate.execute<Unit> { connection ->
            connection.serverCommands().flushDb()
            null
        }
        // DB 데이터 정리
        userRepository?.deleteAll()
    }

    @Test
    @DisplayName("Look-Aside - 첫 요청은 Cache Miss, 두 번째 요청은 Cache Hit")
    fun testLookAside() {
        // given - DB에 테스트 데이터 준비
        val testUser = UserEntity(name = "홍길동", email = "hong@test.com", age = 30)
        val saved = userRepository?.save(testUser)
        val userId = saved?.id ?: 1L
        val key = "user:$userId"

        // when - 첫 번째 요청 (Cache Miss - DB에서 조회)
        val user1 = cachingService.getWithLookAside(key)

        // when - 두 번째 요청 (Cache Hit - Redis에서 조회)
        val user2 = cachingService.getWithLookAside(key)

        // then
        user1 shouldNotBe null
        user2 shouldNotBe null
        user1?.id shouldBe userId
        user2?.id shouldBe userId
        user1?.name shouldBe user2?.name
    }

    @Test
    @DisplayName("Write-Through - 캐시와 DB를 동시에 업데이트")
    fun testWriteThrough() {
        // given
        val user = User(
            id = 100L,
            name = "홍길동",
            email = "hong@example.com",
            age = 30
        )

        // when
        val savedUser = cachingService.saveWithWriteThrough(user, ttlSeconds = 60)

        // then
        savedUser shouldBe user

        // 캐시에서 조회 가능해야 함
        val key = "user:${user.id}"
        val cachedUser = redisTemplate.opsForValue().get(key) as? User
        cachedUser shouldNotBe null
        cachedUser?.id shouldBe user.id
        cachedUser?.name shouldBe user.name
    }

    @Test
    @DisplayName("Write-Behind - 캐시에 먼저 저장")
    fun testWriteBehind() {
        // given
        val user = User(
            id = 200L,
            name = "김철수",
            email = "kim@example.com",
            age = 25
        )

        // when
        val savedUser = cachingService.saveWithWriteBehind(user, ttlSeconds = 60)

        // then
        savedUser shouldBe user

        // 캐시에서 조회 가능해야 함
        val key = "user:${user.id}"
        val cachedUser = redisTemplate.opsForValue().get(key) as? User
        cachedUser shouldNotBe null
        cachedUser?.id shouldBe user.id
    }

    @Test
    @DisplayName("Cache Invalidation - 캐시 무효화")
    fun testCacheInvalidation() {
        // given
        val user = User(id = 300L, name = "이영희", email = "lee@example.com", age = 28)
        cachingService.saveWithWriteThrough(user)

        val key = "user:${user.id}"

        // when - 캐시 무효화
        val deleted = cachingService.invalidateCache(key)

        // then
        deleted shouldBe true
        redisTemplate.opsForValue().get(key) shouldBe null
    }

    @Test
    @DisplayName("Hash - 여러 필드를 하나의 키에 저장")
    fun testHash() {
        // given
        val user = User(id = 400L, name = "박민수", email = "park@example.com", age = 35)

        // when
        cachingService.saveUserWithHash(user, ttlSeconds = 60)

        // then
        val retrievedUser = cachingService.getUserFromHash(user.id)
        retrievedUser shouldNotBe null
        retrievedUser?.id shouldBe user.id
        retrievedUser?.name shouldBe user.name
        retrievedUser?.email shouldBe user.email
        retrievedUser?.age shouldBe user.age
    }

    @Test
    @DisplayName("Hash - 존재하지 않는 사용자 조회")
    fun testHashNotFound() {
        // when
        val retrievedUser = cachingService.getUserFromHash(999L)

        // then
        retrievedUser shouldBe null
    }

    @Test
    @DisplayName("List - 최근 조회 항목 관리")
    fun testRecentViewList() {
        // given
        val userId = 1L
        val productIds = listOf(101L, 102L, 103L, 104L, 105L)

        // when
        productIds.forEach { productId ->
            cachingService.addToRecentViewList(userId, productId, maxSize = 10)
        }

        // then
        val recentViews = cachingService.getRecentViewList(userId)
        recentViews shouldHaveSize 5

        // 최신 항목이 앞에 있어야 함 (LIFO)
        recentViews[0] shouldBe 105L
        recentViews[4] shouldBe 101L
    }

    @Test
    @DisplayName("List - 최대 크기 유지")
    fun testRecentViewListMaxSize() {
        // given
        val userId = 2L
        val maxSize = 5

        // when - maxSize보다 많은 항목 추가
        repeat(10) { index ->
            cachingService.addToRecentViewList(userId, (index + 1).toLong(), maxSize = maxSize)
        }

        // then
        val recentViews = cachingService.getRecentViewList(userId)
        recentViews shouldHaveSize maxSize

        // 최신 5개만 남아있어야 함
        recentViews[0] shouldBe 10L
        recentViews[4] shouldBe 6L
    }

    @Test
    @DisplayName("Sorted Set - 랭킹 시스템")
    fun testRanking() {
        // given
        val products = listOf(
            1L to 95.5,
            2L to 88.0,
            3L to 92.3,
            4L to 97.8,
            5L to 85.5
        )

        // when
        products.forEach { (productId, score) ->
            cachingService.updateRanking(productId, score)
        }

        // then
        val topRankings = cachingService.getTopRankings(3)
        topRankings shouldHaveSize 3

        // 점수가 높은 순서대로 정렬되어야 함
        topRankings[0].first shouldBe 4L // 97.8
        topRankings[0].second shouldBe 97.8

        topRankings[1].first shouldBe 1L // 95.5
        topRankings[1].second shouldBe 95.5

        topRankings[2].first shouldBe 3L // 92.3
        topRankings[2].second shouldBe 92.3
    }

    @Test
    @DisplayName("Sorted Set - 랭킹 업데이트")
    fun testRankingUpdate() {
        // given
        cachingService.updateRanking(1L, 80.0)
        cachingService.updateRanking(2L, 90.0)

        // when - 랭킹 업데이트
        cachingService.updateRanking(1L, 95.0)

        // then
        val topRankings = cachingService.getTopRankings(2)

        topRankings[0].first shouldBe 1L // 업데이트된 점수로 1등
        topRankings[0].second shouldBe 95.0

        topRankings[1].first shouldBe 2L
        topRankings[1].second shouldBe 90.0
    }

    @Test
    @DisplayName("TTL - 캐시 만료 시간 설정 확인")
    fun testTTL() {
        // given
        val user = User(id = 500L, name = "최민지", email = "choi@example.com", age = 27)
        val key = "user:${user.id}"
        val ttlSeconds = 60L

        // when
        cachingService.saveWithWriteThrough(user, ttlSeconds = ttlSeconds)

        // then - TTL이 설정되어 있어야 함
        val ttl = redisTemplate.getExpire(key)
        ttl shouldNotBe null
        ttl!! shouldBeLessThan (ttlSeconds + 1)
    }
}
