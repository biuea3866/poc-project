# Phase 5 상세 PRD: AI + 개인화 + 고도화

> 작성일: 2026-03-22
> 프로젝트: Closet E-commerce
> Phase: 5 (AI + 개인화 + 고도화)
> 도메인: AI 추천 + 어드민 + 이벤트 아키텍처 + 성능 최적화 + 보안 강화 + 인프라 고도화

---

## 1. Phase 5 개요

### 목표
Phase 4에서 구축한 정산/알림/콘텐츠/AI 기반 위에 **AI 추천 서비스를 본격 고도화**하고, **어드민 대시보드**로 운영 효율을 극대화하며, **이벤트 기반 아키텍처(Event Sourcing, CQRS)**로 시스템 확장성을 확보한다. 동시에 **성능 최적화, 보안 강화, Kubernetes 인프라**를 통해 프로덕션 레벨의 플랫폼 성숙도를 달성한다.

Phase 5는 "학습 프로젝트"에서 "프로덕션 레디 플랫폼"으로의 전환점이다.

### 기간
- 예상 기간: 14주 (Phase 4 완료 후)
- Sprint 19-25

### 서비스 구성

| 서비스 | 포트 | 기술 스택 | 신규/변경 |
|--------|------|----------|----------|
| recommendation-service | 8097 | Kotlin, Spring Boot 3.x, Redis, ML Model | 고도화 (Phase 4 ai-service 리팩토링) |
| admin-service | 8098 | Kotlin, Spring Boot 3.x, JPA, QueryDSL | 신규 |
| order-service (CQRS) | 8082 | Kotlin, Spring Boot 3.x, Kafka, MongoDB | 변경 (읽기 모델 분리) |
| api-gateway (보안) | 8080 | Kotlin, Spring Cloud Gateway, Spring Security | 변경 (OAuth2 + Rate Limit) |

### Phase 5 KPI

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| AI 추천 클릭률 (CTR) | 추천 노출 대비 15% | 추천 상품 클릭 / 추천 노출 |
| AI 추천 전환율 (CVR) | 추천 클릭 대비 8% | 추천 상품 구매 / 추천 클릭 |
| 사이즈 추천 정확도 | 85% 이상 | 추천 사이즈 = 실제 구매 사이즈 |
| 사이즈 추천 후 반품률 감소 | 기존 대비 25% 감소 | 사이즈 추천 사용 주문의 반품률 |
| 어드민 대시보드 로딩 시간 | 2초 이내 | 대시보드 메인 페이지 로딩 시간 |
| 어드민 작업 처리 시간 | 기존 대비 50% 단축 | 주요 운영 작업(상품 승인, 정산 등) 소요 시간 |
| API 평균 응답 시간 | 100ms 이내 (P95) | 전체 API P95 레이턴시 |
| 캐시 히트율 | 85% 이상 | Redis 캐시 히트 / 전체 요청 |
| 이벤트 처리 지연 시간 | 500ms 이내 (P99) | Kafka 이벤트 발행 → 소비 지연 |
| DLQ 재처리 성공률 | 95% 이상 | DLQ 재처리 성공 / 전체 DLQ 건수 |
| 소셜 로그인 비율 | 전체 로그인의 60% | 소셜 로그인 / 전체 로그인 |
| 보안 취약점 제로 | 0건 (Critical/High) | OWASP Top 10 스캔 결과 |
| 서비스 가용성 | 99.9% (SLA) | 월간 다운타임 < 43분 |
| 배포 주기 | 하루 1회 이상 | 프로덕션 배포 횟수 |

---

## 2. AI 추천 서비스 고도화 (recommendation-service :8097)

Phase 4의 ai-service를 recommendation-service로 리팩토링하고, 추천 알고리즘을 본격적으로 고도화한다.

### US-1701: 개인화 상품 추천 (구매 이력 + 조회 이력 기반)

**As a** 로그인 회원
**I want to** 내 구매 이력과 조회 이력을 기반으로 개인화된 상품 추천을 받고 싶다
**So that** 내 취향에 맞는 상품을 빠르게 발견할 수 있다

#### Acceptance Criteria
- [ ] 회원의 최근 30일 구매 이력과 조회 이력을 기반으로 추천 상품을 생성한다
- [ ] 추천 알고리즘: User-Item Matrix Factorization (ALS 기반)
- [ ] 추천 결과는 최소 20개, 최대 100개 상품을 반환한다
- [ ] 추천 결과는 Redis에 캐싱하며, TTL은 6시간이다
- [ ] 새로운 구매/조회 이벤트 발생 시 비동기로 추천 모델을 갱신한다
- [ ] Cold Start 문제: 이력이 5건 미만인 회원은 인기 상품 + 카테고리 기반 추천으로 대체한다
- [ ] 이미 구매한 상품은 추천 목록에서 제외한다
- [ ] 품절 상품은 추천 목록에서 제외한다
- [ ] 추천 노출/클릭/구매 이벤트를 수집하여 CTR/CVR을 측정한다
- [ ] A/B 테스트 지원: Feature Flag로 추천 알고리즘 버전을 전환할 수 있다

#### 데이터 모델
```sql
CREATE TABLE member_activity_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    activity_type       VARCHAR(20)     NOT NULL COMMENT '활동 유형: VIEW, PURCHASE, CART_ADD, WISH',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    category_id         BIGINT          NULL COMMENT '카테고리 ID',
    brand_id            BIGINT          NULL COMMENT '브랜드 ID',
    score               DECIMAL(5,2)    NOT NULL DEFAULT 1.0 COMMENT '활동 가중치',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_member_activity (member_id, activity_type),
    INDEX idx_product_id (product_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 활동 로그';

CREATE TABLE recommendation_result (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    algorithm_type      VARCHAR(30)     NOT NULL COMMENT '알고리즘: CF_USER, CF_ITEM, CONTENT, HYBRID, POPULAR',
    product_id          BIGINT          NOT NULL COMMENT '추천 상품 ID',
    score               DECIMAL(10,6)   NOT NULL COMMENT '추천 점수',
    rank_position       INT             NOT NULL COMMENT '추천 순위',
    reason              VARCHAR(200)    NULL COMMENT '추천 사유',
    version             VARCHAR(20)     NOT NULL COMMENT '모델 버전',
    expired_at          DATETIME(6)     NOT NULL COMMENT '만료 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_algo_product (member_id, algorithm_type, product_id),
    INDEX idx_member_algo_rank (member_id, algorithm_type, rank_position),
    INDEX idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='추천 결과';
```

#### API 스펙
```
GET /api/v1/recommendations/personalized?memberId={memberId}&size={size}&page={page}
Authorization: Bearer {token}

Response: 200 OK
{
    "memberId": 12345,
    "algorithmType": "HYBRID",
    "modelVersion": "v2.1",
    "totalCount": 50,
    "items": [
        {
            "productId": 101,
            "productName": "오버핏 코튼 셔츠",
            "brandName": "무탠다드",
            "price": 39900,
            "discountPrice": 29900,
            "thumbnailUrl": "https://cdn.closet.com/products/101/thumb.jpg",
            "score": 0.9523,
            "rank": 1,
            "reason": "최근 구매한 '코튼 티셔츠'와 유사한 스타일"
        }
    ]
}
```

#### 활동 가중치 정책
| 활동 유형 | 가중치 | 설명 |
|----------|--------|------|
| VIEW | 1.0 | 상품 상세 조회 |
| CART_ADD | 3.0 | 장바구니 추가 |
| WISH | 2.0 | 위시리스트 추가 |
| PURCHASE | 5.0 | 구매 완료 |

---

### US-1702: 함께 본 상품 추천 (Item-Item Collaborative Filtering)

**As a** 상품 상세 페이지를 보고 있는 사용자
**I want to** "이 상품을 본 사람이 함께 본 상품"을 확인하고 싶다
**So that** 다른 사용자들의 관심사를 참고하여 비교/탐색할 수 있다

#### Acceptance Criteria
- [ ] 현재 상품을 조회한 사용자들이 같은 세션 내에서 조회한 다른 상품을 추천한다
- [ ] Item-Item Collaborative Filtering 알고리즘을 사용한다 (코사인 유사도)
- [ ] 추천 결과는 최소 4개, 최대 20개 상품을 반환한다
- [ ] 동일 브랜드 상품은 최대 3개까지만 포함한다 (다양성 보장)
- [ ] 추천 결과는 Redis에 캐싱하며, TTL은 24시간이다
- [ ] Item-Item 유사도 행렬은 일간 배치(매일 04:00)로 재계산한다
- [ ] 최소 데이터 기준: 해당 상품의 조회 사용자가 50명 미만이면 카테고리 인기 상품으로 대체한다
- [ ] 품절 상품은 추천 목록에서 제외한다

#### 데이터 모델
```sql
CREATE TABLE item_similarity (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    source_product_id   BIGINT          NOT NULL COMMENT '기준 상품 ID',
    target_product_id   BIGINT          NOT NULL COMMENT '유사 상품 ID',
    similarity_score    DECIMAL(10,6)   NOT NULL COMMENT '유사도 점수 (0~1)',
    co_view_count       INT             NOT NULL COMMENT '공동 조회 사용자 수',
    algorithm_version   VARCHAR(20)     NOT NULL COMMENT '알고리즘 버전',
    calculated_at       DATETIME(6)     NOT NULL COMMENT '계산 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_target_version (source_product_id, target_product_id, algorithm_version),
    INDEX idx_source_score (source_product_id, similarity_score DESC),
    INDEX idx_calculated_at (calculated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 간 유사도 (협업 필터링)';
```

#### API 스펙
```
GET /api/v1/recommendations/also-viewed?productId={productId}&size={size}
Authorization: Optional

Response: 200 OK
{
    "sourceProductId": 101,
    "algorithmType": "CF_ITEM",
    "totalCount": 12,
    "items": [
        {
            "productId": 205,
            "productName": "와이드 데님 팬츠",
            "brandName": "어반클래식",
            "price": 49900,
            "discountPrice": 39900,
            "thumbnailUrl": "https://cdn.closet.com/products/205/thumb.jpg",
            "similarityScore": 0.8721,
            "coViewCount": 234
        }
    ]
}
```

---

### US-1703: 비슷한 상품 추천 (Content-Based Filtering)

**As a** 상품 상세 페이지를 보고 있는 사용자
**I want to** "이 상품과 비슷한 상품"을 확인하고 싶다
**So that** 유사한 스타일/가격대/브랜드의 대안 상품을 탐색할 수 있다

#### Acceptance Criteria
- [ ] 상품의 속성(카테고리, 브랜드, 가격대, 시즌, 색상, 소재, 핏)을 기반으로 유사 상품을 추천한다
- [ ] Content-Based Filtering 알고리즘: TF-IDF 벡터화 + 코사인 유사도
- [ ] 속성별 가중치:
  - 카테고리: 0.30
  - 가격대 (+-30% 범위): 0.20
  - 브랜드: 0.15
  - 시즌: 0.15
  - 색상: 0.10
  - 소재/핏: 0.10
