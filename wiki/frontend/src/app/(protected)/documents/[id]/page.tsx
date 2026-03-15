"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, Tag, DocumentSummary } from "@/types/document";
import MarkdownRenderer from "@/components/MarkdownRenderer";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";

function AiStatusSection({ document, summary, tags }: {
  document: Document;
  summary?: DocumentSummary;
  tags?: Tag[];
}) {
  const queryClient = useQueryClient();

  const retryMutation = useMutation({
    mutationFn: () => apiFetch(`/api/v1/documents/${document.id}/analyze`, { method: "POST" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["document", String(document.id)] });
    }
  });

  if (document.aiStatus === "PENDING" || document.aiStatus === "PROCESSING") {
    return (
      <section className="rounded-xl border border-line bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center gap-3">
          {/* Spinner */}
          <svg className="h-5 w-5 animate-spin text-accent" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <h2 className="text-sm font-bold uppercase tracking-wider text-accent">
            AI {document.aiStatus === "PENDING" ? "대기 중" : "처리 중"}...
          </h2>
        </div>
        <div className="space-y-3">
          <div className="h-4 w-full animate-pulse rounded bg-surface" />
          <div className="h-4 w-3/4 animate-pulse rounded bg-surface" />
          <div className="h-4 w-1/2 animate-pulse rounded bg-surface" />
        </div>
        <div className="mt-4 flex gap-2">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-6 w-16 animate-pulse rounded-full bg-surface" />
          ))}
        </div>
      </section>
    );
  }

  if (document.aiStatus === "FAILED") {
    return (
      <section className="rounded-xl border border-danger/30 bg-danger/5 p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {/* Alert icon */}
            <svg className="h-5 w-5 text-danger" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <div>
              <h2 className="text-sm font-bold text-danger">AI 분석 실패</h2>
              <p className="mt-1 text-sm text-secondary">요약 및 태그 생성에 실패했습니다.</p>
            </div>
          </div>
          <button
            onClick={() => retryMutation.mutate()}
            disabled={retryMutation.isPending}
            className="flex items-center gap-2 rounded-lg border border-danger/30 px-4 py-2 text-sm font-medium text-danger transition hover:bg-danger/10 disabled:opacity-50"
          >
            <svg className={`h-4 w-4 ${retryMutation.isPending ? "animate-spin" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {retryMutation.isPending ? "재시도 중..." : "재시도"}
          </button>
        </div>
      </section>
    );
  }

  // COMPLETED
  if (document.aiStatus === "COMPLETED" && summary) {
    return (
      <section className="rounded-xl border border-line bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center gap-2 text-success">
          {/* Sparkles icon */}
          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
          </svg>
          <h2 className="text-sm font-bold uppercase tracking-wider">AI 요약</h2>
        </div>
        <p className="text-lg leading-relaxed text-secondary italic">
          &quot;{summary.content}&quot;
        </p>
        {tags && tags.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {tags.map((tag) => (
              <span key={tag.id} className="rounded-full bg-accent-light px-3 py-1 text-sm font-medium text-accent">
                # {tag.name}
              </span>
            ))}
          </div>
        )}
      </section>
    );
  }

  return null;
}

export default function DocumentPage() {
  const { id } = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: document, isLoading: isDocLoading } = useQuery<Document>({
    queryKey: ["document", id],
    queryFn: () => apiFetch<Document>(`/api/v1/documents/${id}`),
    refetchInterval: (query) => {
      const doc = query.state.data;
      if (doc && (doc.aiStatus === "PENDING" || doc.aiStatus === "PROCESSING")) {
        return 3000; // Poll every 3s while AI is working
      }
      return false;
    }
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
        <div className="h-12 w-2/3 animate-pulse rounded-lg bg-white" />
        <div className="flex gap-4">
          <div className="h-4 w-24 animate-pulse rounded bg-line" />
          <div className="h-4 w-24 animate-pulse rounded bg-line" />
        </div>
        <div className="h-[400px] animate-pulse rounded-xl bg-white" />
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center text-muted">
        <svg className="mb-4 h-12 w-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
        <p>문서를 찾을 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-10 py-10 animate-in">
      <header className="space-y-6">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-4xl font-extrabold tracking-tight text-primary">
            {document.title}
          </h1>
          {document.status === "DRAFT" && (
            <span className="flex-shrink-0 rounded-full bg-warning/10 px-3 py-1 text-sm font-semibold text-warning">
              DRAFT
            </span>
          )}
        </div>

        <div className="flex flex-wrap items-center justify-between gap-6 text-sm text-secondary">
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2 font-bold">
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              <span>작성자 ID: {document.createdBy}</span>
            </div>
            <div className="flex items-center gap-2">
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span>{new Date(document.createdAt).toLocaleDateString()}</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <Link
              href={`/documents/${id}/edit`}
              className="flex items-center gap-1.5 text-muted hover:text-primary"
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              수정
            </Link>
            <button
              onClick={() => {
                if (confirm("문서를 삭제하시겠습니까? 삭제된 문서는 휴지통으로 이동합니다.")) {
                  deleteMutation.mutate();
                }
              }}
              className="flex items-center gap-1.5 text-danger hover:text-danger/80"
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              삭제
            </button>
          </div>
        </div>
      </header>

      {/* AI Status Section */}
      <AiStatusSection document={document} summary={summary} tags={tags} />

      {/* Content */}
      <article className="min-h-[400px]">
        <MarkdownRenderer content={document.content || ""} />
      </article>

      {/* Footer */}
      <footer className="flex items-center justify-between border-t border-line pt-8 text-sm text-muted">
        <div className="flex items-center gap-1.5">
          <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>최근 수정: {new Date(document.updatedAt).toLocaleString()}</span>
        </div>
        <Link
          href={`/documents/${id}/revisions`}
          className="flex items-center gap-1.5 font-bold text-accent hover:underline"
        >
          <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          변경 이력 보기
        </Link>
      </footer>
    </div>
  );
}
