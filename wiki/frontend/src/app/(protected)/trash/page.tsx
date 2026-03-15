"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, DocumentListResponse } from "@/types/document";

export default function TrashPage() {
  const queryClient = useQueryClient();

  const { data: trashDocs, isLoading } = useQuery<Document[]>({
    queryKey: ["trash"],
    queryFn: () => apiFetch<DocumentListResponse>("/api/v1/documents/trash").then(r => r.documents ?? [])
  });

  const restoreMutation = useMutation({
    mutationFn: (id: number) =>
      apiFetch(`/api/v1/documents/${id}/restore`, { method: "POST" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["trash"] });
      queryClient.invalidateQueries({ queryKey: ["documents"] });
    }
  });

  const purgeMutation = useMutation({
    mutationFn: (id: number) =>
      apiFetch(`/api/v1/documents/${id}/purge`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["trash"] });
    }
  });

  return (
    <div className="space-y-8 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold text-primary">휴지통</h1>
        <p className="mt-2 text-secondary">삭제된 문서들은 이곳에 30일 동안 보관됩니다.</p>
      </header>

      <section className="space-y-3">
        {isLoading ? (
          [1, 2, 3].map((i) => (
            <div key={i} className="h-20 w-full animate-pulse rounded-lg bg-white shadow-sm" />
          ))
        ) : trashDocs && trashDocs.length > 0 ? (
          trashDocs.map((doc) => (
            <div
              key={doc.id}
              className="group flex items-center justify-between rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
            >
              <div className="flex items-center gap-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-surface text-muted">
                  <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <div>
                  <h3 className="font-semibold text-primary">{doc.title}</h3>
                  <div className="mt-1 flex items-center gap-1 text-xs text-muted">
                    <svg className="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    삭제일: {new Date(doc.updatedAt).toLocaleDateString()}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 opacity-0 transition group-hover:opacity-100">
                <button
                  onClick={() => restoreMutation.mutate(doc.id)}
                  disabled={restoreMutation.isPending}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-success transition hover:bg-success/5 disabled:opacity-50"
                >
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  복구
                </button>
                <button
                  onClick={() => {
                    if (confirm("정말로 영구 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
                      purgeMutation.mutate(doc.id);
                    }
                  }}
                  disabled={purgeMutation.isPending}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-danger transition hover:bg-danger/5 disabled:opacity-50"
                >
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  삭제
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="flex h-64 flex-col items-center justify-center rounded-xl border border-dashed border-line text-muted">
            <svg className="mb-4 h-12 w-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            <p>휴지통이 비어 있습니다.</p>
          </div>
        )}
      </section>
    </div>
  );
}
