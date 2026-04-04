# Greeting 분석 시스템

> 11개 파이프라인으로 모든 업무 유형에 체계적으로 대응

---

## 디렉토리 구조

```
.analysis/
├── README.md                          ← 이 파일 (시스템 가이드)
│
├── prd/                               ← 1. PRD 분석 (신규 피쳐, 기능 변경)
│   ├── PIPELINE.md
│   └── results/
│
├── inquiry/                           ← 2. 문의 대응 (버그, QA, CS)
│   ├── PIPELINE.md
│   └── results/
│
├── incident/                          ← 3. 장애 대응 (프로덕션 긴급)
│   ├── PIPELINE.md
│   └── results/
│
├── pr-review/                         ← 4. 코드 리뷰 (PR 영향 분석)
│   ├── PIPELINE.md
│   └── results/
│
├── release/                           ← 5. 배포 영향 분석 (릴리즈 전)
│   ├── PIPELINE.md
│   └── results/
│
├── refactoring/                       ← 6. 리팩토링/마이그레이션
│   ├── PIPELINE.md
│   └── results/
│
├── onboarding/                        ← 7. 온보딩 (도메인 설명)
│   ├── PIPELINE.md
│   └── results/
│
├── api-change/                        ← 8. API 스펙 변경 분석
│   ├── PIPELINE.md
│   └── results/
│
├── be-implementation/                 ← 9. BE 구현 설계 (TDD + 티켓)
│   ├── PIPELINE.md
│   └── results/
│
├── implementation/                    ← 10. 구현 (티켓 → 코드 → 테스트 → PR)
│   ├── PIPELINE.md
│   └── results/
│
├── verification/                      ← 11. 검증 (설계 원칙 + 아키텍처 + AC)
│   ├── PIPELINE.md
│   └── results/
│
├── agents/                            ← 에이전트 역할 정의
│   ├── README.md                      (33개 에이전트 인덱스)
│   ├── prd-*.md                       PRD 분석 에이전트 (9개)
│   ├── bench-*.md                     벤치마킹 에이전트 (2개)
│   ├── tech-*.md                      설계 분석 에이전트 (2개)
│   ├── ticket-splitter.md             티켓 분할 (통합)
│   ├── test-designer.md               테스트 설계
│   ├── review-*.md                    리뷰 에이전트 (3개)
│   ├── impl-*.md                      구현 에이전트 (3개)
│   ├── refactor-*.md                  리팩토링 에이전트 (5개)
│   ├── debt-*.md                      기술 부채 에이전트 (4개)
│   ├── cs-*.md                        CS 문의 에이전트 (4개)
│   └── pr-impact-analyst.md           PR 영향 분석
│
└── common/                            ← 공통 가이드
    ├── README.md
    ├── output-style.md
    ├── mermaid.md
    ├── ticket-guide.md
    ├── jira-sync.md
    ├── document-sync.md
    ├── tdd-template.md
    └── templates/                     ← 출력 템플릿
        ├── prd_analysis.md
        ├── inquiry_analysis.md
        ├── incident_analysis.md
        ├── pr_review_analysis.md
        ├── release_analysis.md
        └── refactoring_analysis.md
```

---

## 파이프라인 요약

| # | 파이프라인 | 에이전트 | 상황 |
|---|----------|---------|------|
| 1 | **PRD 분석** | A(FE) B(BE) C(DB) D(외부) | "이 기능 추가하려면?" |
| 2 | **문의 대응** | α(코드추적) β(데이터/설정) | "이거 왜 안 돼요?" |
| 3 | **장애 대응** | 🔥(장애추적) 🔍(변경분석) 🛡️(임시조치) | "프로덕션 에러!" |
| 4 | **코드 리뷰** | 📐(영향범위) 🧪(품질) | "이 PR 봐줘" |
| 5 | **배포 영향** | 🚀(서비스간) ⚡(위험탐지) | "배포해도 될까?" |
| 6 | **리팩토링** | 🔬(현황) 🗺️(의존성) | "이거 업그레이드하려면?" |
| 7 | **온보딩** | 📖(구조) 🔗(연결) | "이 도메인 설명해줘" |
| 8 | **API 변경** | 🌐(FE소비자) 🔌(BE소비자) | "이 API 바꾸면 영향은?" |
| 9 | **BE 구현 설계** | 🔎(요구사항검증) 🎨(디자인검증) 🏗️(아키텍처) 📊(데이터) 📋(티켓분할) 🧪(테스트) | "PRD 보고 TDD + 티켓 만들어줘" |

---

## 사용법

### 1. PRD 분석
```
"이 PRD 분석해줘: [PRD 내용]"
"이 기능 추가하려면 어디를 수정해야 해?: [기능 설명]"
```

### 2. 문의 대응
```
"이 이슈 분석해줘: [이슈 내용]"
"이 버그 원인 찾아줘: [증상 설명]"
"이거 왜 이렇게 동작해?: [현상 설명]"
```

### 3. 장애 대응
```
"프로덕션에서 500 에러 나고 있어: [에러 로그]"
"서비스 장애야: [증상]"
```

### 4. 코드 리뷰
```
"이 PR 리뷰해줘: [PR URL 또는 변경 내용]"
"이 변경 사이드 이펙트 확인해줘"
```

### 5. 배포 영향 분석
```
"이번에 greeting-new-back 배포하는데 영향 분석해줘"
"이 서비스들 배포 순서 정해줘"
```

### 6. 리팩토링/마이그레이션
```
"React Query v3 → v5 마이그레이션 계획 세워줘"
"Recoil → Zustand 전환 계획 세워줘"
```

### 7. 온보딩
```
"지원자 도메인 설명해줘"
"커뮤니케이션 서비스 구조 알려줘"
```

### 8. API 변경 분석
```
"이 API 응답 필드 바꾸면 영향 범위가?"
"이 엔드포인트 삭제해도 되나?"
```

### 9. BE 구현 설계
```
"이 PRD 기반으로 BE 구현 설계해줘: [PRD 링크]"
"이 PRD + 디자인 보고 TDD 작성해줘: [PRD 링크] [Figma 링크]"
"이 기능 구현 티켓 만들어줘: [기능 설명]"
```

---

## 공통 가이드

파이프라인 간 공유하는 작성 규칙은 `common/` 디렉토리에 있습니다.

| 가이드 | 설명 |
|--------|------|
| [common/output-style.md](common/output-style.md) | 문체, 표기법, 코드 참조 형식, 용어 원칙 |
| [common/mermaid.md](common/mermaid.md) | Mermaid 다이어그램 규칙과 PNG 변환 |
| [common/ticket-guide.md](common/ticket-guide.md) | 티켓 md 파일 구조, 사이즈 정의, 의존성 표기 |
| [common/jira-sync.md](common/jira-sync.md) | md 티켓 → Jira 이슈 변환 규칙 |
| [common/document-sync.md](common/document-sync.md) | TDD 기준 문서 동기화 순서 |
| [common/tdd-template.md](common/tdd-template.md) | TDD 필수 섹션과 작성 규칙 |
| [agents/README.md](agents/README.md) | 에이전트 역할 정의 인덱스 (33개) |

---

## 핵심 원칙

1. **동적 에이전트 할당**: 필요한 에이전트만 실행 (불필요한 분석 없음)
2. **병렬 실행**: 독립적인 분석은 동시에 실행
3. **결과 아카이브**: 모든 분석 결과는 results/에 저장
4. **템플릿 기반**: 일관된 출력 형식
5. **점진적 심화**: 간단한 건 팀장만, 복잡한 건 에이전트 동원
