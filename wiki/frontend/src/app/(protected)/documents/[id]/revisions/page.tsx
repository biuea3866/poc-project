"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, DocumentRevision } from "@/types/document";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import MarkdownRenderer from "@/components/MarkdownRenderer";

export default function RevisionHistoryPage() {
  const { id } = useParams();
  const [selectedRevision, setSelectedRevision] = useState<DocumentRevision | null>(null);

  const { data: document } = useQuery<Document>({
    queryKey: ["document", id],
    queryFn: () => apiFetch<Document>(`/api/v1/documents/${id}`)
  });

  const { data: revisions, isLoading } = useQuery<DocumentRevision[]>({
    queryKey: ["document-revisions", id],
    queryFn: () => apiFetch<DocumentRevision[]>(`/api/v1/documents/${id}/revisions`)
  });

  return (
    <div className="space-y-8 py-6 animate-in">
      <header className="flex items-center gap-4">
        <Link
          href={`/documents/${id}`}
          className="rounded-full p-2 text-muted hover:bg-surface hover:text-primary"
        >
          <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </Link>
        <div>
          <h1 className="text-3xl font-extrabold text-primary">변경 이력</h1>
          <p className="mt-1 text-secondary">{document?.title}의 과거 기록입니다.</p>
        </div>
      </header>

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        {/* Revision List */}
        <aside className="space-y-3">
          <h2 className="px-2 text-xs font-bold uppercase tracking-widest text-muted">버전 목록</h2>
          <div className="space-y-2">
            {isLoading ? (
              [1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-16 w-full animate-pulse rounded-lg bg-white shadow-sm" />
              ))
            ) : revisions && revisions.length > 0 ? (
              revisions.map((rev, index) => {
                const isSelected = selectedRevision?.id === rev.id;
                return (
                  <button
                    key={rev.id}
                    onClick={() => setSelectedRevision(rev)}
                    className={`flex w-full flex-col gap-1 rounded-xl p-4 text-left transition ${
                      isSelected
                        ? "bg-gradient-to-r from-accent to-accent-purple text-white shadow-card"
                        : "border border-line bg-white text-secondary hover:shadow-sm"
                    }`}
                  >
                    <div className="flex items-center justify-between font-semibold">
                      <span>버전 {revisions.length - index}</span>
                      {index === 0 && (
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                          isSelected ? "bg-white/20 text-white" : "bg-accent-light text-accent"
                        }`}>
                          현재
                        </span>
                      )}
                    </div>
                    <div className={`flex items-center gap-1 text-xs ${
                      isSelected ? "text-white/80" : "text-muted"
                    }`}>
                      <svg className="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      {new Date(rev.createdAt).toLocaleString()}
                    </div>
                  </button>
                );
              })
            ) : (
              <p className="px-2 text-sm italic text-muted">변경 이력이 없습니다.</p>
            )}
          </div>
        </aside>

        {/* Content Preview */}
        <main className="min-h-[500px] rounded-xl border border-line bg-white p-8">
          {selectedRevision ? (() => {
            let revData: { title?: string; content?: string } = {};
            try {
              revData = JSON.parse(selectedRevision.data);
            } catch {}

            return (
              <div className="space-y-6 animate-in">
                <div className="flex items-center justify-between border-b border-line pb-6">
                  <div>
                    <h3 className="text-2xl font-bold text-primary">{revData.title}</h3>
                    <div className="mt-2 flex items-center gap-4 text-sm text-muted">
                      <span>작성자 ID: {selectedRevision.createdBy}</span>
                      <span>{new Date(selectedRevision.createdAt).toLocaleString()}</span>
                    </div>
                  </div>
                  <button className="rounded-lg border border-accent px-4 py-2 text-sm font-semibold text-accent transition hover:bg-accent-light">
                    이 버전으로 복구
                  </button>
                </div>
                <article>
                  <MarkdownRenderer content={revData.content || ""} />
                </article>
              </div>
            );
          })() : (
            <div className="flex h-full flex-col items-center justify-center text-muted">
              <svg className="mb-4 h-12 w-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p>이력을 확인하려면 왼쪽 목록에서 버전을 선택하세요.</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
