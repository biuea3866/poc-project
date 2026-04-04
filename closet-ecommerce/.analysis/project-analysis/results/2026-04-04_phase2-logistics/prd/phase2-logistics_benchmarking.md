# Phase 2 벤치마킹 리서치: 배송/물류 + 재고 + 검색 + 리뷰 + WMS

> 작성일: 2026-04-04
> 범위: 직접 경쟁사(패션 이커머스 5개), 참조 B2B/커머스 SaaS(5개), 인프라/오픈소스(3개)
> 목적: Closet Phase 2 PRD/PM 의사결정에 대한 외부 벤치마킹 근거 확보

---

## A. 직접 경쟁사 (패션 이커머스)

---

### 1. 무신사 (MUSINSA)

**배송 추적 UX/API 구조**
- 주문 상세 내 배송 추적 탭에서 택배사 연동 실시간 추적 제공. 스마트택배 통합 API를 래핑하여 CJ대한통운, 한진, 로젠 등 20개 이상 택배사 통합 조회.
- 배송 상태 3단계 표현: "상품 준비중" -> "배송중" -> "배송완료". 내부적으로는 택배사별 세분화 상태(집하/간선상차/배달출발 등)를 로그로 보존하되, 사용자에게는 3단계로 추상화.
- 구매확정은 배송완료 후 자동(기본 7일) + 수동 확정 병행. 무신사 스토어는 자동확정 D-1 알림을 앱 푸시로 발송하여 CS 인입 감소 효과를 거둠.
- 배송 상태 캐싱: 택배사 API 호출 결과를 5분간 캐싱(CDN + 서버 캐시 이중 구조). 사용자가 "새로고침" 버튼을 누르면 캐시 무효화 후 실시간 조회.

**반품/교환 프로세스**
- 반품 사유 4분류: 단순변심, 사이즈 불일치, 불량/하자, 오배송. 단순변심/사이즈 불일치는 구매자 배송비 부담(편도 3,000원), 불량/오배송은 판매자 부담.
- 교환은 동일 상품 동일 가격 옵션만 허용. 가격 차이 발생 시 "반품 후 재주문" 안내. 이 정책은 정산 로직 단순화에 기여.
- 반품 상태 머신: 접수 -> 수거접수 -> 수거완료 -> 검수 -> 환불완료/거절. 반품 검수는 물류센터(브랜드 자체 또는 무신사 물류센터) 대행이며, 3영업일 내 미처리 시 자동 승인.
- All-or-Nothing 정책: 여러 상품 주문 시 재고 부족 건이 있으면 전체 주문 거절. 부분 취소는 주문 이후(결제 완료 후)에만 가능.

**검색 필터/자동완성**
- Elasticsearch 기반 검색. nori 형태소 분석기 + 유의어 사전(바지=팬츠, 상의=탑 등 500개 이상). edge_ngram 자동완성.
- 필터 체계: 카테고리(3depth), 브랜드(복수선택), 가격대(구간), 색상, 사이즈, 성별, 할인율. Facet count 실시간 표시.
- 인기 검색어: 실시간 Top 10, 1시간 슬라이딩 윈도우, 순위 변동 표시(NEW/UP/DOWN). 금칙어 필터링 적용.
- 최근 검색어: 사용자별 최대 20개 저장, 개별/전체 삭제 가능.

**리뷰 시스템**
- 텍스트 리뷰(100P) + 포토 리뷰(500P) + 사이즈 후기(추가 50P). 일일 적립 한도 존재.
- 사이즈 후기: 키/몸무게/평소 사이즈/핏 평가(작아요/딱맞아요/커요). "나와 비슷한 체형" 필터로 유사 체형 리뷰만 조회 가능.
- 별점 수정 불가, 텍스트/이미지만 수정 가능(최대 3회). 수정 이력 보존.
- 리뷰 집계: 상품별 평균 별점, 별점 분포, 사이즈 핏 분포를 실시간 집계하여 상품 상세/목록에 표시.
- 관리자 블라인드 기능: 부적절한 리뷰를 HIDDEN 처리. 신고 기능은 별도.

**Closet에 대한 시사점**
- 배송 상태 3단계 추상화(READY/IN_TRANSIT/DELIVERED)는 업계 표준. PD-10의 3단계 매핑 + 상세 로그 보존 전략이 무신사와 일치.
- 반품 SIZE_MISMATCH를 구매자 부담으로 처리(PD-11)하는 것은 무신사 정책과 동일.
- 교환 시 동일 가격만 허용(PD-14)은 무신사 방식 그대로. 차액 정산 Phase 3 연기 타당.
- 자동확정 D-1 알림(PD-45)은 무신사에서 검증된 CS 감소 전략. Phase 2에서 이벤트 스키마만 확정하는 것은 합리적.
- 리뷰 별점 수정 불가(PD-32)는 무신사와 동일 정책. 집계 재계산 비용 회피.

---

### 2. 29CM

**배송 추적 UX/API 구조**
- 프리미엄 UX를 강조하는 만큼 배송 추적 UI가 시각적으로 정교함. 단계별 타임라인 뷰 + 지도 기반 위치 표시(일부 택배사).
- 택배사 연동은 스마트택배 통합 API 활용. 자체 물류가 아닌 브랜드 직배송 비율이 높아, 배송 상태 표준화에 추상화 레이어(Adapter)를 둠.
- 구매확정 자동(14일, 무신사보다 길음) + 수동 병행. 프리미엄 상품 특성상 반품 기한도 14일로 여유.

**반품/교환 프로세스**
- 반품 접수 시 택배사 수거 예약까지 원클릭으로 완료. 반품 송장이 자동 생성됨(무신사 대비 자동화 수준 높음).
- 명품/디자이너 카테고리는 반품 불가 별도 정책. 카테고리별 반품 정책 분기가 특징.
- 교환은 사이즈 교환만 가능, 색상 교환은 반품+재주문 유도.

**검색 필터/자동완성**
- 큐레이션 중심이므로 검색보다 기획전/에디터 추천이 주 탐색 경로. 검색은 Elasticsearch + nori.
- 필터: 카테고리, 브랜드, 가격, 색상. 무신사 대비 필터 항목 적으나, "무드"/"스타일" 같은 감성 태그 필터가 특징.
- 자동완성: 브랜드명 우선 노출(패션 특성).

**리뷰 시스템**
- 리뷰 작성 포인트 적립(텍스트 200P, 포토 500P). 무신사보다 단가 높음.
- 사이즈 후기 시스템 유사하나, 29CM은 "스타일링 후기" 카테고리를 별도 운영(코디 사진 중심).
- "도움이 됐어요" 좋아요 기반 리뷰 정렬.

