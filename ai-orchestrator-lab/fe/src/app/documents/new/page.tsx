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
        <section
          style={{
            display: "grid",
            gap: 16,
            borderRadius: 18,
            padding: 24,
            background: "rgba(255,255,255,0.72)",
            border: "1px solid rgba(31,42,31,0.12)",
          }}
        >
          <label>
            <div style={{ marginBottom: 8, fontWeight: 700 }}>제목</div>
            <input
              type="text"
              placeholder="AI Wiki 문서 제목"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              style={{ width: "100%", padding: 14, borderRadius: 12, border: "1px solid #cbd5e1" }}
            />
          </label>
          <label>
            <div style={{ marginBottom: 8, fontWeight: 700 }}>본문</div>
            <textarea
              rows={16}
              placeholder="Markdown 문서를 작성하세요."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              required
              style={{ width: "100%", padding: 14, borderRadius: 12, border: "1px solid #cbd5e1" }}
            />
          </label>
          <button
            type="submit"
            disabled={loading}
            style={{
              padding: "14px 28px",
              borderRadius: 12,
              border: "none",
              background: loading ? "#94a3b8" : "#126f54",
              color: "#fff",
              fontWeight: 700,
              fontSize: 16,
              cursor: loading ? "not-allowed" : "pointer",
            }}
          >
            {loading ? "저장 중..." : "저장"}
          </button>
        </section>
      </form>
    </main>
  );
}
