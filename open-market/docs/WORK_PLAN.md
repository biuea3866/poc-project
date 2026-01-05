# 📅 작업 순서 계획서 (WORK_PLAN.md)

> 이 문서는 오픈마켓 프로젝트의 개발 순서와 마일스톤을 정의합니다.
> Human(백엔드)과 LLM(프론트엔드, 인프라)이 병렬로 작업합니다.

---

## 작업 담당 정리

| 담당 | 역할 | 비고 |
|------|------|------|
| **Human** | 백엔드 개발 (Kotlin + Spring Boot) | 도메인 설계, API 개발 |
| **LLM** | 프론트엔드 (Next.js) | 구매자/셀러 UI |
| **LLM** | 인프라 (Docker, k6, Pinpoint) | 개발환경, 테스트 |
| **LLM** | Mock Server (PG, Channel) | 외부 연동 Mock |

---

## Phase 0: 프로젝트 세팅 (Day 1-2)

### Human 작업
- [ ] 백엔드 프로젝트 초기 설정
  - Gradle 멀티모듈 구조 생성
  - 공통 의존성 설정
  - application.yml 프로파일 구성
- [ ] 공통 모듈 개발
  - API 응답 포맷 (ApiResponse)
  - 예외 처리 (GlobalExceptionHandler)
  - 공통 Entity (BaseEntity)

### LLM 작업
- [ ] **인프라**: Docker Compose 로컬 개발 환경
  - MySQL, Redis, Kafka, Elasticsearch
  - Pinpoint (Collector, Web)
  - Kafka-UI, Kibana
- [ ] **프론트엔드**: Next.js 프로젝트 초기 설정
  - 기본 구조 생성
  - Tailwind, shadcn/ui 설정
  - 공통 레이아웃 컴포넌트

### 완료 조건
- `docker-compose up`으로 인프라 실행 가능
- 백엔드 `/health` 엔드포인트 응답
- 프론트엔드 개발 서버 실행

---

## Phase 1: 회원/인증 (Day 3-5)

### Human 작업
- [ ] **회원 도메인**
  - Member Entity (BUYER, INDIVIDUAL_SELLER, BUSINESS_SELLER, ADMIN)
  - 회원가입 API (유형별)
  - 로그인 API (JWT 발급)
  - 토큰 갱신 API
  - 내 정보 조회/수정 API
- [ ] **셀러 가입 심사**
  - SellerApplication Entity
  - 셀러 신청 API
  - (관리자) 심사 승인/반려 API

### LLM 작업
- [ ] **프론트엔드 - 인증**
  - 로그인 페이지
  - 회원가입 페이지 (구매자/셀러 분기)
  - 인증 상태 관리 (Zustand)
  - API 클라이언트 (axios + interceptor)

### 완료 조건
- 회원가입 → 로그인 → 토큰 발급 플로우 동작
- 프론트엔드에서 로그인/로그아웃 가능

---

## Phase 2: 가게/상품 기본 (Day 6-10)

### Human 작업
- [ ] **가게 도메인**
  - Shop Entity
  - 가게 생성/수정 API
  - 가게 조회 API
- [ ] **카테고리 도메인**
  - Category Entity (계층 구조)
  - 카테고리 트리 조회 API
- [ ] **상품 도메인 (기본)**
  - Product Entity
  - ProductOption Entity
  - ProductImage Entity
  - 상품 CRUD API
  - 상품 목록 조회 (페이징, 필터, 정렬)
  - 상품 상세 조회

### LLM 작업
- [ ] **프론트엔드 - 구매자**
  - 메인 페이지 (레이아웃)
  - 상품 목록 페이지
  - 상품 상세 페이지
- [ ] **프론트엔드 - 셀러**
  - 셀러 어드민 레이아웃
  - 상품 목록 페이지
  - 상품 등록/수정 페이지

### 완료 조건
- 셀러가 상품 등록 가능
- 구매자가 상품 목록/상세 조회 가능

---

## Phase 3: 장바구니/주문 (Day 11-15)

### Human 작업
- [ ] **장바구니 도메인**
  - Cart Entity (또는 Redis)
  - 장바구니 담기/수정/삭제 API
  - 장바구니 조회 API
- [ ] **재고 도메인**
  - Stock Entity 또는 ProductOption에 포함
  - 재고 차감 로직 (Redisson 분산락)
  - 재고 복구 로직