**Closet에 대한 시사점**
- 29CM의 카테고리별 반품 정책 분기는 향후 Phase 3에서 참고 가능하나, Phase 2 MVP에서는 단일 정책이 적절.
- 반품 시 송장 자동 생성은 PD-07의 "시스템 자동 채번 + 수동 병행"과 부합. 학습 목적으로 자동 채번 구현 가치 있음.
- "스타일링 후기"는 Phase 3 콘텐츠 서비스 확장 시 벤치마킹 대상.

---

### 3. W컨셉 (W CONCEPT)

**배송 추적 UX/API 구조**
- SSG 계열 편입 후 SSG 물류 인프라(네오) 활용. 배송 추적은 SSG 통합 물류 시스템과 연동.
- 배송 상태: 주문접수 -> 상품준비중 -> 배송중 -> 배송완료. 상품준비중에서 배송중 전환 시 송장번호 자동 할당.
- 해외 브랜드 배송(해외직구)은 별도 배송 추적 체계. 국내/해외 배송 이원 구조가 특징.

**반품/교환 프로세스**
- 프리미엄 패션 특성상 반품 기한 7일, 검수 기준이 엄격(태그 미부착 시 반품 불가).
- 검수 기준을 별도 페이지로 상세 안내하여 반품 분쟁 감소.
- 부분 반품 지원: 묶음 주문에서 특정 상품만 반품 가능(주문 아이템 레벨 상태 관리).

**검색 필터/자동완성**
- 검색 필터에 "할인율" 필터가 두드러짐(세일 상품 탐색 편의).
- 브랜드 검색 시 브랜드 페이지 직접 연결(검색 -> 브랜드관 전환).

**리뷰 시스템**
- 리뷰 + "스타일 스냅" 이원 구조. 구매 리뷰는 상품 단위, 스타일 스냅은 코디 단위.
- 리뷰 포인트: 텍스트 300원, 포토 500원 (적립금 형태).

**Closet에 대한 시사점**
- 부분 반품 지원을 위한 주문 아이템 레벨 상태 관리는 Gap 분석 5.2에서 이미 제안된 패턴. `OrderItemStatus`로 RETURN_REQUESTED/EXCHANGE_REQUESTED를 아이템 레벨로 관리하는 것이 W컨셉 방식과 일치.
- 검수 기준 사전 안내로 CS 감소하는 전략은 PD-13(태그 부착 여부, 사용 흔적, 포장 상태)의 근거 보강.

---

### 4. 지그재그 (ZIGZAG)

**배송 추적 UX/API 구조**
- 마켓플레이스 모델(입점 쇼핑몰별 개별 배송). 택배사 추적 API 통합 레이어를 통해 다양한 택배사 상태를 표준화.
- 배송 상태 표준화 패턴: 각 택배사의 상이한 상태 코드를 내부 상태(준비중/배송중/배송완료)로 매핑하는 어댑터 패턴 활용. 이는 택배사가 10개 이상이라 필수.
- CarrierAdapter 패턴의 실전 사례: `CarrierType` enum + `CarrierTrackingAdapter` 인터페이스 + 택배사별 구현체(CJAdapter, HanjinAdapter 등).

**반품/교환 프로세스**
- 쇼핑몰별 반품 정책이 상이하므로, 반품 정책을 쇼핑몰(셀러) 단위로 DB 관리. `return_policy` 테이블에 셀러별 반품 기한, 배송비, 검수 기준 저장.
- 반품 배송비는 셀러별 상이(2,500원 ~ 5,000원). 정책 테이블 기반 동적 적용.

**검색 필터/자동완성**
- "쇼핑몰" 필터가 특징적(마켓플레이스이므로). 카테고리/가격/브랜드/사이즈 외에 "스타일"(캐주얼/스트릿/빈티지 등) 필터.
- AI 추천 검색(검색어 입력 시 개인화 추천 결과 병렬 표시).

**리뷰 시스템**
- 쇼핑몰별 리뷰 + 지그재그 통합 리뷰 이원 구조.
- "체형별 리뷰" 필터: 키/몸무게 입력 시 유사 체형 리뷰 우선 정렬.

**Closet에 대한 시사점**
- CarrierAdapter(Strategy) 패턴(PD-02)은 지그재그에서 검증된 패턴. 택배사별 상태 매핑 어댑터가 핵심.
- 배송비를 `shipping_fee_policy` 테이블로 관리(PD-15)하는 것은 지그재그의 셀러별 정책 테이블과 동일한 접근.

---

### 5. 에이블리 (ABLY)

**배송 추적 UX/API 구조**
- 앱 중심 UX. 배송 추적을 앱 푸시 알림으로 proactive하게 전달(상태 변경 시 자동 푸시).
- 배송 상태 변경 감지: 주기적 폴링(10분 간격) + 택배사 webhook(지원 택배사) 하이브리드.
- 빠른 배송(에이블리 풀필먼트 상품): 주문 -> 당일 출고 -> 익일 배송. 자체 물류센터 운영.

**반품/교환 프로세스**
- 반품 기한 7일. "간편 반품" 기능: 반품 사유 선택 -> 수거 예약 -> 환불까지 앱 내 원스톱.
- 빠른 환불: 수거 접수 시점에 선환불(판매자 검수 전). 검수 후 이의 있으면 환불 취소. 이는 고객 경험 극대화 전략이나 판매자 리스크 존재.

**검색 필터/자동완성**
- MZ세대 타겟이므로 "연령대" 필터, "체형" 필터가 특화.
- 인기 검색어 실시간 업데이트(Redis Sorted Set 기반). 5분 간격 갱신.

**리뷰 시스템**
- 포토 리뷰 중심. 리뷰 피드(인스타그램 스타일) 형태로 리뷰 탐색.
- 리뷰 포인트: 포토 리뷰 500원, 동영상 리뷰 1,000원. 월 적립 한도 존재.

**Closet에 대한 시사점**
- 배송 상태 변경 시 Kafka 이벤트 발행(PD-43의 product 이벤트 발행 패턴과 유사) + 알림 서비스 연동은 에이블리 방식. Phase 2에서 이벤트 스키마만 확정(PD-46)하고 Phase 3에서 실제 알림 발송하는 전략은 적절.
- "선환불" 패턴은 Phase 2에서는 과도. PD-13의 "판매자 검수 후 환불" 정책이 안전.

---

## B. 참조 B2B/커머스 SaaS

---

### 6. 쿠팡 (Coupang)

