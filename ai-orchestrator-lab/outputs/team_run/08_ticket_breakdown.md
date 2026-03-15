# Stage 08: 티켓 분해

> 06_architecture.md + 07_parallel_plan.md 기준 | 작성일: 2026-03-14

---

## 분해 원칙

- 각 티켓은 독립 PR로 완결 가능한 단위
- lane(FE/BE/DevOps)별로 분리하되, cross-lane 협업 티켓은 별도 표기
- Jira 프로젝트 NAW에 등록 가능한 형태

---

## BE Lane 티켓

### NAW-BE-001: Document 도메인 모델 및 상태 머신

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-001 |
| 제목 | Document 도메인 POJO + 상태 전이 로직 구현 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | architect |
| 모듈 | core/domain |
| 설명 | Document, DocumentStatus(DRAFT/ACTIVE/DELETED), AiStatus(NOT_STARTED~FAILED), Tag, DocumentRevision 도메인 POJO 작성. 상태 전이 메서드(activate, delete, requestAnalysis, startProcessing, completeAnalysis, failAnalysis) 구현. 잘못된 전이 시 IllegalStateException. |
| 수용 기준 | - Document POJO에 상태 전이 메서드 존재 - DRAFT→ACTIVE, ACTIVE→DELETED 전이 성공 - DRAFT에서 analyze 요청 시 예외 - PROCESSING 중 재요청 시 예외 - 단위 테스트 통과 |
| 의존성 | 없음 |
| risk_score | 2 |
| complexity_score | 3 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-BE-002: Port 인터페이스 정의

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-002 |
| 제목 | Outbound Port 인터페이스 정의 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | architect |
| 모듈 | core/domain |
| 설명 | DocumentRepository, SummaryPort, TaggerPort, EmbeddingPort, DocumentEventPublisher 인터페이스 정의. |
| 수용 기준 | - 5개 Port 인터페이스 정의 완료 - 컴파일 성공 |
| 의존성 | NAW-BE-001 |
| risk_score | 1 |
| complexity_score | 1 |
| daily_capacity_cost | 1 |
| recommended_day_bucket | single-day |

### NAW-BE-003: JPA Entity 및 영속성 어댑터

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-003 |
| 제목 | JPA Entity + Repository + Mapper 구현 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | architect |
| 모듈 | adapters/persistence-jpa |
| 설명 | DocumentJpaEntity(@Version updatedAt), TagJpaEntity, DocumentRevisionJpaEntity 작성. DocumentMapper(Entity↔Domain 변환). Spring Data JPA Repository. DocumentRepository Port 구현. |
| 수용 기준 | - Entity ↔ Domain 매핑 정확 - @Version으로 낙관적 잠금 동작 - 통합 테스트(H2) 통과 |
| 의존성 | NAW-BE-001, NAW-BE-002 |
| risk_score | 3 |
| complexity_score | 3 |
| daily_capacity_cost | 3 |
| recommended_day_bucket | single-day |

### NAW-BE-004: 문서 CRUD UseCase 구현

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-004 |
| 제목 | 문서 생성/조회/수정/삭제 UseCase |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | tech_lead |
| 모듈 | core/application |
| 설명 | CreateDocumentUseCase, UpdateDocumentUseCase(낙관적 잠금), DeleteDocumentUseCase, ChangeDocumentStatusUseCase 구현. 본인 문서만 접근 가능하도록 ownerId 검증. |
| 수용 기준 | - CRUD 전체 동작 - 낙관적 잠금 충돌 시 예외 발생 - 타인 문서 접근 시 403 - 단위 테스트 통과 |
| 의존성 | NAW-BE-003 |
| risk_score | 2 |
| complexity_score | 3 |
| daily_capacity_cost | 3 |
| recommended_day_bucket | single-day |

### NAW-BE-005: AI 파이프라인 비동기 실행

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-005 |
| 제목 | AnalyzeDocumentUseCase + AiPipelineService 구현 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | architect |
| 모듈 | core/application |
| 설명 | AnalyzeDocumentUseCase: ACTIVE 검증, PROCESSING 중 409, AI 상태 전이. AiPipelineService: @Async로 summary→tagger→embedding 순차 실행. ApplicationEventPublisher로 단계별 이벤트 발행. 실패 시 FAILED 상태 전이. |
| 수용 기준 | - ACTIVE 문서만 analyze 가능 - PROCESSING 중 재요청 시 409 - 파이프라인 비동기 실행 확인 - 성공 시 COMPLETED, 실패 시 FAILED - 단계별 이벤트 발행 |
| 의존성 | NAW-BE-004, NAW-BE-006 |
| risk_score | 4 |
| complexity_score | 4 |
| daily_capacity_cost | 4 |
| recommended_day_bucket | multi-day |

