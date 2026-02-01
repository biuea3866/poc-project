# 위키

## 0. 목표

---
사용자는 기록하고, 정리는 AI가 할 수 있습니다.

## 1. 요구 사항

---

## 1-1. 우선순위 (즉시 해결 vs 이후 로드맵)

### 지금 당장 해결 (MVP 필수)
1) **상태 모델 정리**
   - `document.status`는 문서 생명주기(예: ACTIVE/DELETED)로 한정
   - AI 처리 상태는 별도 필드(`ai_status`)로 분리: PENDING/PROCESSING/COMPLETED/FAILED
2) **버전 관리 범위 명확화**
   - `document_revision.data`에 포함되는 필드 정의 (title, content, tags, summary 여부)
3) **태그/요약 스키마 정합성**
   - 태그는 전역(`tag`)으로 관리하고 문서-태그는 매핑 테이블(`document_tag_map`)로 관리
   - `document_summary`는 1:1 유지
4) **삭제 정책 정의**
   - 소프트 삭제 기준 및 조회 기본 필터(삭제 제외) 규칙 명시
5) **AI 파이프라인 기본 규칙**
   - 이벤트 발행/소비 순서, 재시도/중복 처리(idempotency) 기본 룰 정리
6) **SSE 규격 확정**
   - `/ai-status/stream` 이벤트 포맷, heartbeat, 재연결 정책 정의

### 앞으로 천천히 해결 (로드맵)
1) **권한/공유 모델**
   - 팀/조직/공유 링크, ACL 정책 수립
2) **검색 고도화**
   - MySQL 풀텍스트 vs 외부 검색엔진 결정
   - 통합 검색 랭킹/스코어링 정책
3) **임베딩 버전/차원 관리**
   - 모델별 차원/버전 관리 전략, 재인덱싱 정책
4) **운영/보안 강화**
   - Refresh 토큰 저장/회전 정책
   - 로그 보관 기간, 감사 추적, 비용 모니터링
5) **협업 기능**
   - 코멘트, 변경 이력 비교(diff), 알림/웹훅
6) **멀티 에이전트 확장**
   - RAG/검색 에이전트 플러그인화, 정책 기반 실행

A. 글 및 지식 관리
* Markdown 지원: 사용자는 마크다운 형식으로 글을 작성할 수 있습니다.
* 버전 관리: AI가 요약을 갱신할 때 원본의 변경 이력을 추적할 수 있습니다.
* 상태 추적: 글별로 AI 처리 상태를 확인할 수 있습니다.
* 계층형 관리: 계층형으로 글들을 관리할 수 있습니다.
* 검색: 태그, 글 내용(like 검색), 제목 다양한 키워드를 통해서 원하는 글들을 찾을 수 있습니다.

B. AI 지능형 기능
* 자동 요약: 메모 저장 시 백그라운데어서 AI가 핵심 내용을 3줄 이내로 요약합니다.
* 자동 태킹: 본문 내용을 분석하여 적절한 기술 스택이나 주제 키워드를 태그로 추출합니다.
* 검색: 사용자가 글을 작성하다 원하는 키워드로 검색할 경우 관련된 지식을 외부 웹사이트로부터 검색하여 사용자에게 제공합니다.
* RAG: DB에 여러 에이전트가 접근하여 업데이트 기록을 계속 남기어 문서 임베딩 기반 유사도 검색 → LLM 컨텍스트 주입합니다. 


## 2. ERD

---

### mysql
* user: 유저
  * id: pk long not null auto_increment
  * email: varchar(255) unique "로그인 email"
  * password: varchar(255) not null "암호화된 비밀번호"
  * name: varchar(100) not null "유저 이름"
  * created_at: datetime not null "생성 일시"
  * updated_at: datetime not null "수정 일시"
  * deleted_at: datetime "삭제 일시"
  * index_list
    * email

* document: 글
  * id: pk long auto_increment
  * title: varchar(255) not null "제목"
  * content: text "글 내용"
  * status: varchar(20) not null '상태(PENDING, COMPLETED, FAILED, DELETED)'
  * parent_id: long
  * created_at: datetime not null '생성 일시'
  * updated_at: datetime not null '수정 일시'
  * deleted_at: datetime '삭제 일시'
  * created_by: long not null '생성 유저 id'
  * updated_by: long not null '수정 유저 Id'
  * index list 
    * parent_id
    * id, created_at

* document_revision: 글 개정 정보
  * id: pk long auto_increment
  * data: text not null "document 전체 정보 json"
  * document_id: long not null "글 id"
  * created_at: datetime not null '생성 일시'
  * created_by: long not null '생성 유저 id'
  * id description
    * document_id: document와 1:n 관계를 위한 id
  * index list
    * document_id
    * created_by

* tag
  * id: pk long auto_increment
  * name: varchar(100) unique "전역 태그 이름"
  * created_at: datetime not null '생성 일시'
  * index list
    * name