- [ ] 추천 결과는 최소 4개, 최대 20개 상품을 반환한다
- [ ] 동일 브랜드 상품은 최대 5개까지만 포함한다
- [ ] 추천 결과는 Redis에 캐싱하며, TTL은 12시간이다
- [ ] 상품 속성 변경 시 해당 상품의 캐시를 무효화한다
- [ ] 품절 상품은 추천 목록에서 제외한다

#### 데이터 모델
```sql
CREATE TABLE product_feature_vector (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    category_vector     VARCHAR(500)    NOT NULL COMMENT '카테고리 벡터 (JSON)',
    brand_vector        VARCHAR(200)    NOT NULL COMMENT '브랜드 벡터 (JSON)',
    price_range         VARCHAR(20)     NOT NULL COMMENT '가격대: BUDGET, MID, PREMIUM, LUXURY',
    season_vector       VARCHAR(200)    NOT NULL COMMENT '시즌 벡터 (JSON)',
    color_vector        VARCHAR(500)    NOT NULL COMMENT '색상 벡터 (JSON)',
    material_vector     VARCHAR(500)    NOT NULL COMMENT '소재 벡터 (JSON)',
    fit_type            VARCHAR(20)     NULL COMMENT '핏: OVERFIT, REGULAR, SLIM',
    gender              VARCHAR(10)     NOT NULL COMMENT '성별: MALE, FEMALE, UNISEX',
    composite_vector    TEXT            NOT NULL COMMENT '통합 특성 벡터 (JSON)',
    version             VARCHAR(20)     NOT NULL COMMENT '벡터 버전',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_version (product_id, version),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 특성 벡터';

CREATE TABLE content_similarity (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    source_product_id   BIGINT          NOT NULL COMMENT '기준 상품 ID',
    target_product_id   BIGINT          NOT NULL COMMENT '유사 상품 ID',
    similarity_score    DECIMAL(10,6)   NOT NULL COMMENT '유사도 점수 (0~1)',
    matching_features   VARCHAR(500)    NOT NULL COMMENT '매칭 속성 목록 (JSON)',
    algorithm_version   VARCHAR(20)     NOT NULL COMMENT '알고리즘 버전',
    calculated_at       DATETIME(6)     NOT NULL COMMENT '계산 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_target_version (source_product_id, target_product_id, algorithm_version),
    INDEX idx_source_score (source_product_id, similarity_score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상품 간 유사도 (콘텐츠 기반)';
```

#### API 스펙
```
GET /api/v1/recommendations/similar?productId={productId}&size={size}
Authorization: Optional

Response: 200 OK
{
    "sourceProductId": 101,
    "algorithmType": "CONTENT",
    "totalCount": 15,
    "items": [
        {
            "productId": 308,
            "productName": "릴렉스핏 린넨 셔츠",
            "brandName": "에센셜",
            "price": 42900,
            "thumbnailUrl": "https://cdn.closet.com/products/308/thumb.jpg",
            "similarityScore": 0.9102,
            "matchingFeatures": ["카테고리: 셔츠", "가격대: MID", "핏: 오버핏", "시즌: SS"]
        }
    ]
}
```

---

### US-1704: AI 사이즈 추천

**As a** 상품을 구매하려는 사용자
**I want to** 내 체형에 맞는 사이즈를 AI가 추천해주길 원한다
**So that** 사이즈 불일치로 인한 반품 없이 만족스러운 구매를 할 수 있다

#### Acceptance Criteria
- [ ] 회원 프로필(키, 몸무게, 평소 사이즈)과 리뷰 사이즈 후기 데이터를 기반으로 사이즈를 추천한다
- [ ] 추천 모델: 해당 상품의 리뷰 중 "나와 비슷한 체형"(키 +-3cm, 몸무게 +-3kg)의 사이즈 선택 분포를 분석한다
- [ ] 추천 결과: 추천 사이즈 + 적합 확률(%) + 근거 데이터
- [ ] 근거 데이터: "키 175cm, 몸무게 70kg인 사용자 중 85%가 L을 선택했습니다"
- [ ] 최소 데이터 기준: 비슷한 체형의 리뷰가 10건 미만이면 "데이터 부족" 안내를 반환한다
- [ ] 브랜드별 사이즈 편차를 학습하여 반영한다 (예: A브랜드는 전반적으로 작게 나옴)
- [ ] 카테고리별 핏 차이를 반영한다 (상의/하의/아우터 사이즈 기준 상이)
- [ ] 추천 사이즈 사용 여부를 추적하여 정확도를 측정한다
- [ ] 추천 결과는 Redis에 캐싱하며, TTL은 7일이다 (리뷰 추가 시 캐시 무효화)

#### 데이터 모델
```sql
CREATE TABLE member_body_profile (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    height              INT             NULL COMMENT '키 (cm)',
    weight              INT             NULL COMMENT '몸무게 (kg)',
    preferred_fit       VARCHAR(20)     NULL COMMENT '선호 핏: TIGHT, REGULAR, LOOSE',
    top_size            VARCHAR(10)     NULL COMMENT '상의 평소 사이즈: XS, S, M, L, XL, XXL',
    bottom_size         VARCHAR(10)     NULL COMMENT '하의 평소 사이즈: 26, 28, 30, 32, 34, 36',
    shoe_size           INT             NULL COMMENT '신발 사이즈 (mm): 230, 240, ..., 300',
    gender              VARCHAR(10)     NOT NULL COMMENT '성별: MALE, FEMALE',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 체형 프로필';

CREATE TABLE size_recommendation_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    product_id          BIGINT          NOT NULL COMMENT '상품 ID',
    recommended_size    VARCHAR(10)     NOT NULL COMMENT '추천 사이즈',
    confidence          DECIMAL(5,2)    NOT NULL COMMENT '신뢰도 (%)',
    similar_review_count INT            NOT NULL COMMENT '참고 리뷰 수',
    selected_size       VARCHAR(10)     NULL COMMENT '실제 선택 사이즈',
    is_accurate         TINYINT(1)      NULL COMMENT '정확 여부 (반품 미발생 = 1)',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_product (member_id, product_id),
    INDEX idx_product_id (product_id),
    INDEX idx_is_accurate (is_accurate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사이즈 추천 로그';

CREATE TABLE brand_size_deviation (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    brand_id            BIGINT          NOT NULL COMMENT '브랜드 ID',
    category_id         BIGINT          NOT NULL COMMENT '카테고리 ID',
    size_label          VARCHAR(10)     NOT NULL COMMENT '사이즈 라벨: S, M, L, ...',
    avg_height          DECIMAL(5,1)    NULL COMMENT '평균 적합 키 (cm)',
    avg_weight          DECIMAL(5,1)    NULL COMMENT '평균 적합 몸무게 (kg)',
    fit_tendency        VARCHAR(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '핏 경향: SMALL, NORMAL, LARGE',
    sample_count        INT             NOT NULL COMMENT '샘플 수',
    calculated_at       DATETIME(6)     NOT NULL COMMENT '계산 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_category_size (brand_id, category_id, size_label),
    INDEX idx_brand_id (brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='브랜드별 사이즈 편차';
```

#### API 스펙
```
GET /api/v1/recommendations/size?productId={productId}&memberId={memberId}
Authorization: Bearer {token}

Response: 200 OK
{
    "productId": 101,
    "memberId": 12345,
    "memberProfile": {
        "height": 175,
        "weight": 70,
        "preferredFit": "REGULAR"
    },
    "recommendation": {
        "recommendedSize": "L",
        "confidence": 85.5,
        "reason": "키 173~177cm, 몸무게 68~72kg 사용자 중 85%가 L을 선택했습니다.",
        "similarReviewCount": 128,
        "brandFitTendency": "NORMAL",
        "alternativeSize": "M",
        "alternativeConfidence": 12.3
    },
    "sizeDistribution": [
        { "size": "S", "percentage": 2.2 },
        { "size": "M", "percentage": 12.3 },
        { "size": "L", "percentage": 85.5 }
    ]
}

Response: 200 OK (데이터 부족)
{
    "productId": 101,
    "memberId": 12345,
    "recommendation": null,
    "message": "비슷한 체형의 리뷰 데이터가 부족합니다. (현재 3건 / 최소 10건 필요)",
    "fallback": {
        "type": "BRAND_AVERAGE",
        "recommendedSize": "L",
        "confidence": 45.0,
        "reason": "해당 브랜드의 평균 사이즈 기준 추천입니다."
    }
}
```

---

### US-1705: 인기 검색어 + 추천 검색어

**As a** 검색창을 사용하는 사용자
**I want to** 인기 검색어와 나에게 맞는 추천 검색어를 확인하고 싶다
**So that** 트렌드를 파악하고 관심 있는 상품을 빠르게 탐색할 수 있다

#### Acceptance Criteria
- [ ] 실시간 인기 검색어: 최근 1시간 기준 검색 빈도 Top 10을 노출한다
- [ ] 인기 검색어는 Redis Sorted Set으로 관리하며, 1분마다 갱신한다
- [ ] 급상승 검색어: 전일 대비 검색량 200% 이상 증가한 키워드를 별도 표시한다
- [ ] 개인화 추천 검색어: 회원의 조회/구매 이력 기반으로 관심 카테고리/브랜드 키워드를 추천한다
- [ ] 비로그인 사용자: 인기 검색어만 노출, 추천 검색어 미노출
- [ ] 금칙어 필터링: 부적절한 검색어는 인기 검색어 목록에서 자동 제외한다
- [ ] 인기 검색어는 Redis에 캐싱하며, TTL은 60초이다
- [ ] 검색어 로그를 Kafka로 발행하여 비동기 집계한다

#### 데이터 모델
```sql
CREATE TABLE search_keyword_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    keyword             VARCHAR(100)    NOT NULL COMMENT '검색 키워드',
    member_id           BIGINT          NULL COMMENT '회원 ID (비로그인은 NULL)',
    result_count        INT             NOT NULL COMMENT '검색 결과 수',
    session_id          VARCHAR(50)     NULL COMMENT '세션 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_keyword (keyword),
    INDEX idx_member_id (member_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='검색 키워드 로그';

CREATE TABLE trending_keyword (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    keyword             VARCHAR(100)    NOT NULL COMMENT '키워드',
    search_count        INT             NOT NULL COMMENT '검색 횟수 (최근 1시간)',
    prev_count          INT             NOT NULL DEFAULT 0 COMMENT '이전 기간 검색 횟수',
    growth_rate         DECIMAL(10,2)   NOT NULL DEFAULT 0 COMMENT '증가율 (%)',
    rank_position       INT             NOT NULL COMMENT '순위',
    is_trending         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '급상승 여부',
    aggregated_at       DATETIME(6)     NOT NULL COMMENT '집계 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_rank (rank_position),
    INDEX idx_aggregated_at (aggregated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='인기/급상승 검색어';

CREATE TABLE banned_keyword (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    keyword             VARCHAR(100)    NOT NULL COMMENT '금칙어',
    reason              VARCHAR(200)    NULL COMMENT '사유',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='금칙어 목록';
```

