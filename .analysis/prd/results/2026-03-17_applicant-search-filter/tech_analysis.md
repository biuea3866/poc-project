# Tech Analysis: 지원자 검색/필터/리스트/스크리닝 (새싹 Phase 2-1)

> 분석일: 2026-03-17 | PRD: 1-Pager 새싹 Phase 2-1 (탐색/선별)

---

## 요약

| 구분 | 신규 개발 | 기존 수정 | 비고 |
|------|-----------|-----------|------|
| BE (greeting-new-back) | 5개 모듈 | 8개 파일 | 스크리닝 도메인 신규 |
| DB | 4개 테이블 신규 | 0개 | 인덱스 추가 별도 |
| OpenSearch (offercent-search) | 1개 인덱스 신규 | 3개 파일 | ATS 지원자 인덱스 |
| Aggregator | 3개 API 신규 | 5개 API 수정 | |
| FE (next-greeting) | 다수 컴포넌트 | 기존 필터 UI 전면 교체 | |

---

## 1. BE 영향 분석 (greeting-new-back)

### 1-1. 현재 구조

```
greeting-new-back/
  interfaces/  -- Controller 계층
    candidate/controller/
      AtsGetApplicantsSortEnum.kt          -- 정렬: SUBMIT_DATE, REJECT_DATE만 지원
      AtsGetApplicantsSortRequest.kt
    aggregate/controller/
      SearchApiController.kt               -- 학교/전공/자격증 등 마스터데이터 검색 (지원자 검색 아님)
  domain/     -- Service/Domain 계층
    candidate/application/
      domain/
        AtsApplicantFilter.kt              -- 현재 필터: 24개 필드
        AtsApplicantSort.kt                -- 현재 정렬: 14개 enum
      repository/
        ApplicantsReader.kt                -- 지원자 조회 인터페이스
        QApplicantsRepositoryImpl.kt       -- QueryDSL 기반 지원자 쿼리
        JPAQueryApplicantFilterExtension.kt -- 필터 적용 확장 함수 (928줄)
    evaluation/ai/                         -- AI 서류평가 (기존 스크리닝)
      entity/AiScreeningResult.kt
      service/AiScreeningFacade.kt
    zconfuse/
      applicants/ApplicantsController.kt   -- 레거시 지원자 컨트롤러
      searching/SearchingController.kt     -- 레거시 검색 컨트롤러
```

### 1-2. 현재 필터 지원 항목 vs PRD 요구 필터 항목

| PRD 필터 항목 | 현재 AtsApplicantFilter 지원 | 비고 |
|---------------|:---:|------|
| 등록일 | O | submitDateGt/Lt |
| 채용 단계 | O | processIds/processNames |
| 지원 경로 | O | refererNames |
| 평가 상태 | O | evaluationStatusList |
| 현 단계 평점 | O | averageScoreGe/Le |
| 지원자 상태 | O | statusList |
| 중복지원자 | X | **신규** |
| 평가자별 평가여부 | X | **신규** |
| 면접상태 | X | **신규** |
| AI서류평가 | O | aiScreeningScoreGe/Le |
| 지원자 잠금 여부 | X | **신규** (ApplicantEntity.isLock 존재) |
| 모집분야 | X | **신규** |
| 개인정보동의항목 | X | **신규** (optionalTermsAgree 존재) |
| 연동평가점수/상태 | X | **신규** |
| 경력 | O | careerFrom/To |
| 나이 | O | ageGt/Lt |
| 병역사항 | O | militaryStatus |
| 보훈여부 | O | veteranStatus |
| 성별 | O | genderTypes |
| 장애사항 | O | isDisability |
| 최종 학력 | O | educationalBackgroundType |
| 태그 | O | tags |
| 국적 | X | **신규** |
| 거주지역 | X | **신규** |
| 신장/체중/시력 | X | **신규** |
| 희망연봉/직전연봉 | X | **신규** |
| 입사가능일자 | X | **신규** |
| 추천인 여부 | X | **신규** (referer 필드 존재하나 여부 필터 없음) |
| 출신학교 | X | **신규** |
| 대학교 전공/학점 | X | **신규** |
| 고용형태 | O | employments |
| 회사명 | X | **신규** |
| 근무상태구분 | X | **신규** |
| 이직빈도 | X | **신규** |
| 담당업무 | X | **신규** |
| 외국어시험(종류/시험명/점수) | X | **신규** |
| 외국어활용능력(종류/수준) | X | **신규** |
| 자격증명 | X | **신규** |
| 컴퓨터활용능력 | X | **신규** |
| 제출서류(유무/유형) | X | **신규** |
| 자기소개서(유무) | X | **신규** |
| 추가질문(유무/유형) | X | **신규** |