* document_tag_map
  * id: pk long auto_increment
  * tag_id: long not null "tag id"
  * document_id: long not null "글 id"
  * document_revision_id: long "글 개정 정보 id (옵션)"
  * created_at: datetime not null '생성 일시'
  * id description
    * document_id: document와 1:n 관계
    * tag_id: tag와 1:n 관계
  * index list
    * document_id
    * tag_id
    * document_id, tag_id (unique)

* document_summary
  * id: pk long auto_increment
  * content: text not null "ai 요약 정보"
  * document_revision_id: long not null "글 개정 정보 id"
  * document_id: long not null "글 id"
  * id description
    * document_id: document와 1:n 관계
    * document_revision_id: document_revision과 1:n 관계
    * document_id, document_revision_id: document와 1:1 관계
  * index list
    * document_id, document_revision_id
  
* ai_agent_log
  * id: pk long auto_increment
  * agent_type: varchar(50) not null "에이전트 타입 (SUMMARY, TAGGER, WEB_SEARCH, RAG)"
  * status: varchar(30) not null "성공 / 실패 여부(SUCCESS, FAILURE)"
  * action_detail: text not null "수행 내용 (예: 외부 웹 검색 수행)"
  * reference_data: text "참고한 url 목록이나 원본 소스"
  * document_revision_id: long not null "글 개정 정보 id"
  * document_id: long not null "글 id"
  * executor_id: long not null "요청 user id"
  * created_at: datetime not null "생성 시점"
  * id description
    * document_id: document와 1:n 관계
    * document_revision_id: document_revision과 1:n 관계
    * document_id, document_revision_id: document와 1:n 관계
  * index list
      * document_id, document_revision_id
      * executor_id

### postgre (AI Knowlegde & RAG)
* document_embeddings (벡터 지식 저장소)
  * id: pk bigserial "임베딩 고유 id"
  * document_id: long not null "mysql 문서 id와 매핑"
  * document_revision_id: long not null "mysql 문서 개정 id와 매핑"
  * embedding: vector(1536) index: HNSW "OpenAPI text-embedding-3-small 기준 claude, gemini도 사용"
  * chunk_content: text not null "검색 시 LLM에 주입할 실제 텍스트 조각"
  * token_count: int "비용 계산을 위한 토큰 수"
  * metadata: jsonb "태그, 카테고리 등 필터링용 메타 정보"

## 3. API 명세

A. Auth API (인증)

|Method|Endpoint|Description|
|--|--|--|
|POST|/api/v1/auth/signup|회원가입. email, password, name 전달|
|POST|/api/v1/auth/login|로그인. JWT 토큰(access, refresh) 반환|
|POST|/api/v1/auth/refresh|Refresh 토큰으로 Access 토큰 재발급|

B. Document API (글 및 계층 관리)

|Method|Endpoint|Description|
|--|--|--|
|GET|/api/v1/documents|전체 트리 구조 또는 특정 레벨 목록 조회. 쿼리 파라미터: `page`, `size`, `parentId`|
|POST|/api/v1/documents|새 글 작성 (Markdown). parent_id 전달 시 하위 문서로 생성|
|GET|/api/v1/documents/{id}|특정 글의 상세 내용, AI 요약, 태그 정보 반환|
|PUT|/api/v1/documents/{id}|글 수정. 기존 내용은 document_revision 테이블로 아카이빙|
|DELETE|/api/v1/documents/{id}|글 소프트 삭제. status를 DELETED로 변경하고 deleted_at 기록|
|GET|/api/v1/documents/{id}/revisions|해당 글의 변경 이력 목록 조회. 쿼리 파라미터: `page`, `size`|
|GET|/api/v1/documents/{id}/tags|해당 글의 태그 목록 조회|

C. AI & Search API (지능형 기능)

|Method|Endpoint|Description|
|--|--|--|
|GET|/api/v1/search/integrated|제목/내용 LIKE 검색 + AI 의미 기반 검색 통합 결과. 쿼리 파라미터: `q`, `page`, `size`|
|GET|/api/v1/search/web|외부 웹 사이트 실시간 검색 (Perplexity 스타일). 쿼리 파라미터: `q`|
|POST|/api/v1/documents/{id}/analyze|AI에게 재요약 및 태깅 수동 요청|
|GET|/api/v1/documents/{id}/ai-status|해당 글의 AI 처리 상태 조회 (Polling용). 응답: status(PENDING, PROCESSING, COMPLETED, FAILED)|
|GET|/api/v1/documents/{id}/ai-status/stream|해당 글의 AI 처리 상태 실시간 스트림 (SSE)|
|GET|/api/v1/ai/logs|에이전트들이 남긴 업데이트 및 참조 기록 확인. 쿼리 파라미터: `documentId`, `agentType`, `page`, `size`|