#### API 스펙
```
GET /api/v1/recommendations/search-keywords?memberId={memberId}
Authorization: Optional

Response: 200 OK
{
    "trending": [
        { "rank": 1, "keyword": "오버핏 반팔", "isTrending": true, "growthRate": 350.0 },
        { "rank": 2, "keyword": "린넨 팬츠", "isTrending": false, "growthRate": 20.0 },
        { "rank": 3, "keyword": "무신사 스탠다드", "isTrending": true, "growthRate": 210.0 }
    ],
    "personalized": [
        { "keyword": "코튼 셔츠", "reason": "최근 셔츠 카테고리 관심" },
        { "keyword": "어반클래식 신상", "reason": "자주 구매한 브랜드" }
    ]
}
```

---

## 3. 어드민 서비스 (admin-service :8098)

관리자 전용 대시보드와 운영 도구를 제공하는 서비스. 플랫폼 전체의 데이터를 조회하고 관리할 수 있다.

### US-1801: 관리자 대시보드 (매출/주문/회원/상품 통계)

**As a** 플랫폼 관리자
**I want to** 실시간 매출, 주문, 회원, 상품 통계를 대시보드에서 확인하고 싶다
**So that** 플랫폼 현황을 한눈에 파악하고 의사 결정을 내릴 수 있다

#### Acceptance Criteria
- [ ] 실시간 대시보드: 금일 매출, 주문 수, 신규 회원 수, 상품 등록 수를 실시간으로 표시한다
- [ ] 기간별 통계: 일간/주간/월간/연간 매출/주문/회원 추이를 차트로 제공한다
- [ ] 핵심 KPI 카드: GMV, 객단가, 전환율, 재구매율, DAU, MAU를 카드 형태로 표시한다
- [ ] 카테고리별/브랜드별 매출 비중을 파이 차트로 제공한다
- [ ] TOP 10: 매출 상위 상품, 매출 상위 브랜드, 검색어 Top 10을 제공한다
- [ ] 실시간 데이터는 Redis에서 읽으며, 1분 주기로 갱신한다
- [ ] 집계 데이터는 일간 배치(매일 03:00)로 계산하여 집계 테이블에 저장한다
- [ ] 대시보드 로딩 시간은 2초 이내를 보장한다

#### 데이터 모델
```sql
CREATE TABLE daily_statistics (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    stat_date           DATE            NOT NULL COMMENT '통계 일자',
    total_gmv           BIGINT          NOT NULL DEFAULT 0 COMMENT '총 거래액 (원)',
    total_orders        INT             NOT NULL DEFAULT 0 COMMENT '총 주문 수',
    total_revenue       BIGINT          NOT NULL DEFAULT 0 COMMENT '총 매출 (수수료 기준)',
    avg_order_amount    INT             NOT NULL DEFAULT 0 COMMENT '평균 객단가',
    new_members         INT             NOT NULL DEFAULT 0 COMMENT '신규 회원 수',
    active_members      INT             NOT NULL DEFAULT 0 COMMENT '활성 회원 수 (DAU)',
    new_products        INT             NOT NULL DEFAULT 0 COMMENT '신규 상품 등록 수',
    conversion_rate     DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '전환율 (%)',
    repurchase_rate     DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '재구매율 (%)',
    refund_rate         DECIMAL(5,2)    NOT NULL DEFAULT 0 COMMENT '반품률 (%)',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stat_date (stat_date),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='일간 통계';

CREATE TABLE category_daily_statistics (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    stat_date           DATE            NOT NULL COMMENT '통계 일자',
    category_id         BIGINT          NOT NULL COMMENT '카테고리 ID',
    category_name       VARCHAR(100)    NOT NULL COMMENT '카테고리명',
    gmv                 BIGINT          NOT NULL DEFAULT 0 COMMENT '거래액',
    order_count         INT             NOT NULL DEFAULT 0 COMMENT '주문 수',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_category (stat_date, category_id),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='카테고리별 일간 통계';

CREATE TABLE brand_daily_statistics (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    stat_date           DATE            NOT NULL COMMENT '통계 일자',
    brand_id            BIGINT          NOT NULL COMMENT '브랜드 ID',
    brand_name          VARCHAR(100)    NOT NULL COMMENT '브랜드명',
    gmv                 BIGINT          NOT NULL DEFAULT 0 COMMENT '거래액',
    order_count         INT             NOT NULL DEFAULT 0 COMMENT '주문 수',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_date_brand (stat_date, brand_id),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='브랜드별 일간 통계';
```

#### API 스펙
```
GET /api/v1/admin/dashboard/summary?date={date}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "date": "2026-03-22",
    "realtime": {
        "todayGmv": 125000000,
        "todayOrders": 3420,
        "todayNewMembers": 156,
        "todayNewProducts": 42
    },
    "kpi": {
        "gmv": 125000000,
        "avgOrderAmount": 36549,
        "conversionRate": 3.2,
        "repurchaseRate": 28.5,
        "dau": 45000,
        "mau": 380000,
        "refundRate": 8.2
    },
    "topProducts": [...],
    "topBrands": [...],
    "topSearchKeywords": [...]
}

GET /api/v1/admin/dashboard/trend?startDate={start}&endDate={end}&period={DAILY|WEEKLY|MONTHLY}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "period": "DAILY",
    "data": [
        {
            "date": "2026-03-21",
            "gmv": 118000000,
            "orders": 3200,
            "newMembers": 142,
            "activeMembers": 43000
        }
    ]
}
```

---

### US-1802: 상품 관리 (승인/반려/삭제)

**As a** 플랫폼 관리자
**I want to** 셀러가 등록한 상품을 검수하고 승인/반려/삭제 처리하고 싶다
**So that** 플랫폼의 상품 품질을 관리할 수 있다

#### Acceptance Criteria
- [ ] 상품 상태: PENDING_APPROVAL → APPROVED / REJECTED → DELETED
- [ ] 상품 목록 조회: 상태별/셀러별/카테고리별/등록일별 필터링 지원
- [ ] 승인 처리: 관리자가 승인하면 상품이 전시 가능 상태가 된다
- [ ] 반려 처리: 반려 사유를 필수 입력하며, 셀러에게 알림을 발송한다
- [ ] 삭제 처리: Soft Delete로 처리하며, 삭제 사유를 기록한다
- [ ] 일괄 처리: 최대 100건까지 일괄 승인/반려/삭제가 가능하다
- [ ] 처리 이력을 admin_action_log에 기록한다
- [ ] 상품 상태 변경 시 `product.status.changed` Kafka 이벤트를 발행한다

#### API 스펙
```
GET /api/v1/admin/products?status={status}&sellerId={sellerId}&categoryId={categoryId}&page={page}&size={size}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "totalCount": 45,
    "items": [
        {
            "productId": 101,
            "productName": "오버핏 코튼 셔츠",
            "sellerName": "무탠다드",
            "categoryName": "셔츠",
            "price": 39900,
            "status": "PENDING_APPROVAL",
            "registeredAt": "2026-03-22T10:30:00"
        }
    ]
}

PUT /api/v1/admin/products/{productId}/approve
Authorization: Bearer {token} (admin)

Response: 200 OK
{ "productId": 101, "status": "APPROVED", "approvedAt": "2026-03-22T11:00:00" }

PUT /api/v1/admin/products/{productId}/reject
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{ "reason": "상품 이미지 품질이 기준 미달입니다. 최소 800x800px 이미지를 등록해주세요." }

Response: 200 OK
{ "productId": 101, "status": "REJECTED", "reason": "...", "rejectedAt": "2026-03-22T11:00:00" }

POST /api/v1/admin/products/batch
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "action": "APPROVE",
    "productIds": [101, 102, 103],
    "reason": null
}

Response: 200 OK
{
    "successCount": 3,
    "failCount": 0,
    "results": [
        { "productId": 101, "status": "APPROVED" },
        { "productId": 102, "status": "APPROVED" },
        { "productId": 103, "status": "APPROVED" }
    ]
}
```

---

### US-1803: 회원 관리 (정지/탈퇴)

**As a** 플랫폼 관리자
**I want to** 정책 위반 회원을 정지하거나 탈퇴 처리하고 싶다
**So that** 플랫폼의 건전한 이용 환경을 유지할 수 있다

#### Acceptance Criteria
- [ ] 회원 목록 조회: 상태별/등급별/가입일별/최근 로그인일별 필터링 지원
- [ ] 회원 상세 조회: 기본 정보, 주문 이력, 리뷰 이력, 포인트 이력, 제재 이력을 한 화면에서 확인한다
- [ ] 정지 처리: 정지 기간(7일/30일/영구)과 사유를 입력한다
- [ ] 정지 기간 중에는 로그인/주문/리뷰 작성이 불가하다
- [ ] 정지 해제: 관리자가 수동으로 정지를 해제하거나, 정지 기간 만료 시 자동 해제한다
- [ ] 탈퇴 처리: 관리자 강제 탈퇴 시 개인정보를 즉시 마스킹 처리한다
- [ ] 처리 이력을 admin_action_log에 기록한다
- [ ] 회원 상태 변경 시 `member.status.changed` Kafka 이벤트를 발행한다

#### 데이터 모델
```sql
CREATE TABLE member_suspension (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    suspension_type     VARCHAR(20)     NOT NULL COMMENT '정지 유형: TEMPORARY, PERMANENT',
    duration_days       INT             NULL COMMENT '정지 기간 (일). 영구 정지는 NULL',
    reason              VARCHAR(500)    NOT NULL COMMENT '정지 사유',
    suspended_at        DATETIME(6)     NOT NULL COMMENT '정지 시작일',
    expires_at          DATETIME(6)     NULL COMMENT '정지 만료일 (영구 정지는 NULL)',
    released_at         DATETIME(6)     NULL COMMENT '해제일',
    released_by         BIGINT          NULL COMMENT '해제 처리 관리자 ID',
    admin_id            BIGINT          NOT NULL COMMENT '처리 관리자 ID',
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '상태: ACTIVE, RELEASED, EXPIRED',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 정지 이력';

CREATE TABLE admin_action_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    admin_id            BIGINT          NOT NULL COMMENT '관리자 ID',
    action_type         VARCHAR(50)     NOT NULL COMMENT '액션: PRODUCT_APPROVE, PRODUCT_REJECT, MEMBER_SUSPEND, ORDER_CANCEL, ...',
    target_type         VARCHAR(30)     NOT NULL COMMENT '대상: PRODUCT, MEMBER, ORDER, SETTLEMENT, COUPON, BANNER',
    target_id           BIGINT          NOT NULL COMMENT '대상 ID',
    before_status       VARCHAR(30)     NULL COMMENT '변경 전 상태',
    after_status        VARCHAR(30)     NOT NULL COMMENT '변경 후 상태',
    reason              VARCHAR(500)    NULL COMMENT '사유',
    ip_address          VARCHAR(45)     NULL COMMENT '접속 IP',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_action_type (action_type),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='관리자 액션 로그';
```

