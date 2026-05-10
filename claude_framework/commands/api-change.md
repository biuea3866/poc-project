---
description: API 변경 영향 분석 — 호환성 전략 + 마이그레이션 가이드
argument-hint: <간단 설명 또는 대상>
---

# /api-change — 절차

**대상**: $ARGUMENTS


외부에 공개된 API 의 변경 영향 분석.

## 진입점

- 사용자: `/api-change <명세 변경>` 또는 release pipeline 의 하위 단계

## 단계

### 1. 변경 분류
- 추가 (additive, 안전)
- 변경 (request/response 필드 의미·타입 변경 — breaking 가능)
- 제거 (breaking)
- 헤더/인증 방식 변경
- 에러 응답 코드 변경

### 2. 호출자 식별
- 내부 (다른 BC, FE) — 코드 grep
- 외부 (파트너, 공개 SDK) — 사용 통계
- 모바일 앱 (강제 업데이트 어려움)

### 3. 호환성 전략
| 케이스 | 전략 |
|--------|------|
| additive | 기존 호출자 영향 없음, 즉시 배포 |
| optional → required | deprecation cycle 1릴리스 + 경고 헤더 |
| 제거 | deprecation cycle 2릴리스 + 410 Gone 응답 |
| 의미 변경 | 신규 엔드포인트 + 기존 유지 → 점진 이관 |

### 4. 명세 갱신
- OpenAPI/Swagger 스키마 업데이트
- BFF 영향 검토 (`feedback/bff_facade` 룰)
- 클라이언트 SDK 자동 생성 갱신

### 5. 마이그레이션 가이드
- 호출자용 변경 안내 (예시 코드)
- 데드라인 명시
- 롤백 절차

### 6. 모니터링
- 구버전/신버전 호출 비율 대시보드
- deprecation 헤더 응답률
- 에러율 추세

## 산출물

- `outputs/api-change/<change-id>/`
  - `compat-analysis.md`
  - `migration-guide.md`
  - `monitoring-plan.md`

## 안전 장치

- breaking 변경은 release pipeline 의 deprecation cycle 강제
- BFF Facade 우회 호출 금지 (FE 룰)
- `@Deprecated` 어노테이션 + 로깅으로 호출 감지

## 참고

- 배포 절차: `commands/release.md`
- BFF 룰: harness-rules `feedback/bff_facade`

## 완료 단언 규칙

> "완료/검증 끝" 같은 단언은 [`rules/COMPLETION-RULE.md`](../COMPLETION-RULE.md) 의 §1~4 (강제 산출물 / 검증 아티팩트 / 도구 호출 선행 / "지금 시작" 단언 금지) 를 모두 충족해야 한다. 충족 안 된 항목이 있으면 `in-progress` 로 보고.