- [ ] **주문 도메인 (기본)**
  - Order Entity
  - OrderItem Entity
  - 주문 생성 API
  - 주문 목록/상세 조회 API
  - 주문 상태 변경 API

### LLM 작업
- [ ] **프론트엔드 - 구매자**
  - 장바구니 페이지
  - 주문/결제 페이지 (Mock 결제)
  - 주문 완료 페이지
  - 주문 내역 페이지
  - 주문 상세 페이지
- [ ] **프론트엔드 - 셀러**
  - 주문 관리 페이지
  - 주문 상세/처리 모달

### 완료 조건
- 장바구니 → 주문 → 상태 변경 플로우 동작
- 재고 차감/복구 정상 동작

---

## Phase 4: 결제 연동 (Day 16-20)

### Human 작업
- [ ] **결제 도메인**
  - Payment Entity
  - 결제 준비 API
  - 결제 승인 API
  - 결제 취소/환불 API
  - Webhook 수신 API
- [ ] **PG 어댑터**
  - PgAdapter 인터페이스
  - TossPaymentsAdapter 구현
  - KakaoPayAdapter 구현
  - (추가 PG는 동일 패턴)

### LLM 작업
- [ ] **Mock Server - PG**
  - 토스페이먼츠 Mock API
  - 카카오페이 Mock API
  - Mock 결제창 HTML
  - 테스트 시나리오 (성공/실패)
- [ ] **프론트엔드**
  - PG 결제창 연동
  - 결제 완료 처리

### 완료 조건
- Mock PG로 결제 플로우 테스트 가능
- 결제 성공/실패/취소 시나리오 동작

---

## Phase 5: 배송/정산 (Day 21-25)

### Human 작업
- [ ] **배송 도메인**
  - Delivery Entity
  - 배송 상태 조회 API
  - 송장 등록 API
- [ ] **정산 도메인**
  - Settlement Entity
  - 정산 계산 로직
  - Spring Batch 정산 Job
  - 정산 내역 조회 API

### LLM 작업
- [ ] **프론트엔드 - 셀러**
  - 정산 관리 페이지
  - 정산 상세 조회
- [ ] **프론트엔드 - 구매자**
  - 배송 추적 연동

### 완료 조건
- 발송 처리 → 배송 상태 변경 동작
- 정산 배치 실행 → 정산 내역 조회 가능

---

## Phase 6: 외부 채널 연동 (Day 26-30)

### Human 작업
- [ ] **채널 연동 도메인**
  - ChannelProduct Entity
  - ChannelOrder Entity
  - ChannelAdapter 인터페이스
  - 각 채널별 Adapter 구현
  - Webhook 수신 Controller
- [ ] **상품/주문 동기화**
  - 상품 등록 시 채널 연동
  - 채널 주문 수신 처리

### LLM 작업
- [ ] **Mock Server - Channel**
  - 11번가 Mock API
  - 네이버 스마트스토어 Mock API
  - (기타 채널)
  - Webhook 발송 기능
- [ ] **프론트엔드 - 셀러**
  - 채널 연동 설정 페이지
  - 채널별 상품 연동 상태 표시

### 완료 조건
- 상품 등록 시 채널 연동 (Mock) 동작
- 채널 주문 Webhook 수신 처리 동작

---

## Phase 7: 검색/전시/광고 (Day 31-35)

### Human 작업
- [ ] **검색 도메인**
  - Elasticsearch 인덱싱
  - 검색 API (키워드, 필터)
  - 자동완성 API
- [ ] **전시 도메인**
  - Display Entity (배너, 기획전)
  - 전시 영역 조회 API
- [ ] **광고 도메인**
  - Ad Entity
  - 광고 등록 API
  - 광고 노출 로직

### LLM 작업
- [ ] **프론트엔드 - 구매자**
  - 검색 결과 페이지 (ES 연동)
  - 자동완성 기능
  - 메인 배너/기획전 영역
- [ ] **프론트엔드 - 셀러**
  - 광고 관리 페이지

### 완료 조건
- Elasticsearch 검색 동작
- 광고 노출 플로우 동작

---

## Phase 8: 쿠폰/포인트/리뷰 (Day 36-40)

### Human 작업
- [ ] **쿠폰 도메인**
  - Coupon Entity
  - CouponIssue Entity
  - 쿠폰 발급/사용 API
- [ ] **포인트 도메인**
  - Point Entity
  - PointHistory Entity
  - 포인트 적립/사용/소멸 API