---

### US-1804: 주문 관리 (강제 취소/환불)

**As a** 플랫폼 관리자
**I want to** 문제가 있는 주문을 강제 취소하거나 환불 처리하고 싶다
**So that** 고객 불만을 신속하게 해결하고 분쟁을 방지할 수 있다

#### Acceptance Criteria
- [ ] 주문 목록 조회: 상태별/회원별/셀러별/기간별/금액별 필터링 지원
- [ ] 주문 상세 조회: 주문 정보, 결제 정보, 배송 정보, CS 이력을 한 화면에서 확인한다
- [ ] 강제 취소: 결제 완료/배송 준비 상태의 주문을 관리자가 강제 취소할 수 있다
- [ ] 배송 중인 주문의 강제 취소 시 셀러에게 알림을 발송한다
- [ ] 환불 처리: 부분 환불(금액 지정) 및 전체 환불을 지원한다
- [ ] 환불 시 결제 수단에 따라 PG 환불 API를 호출한다
- [ ] 환불 금액에 사용된 포인트/쿠폰은 자동 복원한다
- [ ] 처리 이력을 admin_action_log에 기록한다
- [ ] 주문 상태 변경 시 `order.admin.cancelled` Kafka 이벤트를 발행한다

#### API 스펙
```
GET /api/v1/admin/orders?status={status}&memberId={memberId}&sellerId={sellerId}&startDate={start}&endDate={end}&page={page}&size={size}
Authorization: Bearer {token} (admin)

POST /api/v1/admin/orders/{orderId}/force-cancel
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "reason": "고객 분쟁 해결을 위한 관리자 강제 취소",
    "refundAmount": 39900,
    "restorePoint": true,
    "restoreCoupon": true
}

Response: 200 OK
{
    "orderId": 12345,
    "status": "ADMIN_CANCELLED",
    "refundInfo": {
        "refundAmount": 39900,
        "restoredPoint": 1000,
        "restoredCouponId": 567,
        "refundMethod": "CARD",
        "estimatedRefundDate": "2026-03-25"
    }
}
```

---

### US-1805: 정산 관리 (일괄 지급)

**As a** 플랫폼 관리자
**I want to** 셀러 정산을 확인하고 일괄 지급 처리하고 싶다
**So that** 정확하고 효율적으로 셀러에게 수익을 지급할 수 있다

#### Acceptance Criteria
- [ ] 정산서 목록 조회: 상태별/셀러별/정산 기간별 필터링 지원
- [ ] 정산서 상세 조회: 정산 항목 목록, 수수료 내역, 지급 예정 금액을 확인한다
- [ ] 지급 승인: 관리자가 정산서를 확인 후 지급 승인한다
- [ ] 일괄 지급: 여러 정산서를 선택하여 한 번에 지급 처리한다
- [ ] 지급 보류: 이슈가 있는 정산서를 보류 처리하고 사유를 기록한다
- [ ] 지급 완료 시 셀러에게 정산 완료 알림을 발송한다
- [ ] 처리 이력을 admin_action_log에 기록한다

#### API 스펙
```
GET /api/v1/admin/settlements?status={status}&sellerId={sellerId}&startDate={start}&endDate={end}&page={page}&size={size}
Authorization: Bearer {token} (admin)

POST /api/v1/admin/settlements/batch-pay
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "settlementIds": [101, 102, 103],
    "paymentDate": "2026-03-25"
}

Response: 200 OK
{
    "successCount": 3,
    "failCount": 0,
    "totalPaymentAmount": 15600000,
    "results": [
        { "settlementId": 101, "sellerId": 10, "amount": 5200000, "status": "PAID" },
        { "settlementId": 102, "sellerId": 11, "amount": 4800000, "status": "PAID" },
        { "settlementId": 103, "sellerId": 12, "amount": 5600000, "status": "PAID" }
    ]
}
```

---

### US-1806: 배너/기획전 관리

**As a** 플랫폼 관리자
**I want to** 메인 페이지 배너와 기획전을 등록/수정/삭제하고 싶다
**So that** 프로모션을 효과적으로 운영할 수 있다

#### Acceptance Criteria
- [ ] 배너 CRUD: 이미지, 링크 URL, 노출 기간, 노출 순서, 타겟 페이지를 관리한다
- [ ] 기획전 CRUD: 제목, 설명, 상품 목록, 노출 기간, 할인 정책을 관리한다
- [ ] 스케줄링: 노출 시작일/종료일을 설정하여 자동 노출/비노출 처리한다
- [ ] 미리보기: 배너/기획전의 노출 상태를 미리 확인할 수 있다
- [ ] A/B 테스트: 동일 위치에 2개 배너를 등록하고 클릭률을 비교한다
- [ ] 배너 클릭 로그를 수집하여 CTR을 측정한다
- [ ] 처리 이력을 admin_action_log에 기록한다

#### API 스펙
```
POST /api/v1/admin/banners
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "title": "SS 시즌 오프",
    "imageUrl": "https://cdn.closet.com/banners/ss-sale.jpg",
    "linkUrl": "/exhibitions/ss-sale",
    "position": "MAIN_TOP",
    "displayOrder": 1,
    "startAt": "2026-04-01T00:00:00",
    "endAt": "2026-04-30T23:59:59",
    "isActive": true
}

Response: 201 Created
{ "bannerId": 50, "status": "SCHEDULED" }

GET /api/v1/admin/banners?position={position}&status={status}&page={page}&size={size}
Authorization: Bearer {token} (admin)

PUT /api/v1/admin/banners/{bannerId}
Authorization: Bearer {token} (admin)

DELETE /api/v1/admin/banners/{bannerId}
Authorization: Bearer {token} (admin)
```

---

### US-1807: 쿠폰 관리

**As a** 플랫폼 관리자
**I want to** 쿠폰을 생성/수정/발급/중지하고 사용 현황을 모니터링하고 싶다
**So that** 프로모션을 전략적으로 운영할 수 있다

#### Acceptance Criteria
- [ ] 쿠폰 생성: 쿠폰명, 할인 유형(정액/정률), 할인 금액, 최소 주문 금액, 유효 기간, 발급 수량 제한을 설정한다
- [ ] 쿠폰 발급: 전체 회원/특정 등급/특정 회원 대상으로 일괄 발급한다
- [ ] 자동 발급: 회원가입, 첫 구매, 생일 등 이벤트 트리거 기반 자동 발급을 설정한다
- [ ] 쿠폰 중지: 진행 중인 쿠폰을 즉시 중지하고 미사용분을 무효화한다
- [ ] 사용 현황: 발급 수, 사용 수, 사용률, 할인 총액을 실시간 모니터링한다
- [ ] 쿠폰 코드 방식: 코드 입력 시 적용되는 프로모션 쿠폰을 지원한다
- [ ] 처리 이력을 admin_action_log에 기록한다

#### API 스펙
```
POST /api/v1/admin/coupons
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "name": "SS 시즌 웰컴 쿠폰",
    "discountType": "FIXED",
    "discountAmount": 5000,
    "minOrderAmount": 30000,
    "maxDiscountAmount": 5000,
    "validFrom": "2026-04-01T00:00:00",
    "validTo": "2026-04-30T23:59:59",
    "totalQuantity": 10000,
    "perMemberLimit": 1,
    "targetType": "ALL",
    "couponCode": "SS2026WELCOME",
    "isAutoIssue": false
}

Response: 201 Created
{ "couponId": 200, "status": "ACTIVE", "issuedCount": 0 }

POST /api/v1/admin/coupons/{couponId}/issue
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{
    "targetType": "GRADE",
    "targetGrade": "GOLD",
    "memberIds": null
}

Response: 200 OK
{ "couponId": 200, "issuedCount": 5280, "totalIssued": 5280 }

GET /api/v1/admin/coupons/{couponId}/statistics
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "couponId": 200,
    "name": "SS 시즌 웰컴 쿠폰",
    "totalIssued": 5280,
    "totalUsed": 1560,
    "usageRate": 29.5,
    "totalDiscountAmount": 7800000,
    "avgOrderAmount": 52000
}
```

---

## 4. 이벤트 기반 아키텍처 고도화

### US-1901: Kafka 이벤트 브로커 본격 활용

**As a** 시스템 아키텍트
**I want to** 서비스 간 통신을 Kafka 이벤트 기반으로 전환하고 싶다
**So that** 서비스 간 결합도를 낮추고 확장성을 확보할 수 있다

#### Acceptance Criteria
- [ ] 도메인 이벤트 정의: 각 서비스의 핵심 상태 변경을 이벤트로 발행한다
- [ ] 이벤트 토픽 구조: `closet.{service}.{entity}.{action}` (예: `closet.order.order.created`)
- [ ] 이벤트 스키마: Avro 포맷으로 정의하고, Schema Registry에 등록한다
- [ ] Producer: 트랜잭션 내에서 Transactional Outbox 패턴으로 이벤트를 발행한다
- [ ] Consumer: 멱등성(Idempotency)을 보장하는 소비자를 구현한다
- [ ] Consumer Group: 서비스별 독립 Consumer Group을 운영한다
- [ ] 파티셔닝: Entity ID를 파티션 키로 사용하여 순서 보장을 한다
- [ ] 모니터링: Consumer Lag을 Prometheus로 수집하고 임계치 초과 시 알림한다

#### 이벤트 토픽 목록

| 토픽 | Producer | Consumer | 설명 |
|------|----------|----------|------|
| closet.order.order.created | order-service | payment, inventory, notification | 주문 생성 |
| closet.order.order.cancelled | order-service | payment, inventory, notification, promotion | 주문 취소 |
| closet.order.order.confirmed | order-service | settlement, notification | 구매 확정 |
| closet.payment.payment.completed | payment-service | order, notification | 결제 완료 |
| closet.payment.payment.refunded | payment-service | order, settlement, notification | 환불 완료 |
| closet.shipping.shipping.started | shipping-service | order, notification | 배송 시작 |
| closet.shipping.shipping.completed | shipping-service | order, notification | 배송 완료 |
| closet.product.product.created | product-service | search, recommendation | 상품 등록 |
| closet.product.product.updated | product-service | search, recommendation | 상품 수정 |
| closet.product.stock.changed | inventory-service | product, notification | 재고 변경 |
| closet.member.member.created | member-service | notification, promotion | 회원 가입 |
| closet.review.review.created | review-service | product, recommendation | 리뷰 작성 |
| closet.search.keyword.searched | search-service | recommendation | 검색 수행 |

#### Transactional Outbox 패턴
```sql
CREATE TABLE outbox_event (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    aggregate_type      VARCHAR(50)     NOT NULL COMMENT '집합체 유형: ORDER, PAYMENT, ...',
    aggregate_id        BIGINT          NOT NULL COMMENT '집합체 ID',
    event_type          VARCHAR(100)    NOT NULL COMMENT '이벤트 유형',
    topic               VARCHAR(200)    NOT NULL COMMENT 'Kafka 토픽',
    partition_key       VARCHAR(100)    NOT NULL COMMENT '파티션 키',
    payload             TEXT            NOT NULL COMMENT '이벤트 페이로드 (JSON)',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '상태: PENDING, PUBLISHED, FAILED',
    retry_count         INT             NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    published_at        DATETIME(6)     NULL COMMENT '발행 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbox 이벤트';
```

