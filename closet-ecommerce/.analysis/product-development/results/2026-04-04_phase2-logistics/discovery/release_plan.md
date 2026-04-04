# Phase 2 릴리즈 계획

> 확정일: 2026-04-04
> 범위: Sprint 5~8 (Phase 2 본편) + Sprint 9~11 (Phase 2.5 WMS)
> 기준: pm_decisions.md 최종 확정본

---

## 전체 타임라인

```
Sprint 5 (2주)  ─── 재고 기반 + 검색 기반 + 인프라 선행
Sprint 6 (2주)  ─── 배송/반품 + 검색 고도화
Sprint 7 (2주)  ─── 리뷰 + 교환 + 검색 완성
Sprint 8 (2주)  ─── 통합 테스트 + Canary 배포 + Phase 2 GA
Sprint 9 (2주)  ─── WMS 입고/보관
Sprint 10 (2주) ─── WMS 피킹/출고
Sprint 11 (2주) ─── WMS 재고실사/반품입고 + E2E
```

---

## Sprint 5: 재고 기반 + 검색 기반 + 인프라 선행

### 목표
Phase 2의 기반 인프라(Outbox 패턴, Kafka 토픽, Feature Flag)를 구축하고, inventory-service와 search-service의 핵심 기능을 구현합니다.

### 기간
2026-04-07 ~ 2026-04-18 (2주)

### 포함 US/티켓

| 티켓 | US | 내용 | 담당 |
|------|-----|------|------|
| CLO-501 | 인프라 선행 | Outbox 패턴 도입, processed_event 테이블, Kafka 토픽 17개 생성 | BE |
| CLO-502 | 인프라 선행 | closet-product Kafka 이벤트 발행 추가 (PD-43) | BE |
| CLO-503 | 인프라 선행 | 포트 재배정 (PD-05), docker-compose 업데이트, settings.gradle.kts 수정 | DevOps |
| CLO-504 | 인프라 선행 | JWT role claim 추가 (BUYER/SELLER/ADMIN), RoleAuthorizationFilter | BE |
| CLO-505 | US-601 | SKU별 재고 관리 (3단 구조: total/available/reserved) | BE |
| CLO-506 | US-601 | 재고 차감 RESERVE/DEDUCT/RELEASE 3단계 + Kafka Consumer | BE |
| CLO-507 | US-602 | Redis 분산 락(Redisson) + JPA @Version 낙관적 락 | BE |
| CLO-508 | US-601 | 재고 입고(INBOUND) API (PD-42) | BE |
| CLO-509 | US-701 | ES 인덱스 생성 (closet-products) + nori 분석기 설정 | BE |
| CLO-510 | US-701 | Kafka CDC 기반 실시간 인덱싱 Consumer | BE |
| CLO-511 | US-701 | 벌크 인덱싱 API + Phase 1 데이터 마이그레이션 | BE |
| CLO-512 | - | 재고 서비스 FE 관리 화면 (판매자 재고 조회/입고 등록) | FE |
| CLO-513 | - | Feature Flag 기반 점진 활성화 설정 | BE |
| CLO-514 | - | Sprint 5 QA 시나리오 작성 (재고 15건 + 인덱싱 5건) | QA |

### Feature Flag

| Flag | 설명 | 초기값 |
|------|------|--------|
| `INVENTORY_KAFKA_ENABLED` | 재고 Kafka 이벤트 처리 활성화 | OFF |
| `SEARCH_INDEXING_ENABLED` | 검색 인덱싱 활성화 | OFF |
| `OUTBOX_POLLING_ENABLED` | Outbox 폴러 활성화 | OFF |
| `ROLE_AUTHORIZATION_ENABLED` | RBAC 인가 필터 활성화 | OFF |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 재고 서비스 핵심 (CLO-505~508), Kafka 인프라 (CLO-501~502,504), 검색 인덱싱 (CLO-509~511,513) | 10d |
| FE | 재고 관리 화면 (CLO-512) | 5d |
| DevOps | docker-compose, 포트, Kafka 토픽, ES 클러스터 설정 (CLO-503) | 3d |
| QA | 시나리오 작성, 재고 동시성 테스트 설계 (CLO-514) | 3d |

---

## Sprint 6: 배송/반품 + 검색 고도화

### 목표
shipping-service의 핵심 기능(송장 등록, 배송 추적, 자동 구매확정, 반품)과 search-service의 검색/필터를 구현합니다.

### 기간
2026-04-21 ~ 2026-05-02 (2주)

