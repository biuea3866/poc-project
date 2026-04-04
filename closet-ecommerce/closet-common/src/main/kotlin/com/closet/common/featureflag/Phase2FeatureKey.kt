package com.closet.common.featureflag

/**
 * Phase 2 Feature Flag 키 enum.
 *
 * Sprint 5~7에 걸쳐 점진적으로 활성화되는 Phase 2 기능의 런타임 on/off 키.
 * DB의 simple_runtime_config 테이블에 저장되며, FeatureFlagService를 통해 조회한다.
 *
 * 배포 없이 DB 값 변경으로 즉시 반영되며, OFF 시 Phase 1 플로우로 복귀한다.
 */
enum class Phase2FeatureKey(
    override val key: String,
    override val description: String,
) : BooleanFeatureKey {

    // === Sprint 5 ===

    /** Outbox Poller 활성화 (Transactional Outbox 패턴) */
    OUTBOX_POLLING_ENABLED(
        key = "OUTBOX_POLLING_ENABLED",
        description = "Outbox Poller를 통한 Kafka 이벤트 발행 활성화",
    ),

    /** 역할 기반 인가 활성화 (RBAC) */
    ROLE_AUTHORIZATION_ENABLED(
        key = "ROLE_AUTHORIZATION_ENABLED",
        description = "X-Member-Role 헤더 기반 역할 인가 활성화",
    ),

    /** 재고 Kafka Consumer 활성화 */
    INVENTORY_KAFKA_ENABLED(
        key = "INVENTORY_KAFKA_ENABLED",
        description = "재고 서비스 Kafka Consumer (order.created/cancelled) 활성화",
    ),

    /** 검색 인덱싱 활성화 (ES 인덱싱 파이프라인) */
    SEARCH_INDEXING_ENABLED(
        key = "SEARCH_INDEXING_ENABLED",
        description = "product.* Kafka Consumer -> ES 인덱싱 파이프라인 활성화",
    ),

    // === Sprint 6 ===

    /** 배송 서비스 활성화 */
    SHIPPING_SERVICE_ENABLED(
        key = "SHIPPING_SERVICE_ENABLED",
        description = "배송 서비스 (택배사 연동, 배송 추적) 활성화",
    ),

    /** 자동 구매확정 배치 활성화 */
    AUTO_CONFIRM_BATCH_ENABLED(
        key = "AUTO_CONFIRM_BATCH_ENABLED",
        description = "배송 완료 후 7일 자동 구매확정 배치 활성화",
    ),

    /** 반품 요청 기능 활성화 */
    RETURN_REQUEST_ENABLED(
        key = "RETURN_REQUEST_ENABLED",
        description = "반품 요청 접수 및 처리 기능 활성화",
    ),

    /** 검색 필터 기능 활성화 */
    SEARCH_FILTER_ENABLED(
        key = "SEARCH_FILTER_ENABLED",
        description = "카테고리/브랜드/가격/사이즈/색상 검색 필터 활성화",
    ),

    // === Sprint 7 ===

    /** 리뷰 서비스 활성화 */
    REVIEW_SERVICE_ENABLED(
        key = "REVIEW_SERVICE_ENABLED",
        description = "리뷰 작성/조회/삭제 기능 활성화",
    ),

    /** 리뷰 포인트 적립 활성화 */
    REVIEW_POINT_ENABLED(
        key = "REVIEW_POINT_ENABLED",
        description = "리뷰 작성 시 포인트 적립 (텍스트 200P, 포토 500P) 활성화",
    ),

    /** 교환 요청 기능 활성화 */
    EXCHANGE_REQUEST_ENABLED(
        key = "EXCHANGE_REQUEST_ENABLED",
        description = "교환 요청 접수 및 처리 기능 활성화",
    ),

    /** 자동완성 검색 활성화 */
    AUTOCOMPLETE_ENABLED(
        key = "AUTOCOMPLETE_ENABLED",
        description = "edge_ngram 기반 자동완성 검색 활성화",
    ),

    /** 인기 검색어 기능 활성화 */
    POPULAR_KEYWORDS_ENABLED(
        key = "POPULAR_KEYWORDS_ENABLED",
        description = "Redis Sorted Set 기반 인기 검색어 기능 활성화",
    ),

    ;

    companion object {
        /** 키 문자열로 Feature Key를 조회한다. 없으면 null 반환. */
        fun fromKey(key: String): Phase2FeatureKey? {
            return entries.find { it.key == key }
        }
    }
}