---

### US-1902: Event Sourcing (주문 이력)

**As a** 시스템 아키텍트
**I want to** 주문 도메인에 Event Sourcing을 적용하고 싶다
**So that** 주문의 모든 상태 변경 이력을 완전하게 보존하고, 특정 시점의 상태를 재현할 수 있다

#### Acceptance Criteria
- [ ] 주문 Aggregate의 모든 상태 변경을 이벤트 스토어에 저장한다
- [ ] 이벤트 종류: OrderCreated, OrderPaid, OrderShipped, OrderDelivered, OrderCancelled, OrderRefunded
- [ ] 이벤트 스토어에서 이벤트를 순서대로 재생(replay)하여 주문 현재 상태를 복원할 수 있다
- [ ] 스냅샷: 이벤트가 50건을 초과하면 스냅샷을 생성하여 복원 성능을 최적화한다
- [ ] 시간 여행(Time Travel): 특정 시점의 주문 상태를 조회할 수 있다
- [ ] 이벤트 스토어는 MySQL에 저장하되, 추후 MongoDB 이관을 고려한 추상화를 적용한다
- [ ] 기존 주문 조회 API와의 하위 호환성을 유지한다

#### 데이터 모델
```sql
CREATE TABLE order_event_store (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    aggregate_id        BIGINT          NOT NULL COMMENT '주문 ID (Aggregate ID)',
    event_type          VARCHAR(50)     NOT NULL COMMENT '이벤트 유형',
    event_data          TEXT            NOT NULL COMMENT '이벤트 데이터 (JSON)',
    metadata            VARCHAR(500)    NULL COMMENT '메타데이터 (JSON)',
    version             INT             NOT NULL COMMENT '버전 (순서 보장)',
    created_by          BIGINT          NULL COMMENT '생성자 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_aggregate_version (aggregate_id, version),
    INDEX idx_aggregate_id (aggregate_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='주문 이벤트 스토어';

CREATE TABLE order_snapshot (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    aggregate_id        BIGINT          NOT NULL COMMENT '주문 ID (Aggregate ID)',
    snapshot_data       TEXT            NOT NULL COMMENT '스냅샷 데이터 (JSON)',
    version             INT             NOT NULL COMMENT '스냅샷 시점 버전',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_aggregate_version (aggregate_id, version),
    INDEX idx_aggregate_id (aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='주문 스냅샷';
```

---

### US-1903: CQRS 읽기 모델 분리

**As a** 시스템 아키텍트
**I want to** 주문 도메인에 CQRS를 적용하여 읽기 모델과 쓰기 모델을 분리하고 싶다
**So that** 읽기 성능을 최적화하고, 복잡한 조회 요구사항에 유연하게 대응할 수 있다

#### Acceptance Criteria
- [ ] Command (쓰기): 기존 MySQL 기반 주문 서비스를 유지한다
- [ ] Query (읽기): MongoDB에 비정규화된 읽기 모델을 구축한다
- [ ] 읽기 모델은 주문 이벤트를 Kafka로 소비하여 비동기로 갱신한다
- [ ] 읽기 모델에는 주문 + 상품 + 회원 + 배송 + 결제 정보가 Join 없이 포함된다
- [ ] 기존 주문 목록/상세 조회 API를 읽기 모델(MongoDB)에서 처리한다
- [ ] Eventual Consistency: 쓰기와 읽기 사이 최대 3초의 지연을 허용한다
- [ ] 읽기 모델 동기화 실패 시 DLQ에 적재하고 재처리한다
- [ ] MongoDB 인덱스 최적화로 조회 성능 P95 50ms 이내를 달성한다

#### 읽기 모델 (MongoDB Document)
```json
{
    "_id": "order_12345",
    "orderId": 12345,
    "orderNumber": "ORD-2026-0322-12345",
    "status": "DELIVERED",
    "member": {
        "memberId": 100,
        "name": "홍길동",
        "grade": "GOLD",
        "email": "hong@example.com"
    },
    "items": [
        {
            "orderItemId": 1,
            "productId": 101,
            "productName": "오버핏 코튼 셔츠",
            "brandName": "무탠다드",
            "size": "L",
            "color": "WHITE",
            "quantity": 1,
            "price": 39900,
            "discountPrice": 29900,
            "thumbnailUrl": "https://cdn.closet.com/products/101/thumb.jpg",
            "status": "DELIVERED"
        }
    ],
    "payment": {
        "paymentId": 500,
        "method": "CARD",
        "totalAmount": 29900,
        "paidAt": "2026-03-20T10:30:00"
    },
    "shipping": {
        "shippingId": 300,
        "carrier": "CJ대한통운",
        "trackingNumber": "1234567890",
        "status": "DELIVERED",
        "deliveredAt": "2026-03-22T14:00:00"
    },
    "totalAmount": 29900,
    "discountAmount": 10000,
    "shippingFee": 0,
    "createdAt": "2026-03-20T10:00:00",
    "updatedAt": "2026-03-22T14:00:00",
    "events": [
        { "type": "ORDER_CREATED", "timestamp": "2026-03-20T10:00:00" },
        { "type": "PAYMENT_COMPLETED", "timestamp": "2026-03-20T10:30:00" },
        { "type": "SHIPPING_STARTED", "timestamp": "2026-03-21T09:00:00" },
        { "type": "DELIVERED", "timestamp": "2026-03-22T14:00:00" }
    ]
}
```

---

### US-1904: Dead Letter Queue + 재처리

**As a** 시스템 운영자
**I want to** 처리 실패한 이벤트를 Dead Letter Queue에 적재하고 재처리 메커니즘을 구축하고 싶다
**So that** 이벤트 유실 없이 안정적으로 비동기 처리를 운영할 수 있다

#### Acceptance Criteria
- [ ] 이벤트 처리 실패 시 3회 재시도 후 DLQ 토픽으로 전달한다
- [ ] 재시도 간격: 1초, 5초, 30초 (Exponential Backoff)
- [ ] DLQ 토픽 네이밍: `closet.{원본토픽}.dlq` (예: `closet.order.order.created.dlq`)
- [ ] DLQ 이벤트 조회 API: 관리자가 DLQ에 적재된 이벤트 목록을 확인할 수 있다
- [ ] DLQ 재처리 API: 관리자가 특정 이벤트 또는 전체를 수동 재처리할 수 있다
- [ ] DLQ 무시 API: 처리 불가능한 이벤트를 무시(skip) 처리할 수 있다
- [ ] DLQ 적재 시 Slack/이메일 알림을 발송한다
- [ ] DLQ 이벤트에는 실패 원인, 재시도 횟수, 원본 이벤트 정보를 포함한다
- [ ] DLQ 재처리 성공률 95% 이상을 목표로 한다

#### 데이터 모델
```sql
CREATE TABLE dead_letter_event (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    original_topic      VARCHAR(200)    NOT NULL COMMENT '원본 토픽',
    partition_num       INT             NOT NULL COMMENT '파티션 번호',
    offset_num          BIGINT          NOT NULL COMMENT '오프셋',
    event_key           VARCHAR(100)    NULL COMMENT '이벤트 키',
    event_payload       TEXT            NOT NULL COMMENT '이벤트 페이로드',
    error_message       VARCHAR(1000)   NOT NULL COMMENT '에러 메시지',
    error_stack_trace   TEXT            NULL COMMENT '에러 스택 트레이스',
    retry_count         INT             NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '상태: PENDING, RETRYING, RESOLVED, SKIPPED',
    resolved_at         DATETIME(6)     NULL COMMENT '처리 완료 시각',
    resolved_by         BIGINT          NULL COMMENT '처리 관리자 ID',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_original_topic (original_topic),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dead Letter 이벤트';
```

#### API 스펙
```
GET /api/v1/admin/dlq?topic={topic}&status={status}&page={page}&size={size}
Authorization: Bearer {token} (admin)

Response: 200 OK
{
    "totalCount": 15,
    "items": [
        {
            "id": 1,
            "originalTopic": "closet.order.order.created",
            "eventKey": "12345",
            "errorMessage": "inventory-service unavailable",
            "retryCount": 3,
            "status": "PENDING",
            "createdAt": "2026-03-22T10:30:00"
        }
    ]
}

POST /api/v1/admin/dlq/{dlqId}/retry
Authorization: Bearer {token} (admin)

POST /api/v1/admin/dlq/batch-retry
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{ "dlqIds": [1, 2, 3] }

POST /api/v1/admin/dlq/{dlqId}/skip
Authorization: Bearer {token} (admin)
Content-Type: application/json

Request:
{ "reason": "중복 이벤트로 확인되어 무시 처리" }
```

---

### US-1905: 이벤트 스키마 레지스트리 (Avro)

**As a** 시스템 아키텍트
**I want to** 이벤트 스키마를 Avro로 정의하고 Schema Registry에서 관리하고 싶다
**So that** 이벤트 스키마의 호환성을 보장하고 서비스 간 계약을 명확히 할 수 있다

#### Acceptance Criteria
- [ ] Confluent Schema Registry를 Docker로 배포한다
- [ ] 모든 Kafka 이벤트의 스키마를 Avro IDL로 정의한다
- [ ] Schema Compatibility: BACKWARD 호환성을 기본 정책으로 설정한다
- [ ] 스키마 진화 규칙: 필드 추가(기본값 필수), 필드 삭제 금지, 타입 변경 금지
- [ ] Producer는 Schema Registry에서 스키마를 조회하여 직렬화한다
- [ ] Consumer는 Schema Registry에서 스키마를 조회하여 역직렬화한다
- [ ] 스키마 변경 시 CI/CD에서 호환성 검증을 수행한다
- [ ] 스키마 버전 이력을 관리하고, 롤백이 가능하다

#### Avro 스키마 예시 (주문 생성 이벤트)
```json
{
    "type": "record",
    "name": "OrderCreatedEvent",
    "namespace": "com.closet.order.event",
    "fields": [
        { "name": "orderId", "type": "long" },
        { "name": "orderNumber", "type": "string" },
        { "name": "memberId", "type": "long" },
        { "name": "totalAmount", "type": "int" },
        { "name": "discountAmount", "type": "int", "default": 0 },
        { "name": "shippingFee", "type": "int", "default": 0 },
        {
            "name": "items",
            "type": {
                "type": "array",
                "items": {
                    "type": "record",
                    "name": "OrderItemEvent",
                    "fields": [
                        { "name": "productId", "type": "long" },
                        { "name": "productName", "type": "string" },
                        { "name": "quantity", "type": "int" },
                        { "name": "price", "type": "int" }
                    ]
                }
            }
        },
        { "name": "createdAt", "type": "string" }
    ]
}
```

