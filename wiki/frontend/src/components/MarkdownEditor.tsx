"use client";

import { useState, useCallback } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { Document, DocumentStatus } from "@/types/document";

interface MarkdownEditorProps {
  mode: "create" | "edit";
  document?: Document;
}

export default function MarkdownEditor({ mode, document }: MarkdownEditorProps) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState(document?.title ?? "");
  const [content, setContent] = useState(document?.content ?? "");
  const [parentId, setParentId] = useState<number | null>(document?.parentId ?? null);
  const [showPreview, setShowPreview] = useState(false);

  const saveMutation = useMutation({
    mutationFn: async (status: DocumentStatus) => {
      const body = { title, content, parentId, status };

      if (mode === "create") {
        return apiFetch<Document>("/api/v1/documents", {
          method: "POST",
          body: JSON.stringify(body)
        });
      } else {
        return apiFetch<Document>(`/api/v1/documents/${document!.id}`, {
          method: "PUT",
          body: JSON.stringify(body)
        });
      }
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      queryClient.invalidateQueries({ queryKey: ["document", String(result.id)] });
      router.push(`/documents/${result.id}`);
    }
  });

  const handleSaveDraft = useCallback(() => {
    saveMutation.mutate("DRAFT");
  }, [saveMutation]);

  const handlePublish = useCallback(() => {
    saveMutation.mutate("ACTIVE");
  }, [saveMutation]);

  const isSubmitting = saveMutation.isPending;

  return (
    <div className="mx-auto max-w-4xl animate-in">
      {/* Toolbar */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => router.back()}
            className="rounded-lg border border-line px-3 py-2 text-sm text-secondary transition hover:bg-surface"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <h1 className="text-lg font-bold text-primary">
            {mode === "create" ? "새 문서 작성" : "문서 수정"}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          {/* Preview toggle */}
          <button
            type="button"
            onClick={() => setShowPreview(!showPreview)}
            className={`rounded-lg border px-3 py-2 text-sm transition ${
              showPreview
                ? "border-accent bg-accent-light text-accent"
                : "border-line text-secondary hover:bg-surface"
            }`}
          >
            {showPreview ? "편집" : "미리보기"}
          </button>

          {/* Save Draft */}
          <button
            type="button"
            onClick={handleSaveDraft}
            disabled={isSubmitting || !title.trim()}
            className="rounded-lg border border-line px-4 py-2 text-sm font-medium text-secondary transition hover:bg-surface disabled:opacity-50"
          >
            {isSubmitting ? "저장 중..." : "임시저장"}
          </button>

          {/* Publish */}
          <button
            type="button"
            onClick={handlePublish}
            disabled={isSubmitting || !title.trim() || !content.trim()}
            className="rounded-lg bg-gradient-to-r from-accent to-accent-purple px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
          >
            {isSubmitting ? "발행 중..." : "발행"}
          </button>
        </div>
      </div>

      {/* Error */}
      {saveMutation.isError && (
        <div className="mb-4 rounded-lg border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">
          {saveMutation.error instanceof Error ? saveMutation.error.message : "저장에 실패했습니다."}
        </div>
      )}

      {/* Editor area */}
      <div className="rounded-xl border border-line bg-white shadow-sm">
        {/* Title input */}
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="제목을 입력하세요"
          className="w-full border-b border-line px-8 py-6 text-3xl font-extrabold text-primary placeholder:text-muted/50 focus:outline-none"
        />

        {showPreview ? (
          /* Preview mode */
          <div className="min-h-[500px] px-8 py-6">
            <div className="prose prose-slate max-w-none prose-headings:text-primary prose-p:text-secondary prose-a:text-accent prose-code:text-accent prose-code:bg-surface prose-code:px-1 prose-code:rounded prose-pre:bg-primary prose-pre:text-surface">
              {content ? (
                <div dangerouslySetInnerHTML={{ __html: content.replace(/\n/g, "<br/>") }} />
              ) : (
                <p className="text-muted">미리보기할 내용이 없습니다.</p>
              )}
            </div>
          </div>
        ) : (
          /* Edit mode */
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Markdown으로 내용을 작성하세요..."
            className="min-h-[500px] w-full resize-none px-8 py-6 text-base leading-relaxed text-secondary placeholder:text-muted/50 focus:outline-none"
            style={{ fontFamily: "monospace" }}
          />
        )}
      </div>

      {/* Footer info */}
      <div className="mt-4 flex items-center justify-between text-xs text-muted">
        <span>Markdown 문법을 지원합니다</span>
        <span>{content.length}자</span>
      </div>
    </div>
  );
}
