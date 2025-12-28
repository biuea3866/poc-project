package com.example.cachepractice.redis.caching

import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Redis 데이터 캐싱 예제
 * - Look-Aside 패턴: 애플리케이션이 캐시를 먼저 확인하고 없으면 DB 조회
 * - Write-Through 패턴: 데이터 쓰기 시 캐시와 DB를 동시에 업데이트
 * - TTL을 이용한 자동 만료
 */
@Service
@Profile("redis")
class RedisCachingService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val userRepository: UserRepository?
) {

    /**
     * Look-Aside 패턴
     * 1. 캐시에서 먼저 조회
     * 2. 캐시 미스 시 DB에서 조회 후 캐시에 저장
     */
    fun getWithLookAside(key: String, ttlSeconds: Long = 60): User? {
        // 1. 캐시 조회
        val cached = redisTemplate.opsForValue().get(key)
        if (cached != null) {
            return cached as User
        }

        // 2. 캐시 미스 - DB 조회 (시뮬레이션)
        val userFromDb = fetchFromDatabase(key)

        // 3. 캐시에 저장 (TTL 설정)
        if (userFromDb != null) {
            redisTemplate.opsForValue().set(key, userFromDb, ttlSeconds, TimeUnit.SECONDS)
        }

        return userFromDb
    }

    /**
     * Write-Through 패턴
     * 데이터 쓰기 시 캐시와 DB를 동시에 업데이트
     */
    fun saveWithWriteThrough(user: User, ttlSeconds: Long = 60): User {
        val key = "user:${user.id}"

        // 1. DB 저장 (시뮬레이션)
        saveToDatabase(user)

        // 2. 캐시 저장
        redisTemplate.opsForValue().set(key, user, ttlSeconds, TimeUnit.SECONDS)

        return user
    }

    /**
     * Write-Behind (Write-Back) 패턴
     * 캐시에만 먼저 쓰고, 나중에 비동기로 DB 업데이트
     */
    fun saveWithWriteBehind(user: User, ttlSeconds: Long = 60): User {
        val key = "user:${user.id}"

        // 1. 캐시에 먼저 저장
        redisTemplate.opsForValue().set(key, user, ttlSeconds, TimeUnit.SECONDS)

        // 2. 비동기로 DB 저장 (시뮬레이션)
        // 실제로는 별도의 워커나 스케줄러가 주기적으로 DB에 쓰기 작업 수행
        println("Write-Behind: 캐시 저장 완료, DB는 나중에 비동기 업데이트")

        return user
    }

    /**
     * Cache Invalidation
     * 캐시 무효화 - 데이터 업데이트 시 캐시 삭제
     */
    fun invalidateCache(key: String): Boolean {
        return redisTemplate.delete(key)
    }

    /**
     * Hash 자료구조를 이용한 캐싱
     * 여러 필드를 하나의 키에 저장
     */
    fun saveUserWithHash(user: User, ttlSeconds: Long = 60) {
        val key = "user:hash:${user.id}"
        val hashOps = redisTemplate.opsForHash<String, Any>()

        hashOps.put(key, "id", user.id.toString())
        hashOps.put(key, "name", user.name)
        hashOps.put(key, "email", user.email)
        hashOps.put(key, "age", user.age.toString())

        // TTL 설정
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS)
    }

    fun getUserFromHash(userId: Long): User? {
        val key = "user:hash:$userId"
        val hashOps = redisTemplate.opsForHash<String, Any>()

        val entries = hashOps.entries(key)
        if (entries.isEmpty()) {
            return null
        }

        return User(
            id = entries["id"]?.toString()?.toLongOrNull() ?: 0L,
            name = entries["name"]?.toString() ?: "",
            email = entries["email"]?.toString() ?: "",
            age = entries["age"]?.toString()?.toIntOrNull() ?: 0
        )
    }

    /**
     * List 자료구조를 이용한 캐싱
     * 최근 조회한 항목 리스트 관리
     */
    fun addToRecentViewList(userId: Long, productId: Long, maxSize: Int = 10) {
        val key = "user:$userId:recent-views"
        val listOps = redisTemplate.opsForList()

        // 왼쪽에 추가 (최신 항목이 앞에 오도록)
        listOps.leftPush(key, productId.toString())

        // 최대 크기 유지 (오래된 항목 제거)
        listOps.trim(key, 0, (maxSize - 1).toLong())
    }

    fun getRecentViewList(userId: Long): List<Long> {
        val key = "user:$userId:recent-views"
        val listOps = redisTemplate.opsForList()

        return listOps.range(key, 0, -1)
            ?.mapNotNull { it.toString().toLongOrNull() }
            ?: emptyList()
    }

    /**
     * Sorted Set을 이용한 랭킹 캐싱
     */
    fun updateRanking(productId: Long, score: Double) {
        val key = "product:ranking"
        redisTemplate.opsForZSet().add(key, productId.toString(), score)
    }

    fun getTopRankings(topN: Int = 10): List<Pair<Long, Double>> {
        val key = "product:ranking"
        val zSetOps = redisTemplate.opsForZSet()

        // 점수가 높은 순으로 조회
        return zSetOps.reverseRangeWithScores(key, 0, (topN - 1).toLong())
            ?.mapNotNull { typedTuple ->
                val productId = typedTuple.value?.toString()?.toLongOrNull()
                val score = typedTuple.score
                if (productId != null && score != null) {
                    productId to score
                } else {
                    null
                }
            } ?: emptyList()
    }

    // Database Operations
    private fun fetchFromDatabase(key: String): User? {
        println("Cache Miss! Fetching from database: $key")
        val userId = key.removePrefix("user:").toLongOrNull() ?: return null
        return userRepository?.findByIdOrNull(userId)?.toUser()
    }

    private fun saveToDatabase(user: User) {
        println("Saving to database: ${user.id}")
        userRepository?.save(UserEntity.from(user))
    }
}

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val age: Int
)
