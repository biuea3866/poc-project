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
      <input
        type="text"
        placeholder="검색어를 입력하세요..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        style={{
          width: "100%",
          padding: 14,
          borderRadius: 12,
          border: "1px solid #cbd5e1",
          marginBottom: 24,
          fontSize: 16,
        }}
      />
      {loading && <p style={{ color: "#475569" }}>검색 중...</p>}
      {error && (
        <div style={{ padding: 12, marginBottom: 16, borderRadius: 8, background: "#fef2f2", color: "#b91c1c" }}>
          {error}
        </div>
      )}
      {!loading && !error && (
        <div style={{ display: "grid", gap: 16 }}>
          {results.length === 0 ? (
            <p style={{ color: "#475569" }}>검색 결과가 없습니다.</p>
          ) : (
            results.map((item) => (
              <article
                key={item.id}
                style={{
                  borderRadius: 18,
                  padding: 20,
                  background: "rgba(255,255,255,0.7)",
                  border: "1px solid rgba(31,42,31,0.12)",
                }}
              >
                <strong>{item.title}</strong>
                <p style={{ color: "#475569" }}>{item.excerpt}</p>
              </article>
            ))
          )}
        </div>
      )}
    </main>
  );
}