---

## 5. 성능 최적화

### US-2001: Redis 캐싱 전략

**As a** 시스템 아키텍트
**I want to** 주요 조회 API에 Redis 캐싱을 적용하고 싶다
**So that** DB 부하를 줄이고 API 응답 시간을 단축할 수 있다

#### Acceptance Criteria
- [ ] 캐시 대상 및 TTL 정책:

| 캐시 대상 | 키 패턴 | TTL | 무효화 전략 |
|----------|---------|-----|-----------|
| 상품 상세 | `product:{productId}` | 1시간 | 상품 수정 시 즉시 무효화 |
| 상품 목록 (카테고리별) | `products:category:{categoryId}:page:{page}` | 10분 | 상품 등록/수정/삭제 시 무효화 |
| 카테고리 트리 | `categories:tree` | 24시간 | 카테고리 변경 시 무효화 |
| 브랜드 목록 | `brands:all` | 24시간 | 브랜드 변경 시 무효화 |
| 랭킹 (카테고리별) | `ranking:category:{categoryId}` | 5분 | 주기적 갱신 |
| 인기 검색어 | `search:trending` | 1분 | 주기적 갱신 |
| 회원 정보 | `member:{memberId}` | 30분 | 회원 정보 변경 시 무효화 |
| 장바구니 | `cart:{memberId}` | 7일 | 장바구니 변경 시 즉시 갱신 |

- [ ] Cache-Aside 패턴을 기본으로 적용한다
- [ ] 캐시 스탬피드 방지: Mutex Lock (SETNX) + 짧은 TTL 연장
- [ ] 캐시 워밍: 서비스 기동 시 핵심 데이터(카테고리, 브랜드)를 사전 로딩한다
- [ ] 캐시 히트율 모니터링: Prometheus 메트릭으로 수집하고 Grafana 대시보드에 표시한다
- [ ] 캐시 히트율 85% 이상을 목표로 한다

---

### US-2002: N+1 쿼리 최적화

**As a** 시스템 개발자
**I want to** JPA N+1 문제를 체계적으로 해결하고 싶다
**So that** DB 쿼리 성능을 최적화하고 불필요한 DB 호출을 줄일 수 있다

#### Acceptance Criteria
- [ ] 모든 연관 관계에 대해 지연 로딩(LAZY)을 기본으로 설정한다
- [ ] 목록 조회 API: QueryDSL fetchJoin 또는 @EntityGraph를 사용하여 N+1을 방지한다
- [ ] 대량 조회: `@BatchSize(size = 100)` 또는 `default_batch_fetch_size` 글로벌 설정을 적용한다
- [ ] DTO 프로젝션: 필요한 필드만 select하는 QueryDSL DTO 프로젝션을 적용한다
- [ ] 쿼리 로깅: 개발 환경에서 실행 쿼리 수를 카운트하고, 목록 API에서 쿼리 3개 이상 발생 시 경고 로그를 출력한다
- [ ] 슬로우 쿼리 모니터링: 500ms 이상 소요 쿼리를 로깅하고 알림한다
- [ ] 주요 목록 API의 쿼리 수를 3개 이내로 제한한다

---

### US-2003: Connection Pool 튜닝

**As a** 시스템 운영자
**I want to** HikariCP Connection Pool을 최적화하고 싶다
**So that** DB 연결 병목을 방지하고 안정적인 서비스를 운영할 수 있다

#### Acceptance Criteria
- [ ] 서비스별 Connection Pool 설정:

| 서비스 | Maximum Pool Size | Minimum Idle | Connection Timeout | Idle Timeout |
|--------|------------------|-------------|-------------------|-------------|
| product-service | 20 | 5 | 3000ms | 600000ms |
| order-service | 30 | 10 | 3000ms | 600000ms |
| payment-service | 15 | 5 | 3000ms | 600000ms |
| member-service | 15 | 5 | 3000ms | 600000ms |
| admin-service | 10 | 3 | 5000ms | 600000ms |

- [ ] Connection Pool 메트릭을 Prometheus로 수집한다 (active, idle, pending, total)
- [ ] Connection Leak Detection: 30초 이상 반환되지 않은 Connection을 감지하고 로깅한다
- [ ] DB 장애 시 Circuit Breaker를 적용하여 연결 시도를 차단한다
- [ ] 부하 테스트(k6)로 최적 Pool Size를 검증한다

---

### US-2004: 대용량 데이터 파티셔닝 (주문, 로그)

**As a** 시스템 아키텍트
**I want to** 대용량 테이블에 파티셔닝을 적용하고 싶다
**So that** 조회 성능을 유지하고 데이터 관리를 효율화할 수 있다

#### Acceptance Criteria
- [ ] 파티셔닝 대상 테이블:

| 테이블 | 파티션 전략 | 파티션 키 | 보존 기간 |
|--------|-----------|----------|----------|
| order_event_store | RANGE (월별) | created_at | 2년 |
| member_activity_log | RANGE (월별) | created_at | 1년 |
| search_keyword_log | RANGE (월별) | created_at | 6개월 |
| dead_letter_event | RANGE (월별) | created_at | 3개월 |
| admin_action_log | RANGE (월별) | created_at | 1년 |

- [ ] 파티션 자동 생성: 매월 1일 00:00에 다음 월 파티션을 자동 생성하는 이벤트 스케줄러를 설정한다
- [ ] 파티션 삭제: 보존 기간이 지난 파티션을 DROP PARTITION으로 삭제한다 (DELETE보다 고성능)
- [ ] 파티션 프루닝이 동작하는지 EXPLAIN으로 검증한다
- [ ] 파티셔닝 적용 전후 조회 성능을 벤치마크한다

---

## 6. 보안 강화

### US-2101: Spring Security + JWT 인증 강화

**As a** 시스템 아키텍트
**I want to** Spring Security를 적용하여 인증/인가를 강화하고 싶다
**So that** API 보안을 체계적으로 관리할 수 있다

#### Acceptance Criteria
- [ ] Spring Security 6.x를 API Gateway에 적용한다
- [ ] JWT Access Token (만료: 30분) + Refresh Token (만료: 14일) 이중 토큰 체계를 적용한다
- [ ] Refresh Token은 Redis에 저장하고, 토큰 갱신 시 Rotation을 적용한다
- [ ] Refresh Token Rotation: 갱신 시 기존 Refresh Token을 무효화하고 새로운 Refresh Token을 발급한다
- [ ] 동시 로그인 제한: 디바이스 3대까지 허용하며, 초과 시 가장 오래된 세션을 만료한다
- [ ] Role 기반 인가: ROLE_USER, ROLE_SELLER, ROLE_ADMIN 3단계를 적용한다
- [ ] API 엔드포인트별 접근 권한을 설정한다:
  - `/api/v1/admin/**` → ROLE_ADMIN
  - `/api/v1/seller/**` → ROLE_SELLER, ROLE_ADMIN
  - `/api/v1/**` → ROLE_USER 이상
  - `/api/v1/products/**` (GET) → 인증 불필요
- [ ] JWT 블랙리스트: 로그아웃 시 Access Token을 Redis 블랙리스트에 등록한다

---

### US-2102: OAuth2 소셜 로그인 (카카오, 네이버, 구글)

**As a** 회원가입하려는 사용자
**I want to** 카카오/네이버/구글 계정으로 간편하게 로그인하고 싶다
**So that** 복잡한 회원가입 절차 없이 빠르게 서비스를 이용할 수 있다

#### Acceptance Criteria
- [ ] OAuth2 Authorization Code Grant 방식을 적용한다
- [ ] 지원 Provider: 카카오 (Kakao), 네이버 (Naver), 구글 (Google)
- [ ] Provider별 수집 정보:

| Provider | 필수 수집 | 선택 수집 |
|----------|----------|----------|
| 카카오 | 이메일, 닉네임, 프로필 이미지 | 성별, 연령대 |
| 네이버 | 이메일, 이름, 프로필 이미지 | 성별, 생년월일 |
| 구글 | 이메일, 이름, 프로필 이미지 | - |

- [ ] 소셜 계정 연동: 기존 이메일 회원이 동일 이메일의 소셜 계정으로 로그인 시 연동 안내를 제공한다
- [ ] 다중 소셜 연동: 하나의 회원 계정에 여러 소셜 계정을 연동할 수 있다
- [ ] 최초 소셜 로그인 시 자동 회원가입 처리 (추가 정보 입력 페이지로 이동)
- [ ] 소셜 로그인 후 JWT를 발급하여 기존 인증 흐름과 동일하게 처리한다
- [ ] Provider 장애 시 Fallback: 이메일/비밀번호 로그인으로 안내한다

#### 데이터 모델
```sql
CREATE TABLE member_social_account (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    member_id           BIGINT          NOT NULL COMMENT '회원 ID',
    provider            VARCHAR(20)     NOT NULL COMMENT 'KAKAO, NAVER, GOOGLE',
    provider_id         VARCHAR(100)    NOT NULL COMMENT 'Provider 고유 ID',
    email               VARCHAR(200)    NULL COMMENT 'Provider 이메일',
    name                VARCHAR(50)     NULL COMMENT 'Provider 이름',
    profile_image_url   VARCHAR(500)    NULL COMMENT '프로필 이미지 URL',
    access_token        VARCHAR(500)    NULL COMMENT 'Provider Access Token (암호화)',
    refresh_token       VARCHAR(500)    NULL COMMENT 'Provider Refresh Token (암호화)',
    connected_at        DATETIME(6)     NOT NULL COMMENT '연동 시각',
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_provider_id (provider, provider_id),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 소셜 계정 연동';
```

#### API 스펙
```
GET /api/v1/auth/oauth2/{provider}
→ Redirect to Provider Authorization URL

GET /api/v1/auth/oauth2/{provider}/callback?code={code}&state={state}
→ Provider에서 리디렉트 후 토큰 교환

Response: 200 OK
{
    "isNewMember": false,
    "accessToken": "eyJhbGciOiJ...",
    "refreshToken": "eyJhbGciOiJ...",
    "member": {
        "memberId": 100,
        "email": "hong@kakao.com",
        "name": "홍길동",
        "profileImageUrl": "https://k.kakaocdn.net/...",
        "socialAccounts": ["KAKAO", "GOOGLE"]
    }
}

Response: 200 OK (신규 회원)
{
    "isNewMember": true,
    "tempToken": "temp_abc123",
    "provider": "KAKAO",
    "profile": {
        "email": "new@kakao.com",
        "name": "김신규",
        "profileImageUrl": "https://k.kakaocdn.net/..."
    },
    "redirectUrl": "/signup/complete"
}
```

---

### US-2103: API Rate Limiting

**As a** 시스템 운영자
**I want to** API 엔드포인트별 Rate Limiting을 적용하고 싶다
**So that** 서비스 남용과 DDoS 공격으로부터 시스템을 보호할 수 있다

#### Acceptance Criteria
- [ ] Rate Limiting 정책:

