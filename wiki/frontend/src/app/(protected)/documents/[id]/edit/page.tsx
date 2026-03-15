"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document } from "@/types/document";
import MarkdownEditor from "@/components/MarkdownEditor";

export default function EditDocumentPage() {
  const { id } = useParams();

  const { data: document, isLoading } = useQuery<Document>({
    queryKey: ["document", id],
    queryFn: () => apiFetch<Document>(`/api/v1/documents/${id}`)
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="h-10 w-48 animate-pulse rounded-lg bg-white" />
        <div className="h-[600px] animate-pulse rounded-xl bg-white" />
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center text-muted">
        <p>문서를 찾을 수 없습니다.</p>
      </div>
    );
  }

  return <MarkdownEditor mode="edit" document={document} />;
}