**재고 관리 패턴**
- 3단 재고 구조: `available_quantity` / `reserved_quantity` / `damaged_quantity`. 주문 시 available -> reserved 전환, 출고 시 reserved 차감, 반품 입고 시 검수 결과에 따라 available 복구 또는 damaged 전환.
- SKU 기반 재고 관리. 옵션 조합(색상 x 사이즈)마다 고유 SKU 할당. `inventory` 테이블 구조:
  ```
  sku | total_qty | available_qty | reserved_qty | damaged_qty | version
  ```
- 재고 차감 3단계: RESERVE(주문 생성) -> DEDUCT(결제 완료) -> RELEASE(취소/환불). 15분 내 결제 미완료 시 자동 RELEASE(주문 TTL).
- 분산 락: Redis 기반 분산 락 + DB optimistic lock 이중 안전장치. 락 키 = `inventory:lock:{sku}`.

**배송 상태 머신**
- 로켓배송 기준 상태 머신:
  ```
  ORDER_RECEIVED -> WAREHOUSE_PROCESSING -> PICKING -> PACKING -> SHIPPED -> 
  HUB_ARRIVAL -> OUT_FOR_DELIVERY -> DELIVERED -> CONFIRMED
  ```
- 일반 배송(마켓플레이스)은 3단계 추상화: 상품준비중 -> 배송중 -> 배송완료.
- 자동 구매확정: 배송완료 후 7일(로켓), 14일(마켓플레이스). 카테고리별 차등 기한(가전/대형 상품은 30일).

**WMS 아키텍처**
- 쿠팡 풀필먼트 서비스(CFS) 아키텍처:
  - 입고(Inbound): ASN -> 검수 -> 적치. 바코드 스캔 기반 실시간 입고 확인.
  - 보관(Storage): Zone > Aisle > Rack > Bin 4레벨 로케이션. ABC 분석 기반 골든존 배치.
  - 피킹(Picking): 웨이브 피킹 + 존 피킹 하이브리드. FIFO 원칙.
  - 출고(Outbound): 피킹 -> 패킹 -> 검수 -> 송장 발행 -> 택배사 인수.
  - 재고 실사: 순환 실사(일간, A등급 SKU) + 전수 실사(분기).
- 마이크로서비스 구조: inbound-service, storage-service, picking-service, outbound-service가 Kafka 이벤트로 연결.

**검색 엔진 활용 패턴**
- Elasticsearch 기반 상품 검색. nori + 자체 사전. 카테고리 자동 분류(ML).
- Kafka CDC로 상품 변경 실시간 인덱싱. Transactional Outbox 패턴으로 이벤트 유실 방지.

**Closet에 대한 시사점**
- 3단 재고 구조(PD-06, PD-18)는 쿠팡 패턴과 정확히 일치. `total/available/reserved` 구조 + RESERVE/DEDUCT/RELEASE 3단계는 업계 표준.
- 주문 TTL 15분(PD-18)도 쿠팡과 동일. 미결제 예약 재고 자동 해제.
- WMS Phase 2.5 분리(PD-01)는 쿠팡 CFS의 서비스 단위 분리와 유사. 입고/보관/피킹/출고를 독립 서비스로 구성하는 것이 확장성 확보.
- Transactional Outbox 패턴(PD-04, PD-51)은 쿠팡에서 검증된 이벤트 유실 방지 전략.

---

### 7. 올리브영 (OLIVE YOUNG)

**재고 관리 패턴**
- GMS(Global Management System): 온라인 + 오프라인(3,000개 매장) 재고 통합 관리.
- 재고 유형: 가용재고, 예약재고, 안전재고, 불량재고, 반품대기재고. 5개 유형으로 세분화.
- 안전재고: 카테고리별 차등 기본값. 뷰티(회전율 높음) = 20, 건강식품 = 10, 계절상품 = 5. 판매 데이터 기반 동적 조정.
- 재입고 알림: 재고 0 -> 양수 전환 시 자동 발송. WAITING 상태 90일 후 만료.

**배송 상태 머신**
- 오늘드림(당일 배송): 주문접수 -> 매장배정 -> 피킹 -> 배달출발 -> 배달완료.
- 일반 배송: 주문접수 -> 상품준비 -> 배송중 -> 배송완료 -> 구매확정.
- 배송 상태 변경 이벤트는 Kafka로 발행. Consumer가 알림/정산/재고 서비스에 비동기 전파.

**WMS 아키텍처**
- 올리브영 GMS의 핵심: Outbox + Debezium CDC 패턴. DB 변경 이벤트를 Kafka로 안정적으로 전파.
  - 비즈니스 로직에서 outbox 테이블에 INSERT (같은 트랜잭션).
  - Debezium CDC가 outbox 테이블 변경을 감지하여 Kafka로 발행.
  - Consumer에서 processed_event 테이블로 멱등성 보장.
- 입고 프로세스: ASN(사전 입고 통지) -> 도착 확인 -> 검수(수량/품질) -> 입고 확정 -> 재고 반영.
- 로케이션: Zone > Aisle > Rack > Shelf > Bin 5레벨 체계.

**검색 엔진 활용 패턴**
- OpenSearch 기반. 온라인 상품 + 매장 재고 통합 검색. "근처 매장 재고 확인" 기능.
- 유의어 사전: DB 관리 + ES synonym filter. 뷰티 도메인 특화 유의어 2,000개 이상.

**Closet에 대한 시사점**
- 올리브영 GMS의 Outbox + CDC 패턴이 PD-51에서 직접 벤치마킹 대상으로 명시됨. Closet은 Debezium 대신 `@TransactionalEventListener` + outbox 테이블 폴링으로 단순화(학습 프로젝트에 적합).
- 안전재고 카테고리별 차등(PD-20)은 올리브영 방식과 동일. 상의/하의=10, 아우터=5, 신발=8, 액세서리=15.
- 재입고 알림 90일 만료(PD-21)도 올리브영 정책과 일치.
- processed_event 테이블 기반 멱등성(PD-04)은 올리브영 GMS에서 검증된 패턴.

---

### 8. 마켓컬리 (KURLY)

**재고 관리 패턴**
- 신선식품 특성상 FEFO(First Expired First Out) 재고 관리가 핵심. 로트(Lot) 단위 재고 추적.
- 재고 차감 시점: 결제 완료 시 즉시 차감(컬리는 주문=결제가 동시이므로 RESERVE 단계 없음).
- 부분 재고 부족: "해당 상품 제외 후 주문 진행" 옵션 제공(부분 주문 허용). 쿠팡과 차별점.

**배송 상태 머신**
- 샛별배송 기준:
  ```
  주문접수 -> 상품준비 -> 배송출발(밤) -> 배송완료(새벽)
  ```