| 대상 | 제한 | 윈도우 | 식별 기준 |
|------|------|--------|----------|
| 비인증 API | 60 req/min | Sliding Window | IP |
| 인증 API | 300 req/min | Sliding Window | Member ID |
| 검색 API | 30 req/min | Sliding Window | IP or Member ID |
| 결제 API | 10 req/min | Fixed Window | Member ID |
| 어드민 API | 600 req/min | Sliding Window | Admin ID |
| 로그인 시도 | 5 req/min | Fixed Window | IP + Email |

- [ ] Redis Token Bucket 알고리즘으로 구현한다
- [ ] 제한 초과 시 429 Too Many Requests를 반환한다
- [ ] 응답 헤더에 Rate Limit 정보를 포함한다:
  - `X-RateLimit-Limit`: 제한 횟수
  - `X-RateLimit-Remaining`: 남은 횟수
  - `X-RateLimit-Reset`: 리셋 시각 (Unix Timestamp)
- [ ] IP 기반 블랙리스트: 반복적 제한 초과 IP를 자동으로 1시간 차단한다
- [ ] 화이트리스트: 내부 서비스 IP는 Rate Limiting에서 제외한다

---

### US-2104: 결제 데이터 암호화

**As a** 시스템 보안 담당자
**I want to** 결제 관련 민감 데이터를 암호화하고 싶다
**So that** 데이터 유출 시에도 결제 정보를 보호할 수 있다

#### Acceptance Criteria
- [ ] 암호화 대상: 카드 번호 (마스킹 저장), 빌링키, 소셜 로그인 토큰
- [ ] 암호화 알고리즘: AES-256-GCM (대칭키 암호화)
- [ ] 키 관리: 암호화 키를 환경 변수로 주입하고, 90일마다 키 로테이션을 수행한다
- [ ] 카드 번호: 앞 6자리 + 마스킹 + 뒤 4자리만 저장 (예: 123456******7890)
- [ ] DB 컬럼 레벨 암호화: JPA AttributeConverter를 사용하여 투명하게 암호화/복호화한다
- [ ] 로그에 민감 정보 출력 방지: Logback PatternLayout에서 마스킹 필터를 적용한다
- [ ] 감사 로그: 암호화된 데이터의 조회/수정 이력을 기록한다

---

## 7. 인프라 고도화

### US-2201: Kubernetes (EKS) 배포

**As a** 시스템 운영자
**I want to** 모든 서비스를 Kubernetes(EKS)에 배포하고 싶다
**So that** 자동 확장, 자동 복구, 효율적인 리소스 관리를 달성할 수 있다

#### Acceptance Criteria
- [ ] AWS EKS 1.28+ 클러스터를 구축한다
- [ ] 네임스페이스: `closet-dev`, `closet-staging`, `closet-prod`로 환경을 분리한다
- [ ] 서비스별 Deployment + Service + HPA를 구성한다
- [ ] HPA (Horizontal Pod Autoscaler) 정책:

| 서비스 | Min Replicas | Max Replicas | CPU Target | Memory Target |
|--------|-------------|-------------|-----------|-------------|
| product-service | 2 | 10 | 70% | 80% |
| order-service | 3 | 15 | 60% | 75% |
| payment-service | 2 | 8 | 50% | 70% |
| member-service | 2 | 8 | 70% | 80% |
| recommendation-service | 2 | 6 | 60% | 70% |
| admin-service | 1 | 3 | 70% | 80% |
| api-gateway | 3 | 20 | 60% | 75% |

- [ ] Liveness Probe + Readiness Probe를 모든 서비스에 설정한다
- [ ] Resource Requests/Limits를 설정하여 리소스 격리를 보장한다
- [ ] ConfigMap + Secret으로 환경 설정을 관리한다
- [ ] PDB (Pod Disruption Budget): 최소 1개 Pod 유지를 보장한다

---

### US-2202: Helm Chart

**As a** 시스템 운영자
**I want to** 서비스 배포를 Helm Chart로 패키징하고 싶다
**So that** 배포를 표준화하고 환경별 설정을 쉽게 관리할 수 있다

#### Acceptance Criteria
- [ ] 공통 Helm Chart 템플릿: `closet-base-chart`를 생성한다
- [ ] 서비스별 values 파일: `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`
- [ ] Chart 구성 요소: Deployment, Service, HPA, Ingress, ConfigMap, Secret, ServiceAccount
- [ ] 공통 labels/annotations 표준을 정의한다:
  - `app.kubernetes.io/name`, `app.kubernetes.io/version`, `app.kubernetes.io/component`
- [ ] Chart Museum 또는 OCI Registry에 Chart를 게시한다
- [ ] Helm Chart 버전 관리: SemVer를 따른다
- [ ] `helm template`으로 렌더링 결과를 검증하는 CI 단계를 추가한다

---

### US-2203: ArgoCD GitOps

**As a** 시스템 운영자
**I want to** ArgoCD 기반 GitOps로 배포를 자동화하고 싶다
**So that** Git 커밋만으로 안전하게 배포하고, 배포 이력을 완전하게 추적할 수 있다

#### Acceptance Criteria
- [ ] ArgoCD를 EKS 클러스터에 설치한다
- [ ] GitOps 레포지토리: `closet-gitops` 레포에 Helm values와 kustomize overlay를 관리한다
- [ ] Application 구성: 서비스별 ArgoCD Application을 생성한다
- [ ] Sync 정책:
  - `dev`: Auto Sync (커밋 시 자동 배포)
  - `staging`: Manual Sync (승인 후 배포)
  - `prod`: Manual Sync + Sync Window (업무 시간 내만 배포)
- [ ] Rollback: ArgoCD UI에서 이전 버전으로 1-Click Rollback이 가능하다
- [ ] Health Check: ArgoCD가 서비스 헬스를 모니터링하고, 비정상 시 알림한다
- [ ] Notification: Slack 채널에 배포 시작/완료/실패 알림을 전송한다
- [ ] RBAC: 개발자(read-only), DevOps(sync), Admin(all) 권한을 분리한다

---

### US-2204: 서비스 메시 (Istio)

**As a** 시스템 아키텍트
**I want to** Istio 서비스 메시를 적용하고 싶다
**So that** 서비스 간 통신을 안전하게 관리하고, 트래픽 제어/관측성을 확보할 수 있다

#### Acceptance Criteria
- [ ] Istio를 EKS 클러스터에 설치한다 (istioctl 또는 Helm)
- [ ] mTLS: 서비스 간 통신을 자동으로 암호화한다 (STRICT 모드)
- [ ] Traffic Management:
  - Canary 배포: 신규 버전에 10% 트래픽을 점진적으로 할당한다
  - Circuit Breaker: 연속 5xx 에러 5회 이상 시 30초간 트래픽을 차단한다
  - Retry: 5xx 에러 시 2회 자동 재시도한다
  - Timeout: 서비스간 호출 타임아웃을 5초로 설정한다
- [ ] Observability:
  - Kiali: 서비스 간 트래픽 흐름을 시각화한다
  - Jaeger: 분산 트레이싱을 수집한다
  - Prometheus + Grafana: Istio 메트릭을 모니터링한다
- [ ] Authorization Policy: 서비스 간 접근 제어를 설정한다 (예: admin-service만 settlement-service 호출 가능)

---

### US-2205: CI/CD 파이프라인 고도화

**As a** 시스템 운영자
**I want to** CI/CD 파이프라인을 GitHub Actions + ArgoCD로 고도화하고 싶다
**So that** 코드 커밋부터 프로덕션 배포까지 완전 자동화된 파이프라인을 구축할 수 있다

#### Acceptance Criteria
- [ ] CI 파이프라인 (GitHub Actions):
  1. 코드 체크아웃
  2. 빌드 (Gradle)
  3. 단위 테스트 (Kotest)
  4. 통합 테스트 (Testcontainers)
  5. 코드 품질 검사 (ktlint, detekt)
  6. 보안 스캔 (OWASP Dependency Check)
  7. Docker 이미지 빌드 + ECR Push
  8. Helm Chart 버전 업데이트
  9. GitOps 레포에 이미지 태그 업데이트 커밋

- [ ] CD 파이프라인 (ArgoCD):
  1. GitOps 레포 변경 감지
  2. Helm 렌더링 + 검증
  3. Kubernetes Apply
  4. Health Check
  5. Slack 알림

- [ ] 브랜치 전략:
  - `feature/*` → CI (빌드 + 테스트)
  - `dev` → CI + CD (dev 환경 자동 배포)
  - `staging` → CI + CD (staging 환경 수동 승인 배포)
  - `main` → CI + CD (prod 환경 수동 승인 배포)

- [ ] 배포 승인: staging/prod 배포 시 GitHub Environment Protection Rules로 승인을 요구한다
- [ ] 롤백: 이전 이미지 태그로 GitOps 레포를 revert하여 롤백한다
- [ ] 배포 시간 목표: 코드 커밋 → 프로덕션 배포 15분 이내

---

## 8. 기술 스택 요약

### 신규 도입 기술

| 기술 | 용도 | Phase 5 활용 |
|------|------|-------------|
| MongoDB | CQRS 읽기 모델 | 주문 읽기 모델 (비정규화 Document) |
| Confluent Schema Registry | 이벤트 스키마 관리 | Avro 스키마 호환성 관리 |
| AWS EKS | 컨테이너 오케스트레이션 | 전체 서비스 Kubernetes 배포 |
| Helm | 패키지 관리 | 서비스 배포 패키징 |
| ArgoCD | GitOps CD | 선언적 배포 자동화 |
| Istio | 서비스 메시 | mTLS, 트래픽 관리, 관측성 |
| Kiali | 서비스 메시 시각화 | 서비스 간 트래픽 흐름 시각화 |
| Jaeger | 분산 트레이싱 | 요청 추적 및 병목 분석 |
| Spring Security 6.x | 인증/인가 | JWT + OAuth2 + RBAC |
| Spring Cloud Gateway | API Gateway | Rate Limiting, 라우팅 |

### 기존 기술 활용 고도화

| 기술 | Phase 4 활용 | Phase 5 고도화 |
|------|-------------|--------------|
| Kafka | 서비스 간 이벤트 발행 | Transactional Outbox, DLQ, Schema Registry |
| Redis | 캐싱 (기본) | 체계적 캐싱 전략, Rate Limiting, Token 관리 |
| MySQL | 데이터 저장 | 파티셔닝, Connection Pool 튜닝 |
| Spring Batch | 정산 배치 | 유사도 행렬 계산 배치, 통계 집계 배치 |
| Elasticsearch | 검색 | 인기 검색어 집계, 추천 검색어 |

---

## 9. 마일스톤

### Sprint 19 (1주차-2주차): AI 추천 서비스 기반

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| recommendation-service 프로젝트 셋업 (ai-service 리팩토링) | US-1701 | BE | Week 1 |
| 회원 활동 로그 수집 파이프라인 (Kafka Consumer) | US-1701 | BE | Week 1 |
| 개인화 추천 알고리즘 구현 (User-Item ALS) | US-1701 | BE | Week 1-2 |
| 개인화 추천 API 구현 + Redis 캐싱 | US-1701 | BE | Week 2 |
| 개인화 추천 단위 테스트 + 통합 테스트 | US-1701 | QA | Week 2 |

