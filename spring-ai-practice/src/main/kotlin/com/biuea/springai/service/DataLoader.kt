package com.biuea.springai.service

import com.biuea.springai.domain.Order
import com.biuea.springai.domain.OrderRepository
import com.biuea.springai.domain.Product
import com.biuea.springai.domain.ProductRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 시작 시 의류 상품·주문 시드 데이터를 인메모리 저장소로 로드한다.
 * RAG 벡터 적재는 임베딩 모델(Ollama) 가용성을 요구하므로 시작 시 자동 실행하지 않고
 * POST /api/rag/ingest 엔드포인트에서 수동 트리거한다.
 */
@Component
class DataLoader(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val objectMapper: ObjectMapper,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val products: List<Product> =
            objectMapper.readValue(ClassPathResource("data/products.json").inputStream)
        productRepository.saveAll(products)

        val orders: List<Order> =
            objectMapper.readValue(ClassPathResource("data/orders.json").inputStream)
        orderRepository.saveAll(orders)
    }
}
