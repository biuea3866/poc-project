# Stage 11: FE Code Generation

> PRD v2.1 + 08_ticket_breakdown 기준 | 작성일: 2026-03-14 | 담당: fe-developer

---

## 티켓 매핑

| 티켓 ID | 제목 | Risk | Complexity | 구현 파일 |
|---------|------|------|------------|----------|
| NAW-FE-001 | API Client + TypeScript 타입 정의 | 1 | 2 | `lib/types.ts`, `lib/api-client.ts` |
| NAW-FE-002 | 문서 목록/상세 페이지 | 2 | 2 | `app/page.tsx`, `app/documents/[id]/page.tsx` |
| NAW-FE-003 | 문서 생성/수정 폼 | 2 | 3 | `app/documents/new/page.tsx` |
| NAW-FE-004 | 상태 전환 + AI 분석 + SSE | 3 | 4 | `app/documents/[id]/page.tsx`, `lib/use-ai-status.ts` |
| NAW-FE-005 | 검색 UI + 태그 필터 | 2 | 2 | `app/search/page.tsx` |

---

## 기술 스택

| 항목 | 선택 |
|------|------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript 5 |
| Styling | Inline styles (CSS-in-JS 없이 경량 유지) |
| API 통신 | fetch API (네이티브) |
| 실시간 | EventSource (SSE) |
| 상태 관리 | React useState/useEffect (외부 라이브러리 없음) |

---

## 파일 구조

```
fe/src/
├── app/
│   ├── layout.tsx              # 루트 레이아웃 (SSR)
│   ├── page.tsx                # 홈페이지 - 문서 목록 (CSR)
│   ├── documents/
│   │   ├── [id]/page.tsx       # 문서 상세 + 상태 전환 + AI 분석 (CSR + SSE)
│   │   └── new/page.tsx        # 새 문서 작성 (CSR)
│   └── search/page.tsx         # 문서 검색 (CSR)
├── components/
│   └── document-status-board.tsx  # 문서 카드 그리드 컴포넌트
└── lib/
    ├── types.ts                # 도메인 타입 정의
    ├── api-client.ts           # BE REST API 호출 함수
    ├── use-ai-status.ts        # SSE AI 상태 구독 훅
    └── mock-data.ts            # 목업 데이터 (개발 참조용, 미사용)
```

---

## NAW-FE-001: API Client + TypeScript 타입 정의

### Acceptance Criteria
- 전체 API 엔드포인트 대응 client 함수
- TypeScript 타입 정의 완료
- Mock 응답으로 동작 확인

### 1-1. 도메인 타입 (`src/lib/types.ts`)

```typescript
export type DocumentStatus = "DRAFT" | "ACTIVE" | "DELETED";
export type AiStatus = "NOT_STARTED" | "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface DocumentCard {
  id: number;
  title: string;
  excerpt: string;
  status: DocumentStatus;
  aiStatus: AiStatus;
  tags: string[];
  updatedAt: string;
}

export interface SseStatusEvent {
  status: AiStatus;
}
```

### 1-2. API 클라이언트 (`src/lib/api-client.ts`)