### 포함 US/티켓

| 티켓 | US | 내용 | 담당 |
|------|-----|------|------|
| CLO-601 | US-501 | 송장 등록 (자동 채번 + 수동 입력 병행) + CarrierAdapter 패턴 | BE |
| CLO-602 | US-502 | 택배사 API 연동 배송 추적 + Redis 캐싱 (5분 TTL) | BE |
| CLO-603 | US-502 | orderId 기반 배송 추적 API (PD-44) | BE |
| CLO-604 | US-503 | 자동 구매확정 배치 (매일 00:00 + 12:00, 168시간 기준) | BE |
| CLO-605 | US-503 | D-1 사전 알림 배치 (PD-45) | BE |
| CLO-606 | US-504 | 반품 신청 + 사유별 배송비 매핑 (shipping_fee_policy 테이블) | BE |
| CLO-607 | US-504 | 반품 수거/검수/승인 상태 흐름 + 3영업일 자동 승인 | BE |
| CLO-608 | US-504 | 반품 환불 연동 (payment-service 부분 환불 API 추가) | BE |
| CLO-609 | US-702 | 한글 형태소 분석(nori) + 키워드 검색 + 유의어 사전 | BE |
| CLO-610 | US-703 | 필터 (카테고리/브랜드/가격/색상/사이즈) + facet count | BE |
| CLO-611 | - | 배송 추적 FE 화면 (구매자 주문 상세 내 배송 추적) | FE |
| CLO-612 | - | 반품 신청 FE 화면 (사유 선택, 배송비 안내) | FE |
| CLO-613 | - | 검색 결과 페이지 FE (필터 사이드바, 정렬, 페이지네이션) | FE |
| CLO-614 | - | Sprint 6 QA 시나리오 실행 (배송 20건 + 검색 10건) | QA |

### Feature Flag

| Flag | 설명 | 초기값 |
|------|------|--------|
| `SHIPPING_SERVICE_ENABLED` | 배송 서비스 전체 활성화 | OFF |
| `AUTO_CONFIRM_BATCH_ENABLED` | 자동 구매확정 배치 활성화 | OFF |
| `RETURN_REQUEST_ENABLED` | 반품 신청 기능 활성화 | OFF |
| `SEARCH_FILTER_ENABLED` | 검색 필터 기능 활성화 | OFF |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 배송 핵심 (CLO-601~608), 검색 고도화 (CLO-609~610) | 10d |
| FE | 배송 추적/반품 화면 (CLO-611~612), 검색 결과 (CLO-613) | 8d |
| DevOps | 배치 스케줄러 인프라, 모니터링 대시보드 | 2d |
| QA | 배송/반품 E2E 테스트, 검색 정확도 검증 (CLO-614) | 4d |

---

## Sprint 7: 리뷰 + 교환 + 검색 완성

### 목표
review-service 전체 기능과 교환 기능을 구현하고, 검색 서비스의 자동완성/인기 검색어를 완성합니다.

### 기간
2026-05-05 ~ 2026-05-16 (2주)

### 포함 US/티켓

| 티켓 | US | 내용 | 담당 |
|------|-----|------|------|
| CLO-701 | US-801 | 텍스트 + 포토 리뷰 작성 (Thumbnailator 리사이즈) | BE |
| CLO-702 | US-802 | 사이즈 후기 (키/몸무게/핏 평가) + 사이즈 분포 API | BE |
| CLO-703 | US-803 | 리뷰 포인트 적립 (100P/300P/50P) + 일일 한도 5,000P | BE |
| CLO-704 | US-804 | 리뷰 집계 (평균 별점, 사이즈 분포) review_summary 테이블 | BE |
| CLO-705 | PD-32 | 리뷰 수정 (텍스트+이미지, 별점 불변, 최대 3회, 이력 보존) | BE |
| CLO-706 | PD-35 | 관리자 리뷰 관리 (HIDDEN 상태 + 관리자 API) | BE |
| CLO-707 | US-505 | 교환 신청 (동일 가격 옵션만, 재고 선점) | BE |
| CLO-708 | US-603 | 안전재고 알림 (카테고리별 기본값 + 24시간 중복 방지) | BE |
| CLO-709 | US-604 | 재입고 알림 (90일 만료, 최대 50건 제한) | BE |
| CLO-710 | US-704 | 자동완성 (edge_ngram, P99 50ms) | BE |
| CLO-711 | US-705 | 인기 검색어 (Redis Sorted Set sliding window) + 금칙어 | BE |
| CLO-712 | PD-48 | 최근 검색어 (Redis List) | BE |
| CLO-713 | PD-23 | 인기순 복합 점수 정렬 고도화 | BE |
| CLO-714 | - | 리뷰 작성/목록 FE 화면 (포토 리뷰, 사이즈 후기 UI) | FE |
| CLO-715 | - | 교환 신청 FE 화면 | FE |
| CLO-716 | - | 검색 자동완성/인기 검색어/최근 검색어 FE | FE |
| CLO-717 | - | Sprint 7 QA 시나리오 실행 (리뷰 17건 + 교환 5건 + 검색 6건) | QA |

