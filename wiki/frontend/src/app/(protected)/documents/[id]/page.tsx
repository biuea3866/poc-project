"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, Tag, DocumentSummary } from "@/types/document";
import MarkdownRenderer from "@/components/MarkdownRenderer";
import {
  Calendar,
  User,
  Sparkles,
  Clock,
  Trash2,
  Edit3,
  History,
  AlertCircle,
  ChevronLeft
} from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";

export default function DocumentPage() {
  const { id } = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: document, isLoading: isDocLoading } = useQuery<Document>({
    queryKey: ["document", id],
    queryFn: () => apiFetch<Document>(`/api/v1/documents/${id}`)
  });

  const { data: tags } = useQuery<Tag[]>({
    queryKey: ["document-tags", id],
    queryFn: () => apiFetch<Tag[]>(`/api/v1/documents/${id}/tags`)
  });

  const { data: summary } = useQuery<DocumentSummary>({
    queryKey: ["document-summary", id],
    queryFn: () => apiFetch<DocumentSummary>(`/api/v1/documents/${id}/summary`),
    enabled: document?.aiStatus === "COMPLETED"
  });

  const deleteMutation = useMutation({
    mutationFn: () => apiFetch(`/api/v1/documents/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      router.push("/dashboard");
    }
  });

  if (isDocLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-8 py-10 animate-in">
        <div className="h-12 w-2/3 animate-pulse rounded-lg bg-border-line" />
        <div className="flex gap-4">
          <div className="h-4 w-24 animate-pulse rounded bg-border-line" />
          <div className="h-4 w-24 animate-pulse rounded bg-border-line" />
        </div>
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center text-disabled">
        <AlertCircle className="mb-4 h-12 w-12 opacity-20" />
        <p>문서를 찾을 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-12 py-10 animate-in">
      {/* Back button */}
      <Link
        href="/dashboard"
        className="inline-flex items-center gap-1.5 text-sm text-muted transition hover:text-primary"
      >
        <ChevronLeft className="h-4 w-4" />
        뒤로가기
      </Link>

      <header className="space-y-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-primary md:text-4xl">
          {document.title}
        </h1>

        <div className="flex flex-wrap items-center justify-between gap-6 text-sm text-secondary">
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2 font-bold">
              <User className="h-4 w-4" />
              <span>{document.createdBy}</span>
            </div>
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              <span>{new Date(document.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <Link
              href={`/documents/${id}/edit`}
              className="flex items-center gap-1.5 text-muted transition hover:text-primary"
            >
              <Edit3 className="h-4 w-4" />
              수정
            </Link>
            <button
              onClick={() => {
                if (confirm("문서를 삭제하시겠습니까? 삭제된 문서는 휴지통으로 이동합니다.")) {
                  deleteMutation.mutate();
                }
              }}
              className="flex items-center gap-1.5 text-danger transition hover:text-danger/80"
            >
              <Trash2 className="h-4 w-4" />
              삭제
            </button>
          </div>
        </div>

        {tags && tags.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {tags.map((tag) => (
              <span
                key={tag.id}
                className="rounded-full bg-accent-light px-2.5 py-0.5 text-xs font-semibold text-accent"
              >
                # {tag.name}
              </span>
            ))}
          </div>
        )}
      </header>

      {/* AI Summary */}
      {document.aiStatus === "COMPLETED" && summary && (
        <section className="rounded-xl border border-accent/20 bg-accent-light/50 p-6">
          <div className="mb-4 flex items-center gap-2 text-accent">
            <Sparkles className="h-5 w-5" />
            <h2 className="text-sm font-bold uppercase tracking-wider">AI 요약</h2>
          </div>
          <p className="text-lg leading-relaxed text-secondary italic">
            &quot;{summary.content}&quot;
          </p>
        </section>
      )}

      {/* Article Content */}
      <article className="min-h-[400px]">
        <MarkdownRenderer content={document.content || ""} />
      </article>

      {/* Footer */}
      <footer className="flex items-center justify-between border-t border-border-line pt-8 text-sm text-muted">
        <div className="flex items-center gap-1.5">
          <Clock className="h-4 w-4" />
          <span>최근 수정: {new Date(document.updatedAt).toLocaleString()}</span>
        </div>
        <Link
          href={`/documents/${id}/revisions`}
          className="flex items-center gap-1.5 font-bold text-accent hover:underline"
        >
          <History className="h-4 w-4" />
          변경 이력 보기
        </Link>
      </footer>
    </div>
  );
}
