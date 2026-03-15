"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, DocumentRevision } from "@/types/document";
import {
  History,
  ChevronLeft,
  User,
  Clock,
} from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import MarkdownRenderer from "@/components/MarkdownRenderer";

function cn(...inputs: (string | boolean | undefined | null)[]) {
  return inputs.filter(Boolean).join(" ");
}

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
    <div className="space-y-10 py-6 animate-in">
      <header className="flex items-center gap-4">
        <Link
          href={`/documents/${id}`}
          className="rounded-lg p-2 text-muted transition hover:bg-surface hover:text-primary"
        >
          <ChevronLeft className="h-6 w-6" />
        </Link>
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-primary">변경 이력</h1>
          <p className="mt-1 text-secondary">{document?.title}의 과거 기록입니다.</p>
        </div>
      </header>

      <div className="grid gap-8 lg:grid-cols-[350px_1fr]">
        {/* Revision List */}
        <aside className="space-y-4">
          <h2 className="px-2 text-xs font-bold uppercase tracking-widest text-muted">
            버전 목록
          </h2>
          <div className="space-y-2">
            {isLoading ? (
              [1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-16 w-full animate-pulse rounded-xl border border-border-line bg-card" />
              ))
            ) : revisions && revisions.length > 0 ? (
              revisions.map((rev, index) => {
                const isSelected = selectedRevision?.id === rev.id;
                return (
                  <button
                    key={rev.id}
                    onClick={() => setSelectedRevision(rev)}
                    className={cn(
                      "w-full flex flex-col gap-1 rounded-xl p-4 text-left transition",
                      isSelected
                        ? "bg-accent text-white shadow-card"
                        : "border border-border-line bg-card text-secondary hover:border-border-hover"
                    )}
                  >
                    <div className="flex items-center justify-between font-bold">
                      <span>버전 {revisions.length - index}</span>
                      {index === 0 && (
                        <span
                          className={cn(
                            "rounded-full px-1.5 py-0.5 text-[10px] uppercase tracking-tighter",
                            isSelected
                              ? "bg-white/20 text-white"
                              : "bg-accent-light text-accent"
                          )}
                        >
                          현재
                        </span>
                      )}
                    </div>
                    <div
                      className={cn(
                        "flex items-center gap-2 text-xs",
                        isSelected ? "text-white/80" : "text-muted"
                      )}
                    >
                      <Clock className="h-3 w-3" />
                      {new Date(rev.createdAt).toLocaleString()}
                    </div>
                  </button>
                );
              })
            ) : (
              <p className="px-2 text-sm italic text-disabled">변경 이력이 없습니다.</p>
            )}
          </div>
        </aside>

        {/* Content Preview */}
        <main className="min-h-[600px] rounded-xl border border-border-line bg-card p-8">
          {selectedRevision ? (
            <div className="space-y-8 animate-in">
              <div className="flex items-center justify-between border-b border-border-line pb-6">
                <div>
                  <h3 className="text-2xl font-bold text-primary">
                    {JSON.parse(selectedRevision.data).title}
                  </h3>
                  <div className="mt-2 flex items-center gap-4 text-sm text-muted">
                    <div className="flex items-center gap-1.5">
                      <User className="h-4 w-4" />
                      <span>작성자 ID: {selectedRevision.createdBy}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Clock className="h-4 w-4" />
                      <span>{new Date(selectedRevision.createdAt).toLocaleString()}</span>
                    </div>
                  </div>
                </div>
                <button className="btn-accent flex items-center gap-2 rounded-lg">
                  이 버전으로 복구
                </button>
              </div>
              <article>
                <MarkdownRenderer content={JSON.parse(selectedRevision.data).content || ""} />
              </article>
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center text-disabled">
              <History className="mb-4 h-12 w-12 opacity-10" />
              <p>이력을 확인하려면 왼쪽 목록에서 버전을 선택하세요.</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