### Feature Flag

| Flag | 설명 | 초기값 |
|------|------|--------|
| `REVIEW_SERVICE_ENABLED` | 리뷰 서비스 전체 활성화 | OFF |
| `REVIEW_POINT_ENABLED` | 리뷰 포인트 적립 활성화 | OFF |
| `EXCHANGE_REQUEST_ENABLED` | 교환 신청 기능 활성화 | OFF |
| `AUTOCOMPLETE_ENABLED` | 자동완성 기능 활성화 | OFF |
| `POPULAR_KEYWORDS_ENABLED` | 인기 검색어 기능 활성화 | OFF |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 리뷰 전체 (CLO-701~706), 교환 (CLO-707), 재고 알림 (CLO-708~709), 검색 완성 (CLO-710~713) | 10d |
| FE | 리뷰/교환/검색 UI (CLO-714~716) | 8d |
| DevOps | 리뷰 이미지 스토리지 설정, Redis 모니터링 | 2d |
| QA | 리뷰/교환/검색 E2E 테스트 (CLO-717) | 4d |

---

## Sprint 8: 통합 테스트 + Canary 배포 + Phase 2 GA

### 목표
전 도메인 크로스 테스트, 성능 테스트, Canary 배포를 통한 Phase 2 GA(General Availability) 릴리즈입니다.

### 기간
2026-05-19 ~ 2026-05-30 (2주)

### 포함 US/티켓

| 티켓 | US | 내용 | 담당 |
|------|-----|------|------|
| CLO-801 | - | 크로스 도메인 E2E 테스트 (주문->재고->배송->반품->환불 풀 플로우) | QA |
| CLO-802 | - | 크로스 도메인 E2E 테스트 (주문->배송->구매확정->리뷰->포인트 풀 플로우) | QA |
| CLO-803 | - | 동시성 테스트 (100스레드 재고 차감, P99 200ms 검증) | QA |
| CLO-804 | - | 검색 성능 테스트 (인덱싱 P95 3초, 자동완성 P99 50ms) | QA |
| CLO-805 | - | 데이터 마이그레이션 실행 (PD-49: inventory 초기화, ES 벌크 인덱싱) | BE/DevOps |
| CLO-806 | - | Feature Flag 점진 활성화 (Canary 5% -> 25% -> 50% -> 100%) | DevOps |
| CLO-807 | - | 버그 수정 버퍼 (Sprint 5~7 발견 이슈 해소) | BE/FE |
| CLO-808 | - | BFF 통합 API 정리 (신규 서비스 라우팅 추가) | BE |
| CLO-809 | - | 모니터링 대시보드 최종 정비 (Grafana 패널 추가) | DevOps |
| CLO-810 | - | Phase 2 릴리즈 노트 작성 | PM |
| CLO-811 | - | 판매자 가이드 문서 (배송/재고/반품 운영 가이드) | PM/Marketing |

### Feature Flag 활성화 스케줄

| 일차 | 비율 | 활성화 대상 | 모니터링 기준 |
|------|------|-----------|-------------|
| Day 1~2 | 5% | 내부 테스트 계정 | 에러율 < 0.1%, P99 < 500ms |
| Day 3~4 | 25% | 얼리어답터 셀러 그룹 | 에러율 < 0.5%, 주문 전환율 유지 |
| Day 5~7 | 50% | 전체 셀러 50% | 에러율 < 1%, CS 인입 증가율 < 10% |
| Day 8~10 | 100% | 전체 오픈 | 안정화 확인 후 Feature Flag 제거 |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 마이그레이션 (CLO-805), 버그 수정 (CLO-807), BFF 정리 (CLO-808) | 6d |
| FE | 버그 수정 (CLO-807), UI 폴리싱 | 4d |
| DevOps | Canary 배포 (CLO-806), 모니터링 (CLO-809), 마이그레이션 지원 (CLO-805) | 5d |
| QA | 크로스 도메인 E2E (CLO-801~802), 성능 테스트 (CLO-803~804) | 8d |
| PM/Marketing | 릴리즈 노트 (CLO-810), 판매자 가이드 (CLO-811) | 4d |

