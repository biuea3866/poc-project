---
description: 보안 전용 PR 리뷰 — OWASP / 인증·인가 / 시크릿 / 입력 검증 / SSRF / CSRF
argument-hint: <PR number or URL>
---

`security-reviewer` 에이전트로 PR 의 보안 측면만 집중 검토해줘.

**PR**: $ARGUMENTS

## 절차

### 1. 사전 검증
- `python3 .claude/scripts/senior-gate.py --pr $ARGUMENTS` 의 admin/RoleRequired 체크 결과 먼저 확인
- 변경 파일 목록 (`gh pr diff $ARGUMENTS --name-only`)

### 2. 점검 항목 (security-reviewer 의 7가지)
1. **인증·인가** — 관리자 엔드포인트 `@RoleRequired`/`@PreAuthorize`/`@Secured` 누락 / TOCTOU / `alg=none`
2. **입력 검증·SQLi/XSS** — raw SQL 보간 / path traversal / `innerHTML` / `eval`
3. **시크릿·민감정보** — 토큰 하드코딩 / 로그 PII 출력 / 스택 트레이스 노출
4. **CSRF·CORS·헤더** — CSRF 비활성 + cookie / `Allow-Origin: *` + `Allow-Credentials: true` / Secure/HttpOnly/SameSite 누락
5. **외부 호출·SSRF** — 사용자 입력 URL 직접 전달 / 메타데이터 엔드포인트 접근
6. **의존성·구성** — CVE / Dockerfile USER root / 불필요 포트
7. **Frontend 특화** — BFF Facade 우회 / localStorage 토큰 / target=\"_blank\" + rel=\"noopener\" 누락

### 3. 분류 및 verdict
- **Critical** 1건 이상 → `gh pr review --request-changes` + workflow fail
- **Major** 만 → `gh pr review --request-changes`
- **Minor** 만 → `gh pr review --comment`
- 발견 없음 → `gh pr review --approve` (단, 사람 머지)

### 4. 코멘트 형식
```
## 🔒 Security Review
**Verdict**: <verdict>

### Critical
- [`<file>:<line>`] <항목명> — <문제 + 권장 수정>

### Major
- ...

### Minor
- ...

### 검증한 항목
1. 인증·인가 ✓
2. ...
```

## 운영 규칙

- 추측성 "보안 위험 있을 수 있음" 금지 — 정확한 파일/라인 + 패턴 + 근거
- 직접 코드 수정 금지 (리뷰 코멘트만)
- approve 결정은 Critical 패턴 매칭 결과로 강제 (LLM 단독 판단 금지)

## 참고

- 에이전트: `agents/security-reviewer.md`
- 룰: `harness-rules.json` (`security_*` 룰 + `git_guard`)
- 페어 리뷰어: `pr-reviewer` / `be-senior` / `be-tech-lead` / `fe-lead`
