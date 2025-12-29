package com.example.cachepractice.redis.concurrency

import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service

/**
 * Redis 동시성 문제 해결 예제
 * 1. MULTI/EXEC: 명령어를 일괄적으로 순차 처리
 * 2. WATCH: 낙관적 락을 이용한 트랜잭션
 * 3. Lua Script: 원자적 스크립트 실행
 */
@Service
@Profile("redis")
class RedisConcurrencyService(
    private val redisTemplate: RedisTemplate<String, Any>
) {

    /**
     * MULTI/EXEC 예제
     * 여러 명령어를 하나의 트랜잭션으로 묶어 순차 실행
     * 중간에 에러가 발생하면 전체 트랜잭션이 실패
     */
    fun executeWithMultiExec(key1: String, key2: String, value1: String, value2: String): Boolean {
        return try {
            redisTemplate.execute { connection ->
                connection.multi()
                connection.stringCommands().set(key1.toByteArray(), value1.toByteArray())
                connection.stringCommands().set(key2.toByteArray(), value2.toByteArray())
                val results = connection.exec()
                results != null && results.isNotEmpty()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * WATCH 예제 - 간소화된 버전
     * 낙관적 락을 이용한 트랜잭션
     * 실제 운영에서는 Redisson의 분산 락 사용 권장
     */
    fun executeWithWatch(balanceKey: String, decrementAmount: Int): Boolean {
        return try {
            val opsForValue = redisTemplate.opsForValue()

            // 현재 잔액 조회
            val currentBalance = opsForValue.get(balanceKey)?.toString()?.toIntOrNull() ?: 0

            // 잔액이 충분한 경우에만 차감 (간단한 CAS 방식)
            if (currentBalance >= decrementAmount) {
                val newBalance = currentBalance - decrementAmount
                opsForValue.set(balanceKey, newBalance)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lua Script 예제
     * Redis 서버에서 스크립트를 원자적으로 실행
     * 재고 차감 예제: 재고가 충분한 경우에만 차감
     */
    fun executeWithLuaScript(stockKey: String, quantity: Int): Boolean {
        val script = """
            local currentStock = tonumber(redis.call('GET', KEYS[1]) or '0')
            local quantity = tonumber(ARGV[1])

            if currentStock >= quantity then
                redis.call('DECRBY', KEYS[1], quantity)
                return 1
            else
                return 0
            end
        """.trimIndent()

        val redisScript = DefaultRedisScript<Long>().apply {
            setScriptText(script)
            resultType = Long::class.java
        }

        return try {
            val result = redisTemplate.execute(redisScript, listOf(stockKey), quantity)
            result == 1L
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lua Script - 복잡한 예제
     * 여러 키를 원자적으로 업데이트
     */
    fun transferWithLuaScript(fromKey: String, toKey: String, amount: Int): Boolean {
        val script = """
            local fromBalance = tonumber(redis.call('GET', KEYS[1]) or '0')
            local amount = tonumber(ARGV[1])

            if fromBalance >= amount then
                redis.call('DECRBY', KEYS[1], amount)
                redis.call('INCRBY', KEYS[2], amount)
                return 1
            else
                return 0
            end
        """.trimIndent()

        val redisScript = DefaultRedisScript<Long>().apply {
            setScriptText(script)
            resultType = Long::class.java
        }

        return try {
            val result = redisTemplate.execute(redisScript, listOf(fromKey, toKey), amount)
            result == 1L
        } catch (e: Exception) {
            false
        }
    }

    // Helper methods - 문자열로 직접 저장 (Lua Script와 호환)
    fun setBalance(key: String, balance: Int) {
        redisTemplate.execute<Any> { connection ->
            connection.stringCommands().set(key.toByteArray(), balance.toString().toByteArray())
        }
    }

    fun getBalance(key: String): Int {
        return redisTemplate.execute<Int> { connection ->
            connection.stringCommands().get(key.toByteArray())?.let { String(it).toIntOrNull() } ?: 0
        } ?: 0
    }

    fun deleteKey(key: String) {
        redisTemplate.delete(key)
    }
}