---

## Sprint 9 (Phase 2.5): WMS 입고/보관

### 목표
WMS의 입고 관리(ASN, 검수, 입고 확정)와 보관/로케이션 관리를 구현합니다.

### 기간
2026-06-02 ~ 2026-06-13 (2주)

### 포함 US/티켓

| 티켓 | 내용 | 담당 |
|------|------|------|
| CLO-901 | 입고 예정(ASN) 등록 API + 상태 흐름 (EXPECTED -> ARRIVED -> INSPECTING -> CONFIRMED) | BE |
| CLO-902 | 입고 검수 (수량 확인, 불량 검수, 양품/불량/부분합격 판정) | BE |
| CLO-903 | 입고 확정 -> inventory 재고 반영 (wms.inbound.confirmed Kafka 이벤트) | BE |
| CLO-904 | 로케이션 체계 (W01-Z01-A01-R01-C01) + 유형 관리 (GENERAL/COLD/HAZARDOUS/VALUABLE) | BE |
| CLO-905 | 적치(Putaway) 자동 위치 추천 + 작업 상태 관리 | BE |
| CLO-906 | 로케이션별 재고 관리 (location_inventory) | BE |
| CLO-907 | WMS 입고/보관 관리 FE 화면 | FE |
| CLO-908 | Sprint 9 QA 시나리오 (입고 10건 + 보관 5건) | QA |

### Feature Flag

| Flag | 설명 | 초기값 |
|------|------|--------|
| `WMS_INBOUND_ENABLED` | WMS 입고 기능 활성화 | OFF |
| `WMS_LOCATION_ENABLED` | WMS 로케이션 관리 활성화 | OFF |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 입고 (CLO-901~903), 보관 (CLO-904~906) | 10d |
| FE | WMS 관리 화면 (CLO-907) | 6d |
| DevOps | WMS 모듈 빌드 설정, Kafka 토픽 추가 | 2d |
| QA | 입고/보관 테스트 (CLO-908) | 3d |

---

## Sprint 10 (Phase 2.5): WMS 피킹/출고

### 목표
WMS의 피킹(웨이브 피킹, FIFO 전략)과 출고(포장, 송장 발행, 출고 완료) 프로세스를 구현합니다.

### 기간
2026-06-16 ~ 2026-06-27 (2주)

### 포함 US/티켓

| 티켓 | 내용 | 담당 |
|------|------|------|
| CLO-1001 | 피킹 웨이브 생성 (시간대/구역별 다건 주문 묶음) | BE |
| CLO-1002 | 피킹 작업 지시 (SKU + 로케이션 + 수량) + 피킹 전략 enum (FIFO/FEFO/CLOSEST_LOCATION) | BE |
| CLO-1003 | 부분 피킹(SHORT) 처리 + 재고 실사 트리거 | BE |
| CLO-1004 | 출고 지시 자동 생성 (order.paid Kafka 이벤트 수신) | BE |
| CLO-1005 | 포장(Packing) + 출고 검수 (바코드 검증) | BE |
| CLO-1006 | 송장 발행 (택배사 API 연동) + 출고 완료 (wms.outbound.completed 이벤트) | BE |
| CLO-1007 | WMS 피킹/출고 FE 화면 (작업 리스트, 바코드 스캔 UI) | FE |
| CLO-1008 | Sprint 10 QA 시나리오 (피킹 8건 + 출고 7건) | QA |

### Feature Flag

| Flag | 설명 | 초기값 |
|------|------|--------|
| `WMS_PICKING_ENABLED` | WMS 피킹 기능 활성화 | OFF |
| `WMS_OUTBOUND_ENABLED` | WMS 출고 기능 활성화 | OFF |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 피킹 (CLO-1001~1003), 출고 (CLO-1004~1006) | 10d |
| FE | WMS 피킹/출고 화면 (CLO-1007) | 6d |
| DevOps | 바코드 스캔 인프라, 피킹 경로 최적화 모니터링 | 2d |
| QA | 피킹/출고 E2E 테스트 (CLO-1008) | 4d |

---

## Sprint 11 (Phase 2.5): 재고 실사/반품 입고 + E2E

