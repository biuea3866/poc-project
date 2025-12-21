package com.example.cachepractice.repository

import com.example.cachepractice.domain.Product
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Product 저장소 (인메모리)
 * 데이터베이스 조회를 시뮬레이션하기 위해 의도적으로 지연 시간 추가
 */
@Repository
class ProductRepository {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val storage = ConcurrentHashMap<Long, Product>()
    private val idGenerator = AtomicLong(1)

    init {
        // 초기 데이터 생성
        save(Product(idGenerator.getAndIncrement(), "Laptop", BigDecimal("1200.00")))
        save(Product(idGenerator.getAndIncrement(), "Mouse", BigDecimal("25.50")))
        save(Product(idGenerator.getAndIncrement(), "Keyboard", BigDecimal("75.00")))
        save(Product(idGenerator.getAndIncrement(), "Monitor", BigDecimal("350.00")))
        save(Product(idGenerator.getAndIncrement(), "Headset", BigDecimal("89.99")))
    }

    /**
     * ID로 제품 조회
     * DB 조회를 시뮬레이션하기 위해 100ms 지연 추가
     */
    fun findById(id: Long): Product? {
        logger.info("DB 조회 시뮬레이션 시작: productId={}", id)
        Thread.sleep(100) // DB 조회 시뮬레이션
        val product = storage[id]
        logger.info("DB 조회 완료: productId={}, found={}", id, product != null)
        return product
    }

    /**
     * 모든 제품 조회
     */
    fun findAll(): List<Product> {
        logger.info("모든 제품 조회")
        Thread.sleep(100) // DB 조회 시뮬레이션
        return storage.values.toList()
    }

    /**
     * 제품 저장
     */
    fun save(product: Product): Product {
        logger.info("제품 저장: {}", product)
        val savedProduct = if (product.id == 0L) {
            product.copy(id = idGenerator.getAndIncrement())
        } else {
            product
        }
        storage[savedProduct.id] = savedProduct
        return savedProduct
    }

    /**
     * 제품 삭제
     */
    fun deleteById(id: Long) {
        logger.info("제품 삭제: productId={}", id)
        storage.remove(id)
    }

    /**
     * 제품 존재 여부 확인
     */
    fun existsById(id: Long): Boolean {
        return storage.containsKey(id)
    }

    /**
     * 저장소 초기화 (테스트용)
     */
    fun clear() {
        storage.clear()
    }

    /**
     * 저장소를 초기 상태로 리셋 (테스트용)
     */
    fun resetToInitialState() {
        storage.clear()
        idGenerator.set(1)

        // 초기 데이터 재생성
        save(Product(idGenerator.getAndIncrement(), "Laptop", BigDecimal("1200.00")))
        save(Product(idGenerator.getAndIncrement(), "Mouse", BigDecimal("25.50")))
        save(Product(idGenerator.getAndIncrement(), "Keyboard", BigDecimal("75.00")))
        save(Product(idGenerator.getAndIncrement(), "Monitor", BigDecimal("350.00")))
        save(Product(idGenerator.getAndIncrement(), "Headset", BigDecimal("89.99")))

        logger.info("저장소 초기 상태로 리셋 완료")
    }
}