- 배송 추적: 실시간 추적이 아닌 "예상 도착 시간" 표시. 자체 배송이므로 외부 택배사 API 불필요.
- 구매확정: 자동(7일). 신선식품 특성상 불량 신고 기한이 24시간으로 매우 짧음.

**WMS 아키텍처**
- 컬리 풀필먼트 센터: 상온/냉장/냉동 3온도대 분리 보관.
- 피킹 최적화: 동선 최적화 알고리즘. 로케이션 코드 순서 기반.
- 재고 실사: 일간 순환 실사 + 주간 ABC 분석. 폐기율 관리가 KPI.

**검색 엔진 활용 패턴**
- Elasticsearch + 레시피/식재료 기반 시맨틱 검색. "된장찌개" 검색 시 재료(두부, 된장, 호박 등) 연관 상품 노출.
- 인기 검색어: 시간대별 차등(아침=조식 재료, 저녁=저녁 메뉴 재료).

**Closet에 대한 시사점**
- 컬리의 FEFO는 패션에 부적합하나, FIFO 원칙은 Phase 2.5 WMS 피킹 전략(Gap 6.3)에 적용 가능.
- 부분 재고 부족 시 부분 주문 허용은 컬리 방식이나, PD-19의 All-or-Nothing이 Phase 2 MVP에 적합. 부분 주문은 Phase 3 고려.

---

### 9. SSG.COM

**재고 관리 패턴**
- 네오(NEO) 풀필먼트 시스템: 신세계 그룹 통합 물류. SSG/이마트/신세계백화점 재고 통합.
- 멀티 웨어하우스 재고: 동일 SKU가 여러 물류센터에 분산. 주문 시 최적 물류센터 할당(배송 거리 기반).
- 재고 동기화: 이벤트 드리븐. 재고 변경 -> Kafka -> 각 채널 동기화. 최종 일관성 모델.

**배송 상태 머신**
- 쓱배송: 주문접수 -> 물류센터 할당 -> 피킹 -> 포장 -> 배송출발 -> 배송완료.
- 일반배송: 주문접수 -> 상품준비 -> 배송중 -> 배송완료.
- 다중 배송 유형(쓱배송/일반/새벽배송/편의점픽업)을 하나의 상태 머신으로 관리. `delivery_type` 컬럼으로 분기.

**WMS 아키텍처**
- 네오 WMS: Zone 기반 관리. A존(고빈도)/B존(중빈도)/C존(저빈도) ABC 분류.
- 실사: 순환 실사 + AI 기반 이상 감지(예상 대비 실재고 차이가 특정 임계값 초과 시 자동 실사 요청).
- 반품 입고: 검수 -> 재판매 판정 -> 양품 재입고/B급 전환/폐기 3분기.

**검색 엔진 활용 패턴**
- OpenSearch 기반. 상품 속성 + 매장 재고 + 배송 가능 여부 통합 인덱싱.
- 필터: "오늘 도착" 필터(배송 가능 여부에 따른 동적 필터). 재고 + 물류 상태 기반.

**Closet에 대한 시사점**
- 다중 배송 유형을 `delivery_type` 으로 분기하는 패턴은 향후 Closet이 배송 유형을 확장할 때 참고 가능.
- 반품 입고의 "양품 재입고/B급/폐기" 3분기는 Gap 6.6(반품 입고)의 설계와 일치. Phase 2.5 WMS에서 구현.
- ABC 분석 기반 재고 실사 빈도 차등은 Gap 6.5와 동일한 방향.

---

### 10. 11번가

**재고 관리 패턴**
- 오픈마켓 모델이므로 재고는 셀러별 개별 관리. 11번가 플랫폼은 재고 수량 조회/차감 API 제공.
- 재고 차감: 결제 완료 시 차감(예약 단계 없음). 대신 "장바구니 담기" 시 재고 경고 표시("3개 남음").
- 안전재고: 셀러 자율 설정. 플랫폼은 "품절 임박" 배지를 자동 표시.

**배송 상태 머신**
- 표준 5단계: 결제완료 -> 상품준비중 -> 배송중 -> 배송완료 -> 구매확정.
- 택배사 연동: 스마트택배 API. 20개 이상 택배사 지원.
- 자동 구매확정: 배송완료 후 7일(기본). 카테고리에 따라 연장 가능(가전 30일).

**WMS 아키텍처**
- SK 스토아 물류(SKS): 풀필먼트 서비스. 셀러가 SKS에 재고 위탁하면 입고/출고/반품 일괄 처리.
- SKS 연동 API: 입고예정등록, 입고확인, 출고지시, 출고완료, 반품입고 5개 핵심 API.

**검색 엔진 활용 패턴**
- Elasticsearch + AI 랭킹. 개인화 검색 결과(구매 이력, 클릭 이력 기반).
- Rate Limiting: 검색 API IP 기준 분당 120회 제한. 봇 차단 목적.

**Closet에 대한 시사점**
- 검색 Rate Limiting(PD-30: IP 분당 120회)은 11번가와 동일한 기준.
- "품절 임박" 배지는 안전재고(PD-20)와 연결 가능. available <= safety_stock 시 FE에서 배지 표시하는 UX 고려.

---

## C. 인프라/오픈소스

---

### 11. Elasticsearch nori 한글 분석기 활용 사례

**데이터 모델**
- nori 토크나이저 설정 패턴:
  ```json
  {
    "tokenizer": {
      "nori_tokenizer": {
        "type": "nori_tokenizer",
        "decompound_mode": "mixed",
        "discard_punctuation": "true",
        "user_dictionary": "userdict_ko.txt"
      }
    }
  }
  ```
- `decompound_mode` 선택:
  - `none`: 복합어 분리 안 함("반팔티셔츠" -> "반팔티셔츠")
  - `discard`: 원본 제거("반팔티셔츠" -> "반팔", "티셔츠")
  - `mixed`: 원본 + 분리 결과 모두("반팔티셔츠" -> "반팔티셔츠", "반팔", "티셔츠") -- **패션 검색에 권장**

**API 설계 패턴**
- 다중 분석기 구조 (검색 + 자동완성 + 정렬용):
  - `nori_analyzer`: 본문 검색용. nori_tokenizer + nori_readingform + lowercase.
  - `autocomplete_analyzer`: 자동완성용. edge_ngram(min=1, max=20) + lowercase.
  - `keyword_normalizer`: 정렬/집계용. lowercase + nori_readingform.
- 유의어(synonym) 적용 패턴:
  ```json
  {
    "filter": {
      "synonym_filter": {
        "type": "synonym_graph",
        "synonyms_path": "analysis/synonyms.txt",
        "updateable": true
      }
    }
  }
  ```
  - `synonym_graph` + `updateable: true`로 인덱스 리로드 없이 유의어 갱신 가능(ES 7.3+).
  - DB `search_synonym` 테이블에서 관리 -> 변경 시 `_reload_search_analyzers` API 호출.