- [ ] **리뷰 도메인**
  - Review Entity
  - 리뷰 CRUD API

### LLM 작업
- [ ] **프론트엔드 - 구매자**
  - 쿠폰함 페이지
  - 포인트 내역 페이지
  - 리뷰 작성 페이지
  - 상품 상세 리뷰 목록
- [ ] **프론트엔드 - 셀러**
  - 쿠폰 관리 페이지
  - 리뷰 관리 페이지

### 완료 조건
- 쿠폰/포인트 적용 결제 동작
- 리뷰 작성/조회 동작

---

## Phase 9: 알림/고객센터 (Day 41-45)

### Human 작업
- [ ] **알림 도메인**
  - Notification Entity
  - 알림 발송 로직 (Kafka 이벤트)
  - 알림 조회 API
- [ ] **고객센터 도메인**
  - Inquiry Entity
  - 문의 등록/답변 API
  - FAQ API

### LLM 작업
- [ ] **프론트엔드 - 공통**
  - 알림 센터 (헤더 드롭다운)
  - 알림 목록 페이지
- [ ] **프론트엔드 - 구매자**
  - 1:1 문의 페이지
  - FAQ 페이지
- [ ] **프론트엔드 - 셀러**
  - 문의 관리 페이지

### 완료 조건
- 주문/배송 상태 변경 시 알림 발송
- 문의 등록/답변 플로우 동작

---

## Phase 10: 테스트/최적화 (Day 46-50)

### Human 작업
- [ ] 단위 테스트 보강
- [ ] 통합 테스트 작성
- [ ] API 성능 최적화
- [ ] 쿼리 최적화

### LLM 작업
- [ ] **인프라**: k6 부하 테스트 실행
  - 주요 시나리오 테스트
  - 병목 지점 리포트
- [ ] **인프라**: Pinpoint 모니터링 확인
  - 슬로우 쿼리 분석
  - 응답시간 분석
- [ ] **프론트엔드**: 성능 최적화
  - Lighthouse 점수 개선
  - 번들 사이즈 최적화

### 완료 조건
- 성능 목표 달성 (SLO 충족)
- 테스트 커버리지 목표 달성

---

## Phase 11: 관리자 기능 (Day 51-55)

### Human 작업
- [ ] 관리자 전용 API
  - 회원 관리
  - 셀러 관리 (입점 심사)
  - 상품 관리
  - 주문/분쟁 관리
  - 통계 API

### LLM 작업
- [ ] **프론트엔드 - 관리자**
  - 관리자 대시보드
  - 회원 관리 페이지
  - 셀러 심사 페이지
  - 상품 관리 페이지
  - 통계 페이지

### 완료 조건
- 관리자 기능 전체 동작

---

## Phase 12: 마무리 (Day 56-60)

### Human 작업
- [ ] API 문서화 (Swagger)
- [ ] 코드 리팩토링
- [ ] 보안 점검

### LLM 작업
- [ ] **인프라**: CI/CD 파이프라인 완성
- [ ] **프론트엔드**: 최종 QA
- [ ] **문서**: README 정리

### 완료 조건
- 프로젝트 배포 가능 상태
- 문서화 완료

---

## 마일스톤 요약

| Phase | 기간 | 주요 산출물 |
|-------|------|------------|
| 0 | Day 1-2 | 프로젝트 세팅, 개발환경 |
| 1 | Day 3-5 | 회원/인증 |
| 2 | Day 6-10 | 가게/상품 기본 |
| 3 | Day 11-15 | 장바구니/주문 |
| 4 | Day 16-20 | 결제 연동 (Mock) |
| 5 | Day 21-25 | 배송/정산 |
| 6 | Day 26-30 | 외부 채널 연동 (Mock) |
| 7 | Day 31-35 | 검색/전시/광고 |
| 8 | Day 36-40 | 쿠폰/포인트/리뷰 |
| 9 | Day 41-45 | 알림/고객센터 |
| 10 | Day 46-50 | 테스트/최적화 |
| 11 | Day 51-55 | 관리자 기능 |
| 12 | Day 56-60 | 마무리 |

---

## 작업 시작 방법

### Human (백엔드)
```bash
cd ~/feature/flag_project/open-market/backend
# Phase별 도메인 개발 시작
```

### LLM (Claude Code)
```
1. docs/CONTEXT.md 읽기
2. docs/WORK_PLAN.md 확인 (현재 Phase)
3. 담당 작업 수행
4. docs/HISTORY.md 업데이트
```
