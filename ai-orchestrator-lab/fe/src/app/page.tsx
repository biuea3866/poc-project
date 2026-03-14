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
      <section
        style={{
          display: "grid",
          gap: 24,
          gridTemplateColumns: "1.2fr 0.8fr",
          alignItems: "start",
        }}
      >
        <div>
          <p style={{ margin: 0, letterSpacing: "0.2em", fontSize: 12, color: "#126f54" }}>AI WIKI</p>
          <h1 style={{ margin: "12px 0 16px", fontSize: 48, lineHeight: 1.05 }}>
            문서를 쓰면
            <br />
            AI가 검색 가능한 지식으로 바꿉니다.
          </h1>
          <p style={{ margin: 0, maxWidth: 620, fontSize: 18, lineHeight: 1.7, color: "#475569" }}>
            DRAFT 작성, ACTIVE 전환, 비동기 AI 분석, SSE 상태 확인, 본인 ACTIVE 문서 검색까지
            `input/prd.md` 기준 핵심 시나리오를 바로 구현할 수 있는 FE 루트입니다.
          </p>
        </div>
        <div
          style={{
            padding: 24,
            borderRadius: 20,
            background: "#1f2a1f",
            color: "#f8fafc",
          }}
        >
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
        {error && (
          <div style={{ padding: 12, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>{error}</div>
        )}
        {!loading && !error && <DocumentStatusBoard items={documents} />}
      </section>
    </main>
  );
}
