# PR Review Server 구현 프롬프트

> 이 프롬프트를 Claude Code에 전달하면 동일한 PR Review Server를 처음부터 구축할 수 있습니다.

---

## 프롬프트

```
PR이 올라오면 자동으로 리뷰를 생성하고, 내가 로컬 대시보드에서 검증한 후 GitHub에 게시하는 로컬 서버를 만들어줘.

## 기술 스택
- TypeScript + Express (Node.js)
- GitHub API: @octokit/rest, @octokit/webhooks
- 로컬 전용 (127.0.0.1 바인딩)

## 핵심 파이프라인

### 1. Webhook 수신
- GitHub `pull_request` 이벤트 (opened/synchronize/reopened) 수신
- HMAC-SHA256 서명 검증 (WEBHOOK_SECRET)
- webhook payload에서 owner/repo를 동적 추출 (멀티 repo 지원)

### 2. PR 분석 (자동)
- GitHub API로 PR diff + 변경 파일 목록 fetch
- PR 유형 자동 분류 (브랜치명 + 변경 패턴 기반):
  - simple, feature, refactor, bugfix, shared, config, docs
- 유형에 따라 분석기 자동 할당:
  - 📐 영향 범위 분석 (Impact): PR 크기, 아키텍처 위반, 동반 파일 누락
  - 🧪 코드 품질 분석 (Quality): 보안, 버그, 성능, 테스트

### 3. 심각도 분류 (P1~P5 + ASK)
인라인 코멘트와 요약에 다음 태그를 사용:

- [P1] 즉시 수정 — 런타임 에러, 데이터 손상, 보안 취약점
- [P2] 반드시 수정 — 심각한 성능 저하, 계층 위반, 동시성 이슈
- [P3] 수정 권장 — 테스트 누락, 디버그 로그, 빈 catch
- [P4] 개선 제안 — 가독성, 네이밍, 중복 코드
- [P5] 사소한 개선 — 포매팅, import 정리, 오타
- [ASK] 질문 — 의도 확인이 필요한 코드

P1~P2 → REQUEST_CHANGES, P3 → COMMENT, P4~P5+ASK만 → APPROVE

### 4. 로컬 대시보드 (Web UI)
- GitHub 스타일 dark theme
- diff 뷰어에 인라인 코멘트 표시 (코드 바로 아래에 thread)
- 라인 번호 드래그로 멀티라인 선택 → 코멘트 작성 (GitHub처럼)
- `+` 버튼 클릭으로 단일 라인 코멘트
- 자동 생성 코멘트(Auto)와 사용자 코멘트(You) 배지 구분
- Review Body 수정 가능
- Event 변경 가능 (APPROVE/COMMENT/REQUEST_CHANGES)
- SSE로 새 리뷰 실시간 알림

### 5. GitHub 게시
- 대시보드에서 "Post Review to GitHub" 클릭 시에만 게시
- 자동+수동 코멘트 합쳐서 `pulls.createReview` API 호출
- 멀티라인 코멘트는 `start_line` 파라미터 사용
- 자기 PR에 APPROVE 시 자동으로 COMMENT fallback

## 프로젝트별 설정 (review.config.json)
아키텍처 패턴(hexagonal/layered/clean), 언어별 분석 규칙, 크로스 repo 정의,
필수 동반 파일, 무시 경로를 설정 파일로 커스터마이징 가능하게.

## 환경 변수 (.env)
- GITHUB_TOKEN: GitHub PAT (pull_requests:write, contents:read)
- WEBHOOK_SECRET: openssl rand -hex 32로 생성
- PORT: 기본 3847

## 디렉토리 구조
src/
  index.ts              # Express 진입점
  config.ts             # 환경변수 로딩
  webhook/
    handler.ts          # Webhook 수신 + 서명 검증
    parser.ts           # payload에서 PR 정보 추출
  github/
    client.ts           # Octokit 인스턴스
    diff.ts             # PR diff/files fetch
    reviewer.ts         # GitHub review 게시
  review/
    types.ts            # 타입 정의 (InlineComment에 startLine 포함)
    store.ts            # In-memory review 저장소
    config.ts           # review.config.json 로더
    classifier.ts       # PR 유형 분류 + 분석기 할당
    generator.ts        # 4단계 파이프라인 (scope→impact→quality→summary)
  ui/
    routes.ts           # REST API + SSE
    static/
      index.html        # 대시보드 HTML
      styles.css        # GitHub dark theme CSS
      app.js            # 프론트엔드 로직 (드래그 선택, 코멘트 관리)

## Webhook 프록시 (로컬 개발)
smee.io 사용: npm run webhook:proxy
```