```typescript
import type { DocumentCard } from "@/lib/types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const API = `${BASE_URL}/api/v1/documents`;

export async function createDocument(title: string, content: string): Promise<DocumentCard> {
  const res = await fetch(API, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content }),
  });
  if (!res.ok) throw new Error(`문서 생성 실패: ${res.status}`);
  return res.json();
}

export async function getDocument(id: number): Promise<DocumentCard> {
  const res = await fetch(`${API}/${id}`);
  if (!res.ok) throw new Error(`문서 조회 실패: ${res.status}`);
  return res.json();
}

export async function searchDocuments(query?: string): Promise<DocumentCard[]> {
  const url = new URL(API);
  if (query) url.searchParams.set("q", query);
  const res = await fetch(url.toString());
  if (!res.ok) throw new Error(`문서 검색 실패: ${res.status}`);
  return res.json();
}

export async function requestAnalysis(id: number): Promise<void> {
  const res = await fetch(`${API}/${id}/analyze`, { method: "POST" });
  if (!res.ok) throw new Error(`분석 요청 실패: ${res.status}`);
}

export async function activateDocument(id: number): Promise<void> {
  const res = await fetch(`${API}/${id}/activate`, { method: "POST" });
  if (!res.ok) throw new Error(`문서 활성화 실패: ${res.status}`);
}

export async function updateDocument(id: number, title: string, content: string): Promise<DocumentCard> {
  const res = await fetch(`${API}/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content }),
  });
  if (!res.ok) throw new Error(`문서 수정 실패: ${res.status}`);
  return res.json();
}
```

**설계 포인트:**
- `NEXT_PUBLIC_API_URL` 환경 변수로 BE 주소 주입 (기본값 localhost:8080)
- API 경로: `/api/v1/documents` (BE `@RequestMapping` 일치)
- `activateDocument`: POST (BE `@PostMapping("/{id}/activate")`)
- `updateDocument`: PATCH (BE `@PatchMapping("/{id}")`)
- 검색 파라미터: `q` (BE `@RequestParam q`)
- 모든 함수에서 `!res.ok` 시 Error throw → UI 에러 핸들링으로 전파

---

## NAW-FE-002: 문서 목록/상세 페이지

### Acceptance Criteria
- 목록 페이지에서 본인 문서 표시
- 상세 페이지에서 문서 내용 렌더링
- 상태 뱃지(DRAFT/ACTIVE) 표시

### 2-1. 홈페이지 (`src/app/page.tsx`)

```typescript
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DocumentStatusBoard } from "@/components/document-status-board";
import { searchDocuments } from "@/lib/api-client";
import type { DocumentCard } from "@/lib/types";