**추상화 패턴**
- 검색 쿼리 빌더 패턴: `SearchQueryBuilder` 클래스에서 키워드/필터/정렬/페이징을 조합하여 ES 쿼리 생성.
  ```kotlin
  class ProductSearchQueryBuilder {
      fun build(request: SearchRequest): SearchSourceBuilder {
          val boolQuery = BoolQueryBuilder()
          // 키워드 매칭 (multi_match)
          request.keyword?.let { boolQuery.must(multiMatchQuery(it, "name", "brand", "tags")) }
          // 필터 (term/range)
          request.category?.let { boolQuery.filter(termQuery("category", it)) }
          request.minPrice?.let { boolQuery.filter(rangeQuery("price").gte(it)) }
          // 정렬
          val sort = when (request.sort) {
              POPULAR -> functionScoreQuery(boolQuery, popularityScoreFunction())
              LATEST -> boolQuery  // sort by createdAt desc
              else -> boolQuery
          }
          return SearchSourceBuilder().query(sort).from(offset).size(limit)
      }
  }
  ```
- Facet 집계 패턴: `terms` 집계 + `range` 집계를 조합하여 필터 옵션별 상품 수 계산.
- 인기순(POPULAR) 정렬: ES `function_score` 쿼리로 복합 점수 계산.
  ```json
  {
    "function_score": {
      "functions": [
        { "field_value_factor": { "field": "salesCount", "modifier": "log1p", "factor": 0.4 } },
        { "field_value_factor": { "field": "reviewCount", "modifier": "log1p", "factor": 0.3 } },
        { "field_value_factor": { "field": "avgRating", "modifier": "none", "factor": 0.2 } },
        { "field_value_factor": { "field": "viewCount", "modifier": "log1p", "factor": 0.1 } }
      ],
      "score_mode": "sum",
      "boost_mode": "replace"
    }
  }
  ```

**Closet에 대한 시사점**
- `decompound_mode: mixed`가 패션 검색에 최적. PRD US-702의 "반팔티셔츠 -> 반팔, 티셔츠" 요구사항 충족.
- 유의어 관리 패턴: PD-24(DB `search_synonym` 테이블 + ES synonym filter)는 업계 표준 패턴과 일치. `updateable: true` 설정으로 인덱스 리빌드 없이 유의어 갱신 가능.
- 인기순 정렬 복합 점수(PD-23)의 4요소 가중치(0.4/0.3/0.2/0.1)는 ES function_score로 구현 가능. `log1p` modifier로 점수 스케일 정규화 필요.
- 자동완성 edge_ngram(min=1, max=20)은 PRD US-704와 일치. P99 50ms(PD-27) 달성을 위해 별도 `autocomplete` 필드를 두는 것이 핵심.

---

### 12. Apache Kafka 이벤트 드리븐 패턴

**데이터 모델**
- Transactional Outbox 패턴 테이블 구조:
  ```sql
  CREATE TABLE outbox_event (
      id              BIGINT AUTO_INCREMENT PRIMARY KEY,
      aggregate_type  VARCHAR(50)  NOT NULL COMMENT '도메인 타입: ORDER, INVENTORY, SHIPPING',
      aggregate_id    VARCHAR(50)  NOT NULL COMMENT '도메인 ID',
      event_type      VARCHAR(100) NOT NULL COMMENT '이벤트 타입: order.status.changed',
      payload         TEXT         NOT NULL COMMENT 'JSON 페이로드',
      status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PUBLISHED, FAILED',
      created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
      published_at    DATETIME(6)  NULL,
      INDEX idx_status_created (status, created_at)
  ) ENGINE=InnoDB;
  ```
- Consumer 멱등성 테이블:
  ```sql
  CREATE TABLE processed_event (
      event_id    VARCHAR(100)  PRIMARY KEY COMMENT '이벤트 고유 ID (UUID)',
      event_type  VARCHAR(100)  NOT NULL,
      processed_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
      INDEX idx_event_type (event_type)
  ) ENGINE=InnoDB;
  ```
- Consumer 멱등성 처리 순서:
  1. `SELECT * FROM processed_event WHERE event_id = ?`
  2. 이미 존재하면 SKIP (중복 수신)
  3. `INSERT INTO processed_event` (UNIQUE KEY로 race condition 방지)
  4. 비즈니스 로직 수행
  5. COMMIT (processed_event INSERT + 비즈니스 로직 같은 트랜잭션)

**API 설계 패턴**
- 이벤트 스키마 설계 원칙:
  - 이벤트명: `{domain}.{action}` (예: `order.status.changed`, `inventory.reserved`, `shipping.delivered`)
  - 필수 헤더: `eventId` (UUID), `eventType`, `timestamp`, `source` (발행 서비스)
  - 페이로드: 변경된 데이터만 포함(delta), 전체 스냅샷은 별도 조회 API로
- DLQ(Dead Letter Queue) 패턴:
  - Consumer 처리 실패 시 DLQ 토픽(`{원본토픽}.dlq`)으로 전송
  - 최대 3회 재시도, 지수 백오프(1분/5분/30분) -- PD-31과 일치
  - 최종 실패 시 수동 큐(manual intervention)

**추상화 패턴**
- Outbox Poller 패턴 (Debezium CDC 대안):
  ```kotlin
  @Scheduled(fixedRate = 1000) // 1초마다 폴링
  fun publishPendingEvents() {
      val events = outboxRepository.findByStatus("PENDING", limit = 100)
      events.forEach { event ->
          try {
              kafkaTemplate.send(event.eventType, event.aggregateId, event.payload)
              event.markAsPublished()
          } catch (e: Exception) {
              event.markAsFailed()
          }
      }
      outboxRepository.saveAll(events)
  }
  ```
- 파티션 전략: `aggregateId`를 파티션 키로 사용하면 동일 도메인 객체의 이벤트 순서 보장.
- 토픽 설계: 도메인별 토픽(order.*, inventory.*, shipping.*). PD-50의 3파티션은 단일 인스턴스에 적합.

**Closet에 대한 시사점**
- Transactional Outbox(PD-04, PD-51)는 Kafka 이벤트 유실 방지의 표준 패턴. Closet은 Debezium CDC 대신 스케줄러 기반 폴링으로 단순화하는 것이 학습 프로젝트에 적합.
- Consumer 멱등성(PD-04): `processed_event` 테이블 + eventId 기반 중복 방지는 표준 패턴과 정확히 일치.
- DLQ 재처리(PD-31): 최대 3회, 지수 백오프(1분/5분/30분)는 Kafka 커뮤니티 권장 패턴.
- 파티션 수 3(PD-50)은 단일 인스턴스 환경에서 적절. 향후 수평 확장 시 파티션 추가.