**결과: 기존 15개 필터 지원 / PRD 요구 40+ 항목 중 25+ 항목 신규 추가 필요**

### 1-3. 현재 정렬 vs PRD 요구 정렬

| PRD 정렬 항목 | 현재 AtsApplicantSort 지원 | 비고 |
|---------------|:---:|------|
| 이름 | O | NAME_ASC/DESC |
| 평점 | O | AVERAGE_SCORE_ASC/DESC |
| 지원경로 | O | SOURCE_NAME_ASC/DESC |
| 등록일 | O | SUBMIT_DATE_ASC/DESC |
| 단계 이동일 | O | LAST_MOVED_DATE_ASC/DESC |
| 채용단계 | O | POO_PROCEDURE_ASC/DESC |
| 공고 | O | OPENING_TITLE_ASC/DESC |
| 수험번호 | X | **신규** |
| 평가상태 | X | **신규** |
| 면접일시 | X | **신규** |

### 1-4. 수정/신규 필요 파일 목록

#### 수정 필요

| 파일 | 변경 내용 |
|------|-----------|
| `domain/.../AtsApplicantFilter.kt` | 25+ 필터 필드 추가 |
| `domain/.../JPAQueryApplicantFilterExtension.kt` | 신규 필터 조건 적용 로직 추가 (또는 OpenSearch로 전환) |
| `domain/.../AtsApplicantSort.kt` | 수험번호/평가상태/면접일시 정렬 추가 |
| `interfaces/.../AtsGetApplicantsSortEnum.kt` | 신규 정렬 enum 추가 |
| `domain/.../QApplicantsRepositoryImpl.kt` | 정렬 로직 확장 |
| `domain/.../ApplicantsReader.kt` | 인터페이스 메서드 시그니처 변경 (패싯 카운트 등) |
| `domain/.../AtsApplicantReaderImpl.kt` | 구현체 수정 |
| `domain/.../AtsApplicantRepositoryCustomImpl.kt` | 커스텀 쿼리 수정 |

#### 신규 개발

| 모듈 | 내용 |
|------|------|
| `domain/screening/` | 스크리닝 도메인 신규 (규칙 정의, 점수 계산, 자동 판정) |
| `domain/screening/entity/` | ScreeningRule, ScreeningResult, ScreeningCondition 엔티티 |
| `domain/screening/service/` | ScreeningService (규칙 CRUD, 점수 계산, 자동 태그) |
| `domain/.../SavedFilterEntity.kt` | 필터 저장 엔티티 |
| `domain/.../ListViewCustomSettingEntity.kt` | 리스트뷰 커스텀 설정 엔티티 |
| `interfaces/screening/controller/` | 스크리닝 API 컨트롤러 |
| 패싯 카운트 Service | OpenSearch Aggregation 기반 필터별 카운트 조회 |

---

## 2. DB/인프라 영향 분석

### 2-1. 신규 테이블 필요

| 테이블명 (안) | 용도 | 주요 컬럼 |
|--------------|------|-----------|
| `screening_rules` | 공고별 스크리닝 규칙 정의 | id, opening_id, name, rule_type(가점/감점/부적격), created_by, created_at |
| `screening_conditions` | 스크리닝 규칙 내 조건 | id, rule_id, filter_field, operator, value, score |
| `screening_results` | 지원자별 스크리닝 결과 | id, applicant_id, rule_id, total_score, pass_status, calculated_at |
| `saved_filters` | 저장된 필터 | id, workspace_id, user_id, opening_id, name, filter_json, created_at |
| `list_view_settings` | 리스트뷰 커스텀 설정 | id, workspace_id, user_id, opening_id, columns_json, created_at |

### 2-2. 기존 테이블 인덱스 추가 검토

현재 `AtsApplicantFilter`의 신규 필터 항목들은 대부분 관련 테이블에 이미 컬럼이 존재하나, 필터 쿼리 성능을 위해 인덱스 추가가 필요할 수 있음:

- `ApplicantAdditionalInfo` 테이블: 국적, 거주지역, 신장, 체중, 시력 관련 컬럼 존재 여부 확인 필요
- `ApplicantSummary` 테이블: 새싹 Phase 1에서 추가된 정규화 테이블 활용
- `ApplicantEducationalBackground` 테이블: 출신학교, 전공, 학점 필터용 인덱스
- `ApplicantCareer` 테이블: 회사명, 근무상태 필터용 인덱스

### 2-3. OpenSearch 인덱스

#### 현재 상태

| 인덱스 | 용도 | 위치 |
|--------|------|------|
| `candidate_embedding` | TRM 후보자 검색 | `offercent-search/api/src/main/resources/elasticsearch/indices/candidate-embedding-index.json` |
| `candidate_trm` | TRM 후보자 인덱싱 | `offercent-search/indexer/.../CandidateTrmDocument.kt` |

**ATS 지원자용 인덱스는 존재하지 않음.**

#### 신규 필요

| 인덱스 | 용도 | 포함 필드 |
|--------|------|-----------|
| `applicant_ats` (신규) | ATS 지원자 전문검색 + 패싯 필터 | 지원자 인적사항, 학력, 경력, 자격증, 외국어, 제출서류 텍스트, 태그, 사전질문 응답, 자기소개서 파싱 텍스트, AI 스크리닝 점수 등 |

#### CDC 파이프라인 확장 필요

현재 `offercent-search`의 CDC 경로:

```
ATS DB (MySQL) 변경
  -> cdc-router (Debezium CDC)
    -> Kafka (IndexingRequestEvent)
      -> indexer (CandidateIndexingRequestListener)
        -> OpenSearch (candidate_trm)
```

CandidateTableSource 현재 지원 테이블:
- Candidates, TagsOnCandidate, ProjectOnCandidate, CompaniesOnCandidate, Educations, CandidateSkill, SubEmails, CandidateAccessMember, CandidateAccessUserGroup, UserGroupMember, CandidateReviewResult, Feeds, CertificationsOrExam, CandidateResume

**ATS 지원자(Applicants) 테이블의 CDC는 현재 미지원. 신규 파이프라인 구축 필요.**

---

## 3. FE/Aggregator 영향 분석

### 3-1. greeting-aggregator

현재 관련 InputPort:
- `ApplicantInputPort` - 지원자 CRUD
- `GetCandidateDetailInputPort` - 지원자 상세 조회
- `AiScreeningInputPort` - AI 서류평가
- `OpenApiApplicantQueryInputPort` - OpenAPI 지원자 조회

#### 수정/신규 필요 API

| API | 변경 내용 | 신규/수정 |
|-----|-----------|-----------|
| GET 지원자 리스트 | 필터 파라미터 25+ 추가, 응답에 동적 컬럼 포함 | 수정 |
| GET 지원자 칸반 | 필터 파라미터 확장 | 수정 |
| GET 패싯 카운트 | 필터별 결과 수 반환 | **신규** |
| POST 필터 저장 | 필터 조건 저장 | **신규** |
| GET 저장된 필터 목록 | 저장된 필터 조회 | **신규** |
| DELETE 저장된 필터 | 저장된 필터 삭제 | **신규** |
| PUT 리스트뷰 설정 | 커스텀 노출 항목 저장 | **신규** |
| GET 리스트뷰 설정 | 커스텀 설정 조회 | **신규** |
| POST 스크리닝 규칙 | 스크리닝 조건/점수 설정 | **신규** |
| PUT 스크리닝 규칙 | 스크리닝 조건 수정 | **신규** |
| POST 스크리닝 실행 | 규칙 기반 자동 점수/태그 부여 | **신규** |
| GET 스크리닝 결과 | 지원자별 스크리닝 점수 조회 | **신규** |

### 3-2. next-greeting (FE)

관련 앱: `greeting-forms` (ATS 메인)

#### 주요 변경 영역

| 영역 | 현재 상태 | 변경 필요 |
|------|-----------|-----------|
| 필터 UI | 점진적 노출 (필터 아이콘 > 하나씩 추가) | 기본 노출 패싯 UI로 전면 교체 |
| 필터 항목 | 제한된 항목 | 40+ 항목으로 확장 |
| 검색 | 이름/이메일/전화번호 | 전문 텍스트 검색으로 교체 |
| 리스트뷰 | 고정 컬럼 | 커스텀 컬럼 설정 UI 추가 |
| 칸반뷰 | 제한된 정보 | 필터 적용 항목 동적 표시 |
| 정렬 | 제한된 항목 | 정렬 항목 확대 + 전체 테이블 헤더 정렬 |
| 스크리닝 | 없음 | 스크리닝 패널 전체 신규 |