## 4. FE

이 프롬프트는 UI/UX 구성뿐만 아니라 백엔드의 비동기 AI 처리 상태를 핸들링하는 데 초점을 맞춥니다.

> **[Prompt for FE AI Agent]**
> **주제:** AI 기반 계층형 지식 위키 (AI Wiki) 프론트엔드 구축
> **기술 스택:** Next.js (App Router), TypeScript, Tailwind CSS, React Query, Lucide-react
> **핵심 요구사항:**
> 1. **Auth:** JWT 기반 로그인/회원가입. `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` API 연동. 모든 API 요청 헤더에 Bearer 토큰 삽입 및 Route Guard 설정. Access 토큰 만료 시 Refresh 토큰으로 자동 재발급.
> 2. **계층형 사이드바:** `GET /api/v1/documents`로 트리 데이터를 조회하고, `parent_id` 구조를 재귀적으로 렌더링하는 트리 메뉴. 폴더 접기/펴기 및 글 선택 기능.
> 3. **Markdown 에디터:** `react-markdown` 또는 `Milkdown` 등을 활용한 편집기. 툴바 및 실시간 미리보기 지원.
> 4. **AI 상태 관리:**
>    - 글 저장(`POST /api/v1/documents`) 시 백엔드는 즉시 응답하되 AI는 비동기로 동작함.
>    - `GET /api/v1/documents/{id}/ai-status` (Polling) 또는 `GET /api/v1/documents/{id}/ai-status/stream` (SSE)으로 상태를 실시간 확인.
>    - 상태값: `PENDING`(대기) → `PROCESSING`(처리중) → `COMPLETED`(완료) / `FAILED`(실패).
>    - 요약본과 태그 영역에 상태별 UI 분기: PENDING/PROCESSING → 스켈레톤 UI, COMPLETED → 요약 텍스트 및 태그 노출, FAILED → 재시도 버튼.
> 5. **태그 표시:** 글 상세 페이지에서 `GET /api/v1/documents/{id}/tags`로 자동 추출된 태그를 뱃지 형태로 표시.
> 6. **검색 인터페이스:** 통합 검색창 제공. `GET /api/v1/search/integrated`로 '키워드 검색 결과'와 'AI 추천(RAG) 결과'를 탭이나 섹션으로 구분하여 표시. 페이지네이션 지원.
> 7. **버전 관리 UI:** 글 상세 페이지에서 `GET /api/v1/documents/{id}/revisions`로 과거 버전 목록을 확인하고, 선택 시 해당 내용을 모달로 띄우는 기능.
> 8. **글 삭제:** `DELETE /api/v1/documents/{id}`를 통한 소프트 삭제. 삭제 전 확인 다이얼로그 표시.
>
> **디자인 컨셉:** Notion이나 Linear 스타일의 깔끔하고 다크모드를 지원하는 모던한 UI를 생성해줘.

---

## 5. DevOps

> **[Prompt for DevOps AI Agent]**
> **주제:** 다중 DB(MySQL, Postgres) 및 메시지 브로커(Kafka)를 포함한 AI 서비스 인프라 구축
> **환경:** Docker Compose (로컬 개발 환경 타겟)
> **상세 구성 요구사항:**
> 1. **MySQL 8.0:** 서비스 메타데이터 및 유저 정보 저장용. ERD 기반 초기 스키마 포함. 테이블: `user`, `document`, `document_revision`, `tag`, `document_tag_map`, `document_summary`, `ai_agent_log`.
> 2. **PostgreSQL 16 + pgvector:** 벡터 데이터 저장용.
>    - `ankane/pgvector` 이미지를 사용하여 컨테이너 실행 시 자동으로 `CREATE EXTENSION vector;`가 수행되도록 설정.
>    - 테이블: `document_embeddings`. HNSW 인덱스 포함.
> 3. **Apache Kafka (KRaft 모드):**
>    - 백엔드에서 발행한 이벤트를 AI 워커가 소비하기 위한 브로커.
>    - Topic 자동 생성: `document-created` (글 생성/수정 시 발행), `ai-summary-finished` (요약 완료), `ai-tagging-finished` (태깅 완료), `ai-embedding-finished` (임베딩 완료), `ai-processing-failed` (처리 실패).
> 4. **Redpanda Console (Optional):** Kafka 메시지 모니터링을 위한 웹 UI 추가.
> 5. **Network & Persistence:** 모든 DB와 브로커의 데이터는 Docker Volume에 영구 저장되어야 하며, 각 서비스는 내부 네트워크로 통신하도록 구성.
> 6. **Spring Boot 연동:** `application.yml`에서 참조할 수 있는 환경변수(`.env` 파일 형식) 예시도 함께 작성해줘.
>
> 위 인프라를 한 번에 띄울 수 있는 `docker-compose.yml`과 초기 SQL 스크립트를 생성해줘.