### NAW-BE-006: AI Mock Adapter 구현

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-006 |
| 제목 | Mock AI Adapter (Summary, Tagger, Embedding) |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | qa |
| 모듈 | adapters/ai |
| 설명 | MockSummaryAdapter, MockTaggerAdapter, MockEmbeddingAdapter 구현. 고정 응답 반환. AI 서비스 확정 전 파이프라인 흐름 검증용. |
| 수용 기준 | - 3개 Mock Adapter가 Port 인터페이스 구현 - 고정 응답 반환 - 프로파일(mock)로 활성화 |
| 의존성 | NAW-BE-002 |
| risk_score | 1 |
| complexity_score | 1 |
| daily_capacity_cost | 1 |
| recommended_day_bucket | single-day |

### NAW-BE-007: SSE 엔드포인트

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-007 |
| 제목 | SSE 이벤트 스트리밍 엔드포인트 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | tech_lead |
| 모듈 | apps/api |
| 설명 | SseEmitterManager: documentId별 emitter 관리. SseController: GET /api/v1/documents/{id}/events. @EventListener로 AI 파이프라인 이벤트 수신 후 SSE 전송. 타임아웃 5분. |
| 수용 기준 | - SSE 연결 성공 - AI 단계별 이벤트 수신 (STAGE_COMPLETED, PIPELINE_COMPLETED, PIPELINE_FAILED) - 타임아웃 후 정리 |
| 의존성 | NAW-BE-005 |
| risk_score | 3 |
| complexity_score | 3 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-BE-008: REST Controller + Security

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-008 |
| 제목 | DocumentController + JWT 인증 설정 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | architect |
| 모듈 | apps/api |
| 설명 | DocumentController: CRUD + status + analyze + search 엔드포인트. SecurityConfig: JWT 필터, 인증 예외 처리. GlobalExceptionHandler: 409(잠금충돌), 400(잘못된 상태전이), 403(권한 없음). |
| 수용 기준 | - 전체 API 엔드포인트 동작 - JWT 인증 적용 - 에러 응답 형식 통일 - 통합 테스트 통과 |
| 의존성 | NAW-BE-004, NAW-BE-005, NAW-BE-007 |
| risk_score | 3 |
| complexity_score | 3 |
| daily_capacity_cost | 3 |
| recommended_day_bucket | single-day |

### NAW-BE-009: 문서 검색 기능

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-009 |
| 제목 | 제목/본문/태그 기반 검색 구현 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | tech_lead |
| 모듈 | core/application, adapters/persistence-jpa |
| 설명 | SearchDocumentUseCase 구현. PostgreSQL Full-text search(tsvector) 활용. 본인 ACTIVE 문서만 대상. 태그 필터링. 페이징(offset 기반). |
| 수용 기준 | - 제목/본문 키워드 검색 동작 - 태그 필터링 동작 - 본인 ACTIVE 문서만 반환 - 페이징 동작 |
| 의존성 | NAW-BE-003 |
| risk_score | 3 |
| complexity_score | 3 |
| daily_capacity_cost | 3 |
| recommended_day_bucket | single-day |

### NAW-BE-010: Revision 자동 생성

| 항목 | 내용 |
|------|------|
| ID | NAW-BE-010 |
| 제목 | ACTIVE 문서 수정 시 Revision 자동 생성 |
| 우선순위 | P0 |
| lane | be |
| 담당 | be_engineer |
| 리뷰 | qa |
| 모듈 | core/application, adapters/persistence-jpa |
| 설명 | UpdateDocumentUseCase에서 ACTIVE 문서 수정 시 DocumentRevision 자동 생성. revision 이력 조회 API. |
| 수용 기준 | - ACTIVE 수정 시 revision 자동 생성 - revision 번호 순차 증가 - 이력 조회 API 동작 |
| 의존성 | NAW-BE-004 |
| risk_score | 2 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

