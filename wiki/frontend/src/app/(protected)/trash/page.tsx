"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document } from "@/types/document";
import {
  Trash2,
  RotateCcw,
  FileText,
  Clock,
} from "lucide-react";

export default function TrashPage() {
  const queryClient = useQueryClient();

  const { data: trashDocs, isLoading } = useQuery<Document[]>({
    queryKey: ["trash"],
    queryFn: () => apiFetch<Document[]>("/api/v1/documents?status=DELETED")
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
    <div className="space-y-10 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold tracking-tight text-primary">휴지통</h1>
        <p className="mt-2 text-secondary">삭제된 문서들은 이곳에 30일 동안 보관됩니다.</p>
      </header>

      <section className="space-y-4">
        {isLoading ? (
          [1, 2, 3].map((i) => (
            <div key={i} className="h-24 w-full animate-pulse rounded-xl border border-border-line bg-card" />
          ))
        ) : trashDocs && trashDocs.length > 0 ? (
          trashDocs.map((doc) => (
            <div
              key={doc.id}
              className="group flex items-center justify-between rounded-xl border border-border-line bg-card p-5 transition hover:border-border-hover hover:shadow-soft"
            >
              <div className="flex items-center gap-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-surface text-disabled">
                  <FileText className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="font-bold text-primary">{doc.title}</h3>
                  <div className="mt-1 flex items-center gap-3 text-xs text-muted">
                    <span className="flex items-center gap-1">
                      <Clock className="h-3 w-3" />
                      삭제일: {new Date(doc.updatedAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 opacity-0 transition group-hover:opacity-100">
                <button
                  onClick={() => restoreMutation.mutate(doc.id)}
                  disabled={restoreMutation.isPending}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-success transition hover:bg-accent-light"
                  title="복구하기"
                >
                  <RotateCcw className="h-4 w-4" />
                  복구
                </button>
                <button
                  onClick={() => {
                    if (confirm("정말로 영구 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
                      purgeMutation.mutate(doc.id);
                    }
                  }}
                  disabled={purgeMutation.isPending}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-danger transition hover:bg-danger-light"
                  title="영구 삭제"
                >
                  <Trash2 className="h-4 w-4" />
                  삭제
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="flex h-64 flex-col items-center justify-center rounded-xl border border-dashed border-border-line text-disabled">
            <Trash2 className="mb-4 h-12 w-12 opacity-10" />
            <p>휴지통이 비어 있습니다.</p>
          </div>
        )}
      </section>
    </div>
  );
}