---

## 4. 아키텍처 의사결정 포인트

### 4-1. 검색/필터 엔진 선택 (가장 중요한 결정)

| 옵션 | 장점 | 단점 | 권장 |
|------|------|------|------|
| **A) MySQL QueryDSL 유지 + 확장** | 기존 코드 활용, 트랜잭션 일관성 | 전문검색 불가, 패싯 카운트 비효율, JOIN 폭증 | X |
| **B) ATS 지원자용 OpenSearch 인덱스 신규** | 전문검색/패싯/성능 모두 해결, TRM과 동일 패턴 | 인덱스 설계/CDC 구축 비용, 데이터 동기화 지연 | **O** |
| **C) 하이브리드 (필터는 MySQL, 검색은 OpenSearch)** | 단계적 전환 가능 | 두 시스템 유지 복잡, 검색+필터 결합 시 비효율 | 단계적 전략으로 고려 |

**권장: 옵션 B**. 40+ 필터 + 전문검색 + 패싯 카운트를 MySQL만으로 처리하면 `JPAQueryApplicantFilterExtension.kt`의 JOIN 수가 기하급수적으로 증가하고, 5000+ 지원자 공고에서 성능 이슈가 불가피.

### 4-2. 스크리닝 아키텍처

| 옵션 | 설명 |
|------|------|
| **A) 동기 처리** | 규칙 저장 즉시 모든 지원자에게 점수 계산 |
| **B) 비동기 이벤트** | 규칙 저장 -> 이벤트 발행 -> 배치/비동기 점수 계산 |

**권장: 옵션 B**. 수천 명 지원자 일괄 스크리닝은 비동기 처리가 적합. 기존 `AiScreeningRequestProcessor` 패턴 참고.

### 4-3. 단계별 구현 제안

| 단계 | 범위 | 예상 복잡도 |
|------|------|-------------|
| **Phase 1** | 기존 MySQL 필터 항목 확장 (신규 25개 필터 추가) + 정렬 추가 | 중 |
| **Phase 2** | ATS OpenSearch 인덱스 구축 + 전문검색 전환 + 패싯 카운트 | 상 |
| **Phase 3** | 필터 저장 + 리스트뷰 커스텀 | 중 |
| **Phase 4** | 스크리닝 규칙/점수/자동판정 | 상 |

---

## 5. 관련 파일 인덱스

### greeting-new-back (BE)
- `domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/AtsApplicantFilter.kt`
- `domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/AtsApplicantSort.kt`
- `domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/JPAQueryApplicantFilterExtension.kt`
- `domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/QApplicantsRepositoryImpl.kt`
- `domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/ApplicantsReader.kt`
- `domain/src/main/kotlin/doodlin/greeting/candidate/application/entity/ApplicantEntity.kt`
- `domain/src/main/kotlin/doodlin/greeting/evaluation/ai/service/AiScreeningFacade.kt`
- `interfaces/src/main/kotlin/doodlin/greeting/candidate/controller/AtsGetApplicantsSortEnum.kt`

### offercent-search (OpenSearch)
- `common-opensearch/src/main/kotlin/com/doodlin/search/opensearch/OpenSearchIndex.kt`
- `api/src/main/kotlin/com/doodlin/search/domain/candidate/api/request/SearchCandidateFilter.kt`
- `api/src/main/kotlin/com/doodlin/search/domain/candidate/infrastructure/persistence/query/CandidateQueryBuilder.kt`
- `api/src/main/kotlin/com/doodlin/search/domain/candidate/infrastructure/persistence/sort/CandidateSortBuilder.kt`
- `indexer/src/main/kotlin/com.doodlin.search.indexer/application/model/CandidateTrmDocument.kt`
- `common/src/main/kotlin/com/doodlin/search/common/cdc/CandidateTableSource.kt`
- `api/src/main/resources/elasticsearch/indices/candidate-embedding-index.json`

### greeting-aggregator
- `business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/candidate/port/in/ApplicantInputPort.kt`
- `business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/candidate/port/in/AiScreeningInputPort.kt`

### greeting-db-schema
- `sql/ats/` (Flyway 마이그레이션)