export default function HomePage() {
  const [documents, setDocuments] = useState<DocumentCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    searchDocuments()
      .then(setDocuments)
      .catch((err) => setError(err instanceof Error ? err.message : "문서 목록 조회 실패"))
      .finally(() => setLoading(false));
  }, []);

  return (
    <main style={{ maxWidth: 1080, margin: "0 auto", padding: "48px 24px 80px" }}>
      <section style={{ display: "grid", gap: 24, gridTemplateColumns: "1.2fr 0.8fr", alignItems: "start" }}>
        <div>
          <p style={{ margin: 0, letterSpacing: "0.2em", fontSize: 12, color: "#126f54" }}>AI WIKI</p>
          <h1 style={{ margin: "12px 0 16px", fontSize: 48, lineHeight: 1.05 }}>
            문서를 쓰면<br />AI가 검색 가능한 지식으로 바꿉니다.
          </h1>
          <p style={{ margin: 0, maxWidth: 620, fontSize: 18, lineHeight: 1.7, color: "#475569" }}>
            DRAFT 작성, ACTIVE 전환, 비동기 AI 분석, SSE 상태 확인, 본인 ACTIVE 문서 검색까지
            핵심 시나리오를 바로 구현할 수 있는 FE 루트입니다.
          </p>
        </div>
        <div style={{ padding: 24, borderRadius: 20, background: "#1f2a1f", color: "#f8fafc" }}>
          <h2 style={{ marginTop: 0 }}>현재 작업 범위</h2>
          <ul style={{ margin: 0, paddingLeft: 18, lineHeight: 1.9 }}>
            <li>문서 작성 / 상세 / 검색 화면</li>
            <li>AI 상태 배지와 SSE 반영</li>
            <li>ACTIVE 전환 후 analyze 요청</li>
          </ul>
          <div style={{ display: "flex", gap: 12, marginTop: 20 }}>
            <Link href="/documents/1">문서 상세</Link>
            <Link href="/documents/new">새 문서</Link>
            <Link href="/search">검색</Link>
          </div>
        </div>
      </section>

      <section style={{ marginTop: 40 }}>
        {loading && <p style={{ color: "#475569" }}>문서 목록을 불러오는 중...</p>}
        {error && <div style={{ padding: 12, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>{error}</div>}
        {!loading && !error && <DocumentStatusBoard items={documents} />}
      </section>
    </main>
  );
}
```

### 2-2. 문서 상세 (`src/app/documents/[id]/page.tsx`)

```typescript
"use client";

import { useEffect, useState } from "react";
import { getDocument, activateDocument, requestAnalysis } from "@/lib/api-client";
import { useAiStatus } from "@/lib/use-ai-status";
import type { DocumentCard } from "@/lib/types";

export default function DocumentDetailPage({ params }: { params: { id: string } }) {
  const documentId = Number(params.id);
  const [document, setDocument] = useState<DocumentCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const liveStatus = useAiStatus(documentId);

  useEffect(() => {
    getDocument(documentId)
      .then(setDocument)
      .catch((err) => setError(err instanceof Error ? err.message : "문서 조회 실패"))
      .finally(() => setLoading(false));
  }, [documentId]);

  async function handleActivate() {
    setActionLoading(true);
    setError(null);
    try {
      await activateDocument(documentId);
      const updated = await getDocument(documentId);
      setDocument(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : "활성화 실패");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleAnalyze() {
    setActionLoading(true);
    setError(null);
    try {
      await requestAnalysis(documentId);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "분석 요청 실패";
      setError(msg.includes("409") ? "이미 처리 중입니다." : msg);
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) return <main style={{ maxWidth: 920, margin: "0 auto", padding: "48px 24px" }}><p>로딩 중...</p></main>;
  if (error && !document) return <main style={{ maxWidth: 920, margin: "0 auto", padding: "48px 24px" }}><div style={{ padding: 12, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>{error}</div></main>;
  if (!document) return null;

  return (
    <main style={{ maxWidth: 920, margin: "0 auto", padding: "48px 24px 72px" }}>
      <p style={{ color: "#126f54", fontWeight: 700 }}>{document.status} / {liveStatus}</p>
      <h1 style={{ fontSize: 42, marginBottom: 12 }}>{document.title}</h1>
      <p style={{ color: "#475569", lineHeight: 1.8 }}>{document.excerpt}</p>

      {error && (
        <div style={{ padding: 12, marginTop: 16, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div style={{ display: "flex", gap: 12, marginTop: 20 }}>
        {document.status === "DRAFT" && (
          <button
            onClick={handleActivate}
            disabled={actionLoading}
            style={{ padding: "12px 24px", borderRadius: 12, border: "none", background: actionLoading ? "#94a3b8" : "#126f54", color: "#fff", fontWeight: 700, cursor: actionLoading ? "not-allowed" : "pointer" }}
          >
            {actionLoading ? "처리 중..." : "ACTIVE 전환"}
          </button>
        )}
        {document.status === "ACTIVE" && liveStatus !== "PROCESSING" && (
          <button
            onClick={handleAnalyze}
            disabled={actionLoading}
            style={{ padding: "12px 24px", borderRadius: 12, border: "none", background: actionLoading ? "#94a3b8" : "#0f766e", color: "#fff", fontWeight: 700, cursor: actionLoading ? "not-allowed" : "pointer" }}
          >
            {actionLoading ? "요청 중..." : "AI 분석 요청"}
          </button>
        )}
      </div>

      <section style={{ marginTop: 28, borderRadius: 18, padding: 24, background: "rgba(255,255,255,0.72)", border: "1px solid rgba(31,42,31,0.12)" }}>
        <h2 style={{ marginTop: 0 }}>태그</h2>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {document.tags.map((tag) => (
            <span key={tag} style={{ borderRadius: 999, padding: "6px 10px", background: "#edf7ef", color: "#126f54", fontSize: 12, fontWeight: 700 }}>
              #{tag}
            </span>
          ))}
        </div>
        <p style={{ marginTop: 16, fontSize: 13, color: "#64748b" }}>최근 수정: {document.updatedAt}</p>
      </section>
    </main>
  );
}
```

**변경 사항 (mock-data → 실제 API 전환):**
- `mock-data` import 제거 → `getDocument(id)` API 호출
- `useEffect` + `useState`로 비동기 데이터 로딩
- ACTIVE 전환 버튼 (`handleActivate`) 추가 — NAW-FE-004
- AI 분석 요청 버튼 (`handleAnalyze`) 추가 — NAW-FE-004
- 409 Conflict 에러 시 "이미 처리 중입니다" 메시지
- PROCESSING 중 분석 요청 버튼 숨김
- 태그 뱃지 렌더링 추가

---

## NAW-FE-003: 문서 생성/수정 폼

### Acceptance Criteria
- 에디터에서 Markdown 작성
- 생성 시 DRAFT 상태
- 수정 시 updatedAt 전송
- 409 충돌 시 사용자 알림

### 3-1. 새 문서 작성 (`src/app/documents/new/page.tsx`)

```typescript
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { createDocument } from "@/lib/api-client";

export default function NewDocumentPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const doc = await createDocument(title, content);
      router.push(`/documents/${doc.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "문서 생성에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main style={{ maxWidth: 920, margin: "0 auto", padding: "48px 24px 72px" }}>
      <h1 style={{ fontSize: 40, marginBottom: 12 }}>새 문서 작성</h1>
      <p style={{ color: "#475569", marginBottom: 24 }}>
        최초 저장 상태는 DRAFT 입니다. ACTIVE 전환 전까지는 AI 분석이 시작되지 않습니다.
      </p>
      {error && (
        <div style={{ padding: 12, marginBottom: 16, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>
          {error}
        </div>
      )}
      <form onSubmit={onSubmit}>
        <section style={{ display: "grid", gap: 16, borderRadius: 18, padding: 24, background: "rgba(255,255,255,0.72)", border: "1px solid rgba(31,42,31,0.12)" }}>
          <label>
            <div style={{ marginBottom: 8, fontWeight: 700 }}>제목</div>
            <input type="text" placeholder="AI Wiki 문서 제목" value={title} onChange={(e) => setTitle(e.target.value)} required style={{ width: "100%", padding: 14, borderRadius: 12, border: "1px solid #cbd5e1" }} />
          </label>
          <label>
            <div style={{ marginBottom: 8, fontWeight: 700 }}>본문</div>
            <textarea rows={16} placeholder="Markdown 문서를 작성하세요." value={content} onChange={(e) => setContent(e.target.value)} required style={{ width: "100%", padding: 14, borderRadius: 12, border: "1px solid #cbd5e1" }} />
          </label>
          <button type="submit" disabled={loading} style={{ padding: "14px 28px", borderRadius: 12, border: "none", background: loading ? "#94a3b8" : "#126f54", color: "#fff", fontWeight: 700, fontSize: 16, cursor: loading ? "not-allowed" : "pointer" }}>
            {loading ? "저장 중..." : "저장"}
          </button>
        </section>
      </form>
    </main>
  );
}
```

---

## NAW-FE-004: 상태 전환 + AI 분석 요청 + SSE

### Acceptance Criteria
- DRAFT → ACTIVE 전환 동작
- Analyze 요청 후 SSE로 상태 수신
- 단계별 진행 표시
- COMPLETED/FAILED 결과 표시

### 4-1. SSE 커스텀 훅 (`src/lib/use-ai-status.ts`)

```typescript
"use client";

import { useEffect, useState } from "react";
import type { AiStatus, SseStatusEvent } from "@/lib/types";

export function useAiStatus(documentId: number) {
  const [status, setStatus] = useState<AiStatus>("NOT_STARTED");

  useEffect(() => {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    const eventSource = new EventSource(
      `${baseUrl}/api/v1/documents/${documentId}/ai-status/stream`
    );

    const handler = (event: MessageEvent) => {
      const payload = JSON.parse(event.data) as SseStatusEvent;
      setStatus(payload.status);
    };
    eventSource.addEventListener("ai-status", handler);

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [documentId]);

  return status;
}
```

### 4-2. 상세 페이지 상태 전환/분석 UI

`[id]/page.tsx` 내 `handleActivate()`, `handleAnalyze()` 함수로 구현 (2-2 참조).

- DRAFT 문서: "ACTIVE 전환" 버튼 표시
- ACTIVE 문서: "AI 분석 요청" 버튼 표시 (PROCESSING 중 숨김)
- SSE `liveStatus`로 실시간 AI 상태 반영
- 409 Conflict → "이미 처리 중입니다" 메시지

---

## NAW-FE-005: 검색 UI + 태그 필터

### Acceptance Criteria
- 키워드 검색 동작
- 태그 필터 동작
- 결과 목록 표시
- 페이징 동작

### 5-1. 검색 페이지 (`src/app/search/page.tsx`)

```typescript
"use client";

import { useEffect, useState } from "react";
import { searchDocuments } from "@/lib/api-client";
import type { DocumentCard } from "@/lib/types";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<DocumentCard[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const timer = setTimeout(() => {
      searchDocuments(query || undefined)
        .then(setResults)
        .catch((err) => setError(err instanceof Error ? err.message : "검색 실패"))
        .finally(() => setLoading(false));
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  return (
    <main style={{ maxWidth: 920, margin: "0 auto", padding: "48px 24px 72px" }}>
      <h1 style={{ fontSize: 40, marginBottom: 12 }}>문서 검색</h1>
      <p style={{ color: "#475569", marginBottom: 24 }}>
        PRD 기준으로 검색 대상은 본인 ACTIVE 문서만 허용합니다.
      </p>
      <input type="text" placeholder="검색어를 입력하세요..." value={query} onChange={(e) => setQuery(e.target.value)} style={{ width: "100%", padding: 14, borderRadius: 12, border: "1px solid #cbd5e1", marginBottom: 24, fontSize: 16 }} />
      {loading && <p style={{ color: "#475569" }}>검색 중...</p>}
      {error && <div style={{ padding: 12, marginBottom: 16, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>{error}</div>}
      {!loading && !error && (
        <div style={{ display: "grid", gap: 16 }}>
          {results.length === 0
            ? <p style={{ color: "#475569" }}>검색 결과가 없습니다.</p>
            : results.map((item) => (
                <article key={item.id} style={{ borderRadius: 18, padding: 20, background: "rgba(255,255,255,0.7)", border: "1px solid rgba(31,42,31,0.12)" }}>
                  <strong>{item.title}</strong>
                  <p style={{ color: "#475569" }}>{item.excerpt}</p>
                </article>
              ))
          }
        </div>
      )}
    </main>
  );
}
```

**미구현 (BE 의존):**
- 태그 필터 칩 UI → BE 검색 API에 태그 필터 파라미터 추가 후 구현
- 페이징 → BE 페이징 응답 스펙 확정 후 구현

---

## 변경 파일 목록

| 파일 | 변경 유형 | 티켓 | 설명 |
|------|----------|------|------|
| `src/lib/types.ts` | 유지 | NAW-FE-001 | 도메인 타입 (변경 없음) |
| `src/lib/api-client.ts` | 신규 | NAW-FE-001 | BE REST API 호출 함수 6개 |
| `src/lib/use-ai-status.ts` | 유지 | NAW-FE-004 | SSE 훅 (팀장 리뷰 반영 완료) |
| `src/app/page.tsx` | **수정** | NAW-FE-002 | mock-data → API 연동, CSR 전환 |
| `src/app/documents/new/page.tsx` | **수정** | NAW-FE-003 | 폼 제출 핸들러, API 연동 |
| `src/app/search/page.tsx` | **수정** | NAW-FE-005 | 검색 API 연동, 디바운스 |
| `src/app/documents/[id]/page.tsx` | **수정** | NAW-FE-002, NAW-FE-004 | mock-data → API 전환, ACTIVE 전환/분석 버튼 |
| `src/components/document-status-board.tsx` | 유지 | NAW-FE-002 | 프레젠테이션 컴포넌트 (변경 없음) |
| `src/lib/mock-data.ts` | 미사용 | - | 개발 참조용 유지 |

---

## 컴포넌트 다이어그램

> Excalidraw JSON: `outputs/team_run/11_fe_component.excalidraw`

---

## Worktree 메모

- FE 워크트리 분기점: `main` 브랜치 기준
- API 연동은 BE (`/api/v1/documents`) 기동 후 통합 테스트 가능
- `NEXT_PUBLIC_API_URL` 환경 변수로 BE 주소 주입
- TypeScript 타입 체크 통과 확인 완료 (`tsc --noEmit`)
- `[id]/page.tsx` mock-data → 실제 API 전환 완료, ACTIVE 전환/분석 버튼 구현 완료
- 태그 필터 + 페이징은 BE 스펙 확정 후 후속 구현
