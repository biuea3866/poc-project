# Project Analysis 파이프라인 시각화

> 상세: [CLAUDE.md](CLAUDE.md)

---

## 전체 흐름

```mermaid
flowchart TB
    START([PRD 입력])

    subgraph Phase1["Phase 1: PRD 분석"]
        direction TB
        P1_1[1-1. 사전 준비]
        P1_2[1-2. 병렬 분석<br/>에이전트 9종]
        P1_GATE{1-2.5<br/>리뷰 게이트}
        P1_3[1-3. Gap 분석 보고서]

        P1_1 --> P1_2 --> P1_GATE
        P1_GATE -->|통과| P1_3
        P1_GATE -->|미통과| P1_2
    end

    subgraph Phase15["Phase 1.5: 벤치마킹"]
        direction TB
        P15_1[1.5-1. 벤치마킹 수행]
        P15_2[1.5-2. 아키텍처 설계]
        P15_GATE{1.5-2.5<br/>리뷰 게이트}
        P15_3[1.5-3. Phase 1 소급 업데이트]

        P15_1 --> P15_2 --> P15_GATE
        P15_GATE -->|통과| P15_3
        P15_GATE -->|미통과| P15_2
    end

    subgraph Phase2["Phase 2: BE 구현 설계"]
        direction TB
        P2_0[2-0. 코드베이스 최신화]
        P2_1[2-1. 기술 스택 분석<br/>에이전트 2종]
        P2_2[2-2. TDD 작성]
        P2_GATE{2-2.5<br/>리뷰 게이트}
        P2_3[2-3. 구현 티켓 작성<br/>에이전트 2종]
        P2_4[2-4. 최종 정리]

        P2_0 --> P2_1 --> P2_2 --> P2_GATE
        P2_GATE -->|통과| P2_3 --> P2_4
        P2_GATE -->|미통과| P2_2
    end

    subgraph Phase3["Phase 3: 구현"]
        direction TB
        P3_0[3-0. 코드베이스 최신화]
        P3_1[3-1. 티켓 단위<br/>TDD 사이클]
        P3_GATE{3-2<br/>리뷰 게이트}
        P3_3[3-3. PR 체크리스트]

        P3_0 --> P3_1 --> P3_GATE
        P3_GATE -->|통과| P3_3
        P3_GATE -->|미통과| P3_1
    end

    START --> Phase1 --> Phase15 --> Phase2 --> Phase3
    Phase3 --> DONE([PR 제출])
```

---

## Phase 1: PRD 분석 에이전트

```mermaid
flowchart LR
    PRD([PRD 입력])

    subgraph Always["항상 실행"]
        A1[기능 요구사항]
        A2[비기능 요구사항]
        A3[모호성/일관성]
        A4[기술 실현성]
        A5[FE/BE 작업범위]
    end

    subgraph Conditional["조건부 실행"]
        B1[FE 의존성<br/>API 변경 시]
        B2[라우팅/인프라<br/>API 변경 시]
        B3[도메인 아키텍처<br/>시스템 변경 시]
        B4[데이터 마이그레이션<br/>스키마 변경 시]
        B5[디자인 검증<br/>Figma 제공 시]
    end

    GAP[Gap 분석 보고서]

    PRD --> Always
    PRD --> Conditional
    Always --> GAP
    Conditional --> GAP
```

---

## Phase 2: BE 설계 흐름

```mermaid
flowchart LR
    subgraph Input["입력"]
        GA[Gap 분석]
        BM[벤치마킹]
        ARCH[아키텍처]
    end

    subgraph Analysis["2-1 기술 분석"]
        TA[아키텍처 분석]
        DA[데이터 분석]
    end

    TDD[2-2 TDD 작성]

    subgraph Tickets["2-3 티켓 분할"]
        TS[티켓 분할]
        TD[테스트 설계]
    end

    OV[2-4 최종 정리<br/>overview + 개별 티켓]

    Input --> Analysis --> TDD --> Tickets --> OV
```

---

## Phase 3: 구현 사이클 (티켓마다 반복)

```mermaid
flowchart LR
    subgraph Cycle["티켓 단위 사이클"]
        RED[1. 테스트 작성<br/>RED]
        GREEN[2. 구현<br/>GREEN]
        REVIEW[3. 자체 리뷰]
        VERIFY[4. 검증<br/>detekt + test]
        COMMIT[5. 커밋]
        DOC[6. 문서 소급 반영]
    end

    RED --> GREEN --> REVIEW --> VERIFY
    VERIFY -->|통과| COMMIT --> DOC
    VERIFY -->|실패| GREEN
```

---

## 리뷰 게이트 체크 항목

```mermaid
flowchart LR
    subgraph G1["1-2.5 (PRD 분석 후)"]
        G1A[산출물 완전성]
        G1B[코드 근거]
        G1C[누락 에이전트]
        G1D[상호 모순]
    end

    subgraph G2["1.5-2.5 (벤치마킹 후)"]
        G2A[벤치마킹 근거]
        G2B[채택/미채택 표]
        G2C[추상화 원칙]
        G2D[Phase 1 일관성]
    end

    subgraph G3["2-2.5 (TDD 후)"]
        G3A[클래스 역할]
        G3B[Solutions 풀어쓰기]
        G3C[다이어그램]
        G3D[PRD 완전 커버]
        G3E[Release Scenario]
    end

    subgraph G4["3-2 (구현 후)"]
        G4A[전체 테스트]
        G4B[코드 컨벤션]
        G4C[문서 동기화]
        G4D[PRD 대조]
    end
```

---

## 산출물 구조

```mermaid
flowchart LR
    subgraph Project["results/{날짜}_{기능명}/"]
        subgraph PRD["prd/"]
            P1[PRD_*_기능요구사항]
            P2[PRD_*_모호성]
            P3[PRD_*_기술실현성]
            P4[gap_analysis]
            P5[벤치마킹]
            P6[아키텍처 초기설계]
        end

        subgraph BE["be/"]
            B1[tdd.md]
            B2[detailed_design.md]
            subgraph TK["tickets/"]
                T1[_overview]
                T2[ticket_01..N]
            end
        end

        README[README.md]
    end
```

---

## 에이전트 역할 맵

```mermaid
flowchart TB
    subgraph P1_Agents["Phase 1 에이전트 (9종, 병렬)"]
        direction LR
        PA1[prd-functional]
        PA2[prd-nonfunctional]
        PA3[prd-ambiguity]
        PA4[prd-feasibility]
        PA5[prd-scope]
        PA6[prd-fe-dependency]
        PA7[prd-routing]
        PA8[prd-domain-arch]
        PA9[prd-migration]
    end

    subgraph P15_Agents["Phase 1.5 에이전트 (2종)"]
        direction LR
        BA1[bench-researcher]
        BA2[bench-architect]
    end

    subgraph P2_Agents["Phase 2 에이전트 (4종)"]
        direction LR
        TA1[tech-arch]
        TA2[tech-data]
        TA3[ticket-splitter]
        TA4[test-designer]
    end

    subgraph P3_Agents["Phase 3 에이전트 (4종)"]
        direction LR
        IA1[impl-developer]
        IA2[impl-reviewer]
        IA3[impl-prd-checker]
        IA4[impl-doc-sync]
    end

    subgraph Review_Agents["리뷰 게이트 (3종, foreground)"]
        direction LR
        RA1[review-completeness]
        RA2[review-tdd]
        RA3[review-impl]
    end
```
