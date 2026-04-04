# Closet 의류 이커머스 — 전체 제품 로드맵

> 최종 업데이트: 2026-04-04 (Phase 2 BE 전체 구현 완료)
> 벤치마킹: 무신사, 올리브영 물류시스템 (SpringCamp 2025)

## 전체 타임라인

```mermaid
gantt
    title Closet 전체 제품 로드맵
    dateFormat YYYY-MM-DD
    axisFormat %m월

    section Phase 1: 핵심 커머스 (Done)
    상품 카탈로그           :done, ph1-1, 2026-03-22, 14d
    회원/인증               :done, ph1-2, 2026-03-22, 14d
    주문/장바구니           :done, ph1-3, after ph1-1, 14d
    결제 (Toss Payments)    :done, ph1-4, after ph1-3, 7d
    BFF 레이어              :done, ph1-5, after ph1-4, 7d
    API Gateway             :done, ph1-6, after ph1-5, 5d

    section Phase 2: 성장 (Done)
    Mock 외부 API 서버        :done, ph2-0, 2026-04-04, 1d
    BFF Facade 리팩토링       :done, ph2-0b, 2026-04-04, 1d
    인프라 공통 (Outbox/멱등성/RBAC) :done, ph2-infra, 2026-04-04, 1d
    재고 서비스              :done, ph2-1, 2026-04-04, 1d
    배송/물류               :done, ph2-2, 2026-04-04, 1d
    검색 (Elasticsearch)    :done, ph2-3, 2026-04-04, 1d
    리뷰 시스템              :done, ph2-4, 2026-04-04, 1d
    반품/교환               :done, ph2-5, 2026-04-04, 1d
    BFF 통합                :done, ph2-6, 2026-04-04, 1d

    section Phase 3: 확장
    프로모션 (쿠폰/할인)    :ph3-1, after ph2-4, 14d
    전시 (배너/기획전)     :ph3-2, after ph2-4, 14d
    알림 (SMS/푸시)        :ph3-3, after ph3-1, 10d
    CS (문의/FAQ)           :ph3-4, after ph3-2, 10d
    콘텐츠 (매거진/OOTD)   :ph3-5, after ph3-3, 14d

    section Phase 4: 고도화
    정산 시스템              :ph4-1, after ph3-4, 14d
    셀러 어드민              :ph4-2, after ph4-1, 14d
    AI 추천                  :ph4-3, after ph4-1, 14d
    WMS (간소화)             :ph4-4, after ph4-2, 14d
```

---

## Phase 1: 핵심 커머스 기반 ✅ Done

| 서비스 | 핵심 기능 | 상태 |
|--------|----------|------|
| closet-product | 상품 CRUD, 카테고리, 브랜드, 옵션(사이즈/색상), SKU | ✅ |
| closet-member | 회원가입, JWT 인증, 배송지 관리 | ✅ |
| closet-order | 장바구니, 주문 생성/취소, 상태 머신 | ✅ |
| closet-payment | Toss Payments 연동, 결제 확인/취소 | ✅ |
| closet-bff | Facade 패턴, Feign Client, 병렬 호출 | ✅ |
| closet-gateway | API Gateway, 라우팅, JWT 필터 | ✅ |

---

## Phase 2: 성장 ✅ Done

> 30 BE 티켓 (CP-01~CP-30), Sprint 5~7, ~235 파일, ~14,320 LOC

| 서비스 | 핵심 기능 | 상태 | PR | 벤치마킹 |
|--------|----------|------|-----|---------|
| closet-common (인프라) | Outbox 패턴, 멱등성, RBAC, Feature Flag | ✅ | #73 | 올리브영 Outbox |
| closet-external-api | Mock PG 4개 + 택배사 4개 (MySQL 저장) | ✅ | #68 | - |
| BFF Facade 리팩토링 | Controller→Facade→Client 패턴 통일 | ✅ | #68 | - |
| closet-inventory | 3단 재고, Redisson 분산락, All-or-Nothing, Kafka Consumer | ✅ | #75 | 올리브영 Inventory API |
| closet-shipping | 송장 등록, 배송 추적, 택배사 어댑터, 반품/교환, 배송비 정책 | ✅ | #77 | 올리브영 WMS |
| closet-search | nori 검색, 필터/정렬, 인기/최근/금지 키워드, Feature Flag | ✅ | #76 | 무신사 검색 UX |
| closet-review | 별점, 포토 리뷰, 사이즈 후기, 이미지 리사이즈, 관리자 블라인드 | ✅ | #78 | 무신사 리뷰 시스템 |
| closet-bff (통합) | Shipping/Review/Search Facade + FeignClient | ✅ | #78 | - |

### Mock 외부 API 상세 (Done)

**PG사 4개:**
- Toss Payments (`/toss/v1/payments`) — confirm, cancel, 조회
- Kakao Pay (`/kakaopay/online/v1/payment`) — ready, approve, cancel, 조회
- Naver Pay (`/naverpay/payments`) — reserve, apply, cancel, history
- Danal (`/danal/payments`) — ready, approve, cancel, 조회

**택배사 4개:**
- CJ 대한통운 (`/carrier/cj/api/v1`) — 접수, 추적, advance
- 로젠 (`/carrier/logen/api/v1`) — 접수, 추적, advance
- 롯데글로벌 (`/carrier/lotte/api/v1`) — 접수, 추적, advance
- 우체국 (`/carrier/epost/api/v1`) — 접수, 추적, advance

---

## Phase 3: 확장

| 서비스 | 핵심 기능 | 벤치마킹 |
|--------|----------|---------|
| closet-promotion | 쿠폰 발급/사용, 할인, 타임세일, 적립금 | 무신사 쿠폰 시스템 |
| closet-display | 메인 페이지, 배너, 기획전, 랭킹 | 무신사 전시 관리 |
| closet-notification | SMS, 푸시 알림, 재입고 알림 | - |
| closet-cs | 1:1 문의, FAQ, 반품/교환 접수 | - |
| closet-content | 스타일 매거진, 코디 추천, OOTD 스냅 | 무신사 스냅 |

---

## Phase 4: 고도화

| 서비스 | 핵심 기능 | 벤치마킹 |
|--------|----------|---------|
| closet-settlement | 브랜드/셀러 정산, 수수료, 정산 주기 | - |
| closet-seller | 브랜드 입점, 셀러 어드민, 상품 등록 | 무신사 셀러 시스템 |
| AI 추천 | 코디/스타일링 추천, 개인화 | - |
| WMS (간소화) | 출고 지시, 피킹, 입고 관리 | 올리브영 GMS (간소화) |

---

## 기술 인프라 현황

| 인프라 | 상태 | 비고 |
|--------|------|------|
| MySQL 8.0 | ✅ | 각 서비스 독립 DB |
| Redis 7.0 | ✅ | 캐시, 세션 |
| Apache Kafka | ✅ | Outbox Poller, 13개 Consumer 구현 |
| Elasticsearch | ✅ | nori 분석기, 상품 검색/자동완성 구현 |
| Redisson | ✅ | 분산락 (재고 동시성) |
| Prometheus + Grafana | ✅ | docker-compose |
| TestContainers | ✅ | 통합 테스트 |
| Flyway | ✅ | 스키마 마이그레이션 |

## 벤치마킹 참조

- [올리브영 물류 시스템의 진화 | 테크블로그](https://oliveyoung.tech/2025-08-01/logistics-system/)
- [SpringCamp 2025 "올리브영 물류시스템 개선기"](https://www.youtube.com/watch?v=Tlh-8msdOM8)
- 무신사 — 카테고리 구조, 검색 UX, 리뷰 시스템, 스냅