---

## FE Lane 티켓

### NAW-FE-001: API Client 및 타입 정의

| 항목 | 내용 |
|------|------|
| ID | NAW-FE-001 |
| 제목 | API Client + TypeScript 타입 정의 |
| 우선순위 | P0 |
| lane | fe |
| 담당 | fe_engineer |
| 리뷰 | tech_lead |
| 설명 | Axios/fetch 기반 API client. Document, Tag, DocumentStatus, AiStatus 타입. React Query hooks (useDocuments, useDocument, useCreateDocument 등). |
| 수용 기준 | - 전체 API 엔드포인트 대응 client 함수 - TypeScript 타입 정의 완료 - Mock 응답으로 동작 확인 |
| 의존성 | 없음 |
| risk_score | 1 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-FE-002: 문서 목록/상세 페이지

| 항목 | 내용 |
|------|------|
| ID | NAW-FE-002 |
| 제목 | 문서 목록 및 상세 조회 페이지 |
| 우선순위 | P0 |
| lane | fe |
| 담당 | fe_engineer |
| 리뷰 | tech_lead |
| 설명 | 문서 목록 페이지(계층 구조 표시, 상태 뱃지). 문서 상세 페이지(Markdown 렌더링, AI 요약/태그 표시). |
| 수용 기준 | - 목록 페이지에서 본인 문서 표시 - 상세 페이지에서 Markdown 렌더링 - 상태 뱃지(DRAFT/ACTIVE) 표시 |
| 의존성 | NAW-FE-001 |
| risk_score | 2 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-FE-003: 문서 생성/수정 폼

| 항목 | 내용 |
|------|------|
| ID | NAW-FE-003 |
| 제목 | 문서 생성/수정 폼 (Markdown 에디터) |
| 우선순위 | P0 |
| lane | fe |
| 담당 | fe_engineer |
| 리뷰 | qa |
| 설명 | Markdown 에디터 컴포넌트. 문서 생성(DRAFT). 문서 수정(낙관적 잠금 충돌 시 사용자 알림). |
| 수용 기준 | - 에디터에서 Markdown 작성/미리보기 - 생성 시 DRAFT 상태 - 수정 시 updatedAt 전송 - 409 충돌 시 사용자 알림 |
| 의존성 | NAW-FE-001 |
| risk_score | 2 |
| complexity_score | 3 |
| daily_capacity_cost | 3 |
| recommended_day_bucket | single-day |

### NAW-FE-004: 상태 전환 + AI 분석 요청 UI

| 항목 | 내용 |
|------|------|
| ID | NAW-FE-004 |
| 제목 | DRAFT→ACTIVE 전환 + AI 분석 요청 + SSE 상태 표시 |
| 우선순위 | P0 |
| lane | fe |
| 담당 | fe_engineer |
| 리뷰 | tech_lead |
| 설명 | ACTIVE 전환 버튼. Analyze 요청 버튼. SSE 연결로 AI 파이프라인 진행 상태 실시간 표시(프로그레스 바). 완료/실패 결과 표시. |
| 수용 기준 | - DRAFT→ACTIVE 전환 동작 - Analyze 요청 후 SSE로 상태 수신 - 단계별 진행 표시 - COMPLETED/FAILED 결과 표시 |
| 의존성 | NAW-FE-002 |
| risk_score | 3 |
| complexity_score | 4 |
| daily_capacity_cost | 3 |
| recommended_day_bucket | single-day |

### NAW-FE-005: 검색 UI

| 항목 | 내용 |
|------|------|
| ID | NAW-FE-005 |
| 제목 | 문서 검색 페이지 + 태그 필터 |
| 우선순위 | P0 |
| lane | fe |
| 담당 | fe_engineer |
| 리뷰 | qa |
| 설명 | 검색 입력 필드(debounce). 검색 결과 목록. 태그 필터 칩. 페이징. |
| 수용 기준 | - 키워드 검색 동작 - 태그 필터 동작 - 결과 목록 표시 - 페이징 동작 |
| 의존성 | NAW-FE-001 |
| risk_score | 2 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

---

## DevOps Lane 티켓

### NAW-DO-001: 개발 환경 Docker Compose

