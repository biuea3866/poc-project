"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getDocument, activateDocument, requestAnalysis, deleteDocument } from "@/lib/api-client";
import { useAiStatus } from "@/lib/use-ai-status";
import type { DocumentCard } from "@/lib/types";

export default function DocumentDetailPage({ params }: { params: { id: string } }) {
  const documentId = Number(params.id);
  const router = useRouter();
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

  async function handleDelete() {
    if (!window.confirm("문서를 삭제하시겠습니까?")) return;
    setActionLoading(true);
    setError(null);
    try {
      await deleteDocument(documentId);
      router.push("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "삭제 실패");
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
        {document.status !== "DELETED" && (
          <button
            onClick={handleDelete}
            disabled={actionLoading}
            style={{ padding: "12px 24px", borderRadius: 12, border: "none", background: actionLoading ? "#94a3b8" : "#dc2626", color: "#fff", fontWeight: 700, cursor: actionLoading ? "not-allowed" : "pointer" }}
          >
            삭제
          </button>
        )}
      </div>

      <section
        style={{
          marginTop: 28,
          borderRadius: 18,
          padding: 24,
          background: "rgba(255,255,255,0.72)",
          border: "1px solid rgba(31,42,31,0.12)",
        }}
      >
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
