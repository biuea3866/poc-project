# Closet 분석 시스템

> 10개 파이프라인으로 모든 업무 유형에 체계적으로 대응

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
├── api-change/                        ← 7. API 스펙 변경 분석
│   ├── PIPELINE.md
│   └── results/
│
├── be-implementation/                 ← 8. BE 구현 설계 (TDD + 티켓)
│   ├── PIPELINE.md
│   └── results/
│
├── implementation/                    ← 9. 구현 (티켓 → 코드 → 테스트 → PR)
│   ├── PIPELINE.md
│   └── results/
│
├── verification/                      ← 10. 검증 (설계 원칙 + 아키텍처 + AC)
│   ├── PIPELINE.md
│   └── results/
│
└── common/
    ├── AGENT_DESIGN_PRINCIPLES.md     ← 에이전트 설계 원칙
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
| 2 | **문의 대응** | a(코드추적) b(데이터/설정) | "이거 왜 안 돼요?" |
| 3 | **장애 대응** | :fire:(장애추적) :mag:(변경분석) :shield:(임시조치) | "프로덕션 에러!" |
| 4 | **코드 리뷰** | :triangular_ruler:(영향범위) :test_tube:(품질) | "이 PR 봐줘" |
| 5 | **배포 영향** | :rocket:(서비스간) :zap:(위험탐지) | "배포해도 될까?" |
| 6 | **리팩토링** | :microscope:(현황) :world_map:(의존성) | "이거 업그레이드하려면?" |
| 7 | **API 변경** | :globe_with_meridians:(FE소비자) :electric_plug:(BE소비자) | "이 API 바꾸면 영향은?" |
| 8 | **BE 구현 설계** | :mag:(요구사항검증) :art:(디자인검증) :building_construction:(아키텍처) :bar_chart:(데이터) :clipboard:(티켓분할) :test_tube:(테스트) | "PRD 보고 TDD + 티켓 만들어줘" |
| 9 | **구현** | - | "이 티켓 구현해줘" |
| 10 | **검증** | - | "구현 결과 검증해줘" |

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
"이번에 closet-order 배포하는데 영향 분석해줘"
"이 서비스들 배포 순서 정해줘"
```

### 6. 리팩토링/마이그레이션
```
"Spring Boot 2.x → 3.x 마이그레이션 계획 세워줘"
"Elasticsearch → OpenSearch 전환 계획 세워줘"
```

### 7. API 변경 분석
```
"이 API 응답 필드 바꾸면 영향 범위가?"
"이 엔드포인트 삭제해도 되나?"
```

### 8. BE 구현 설계
```
"이 PRD 기반으로 BE 구현 설계해줘: [PRD 링크]"
"이 PRD + 디자인 보고 TDD 작성해줘: [PRD 링크] [Figma 링크]"
"이 기능 구현 티켓 만들어줘: [기능 설명]"
```

### 9. 구현
```
"이 티켓 구현해줘: [티켓 파일 경로]"
```

### 10. 검증
```
"이 구현 검증해줘: [PR URL 또는 브랜치]"
```

---

## 핵심 원칙

1. **동적 에이전트 할당**: 필요한 에이전트만 실행 (불필요한 분석 없음)
2. **병렬 실행**: 독립적인 분석은 동시에 실행
3. **결과 아카이브**: 모든 분석 결과는 results/에 저장
4. **템플릿 기반**: 일관된 출력 형식
5. **점진적 심화**: 간단한 건 팀장만, 복잡한 건 에이전트 동원
