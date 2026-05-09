# Inquiry / Bug Triage Pipeline

문의·버그 신고 대응 절차 (장애 직전 단계).

## 진입점

- 사용자: `/inquiry <설명>` 또는 main-orchestrator 호출

## 단계

### 1. 분류
- 사용자 문의 (어떻게 쓰는가) — `/help` 영역
- 동작 불일치 (의도와 다른 결과) — bug 후보
- 성능 (느림/타임아웃) — 부하/리소스
- 데이터 정합성 (값 이상) — 가장 위험

데이터 정합성 의심 → 즉시 incident pipeline 으로 에스컬레이션.

### 2. 재현 시도
- 사용자 환경 정보 수집 (계정, 시간, 요청 ID)
- 로컬/staging 재현
- 재현 안 되면: 로그·DB 스냅샷·세션 데이터 확인

### 3. 가설·검증
- 코드 변경 이력 (`git log -- <파일>`)
- 최근 배포 (release pipeline 결과)
- 외부 의존 상태 (3rd party 장애)

### 4. 응답·임시 조치
- 사용자 응답 초안 (사실 + 다음 단계)
- 데이터 보정 필요 시 SQL 작성 → 사람 승인 후 실행

### 5. 영구 fix
- 단순 fix → PR + `.analysis/pr-review/PIPELINE.md`
- 구조적 문제 → refactoring pipeline 으로 이관
- 룰로 막을 수 있었나? → harness-rules 보강 후보

### 6. 메타-피드백 트리거
- 같은 문의가 7일 내 N회 반복 → process-reviewer 발화
- 룰 누락이 원인이면 harness-rules 갱신 제안

## 산출물

- `.analysis/inquiry/<YYYYMMDD>-<topic>/`
  - `triage.md` (분류 + 재현 + 가설)
  - `response.md` (사용자 응답)
  - `fix-pr.md` (해결 PR 링크)

## 안전 장치

- 데이터 보정 SQL 은 사람 승인 + dry-run 우선
- 운영 DB 직접 수정 금지 (반드시 마이그레이션 또는 백오피스)
- 동일 문의 반복 시 자동 에스컬레이션

## 참고

- 장애 대응: `.analysis/incident/PIPELINE.md`
- 메타-피드백: `.analysis/feedback-loop/PIPELINE.md`
