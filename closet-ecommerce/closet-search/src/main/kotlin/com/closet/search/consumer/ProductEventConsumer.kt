package com.closet.search.consumer

import com.closet.common.event.ClosetTopics
import com.closet.search.application.facade.SearchFacade
import com.closet.search.consumer.event.ProductEvent
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * event.closet.product 토픽 Consumer.
 *
 * 상품 도메인 이벤트를 수신하여 eventType으로 분기한다.
 * - ProductCreated: ES 인덱싱
 * - ProductUpdated: ES 문서 갱신
 * - ProductDeleted: ES 문서 삭제
 *
 * Consumer는 이벤트 수신 및 라우팅만 담당하고,
 * 실제 비즈니스 로직은 SearchFacade에 위임한다.
 *
 * SEARCH_INDEXING_ENABLED Feature Flag로 활성화/비활성화를 제어한다.
 */
@Component
@ConditionalOnProperty(name = ["feature.search-indexing-enabled"], havingValue = "true", matchIfMissing = true)
class ProductEventConsumer(
    private val searchFacade: SearchFacade,
) {
    @KafkaListener(topics = [ClosetTopics.PRODUCT], groupId = "search-service")
    fun handle(event: ProductEvent) {
        logger.info { "${ClosetTopics.PRODUCT} 수신: eventType=${event.eventType}, productId=${event.productId}" }

        try {
            when (event.eventType) {
                "ProductCreated" ->
                    searchFacade.handleProductCreated(
                        productId = event.productId,
                        name = event.name,
                        description = event.description,
                        brandId = event.brandId,
                        categoryId = event.categoryId,
                        basePrice = event.basePrice,
                        salePrice = event.salePrice,
                        discountRate = event.discountRate,
                        status = event.status,
                        season = event.season,
                        fitType = event.fitType,
                        gender = event.gender,
                        sizes = event.sizes,
                        colors = event.colors,
                        imageUrl = event.imageUrl,
                    )

                "ProductUpdated" ->
                    searchFacade.handleProductUpdated(
                        productId = event.productId,
                        name = event.name,
                        description = event.description,
                        brandId = event.brandId,
                        categoryId = event.categoryId,
                        basePrice = event.basePrice,
                        salePrice = event.salePrice,
                        discountRate = event.discountRate,
                        status = event.status,
                        season = event.season,
                        fitType = event.fitType,
                        gender = event.gender,
                        sizes = event.sizes,
                        colors = event.colors,
                        imageUrl = event.imageUrl,
                    )

                "ProductDeleted" -> searchFacade.handleProductDeleted(event.productId)

                else -> logger.info { "처리하지 않는 eventType 무시: ${event.eventType}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "${ClosetTopics.PRODUCT} 처리 실패: eventType=${event.eventType}, productId=${event.productId}" }
            throw e // DLQ로 전달하기 위해 재throw
        }
    }
}
