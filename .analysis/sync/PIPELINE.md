# 코드 동기화 & 변경 추적 파이프라인

> 모든 분석 작업 전에 자동 실행되는 사전 동기화 프로세스

---

## 핵심 원칙

```
1. 매 분석 작업 시작 전, 관련 레포의 최신 코드를 pull
2. 전체 재분석이 아닌, "변경분(delta)"만 빠르게 파악
3. 기존 분석 보고서(PROJECT_ANALYSIS_REPORT.md)는 "기반 지식"으로 유지
4. 변경분은 그 위에 레이어처럼 얹어서 최신 상태 인식
```

---

## 자동 실행 흐름

```
[사용자 요청 접수]
    │
    ▼
━━ Step 0: 코드 동기화 (모든 파이프라인 공통 사전 작업) ━━
    │
    │  1) 관련 레포만 선별 (요청 내용 기반)
    │  2) git fetch + pull (해당 레포만)
    │  3) 마지막 분석 이후 변경 사항 확인 (git log/diff)
    │  4) 변경이 분석에 영향을 주는지 빠르게 판단
    │
    ▼
[해당 파이프라인 Phase 1 진입]
```

---

## 동기화 전략: 3단계

### Level 1: 타겟 동기화 (매 요청마다)

**모든 분석 요청 시 자동 수행.** 요청과 관련된 레포만 pull.

```bash
# 예: 지원자 관련 문의 → 관련 레포만 동기화
cd greeting-new-back && git pull --ff-only
cd greeting-ats && git pull --ff-only
cd greeting_front && git pull --ff-only
```

**변경 확인:**
```bash
# 마지막 분석 시점(2026-03-16) 이후 변경 사항
git log --oneline --since="2026-03-16"
git diff HEAD@{2026-03-16}..HEAD --stat
```

**판단 기준:**
- 변경 파일이 분석 대상과 관련 있으면 → 해당 부분만 추가 분석
- 변경 파일이 무관하면 → 기존 보고서 기반으로 진행

### Level 2: 전체 동기화 (주기적 / 요청 시)

**사용자가 "전체 동기화해줘" 또는 대규모 분석 전에 수행.**

```bash
# 모든 레포 최신화
for dir in */; do
  cd "$dir" && git fetch --all && git pull --ff-only && cd ..
done
```

**전체 변경 요약 생성:**
- 각 레포별 최근 변경 커밋 수
- 주요 변경 파일 목록
- 새로 추가된 API/Kafka 토픽/DB 마이그레이션

### Level 3: 기반 보고서 갱신 (월 1회 또는 대규모 변경 후)

**PROJECT_ANALYSIS_REPORT.md 자체를 재생성.**
- 7개 에이전트 재실행 (초기 분석과 동일)
- 변경 누적이 많아 delta 추적으로 불충분할 때

---

## 변경 감지 자동화 스크립트

매 분석 시작 시 팀장이 실행하는 체크:

```bash
# 1. 관련 레포 동기화
cd /Users/biuea/doodlin_workspace/{repo}
git fetch --all --quiet
git pull --ff-only 2>/dev/null || echo "WARN: needs manual merge"

# 2. 최근 변경 확인 (마지막 분석 이후)
git log --oneline --after="YYYY-MM-DD" --no-merges

# 3. 변경 파일 목록
git diff HEAD@{YYYY-MM-DD}..HEAD --name-only 2>/dev/null \
  || git log --after="YYYY-MM-DD" --name-only --pretty=format:"" | sort -u

# 4. 핵심 변경 탐지 (API, 도메인, 설정)
git log --after="YYYY-MM-DD" --name-only --pretty=format:"" | \
  grep -E "(Controller|Service|Domain|Entity|application\.yml|KafkaTopics|routes)" | \
  sort -u
```

---

## 변경 영향 빠른 판단 기준

| 변경 파일 패턴 | 영향 | 필요 조치 |
|---------------|------|----------|
| `*Controller.kt` | API 변경 가능 | FE 호출부 재확인 |
| `*Service.kt`, `*InputPort.kt` | 비즈니스 로직 변경 | 도메인 흐름 재추적 |
| `*Entity.kt`, `*.sql` | DB 스키마 변경 | 마이그레이션 확인 |
| `KafkaTopics.kt`, `*Consumer.kt` | 이벤트 흐름 변경 | Producer/Consumer 재확인 |
| `application*.yml` | 설정 변경 | 환경별 차이 확인 |
| `routes/*.yaml` | Gateway 라우팅 변경 | FE 호출 경로 확인 |
| `package.json` | 의존성 변경 | 버전 호환성 확인 |
| `src/api/*` | FE API 호출 변경 | BE 엔드포인트 확인 |
| `build.gradle.kts` | 빌드/의존성 변경 | 모듈 구조 확인 |

---

## 분석 시점 추적

`.analysis/sync/LAST_SYNC.md`에 마지막 동기화 시점 기록:

```markdown
# 마지막 동기화 기록

| 레포 | 마지막 동기화 | 마지막 커밋 해시 |
|------|-------------|----------------|
| greeting-new-back | 2026-03-16 | abc1234 |
| greeting-ats | 2026-03-16 | def5678 |
| ... | ... | ... |
```

---

## 실제 사용 예시

### 시나리오: "지원자 관련 버그 분석해줘"

```
Step 0 (자동):
  1. greeting-new-back, greeting-ats, greeting_front pull
  2. git log 확인 → 최근 5개 커밋에서 ApplicantService 수정 발견
  3. 해당 변경 내용 빠르게 파악
  4. 기존 보고서 + 최신 변경분 결합하여 분석 시작

Step 1~4 (문의 대응 파이프라인):
  → 최신 코드 기반으로 정확한 분석
```

### 시나리오: "전체 동기화해줘"

```
  1. 46개 레포 전체 git pull
  2. 각 레포별 변경 커밋 수 통계
  3. 주요 변경 사항 요약 생성
  4. LAST_SYNC.md 업데이트
```

---

## 출력 어조

산출물은 **팀원과 공유하는 문서**입니다. 읽는 사람이 지치지 않도록 아래 원칙을 따릅니다.

- **핵심부터** — 결론·액션 아이템을 앞에 씁니다. 이유와 배경은 뒤에 써도 됩니다.
- **짧게** — 한 문장으로 쓸 수 있으면 세 문장으로 쓰지 않습니다.
- **구체적으로** — "여러 곳" 대신 "3곳", "느릴 수 있음" 대신 "users 테이블 full scan" 처럼 씁니다.
- **표·불릿 우선** — 비교·목록은 문장보다 표나 불릿으로 씁니다.
- **중립적으로** — 문제를 발견해도 단정짓지 않고 확인 사항으로 전달합니다.