### Sprint 20 (3주차-4주차): AI 추천 고도화

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| Item-Item Collaborative Filtering 구현 | US-1702 | BE | Week 3 |
| "함께 본 상품" API + 일간 배치 | US-1702 | BE | Week 3 |
| Content-Based Filtering 구현 | US-1703 | BE | Week 3-4 |
| "비슷한 상품" API | US-1703 | BE | Week 4 |
| AI 사이즈 추천 모델 구현 | US-1704 | BE | Week 4 |
| 사이즈 추천 API + 브랜드 사이즈 편차 학습 | US-1704 | BE | Week 4 |

### Sprint 21 (5주차-6주차): AI 추천 완성 + 어드민 기반

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| 인기 검색어 + 추천 검색어 구현 | US-1705 | BE | Week 5 |
| AI 추천 A/B 테스트 프레임워크 | US-1701 | BE | Week 5 |
| AI 추천 전체 통합 테스트 | US-1701~1705 | QA | Week 5 |
| admin-service 프로젝트 셋업 | US-1801 | BE | Week 5 |
| 관리자 대시보드 API 구현 (통계 집계) | US-1801 | BE | Week 5-6 |
| 상품 관리 API (승인/반려/삭제/일괄 처리) | US-1802 | BE | Week 6 |

### Sprint 22 (7주차-8주차): 어드민 완성

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| 회원 관리 API (정지/탈퇴) | US-1803 | BE | Week 7 |
| 주문 관리 API (강제 취소/환불) | US-1804 | BE | Week 7 |
| 정산 관리 API (일괄 지급) | US-1805 | BE | Week 7-8 |
| 배너/기획전 관리 API | US-1806 | BE | Week 8 |
| 쿠폰 관리 API | US-1807 | BE | Week 8 |
| 어드민 전체 통합 테스트 | US-1801~1807 | QA | Week 8 |

### Sprint 23 (9주차-10주차): 이벤트 아키텍처 고도화

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| Transactional Outbox 패턴 구현 | US-1901 | BE | Week 9 |
| Kafka 이벤트 토픽 재정의 + Producer 리팩토링 | US-1901 | BE | Week 9 |
| Event Sourcing (주문 이벤트 스토어) | US-1902 | BE | Week 9-10 |
| CQRS 읽기 모델 (MongoDB) 구현 | US-1903 | BE | Week 10 |
| DLQ + 재처리 메커니즘 구현 | US-1904 | BE | Week 10 |
| Schema Registry (Avro) 도입 | US-1905 | BE | Week 10 |
| 이벤트 아키텍처 통합 테스트 | US-1901~1905 | QA | Week 10 |

### Sprint 24 (11주차-12주차): 성능 + 보안

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| Redis 캐싱 전략 적용 (서비스 전체) | US-2001 | BE | Week 11 |
| N+1 쿼리 최적화 (주요 API) | US-2002 | BE | Week 11 |
| Connection Pool 튜닝 | US-2003 | BE | Week 11 |
| 대용량 데이터 파티셔닝 | US-2004 | BE | Week 11-12 |
| Spring Security + JWT 강화 | US-2101 | BE | Week 12 |
| OAuth2 소셜 로그인 구현 | US-2102 | BE | Week 12 |
| API Rate Limiting 구현 | US-2103 | BE | Week 12 |
| 결제 데이터 암호화 | US-2104 | BE | Week 12 |
| 성능 + 보안 통합 테스트 | US-2001~2104 | QA | Week 12 |

### Sprint 25 (13주차-14주차): 인프라 고도화

| 태스크 | User Story | 담당 | 일정 |
|--------|-----------|------|------|
| EKS 클러스터 구축 + 서비스 배포 | US-2201 | DevOps | Week 13 |
| Helm Chart 작성 (공통 템플릿 + 서비스별 values) | US-2202 | DevOps | Week 13 |
| ArgoCD 설치 + GitOps 레포 구성 | US-2203 | DevOps | Week 13-14 |
| Istio 설치 + mTLS + 트래픽 정책 | US-2204 | DevOps | Week 14 |
| CI/CD 파이프라인 고도화 (GitHub Actions + ArgoCD) | US-2205 | DevOps | Week 14 |
| 전체 E2E 테스트 + 부하 테스트 (k6) | All | QA | Week 14 |
| Phase 5 최종 검증 + 문서화 | All | ALL | Week 14 |

---

## 10. 의존성 맵

```
Sprint 19-20: AI 추천 서비스
    └── Phase 4 ai-service 기반
    └── Phase 2 review-service (사이즈 리뷰 데이터)
    └── Phase 1 product-service, member-service

Sprint 21-22: 어드민 서비스
    └── Phase 1~4 전체 서비스 (통계 집계 대상)
    └── Phase 3 promotion-service (쿠폰)
    └── Phase 3 display-service (배너/기획전)
    └── Phase 4 settlement-service (정산)

Sprint 23: 이벤트 아키텍처
    └── Phase 1~4 전체 서비스 (이벤트 발행/소비 리팩토링)
    └── Sprint 19-20 recommendation-service (이벤트 소비)

Sprint 24: 성능 + 보안
    └── Sprint 19-23 전체 서비스
    └── Phase 1 member-service (OAuth2, JWT)
    └── Phase 1 payment-service (결제 암호화)

Sprint 25: 인프라
    └── Sprint 19-24 전체 서비스 (Kubernetes 배포 대상)
    └── Sprint 23 Kafka + Schema Registry (인프라 연동)
```

---

## 11. 리스크 및 대응 전략

| 리스크 | 영향 | 확률 | 대응 전략 |
|--------|------|------|----------|
| AI 추천 Cold Start | 신규 회원 UX 저하 | 높음 | 인기 상품 + 카테고리 기반 Fallback 추천 |
| CQRS Eventual Consistency | 읽기-쓰기 불일치 UX 혼란 | 중간 | 쓰기 직후 3초간 원본 DB 조회, UI에 "동기화 중" 표시 |
| Kafka 이벤트 유실 | 데이터 불일치 | 낮음 | Transactional Outbox + DLQ + 재처리 |
| Schema Registry 장애 | 이벤트 직렬화 실패 | 낮음 | 스키마 로컬 캐싱 (30분 TTL) |
| EKS 마이그레이션 | 서비스 다운타임 | 중간 | Blue-Green 배포, 단계적 마이그레이션 |
| Istio 리소스 오버헤드 | 서비스 성능 저하 (10~15%) | 중간 | Sidecar 리소스 제한, 불필요 서비스 Sidecar 제외 |
| 소셜 로그인 Provider 장애 | 로그인 불가 | 낮음 | 이메일/비밀번호 Fallback, Provider별 헬스체크 |
| 대용량 데이터 파티셔닝 마이그레이션 | 데이터 유실, 다운타임 | 중간 | 무중단 마이그레이션 (pt-online-schema-change) |

---

## 12. Phase 5 완료 후 플랫폼 전체 아키텍처

```
                              ┌─────────────────┐
                              │   CloudFront    │
                              └────────┬────────┘
                                       │
                              ┌────────▼────────┐
                              │   Istio Ingress │
                              │    Gateway      │
                              └────────┬────────┘
                                       │
                              ┌────────▼────────┐
                              │   API Gateway   │
                              │ (Spring Security│
                              │  OAuth2 + JWT   │
                              │  Rate Limiting) │
                              └────────┬────────┘
                                       │
     ┌──────────┬──────────┬───────────┼───────────┬──────────┬──────────┐
     │          │          │           │           │          │          │
┌────▼───┐ ┌───▼───┐ ┌────▼────┐ ┌────▼────┐ ┌────▼───┐ ┌───▼────┐ ┌──▼───┐
│product │ │order  │ │payment  │ │shipping │ │member  │ │search  │ │admin │
│service │ │service│ │service  │ │service  │ │service │ │service │ │svc   │
└───┬────┘ └──┬────┘ └────┬────┘ └────┬────┘ └───┬────┘ └───┬────┘ └──┬───┘
    │         │           │           │          │          │         │
    │    ┌────▼────┐      │      ┌────▼────┐     │     ┌────▼───┐    │
    │    │  CQRS   │      │      │inventory│     │     │  ES/   │    │
    │    │(MongoDB)│      │      │service  │     │     │OpenSrch│    │
    │    └─────────┘      │      └─────────┘     │     └────────┘    │
    │                     │                      │                   │
┌───▼────┐ ┌──────────┐ ┌▼──────────┐ ┌─────────▼───┐ ┌─────────┐  │
│display │ │promotion │ │settlement │ │notification │ │content  │  │
│service │ │service   │ │service    │ │service      │ │service  │  │
└────────┘ └──────────┘ └───────────┘ └─────────────┘ └─────────┘  │
                                                                    │
┌──────────────┐  ┌────────────┐  ┌────────────┐  ┌───────────────┐│
│recommendation│  │  cs-svc    │  │review-svc  │  │ seller-svc    ││
│service (AI)  │  │            │  │            │  │               ││
└──────┬───────┘  └────────────┘  └────────────┘  └───────────────┘│
       │                                                            │
       └────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
              ┌─────▼─────┐  ┌─────▼─────┐  ┌──────▼──────┐
              │   Kafka   │  │   Redis   │  │   MySQL     │
              │ + Schema  │  │ (캐시/세션 │  │ (파티셔닝)  │
              │ Registry  │  │  /Rate)   │  │             │
              └───────────┘  └───────────┘  └─────────────┘

              ┌─────────┐  ┌─────────┐  ┌──────────┐
              │ ArgoCD  │  │  Istio  │  │  Jaeger  │
              │ (GitOps)│  │ (Mesh)  │  │ (Trace)  │
              └─────────┘  └─────────┘  └──────────┘
```

---

## 13. Phase 5 성공 기준 체크리스트

- [ ] AI 추천 CTR 15% 이상 달성
- [ ] AI 사이즈 추천 정확도 85% 이상 달성
- [ ] 어드민 대시보드 로딩 2초 이내
- [ ] 전체 API P95 레이턴시 100ms 이내
- [ ] Redis 캐시 히트율 85% 이상
- [ ] Kafka 이벤트 처리 지연 P99 500ms 이내
- [ ] DLQ 재처리 성공률 95% 이상
- [ ] 소셜 로그인 비율 60% 이상
- [ ] OWASP Top 10 보안 취약점 0건
- [ ] 서비스 가용성 99.9% SLA 달성
- [ ] 코드 커밋 → 프로덕션 배포 15분 이내
- [ ] k6 부하 테스트 통과 (동시 사용자 1,000명 기준)

---

> Phase 5 완료 시 Closet 플랫폼은 AI 개인화, 어드민 운영, 이벤트 기반 아키텍처, 성능/보안/인프라 측면에서 프로덕션 레디 수준의 성숙도를 달성한다.