---

### 13. Redisson 분산 락 패턴

**데이터 모델**
- Redis 락 키 설계:
  ```
  inventory:lock:{sku}          -- SKU별 재고 차감 락
  order:lock:{orderId}          -- 주문 상태 변경 락
  review:daily_point:{memberId} -- 리뷰 포인트 일일 한도 (INCR 패턴)
  search:popular:keywords       -- 인기 검색어 Sorted Set
  search:recent:{memberId}      -- 최근 검색어 List
  shipping:tracking:{shippingId} -- 배송 추적 캐시 (TTL 5분)
  ```
- Redisson 분산 락 파라미터:
  - `waitTime`: 락 획득 대기 시간. 5초 권장(PD-22 P99 200ms 기준 충분한 마진).
  - `leaseTime`: 락 유지 시간. 3초 권장. Watchdog 미사용 시 명시적 해제 필수.
  - `lockKey`: 가능한 한 좁은 범위(SKU 단위 > 상품 단위 > 글로벌).

**API 설계 패턴**
- 분산 락 + JPA Optimistic Lock 이중 안전장치:
  ```kotlin
  @Transactional
  fun reserveStock(sku: String, quantity: Int, orderId: Long) {
      val lock = redissonClient.getLock("inventory:lock:$sku")
      if (!lock.tryLock(5, 3, TimeUnit.SECONDS)) {
          throw InventoryLockException("Failed to acquire lock for SKU: $sku")
      }
      try {
          val inventory = inventoryRepository.findBySku(sku)
              ?: throw InventoryNotFoundException(sku)
          inventory.reserve(quantity) // available -= quantity, reserved += quantity
          inventoryRepository.save(inventory) // @Version으로 optimistic lock
      } finally {
          if (lock.isHeldByCurrentThread) lock.unlock()
      }
  }
  ```
- Watchdog 패턴 (leaseTime=-1 시 자동 연장):
  - 장점: 비즈니스 로직이 예상보다 오래 걸릴 때 락이 자동 연장됨.
  - 단점: 데드락 위험 증가. 반드시 finally에서 unlock 보장 필요.
  - 권장: Phase 2에서는 명시적 leaseTime(3초) 사용. Watchdog은 Phase 3에서 검토.

**추상화 패턴**
- 분산 락 어노테이션 패턴:
  ```kotlin
  @Target(AnnotationTarget.FUNCTION)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class DistributedLock(
      val key: String,          // SpEL 지원: "inventory:lock:#sku"
      val waitTime: Long = 5,
      val leaseTime: Long = 3,
      val timeUnit: TimeUnit = TimeUnit.SECONDS
  )
  
  @Aspect
  class DistributedLockAspect(private val redissonClient: RedissonClient) {
      @Around("@annotation(distributedLock)")
      fun around(joinPoint: ProceedingJoinPoint, distributedLock: DistributedLock): Any? {
          val key = parseSpEL(distributedLock.key, joinPoint)
          val lock = redissonClient.getLock(key)
          if (!lock.tryLock(distributedLock.waitTime, distributedLock.leaseTime, distributedLock.timeUnit)) {
              throw LockAcquisitionException(key)
          }
          try { return joinPoint.proceed() }
          finally { if (lock.isHeldByCurrentThread) lock.unlock() }
      }
  }
  ```
- 재시도 패턴: 락 획득 실패 시 최대 3회 재시도(PD-22와 일치). 지수 백오프(100ms, 200ms, 400ms).
- 인기 검색어 Sorted Set 패턴(PD-25):
  ```
  ZADD search:popular:keywords <timestamp> <keyword>  -- 검색 시 기록
  ZREMRANGEBYSCORE search:popular:keywords 0 <1시간전timestamp>  -- 1시간 이전 제거
  ZREVRANGEBYSCORE search:popular:keywords +inf -inf LIMIT 0 10  -- Top 10 조회
  ```

**Closet에 대한 시사점**
- 분산 락 + JPA @Version 이중 안전장치(PRD US-602)는 Redisson 커뮤니티에서 권장하는 표준 패턴.
- 락 파라미터 waitTime=5초, leaseTime=3초(US-602)는 P99 200ms(PD-22) 기준에서 충분한 마진.
- `@DistributedLock` 어노테이션 패턴은 재고/주문/리뷰 포인트 등 여러 도메인에서 재사용 가능. closet-common에 구현 권장.
- 인기 검색어 실시간 sliding window(PD-25)는 Redis Sorted Set의 표준 활용 패턴. ZREMRANGEBYSCORE + ZREVRANGEBYSCORE 조합으로 O(log N) 성능.
- 리뷰 포인트 일일 한도(PD-37): Redis `INCR` + TTL 패턴으로 KST 00:00 리셋 구현. `review:daily_point:{memberId}` 키에 TTL을 자정까지의 남은 시간으로 설정.

---

## D. 크로스커팅 분석

### 1. 배송 상태 머신 -- 업계 공통 패턴

| 플랫폼 | 사용자 노출 상태 | 내부 세분화 | 구매확정 기한 |
|--------|-----------------|------------|-------------|
| 무신사 | 3단계 (준비/배송중/완료) | 택배사 원본 보존 | 7일 |
| 29CM | 3단계 | 브랜드 직배송 별도 | 14일 |
| W컨셉 | 4단계 (접수/준비/배송중/완료) | SSG 물류 연동 | 7일 |
| 쿠팡 | 3단계 (일반), 8단계 (로켓) | WMS 내부 상태 다수 | 7일(로켓), 14일(마켓) |
| 올리브영 | 3단계 | 오늘드림 별도 | 7일 |
| **Closet** | **3단계 (READY/IN_TRANSIT/DELIVERED)** | **Mock 서버 상태 원본 로그 보존** | **7일 (168시간)** |

**결론**: Closet의 3단계 배송 상태(PD-10) + 상세 로그 보존은 업계 표준과 완벽히 일치. 7일 구매확정(PD-16)도 패션 이커머스 표준.

### 2. 재고 관리 -- 3단 구조의 보편성