### 목표
WMS의 재고 실사, 반품 입고 처리를 구현하고, WMS 전체 E2E 통합 테스트 및 Canary 배포를 수행합니다.

### 기간
2026-06-30 ~ 2026-07-11 (2주)

### 포함 US/티켓

| 티켓 | 내용 | 담당 |
|------|------|------|
| CLO-1101 | 재고 실사 (FULL/CYCLE/SPOT) 요청 + 실사 진행 | BE |
| CLO-1102 | 차이 분석 + 차이 조정 (wms.stocktake.adjusted 이벤트 -> inventory 동기화) | BE |
| CLO-1103 | 반품 입고 검수 + 재판매 판정 (양품 재입고/불량 처리) | BE |
| CLO-1104 | 교환 출고 자동 연결 (outbound_order 자동 생성) | BE |
| CLO-1105 | WMS 대시보드 (입고/출고/재고 현황 종합 뷰) | BE/FE |
| CLO-1106 | WMS 전체 E2E 통합 테스트 (입고->보관->피킹->출고->배송 풀 플로우) | QA |
| CLO-1107 | WMS Canary 배포 (5% -> 25% -> 50% -> 100%) | DevOps |
| CLO-1108 | 버그 수정 버퍼 (Sprint 9~10 발견 이슈) | BE/FE |
| CLO-1109 | WMS 운영 가이드 문서 | PM |
| CLO-1110 | Phase 2.5 릴리즈 노트 | PM |

### Feature Flag

| Flag | 설명 | 초기값 |
|------|------|--------|
| `WMS_STOCKTAKE_ENABLED` | WMS 재고 실사 활성화 | OFF |
| `WMS_RETURN_RECEIVING_ENABLED` | WMS 반품 입고 활성화 | OFF |

### 직군별 작업 배분

| 직군 | 작업 | 공수 |
|------|------|------|
| BE | 재고 실사 (CLO-1101~1102), 반품 입고 (CLO-1103~1104), 대시보드 (CLO-1105), 버그 수정 (CLO-1108) | 8d |
| FE | WMS 대시보드 (CLO-1105), 버그 수정 (CLO-1108), UI 폴리싱 | 5d |
| DevOps | Canary 배포 (CLO-1107), 모니터링 최종 정비 | 3d |
| QA | 전체 E2E 통합 (CLO-1106), 성능 검증 | 6d |
| PM | WMS 가이드 (CLO-1109), 릴리즈 노트 (CLO-1110) | 3d |

---

## Canary 배포 전략

### Phase 2 본편 (Sprint 8)

```
                  Feature Flag
                      │
                      ▼
┌─────────────────────────────────────────┐
│  Day 1-2: 5% (내부 테스트 계정)          │
│  ├─ 모니터링: 에러율, P99 레이턴시        │
│  ├─ 통과 기준: 에러율 < 0.1%             │
│  └─ 실패 시: 즉시 Flag OFF → 롤백        │
│                                         │
│  Day 3-4: 25% (얼리어답터 셀러)          │
│  ├─ 모니터링: + 주문 전환율, CS 인입      │
│  ├─ 통과 기준: 에러율 < 0.5%             │
│  └─ 실패 시: Flag OFF → 5% 복귀          │
│                                         │
│  Day 5-7: 50% (전체 셀러 50%)            │
│  ├─ 모니터링: + 매출 영향, NPS            │
│  ├─ 통과 기준: 에러율 < 1%               │
│  └─ 실패 시: Flag OFF → 25% 복귀         │
│                                         │
│  Day 8-10: 100% (전체 오픈)              │
│  ├─ 안정화 확인 후 Feature Flag 코드 제거 │
│  └─ Phase 2 GA 선언                     │
└─────────────────────────────────────────┘
```

### Phase 2.5 WMS (Sprint 11)

동일한 Canary 전략을 적용하되, WMS는 판매자/물류센터 관리자 대상이므로:
- Day 1~2: 내부 테스트 (자체 창고 시뮬레이션)
- Day 3~5: 파일럿 셀러 2~3개사
- Day 6~8: 전체 셀러 50%
- Day 9~10: 100% GA

---

## 롤백 시나리오

### 레벨 1: Feature Flag 롤백 (무중단)

**트리거**: 에러율 > 1% 또는 P99 > 1초
**방법**: Feature Flag OFF
**영향**: 신규 기능만 비활성화. 기존 Phase 1 플로우로 자동 복귀합니다.
**소요 시간**: 즉시 (1분 이내)

