package com.example.cachepractice.redis

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Redis + MySQL을 사용하는 테스트의 Base 클래스
 * 테스트 실행 전에 Docker Compose로 Redis(6379)와 MySQL(3306)를 실행해야 합니다:
 * docker-compose up -d
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("redis")
abstract class RedisTestBase