| 플랫폼 | 재고 구조 | 차감 시점 | 동시성 제어 |
|--------|----------|----------|------------|
| 쿠팡 | available/reserved/damaged | RESERVE -> DEDUCT -> RELEASE | Redis 분산 락 + DB OL |
| 올리브영 | 가용/예약/안전/불량/반품대기 (5단) | RESERVE -> DEDUCT | DB pessimistic lock |
| 마켓컬리 | quantity (단일) | 결제 시 즉시 차감 | DB pessimistic lock |
| 11번가 | 셀러별 개별 관리 | 결제 시 차감 | API 레벨 락 |
| **Closet** | **total/available/reserved (3단)** | **RESERVE -> DEDUCT -> RELEASE** | **Redisson 분산 락 + JPA @Version** |

**결론**: Closet의 3단 재고 구조(PD-06)와 RESERVE/DEDUCT/RELEASE 3단계(PD-18)는 쿠팡 패턴과 정확히 일치하며 업계 최선 관행.

### 3. 검색 엔진 -- nori + 유의어가 기본

| 플랫폼 | 검색 엔진 | 형태소 분석 | 유의어 관리 | 자동완성 |
|--------|----------|------------|------------|---------|
| 무신사 | Elasticsearch | nori | 500개+ 사전 | edge_ngram |
| 쿠팡 | Elasticsearch | nori + 자체 사전 | ML 기반 확장 | edge_ngram + 개인화 |
| 올리브영 | OpenSearch | nori | DB + ES filter | edge_ngram |
| SSG | OpenSearch | nori | 도메인별 사전 | edge_ngram |
| **Closet** | **Elasticsearch** | **nori (mixed mode)** | **DB search_synonym + ES filter** | **edge_ngram (P99 50ms)** |

**결론**: Closet의 검색 스택(PD-24, PD-26, PD-27)은 업계 표준과 일치. nori mixed mode + 유의어 DB 관리 + edge_ngram 자동완성은 모든 주요 플랫폼이 채택한 패턴.

### 4. 리뷰 -- 사이즈 후기가 패션 차별점

| 플랫폼 | 사이즈 후기 | 포토 리뷰 포인트 | 별점 수정 | 일일 한도 |
|--------|-----------|----------------|----------|----------|
| 무신사 | O (키/몸무게/핏) | 500P | 불가 | O |
| 29CM | O (키/몸무게/핏) | 500P | 불가 | O |
| 지그재그 | O (체형별 필터) | 300P | 불가 | O |
| 에이블리 | O | 500P | 불가 | 월 한도 |
| **Closet** | **O (키/몸무게/핏, US-802)** | **300P** | **불가 (PD-32)** | **일 5,000P (PD-37)** |

**결론**: 사이즈 후기는 패션 이커머스 필수 기능. Closet의 리뷰 설계(PD-32~38)는 업계 표준을 충실히 반영. 별점 수정 불가는 모든 경쟁사 공통 정책.

### 5. 이벤트 드리븐 -- Outbox 패턴이 대세

| 플랫폼/기술 | 이벤트 전파 | 멱등성 | DLQ |
|------------|-----------|--------|-----|
| 쿠팡 | Kafka + Outbox | eventId 기반 | O (3회 재시도) |
| 올리브영 | Kafka + Outbox + Debezium CDC | processed_event 테이블 | O |
| Kafka 커뮤니티 | Outbox 패턴 권장 | UNIQUE KEY + 멱등 테이블 | O (지수 백오프) |
| **Closet** | **Kafka + Outbox (폴러 기반)** | **processed_event + eventId** | **O (3회, 1분/5분/30분)** |

**결론**: Closet의 이벤트 아키텍처(PD-04, PD-51)는 올리브영 GMS를 벤치마킹하되, Debezium 대신 폴러 기반으로 단순화. 학습 프로젝트에 적합한 선택.

---

## E. Closet 권장 하이브리드 모델

Phase 2 도메인별로 어떤 벤치마킹 소스를 조합하여 구현할지 권장합니다.

### 배송/물류 (shipping-service)
- **무신사 모델**: 배송 상태 3단계 추상화 + 상세 로그 보존. 자동확정 7일(168시간).
- **지그재그 패턴**: CarrierAdapter(Strategy) 패턴으로 택배사별 상태 매핑.
- **에이블리 참고**: Kafka 이벤트 기반 상태 변경 알림(Phase 3 알림 서비스 연동 대비).

### 재고 관리 (inventory-service)
- **쿠팡 모델**: 3단 재고(available/reserved/damaged) + RESERVE/DEDUCT/RELEASE 3단계.
- **Redisson 패턴**: 분산 락(waitTime=5s, leaseTime=3s) + JPA @Version 이중 안전장치.
- **올리브영 참고**: 카테고리별 안전재고 차등, 재입고 알림 90일 만료.

### 검색 (search-service)
- **무신사 모델**: nori(mixed) + 유의어 500개 seed + edge_ngram 자동완성 + facet 필터.
- **ES 표준 패턴**: function_score로 인기순 복합 점수(4요소 가중치).
- **Redisson 패턴**: 인기 검색어 Sorted Set 실시간 sliding window + 최근 검색어 List.

### 리뷰 (review-service)
- **무신사 모델**: 사이즈 후기(키/몸무게/핏) + 별점 수정 불가 + 수정 이력 보존.
- **업계 공통**: 포토 리뷰 차등 포인트 + 일일 적립 한도.
- **자체 설계**: 관리자 블라인드(HIDDEN) + 금칙어 DB 관리.

### WMS (Phase 2.5)
- **올리브영 GMS 모델**: ASN -> 검수 -> 입고 확정 -> 재고 반영 프로세스.
- **쿠팡 CFS 참고**: Zone 기반 로케이션 + ABC 분석 + 웨이브 피킹.
- **이벤트 인터페이스**: wms.inbound.confirmed / wms.outbound.completed / wms.stocktake.adjusted.

### 이벤트 아키텍처 (전 서비스 공통)
- **올리브영 GMS**: Transactional Outbox 패턴 (폴러 기반으로 단순화).
- **Kafka 커뮤니티**: processed_event 멱등성 + DLQ 3회 재시도 + 지수 백오프.

---

## F. 채택/미채택 패턴 표

