---
name: pr-create
description: 현재 브랜치의 PR을 생성하고 Draft→Ready 전환, 머지 후 Jira 전이까지 처리한다. Jira 티켓 번호를 인수로 받는다.
model: sonnet
user-invocable: true
---

대상: $ARGUMENTS (<TICKET-ID> 티켓 번호)

현재 브랜치: !`git branch --show-current`
현재 날짜: !`date +%Y-%m-%d`

---

## Step 1 — 사전 확인

```bash
git status
git log origin/dev..HEAD --oneline
```

커밋이 없거나 origin/dev와 동일하면 중단한다.

---

## Step 2 — PR 생성

`.github/pull_request_template.md`가 있으면 그대로 본문으로 사용한다:

```bash
gh pr create \
  --title "[<TICKET-ID>] - {type} : {제목}" \
  --body "$(cat .github/pull_request_template.md)" \
  --base dev \
  --draft
```

없으면 아래 표준 템플릿을 사용한다:

```markdown
### 개요
<!-- Jira 티켓 링크 포함 -->

- Jira: https://<COMPANY>.atlassian.net/browse/<TICKET-ID>
- (한 줄 설명)

### 작업 내용
<!-- no-jira 작업의 경우 내용 필수 -->
<!-- Jira 본문에 내용이 있다면 간략하게 작성 -->

- (핵심 변경사항 bullet)

### 체크리스트
<!-- 필요에 따라 항목 추가/수정/삭제 -->
- [ ] 빌드 확인
- [ ] 테스트 통과

### 연관된 backend application
<!-- 해당 PR 내용에 영향받는 backend application -->

### 추가 유의사항
<!-- 리뷰어나 배포 시 반드시 알아야 할 사항 -->
```

**PR 제목 type 선택**

| type | 용도 |
|------|------|
| `feat` | 신규 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 개선 |
| `chore` | 빌드·설정·의존성 변경 |
| `deploy` | 배포 관련 작업 |
| `db` | 스키마 마이그레이션 |
| `docs` | 문서 수정 |

---

## Step 3 — Draft → Ready 전환 (선택)

리뷰 준비가 완료되었을 때 실행한다:

```bash
gh pr ready <PR번호>
```

---

## Step 4 — 머지 후 Jira 전이 (선택)

PR 머지 후 Jira 상태를 배포 대기로 전이한다.

```bash
# transition ID 조회
curl -s "https://<COMPANY>.atlassian.net/rest/api/3/issue/<TICKET-ID>/transitions" \
  -u "<USER>@<COMPANY>.com:${ATLASSIAN_API_TOKEN}" \
  | python3 -c "import json,sys; [print(t['id'], t['name']) for t in json.load(sys.stdin)['transitions']]"

# 전이 실행
curl -s -X POST \
  "https://<COMPANY>.atlassian.net/rest/api/3/issue/<TICKET-ID>/transitions" \
  -u "<USER>@<COMPANY>.com:${ATLASSIAN_API_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"transition": {"id": "<ID>"}}'

# 로컬 브랜치 정리
git branch -d <브랜치명>
```

---

## 완료 보고

```
PR 생성 완료
URL: https://github.com/<COMPANY>/<PROJECT>/pull/XXX
상태: Draft (리뷰 준비 후 gh pr ready <번호>)
```