```
1. Feature Flag DB 값 변경 (SimpleRuntimeConfig 패턴)
2. 서비스가 다음 요청부터 즉시 반영
3. 진행 중인 트랜잭션은 완료 후 Flag 적용
```

### 레벨 2: 서비스 롤백 (무중단)

**트리거**: Feature Flag 롤백으로 해소 불가한 데이터 정합성 이슈
**방법**: 이전 버전 컨테이너 배포
**영향**: 해당 서비스 전체 이전 버전으로 복귀합니다.
**소요 시간**: 5분 이내

```
1. 이전 버전 Docker 이미지로 롤백 배포
2. Kafka Consumer 오프셋 리셋 (필요 시)
3. 데이터 정합성 확인 스크립트 실행
```

### 레벨 3: 데이터 롤백 (점검 필요)

**트리거**: DB 스키마 변경으로 인한 호환성 이슈
**방법**: Flyway 롤백 마이그레이션 실행
**영향**: 점검 필요. 사전에 롤백 마이그레이션 스크립트를 준비합니다.
**소요 시간**: 30분~1시간

```
1. 서비스 일시 중지 (maintenance mode)
2. 롤백 마이그레이션 실행
3. 이전 버전 서비스 배포
4. 데이터 정합성 검증
5. 서비스 재개
```

### 도메인별 롤백 특이사항

| 도메인 | 특이사항 |
|--------|---------|
| **재고** | RESERVE 상태의 미확정 재고를 RELEASE 처리해야 합니다. `inventory_history` 기반 복구 스크립트 준비합니다. |
| **배송** | 이미 발송된 배송 건은 롤백 불가. shipping 테이블 데이터는 보존하고, 기능만 비활성화합니다. |
| **검색** | ES 인덱스 삭제 -> 기존 DB 쿼리 검색으로 폴백. `SEARCH_INDEXING_ENABLED` Flag OFF로 즉시 전환 가능합니다. |
| **리뷰** | 작성된 리뷰 데이터는 보존. 포인트 적립 건은 `point.earn` 이벤트의 멱등성으로 중복 방지됩니다. |
| **WMS** | 진행 중인 입고/출고 건은 수동 완료 처리. 재고 동기화 이벤트 재발행이 필요합니다. |

---

## 전 직군 일정 총괄

| 직군 | Sprint 5 | Sprint 6 | Sprint 7 | Sprint 8 | Sprint 9 | Sprint 10 | Sprint 11 |
|------|----------|----------|----------|----------|----------|-----------|-----------|
| **BE** | 재고+인프라+인덱싱 | 배송+반품+검색필터 | 리뷰+교환+검색완성 | 마이그레이션+버그 | WMS입고/보관 | WMS피킹/출고 | WMS실사/반품입고 |
| **FE** | 재고관리화면 | 배송추적+반품+검색 | 리뷰+교환+자동완성 | 버그수정+폴리싱 | WMS입고/보관UI | WMS피킹/출고UI | WMS대시보드 |
| **DevOps** | docker-compose+포트 | 배치인프라+모니터링 | 스토리지+Redis | Canary배포+모니터링 | WMS빌드설정 | 바코드인프라 | Canary배포 |
| **QA** | 시나리오작성 | 배송+검색테스트 | 리뷰+교환+검색 | 크로스도메인E2E | 입고/보관 | 피킹/출고 | 전체E2E |
| **PM** | - | - | - | 릴리즈노트+가이드 | - | - | 가이드+릴리즈노트 |
| **Marketing** | - | - | - | 판매자가이드 | - | - | - |
| **Growth** | - | - | - | 전환율모니터링 | - | - | WMS효율측정 |

### 주요 마일스톤

| 일자 | 마일스톤 |
|------|---------|
| 2026-04-18 | Sprint 5 완료: 재고/검색 기반 구축 |
| 2026-05-02 | Sprint 6 완료: 배송/반품/검색 필터 |
| 2026-05-16 | Sprint 7 완료: 리뷰/교환/검색 완성 |
| 2026-05-30 | **Phase 2 GA**: Sprint 8 완료, Canary 100% 배포 |
| 2026-06-13 | Sprint 9 완료: WMS 입고/보관 |
| 2026-06-27 | Sprint 10 완료: WMS 피킹/출고 |
| 2026-07-11 | **Phase 2.5 GA**: Sprint 11 완료, WMS Canary 100% 배포 |