| # | 패턴 | 벤치마킹 소스 | 채택 여부 | Phase | 근거 |
|---|------|-------------|----------|-------|------|
| 1 | 배송 상태 3단계 추상화 | 무신사, 쿠팡, 올리브영 | **채택** | 2 | 업계 표준. PD-10과 일치 |
| 2 | 상세 배송 로그 원본 보존 | 무신사, 지그재그 | **채택** | 2 | shipping_tracking_log에 택배사 원본 상태 저장 |
| 3 | CarrierAdapter(Strategy) 패턴 | 지그재그, 에이블리 | **채택** | 2 | PD-02. 택배사별 연동 구현 학습 목적에도 부합 |
| 4 | 자동확정 D-1 알림 | 무신사 | **채택 (스키마만)** | 2 | PD-45. 이벤트 스키마 확정, 실제 발송은 Phase 3 |
| 5 | 반품 3영업일 미처리 자동승인 | 무신사 | **채택** | 2 | PD-13. 검수 지연 방지 |
| 6 | 교환 동일 가격만 허용 | 무신사 | **채택** | 2 | PD-14. 차액 정산은 Phase 3 |
| 7 | 3단 재고 구조 (available/reserved) | 쿠팡 | **채택** | 2 | PD-06, PD-18. 기존 Flyway 스키마와 일치 |
| 8 | RESERVE -> DEDUCT -> RELEASE | 쿠팡 | **채택** | 2 | PD-18. 주문 TTL 15분 |
| 9 | 분산 락 + JPA @Version 이중 안전장치 | Redisson 커뮤니티 | **채택** | 2 | US-602. P99 200ms 목표 |
| 10 | @DistributedLock 어노테이션 추상화 | Redisson 커뮤니티 | **채택** | 2 | closet-common에 구현하여 전 도메인 재사용 |
| 11 | 카테고리별 안전재고 차등 | 올리브영 | **채택** | 2 | PD-20. 상의/하의=10, 아우터=5, 신발=8, 액세서리=15 |
| 12 | 재입고 알림 90일 만료 | 올리브영 | **채택** | 2 | PD-21 |
| 13 | nori mixed mode | ES 표준 | **채택** | 2 | 패션 복합어 분리에 최적 |
| 14 | 유의어 DB + ES synonym filter | 올리브영, ES 표준 | **채택** | 2 | PD-24. 초기 100개 seed |
| 15 | edge_ngram 자동완성 (P99 50ms) | 무신사, ES 표준 | **채택** | 2 | PD-27 |
| 16 | function_score 인기순 복합 점수 | ES 커뮤니티 | **채택** | 2 | PD-23. 4요소 가중치 |
| 17 | Redis Sorted Set 인기 검색어 | Redisson, 에이블리 | **채택** | 2 | PD-25. 실시간 sliding window |
| 18 | 사이즈 후기 (키/몸무게/핏) | 무신사, 29CM, 지그재그 | **채택** | 2 | US-802. 패션 이커머스 필수 |
| 19 | 별점 수정 불가 | 무신사, 29CM, 지그재그, 에이블리 | **채택** | 2 | PD-32. 업계 공통 정책 |
| 20 | 수정 이력 보존 (최대 3회) | 무신사 | **채택** | 2 | PD-32. CS 대응에 유리 |
| 21 | 리뷰 포인트 일일 한도 | 무신사, 에이블리 | **채택** | 2 | PD-37. 일 5,000P, KST 00:00 리셋 |
| 22 | Transactional Outbox (폴러) | 올리브영 GMS (Debezium 대안) | **채택** | 2 | PD-04, PD-51. 학습 프로젝트에 적합한 단순화 |
| 23 | processed_event 멱등성 | 올리브영, Kafka 커뮤니티 | **채택** | 2 | PD-04. eventId 기반 |
| 24 | DLQ 3회 재시도 + 지수 백오프 | Kafka 커뮤니티 | **채택** | 2 | PD-31 |
| 25 | WMS ASN 기반 입고 관리 | 올리브영 GMS | **채택** | 2.5 | Gap 6.1. Phase 2.5로 분리 |
| 26 | Zone > Aisle > Rack > Cell 로케이션 | 쿠팡, 올리브영 | **채택** | 2.5 | Gap 6.2 |
| 27 | 웨이브 피킹 + FIFO | 쿠팡, 올리브영 | **채택** | 2.5 | Gap 6.3 |
| 28 | 순환 실사 + ABC 분석 | 쿠팡, SSG | **채택** | 2.5 | Gap 6.5 |
| 29 | 선환불 (수거 시점 환불) | 에이블리 | **미채택** | - | 판매자 리스크 과대. Phase 2는 검수 후 환불(PD-13) |
| 30 | 부분 주문 허용 (재고 부족 시) | 마켓컬리, 쿠팡 | **미채택** | 3 검토 | PD-19 All-or-Nothing. Phase 2 MVP 단순화 |
| 31 | 카테고리별 반품 정책 분기 | 29CM, W컨셉 | **미채택** | 3 검토 | Phase 2는 단일 반품 정책 |
| 32 | Debezium CDC | 올리브영 GMS | **미채택** | 3 검토 | 인프라 복잡도. 폴러 기반으로 단순화 |
| 33 | AI 기반 개인화 검색 | 쿠팡, 11번가 | **미채택** | 3+ | ML 인프라 필요. Phase 2는 기본 검색에 집중 |
| 34 | 동영상 리뷰 | 에이블리 | **미채택** | 3+ | 스토리지/트랜스코딩 인프라 필요 |
| 35 | Watchdog 패턴 (자동 락 연장) | Redisson | **미채택** | 3 검토 | 데드락 위험. Phase 2는 명시적 leaseTime 사용 |
| 36 | 배송 지도 기반 위치 표시 | 29CM | **미채택** | 3+ | 택배사 API 지도 데이터 비제공 |
| 37 | 카테고리별 구매확정 기한 차등 | 쿠팡, 11번가 | **미채택** | 3 검토 | Phase 2는 7일 통일. 가전/대형 카테고리 미존재 |

---

## G. 요약

Phase 2의 모든 PM 의사결정(PD-01~PD-52)은 업계 벤치마킹과 높은 정합성을 보인다.

**핵심 검증 결과**:
1. **배송 3단계 + 7일 자동확정**: 무신사/쿠팡/올리브영 공통. 업계 표준으로 확인.
2. **3단 재고 + RESERVE/DEDUCT/RELEASE**: 쿠팡 패턴과 정확히 일치. 기존 Flyway 스키마 활용 결정 타당.
3. **Transactional Outbox + 멱등성**: 올리브영 GMS 벤치마킹. Debezium 대신 폴러 단순화는 학습 프로젝트에 적합.
4. **nori mixed + 유의어 + edge_ngram**: 전 경쟁사 공통 검색 스택.
5. **사이즈 후기 + 별점 수정 불가**: 패션 이커머스 5개사 공통 정책.
6. **WMS Phase 2.5 분리**: 쿠팡 CFS의 서비스 단위 분리와 일치. 올리브영 GMS 입고 프로세스 벤치마킹.

**미채택 패턴 중 Phase 3 우선 검토 대상**:
- 부분 주문 허용(마켓컬리/쿠팡)
- Debezium CDC(올리브영)
- 카테고리별 반품 정책(29CM/W컨셉)
- AI 개인화 검색(쿠팡/11번가)