| 항목 | 내용 |
|------|------|
| ID | NAW-DO-001 |
| 제목 | 개발 환경 Docker Compose (PostgreSQL + API) |
| 우선순위 | P0 |
| lane | devops |
| 담당 | devops_engineer |
| 리뷰 | tech_lead |
| 설명 | PostgreSQL 15 컨테이너. API 서비스 (빌드 또는 이미지). .env.example. 네트워크 구성. |
| 수용 기준 | - `docker compose up`으로 DB + API 기동 - DB 마이그레이션 자동 적용 - .env.example 문서화 |
| 의존성 | 없음 |
| risk_score | 1 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-DO-002: Pinpoint 모니터링 환경

| 항목 | 내용 |
|------|------|
| ID | NAW-DO-002 |
| 제목 | Pinpoint Docker Compose (HBase + Collector + Web) |
| 우선순위 | P0 |
| lane | devops |
| 담당 | devops_engineer |
| 리뷰 | tech_lead |
| 설명 | pinpointdocker 이미지 2.5.4. HBase, Collector, Web 구성. 네트워크 연결. .env.example 업데이트. |
| 수용 기준 | - Pinpoint Web(:28080) 접속 확인 - Collector(:9994) 수신 대기 - HBase 정상 기동 |
| 의존성 | 없음 |
| risk_score | 2 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-DO-003: BE Dockerfile + Pinpoint Agent 연동

| 항목 | 내용 |
|------|------|
| ID | NAW-DO-003 |
| 제목 | API Dockerfile에 Pinpoint Agent 통합 |
| 우선순위 | P0 |
| lane | devops |
| 담당 | devops_engineer |
| 리뷰 | architect |
| cross_lane | true (BE 협업) |
| 설명 | Dockerfile에 Pinpoint Agent 다운로드/설치. JAVA_TOOL_OPTIONS 환경변수 설정. agentId, applicationName, collector IP 환경변수화. |
| 수용 기준 | - 컨테이너 기동 시 Pinpoint Agent 로드 확인 - Pinpoint Web에서 애플리케이션 표시 - 환경변수로 설정 변경 가능 |
| 의존성 | NAW-DO-001, NAW-DO-002 |
| risk_score | 3 |
| complexity_score | 3 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

### NAW-DO-004: CI/CD 파이프라인

| 항목 | 내용 |
|------|------|
| ID | NAW-DO-004 |
| 제목 | GitHub Actions CI/CD (빌드, 테스트, 이미지) |
| 우선순위 | P1 |
| lane | devops |
| 담당 | devops_engineer |
| 리뷰 | tech_lead |
| 설명 | PR 시 빌드 + 테스트. main 병합 시 Docker 이미지 빌드 + push. BE/FE 각각 workflow. |
| 수용 기준 | - PR 시 자동 빌드/테스트 - main 병합 시 이미지 빌드 - 실패 시 알림 |
| 의존성 | NAW-DO-001 |
| risk_score | 2 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

---

## Cross-Lane 티켓

### NAW-CL-001: Pinpoint 추적 대상 및 운영 체크리스트

| 항목 | 내용 |
|------|------|
| ID | NAW-CL-001 |
| 제목 | BE-DevOps 협업: Pinpoint 추적 범위 + 운영 체크리스트 |
| 우선순위 | P0 |
| lane | common |
| 담당 | be_engineer, devops_engineer |
| 리뷰 | architect |
| cross_lane | true |
| 설명 | 추적 대상 레이어 합의(Controller, Service, 외부 HTTP). 배포 방식 문서화. 환경변수 목록 최종 정리. 운영 체크리스트(헬스체크, 로그, 알림). |
| 수용 기준 | - 추적 대상 문서화 - 운영 체크리스트 완성 - BE/DevOps 양쪽 확인 |
| 의존성 | NAW-BE-008, NAW-DO-003 |
| risk_score | 2 |
| complexity_score | 2 |
| daily_capacity_cost | 2 |
| recommended_day_bucket | single-day |

---

## 티켓 요약

| Lane | 티켓 수 | P0 | P1 |
|------|---------|----|----|
| BE | 10 | 10 | 0 |
| FE | 5 | 5 | 0 |
| DevOps | 4 | 3 | 1 |
| Cross-Lane | 1 | 1 | 0 |
| **합계** | **20** | **19** | **1** |
