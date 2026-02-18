"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, Tag, DocumentSummary } from "@/types/document";
import MarkdownRenderer from "@/components/MarkdownRenderer";
import { 
  Calendar, 
  User, 
  Sparkles, 
  Tag as TagIcon, 
  Clock, 
  Trash2,
  Edit3,
  History,
  AlertCircle
} from "lucide-react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";

export default function DocumentPage() {
  const { id } = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();

  // 문서 정보 조회
  const { data: document, isLoading: isDocLoading } = useQuery<Document>({
    queryKey: ["document", id],
    queryFn: () => apiFetch<Document>(`/api/v1/documents/${id}`)
  });

  // 태그 조회
  const { data: tags } = useQuery<Tag[]>({
    queryKey: ["document-tags", id],
    queryFn: () => apiFetch<Tag[]>(`/api/v1/documents/${id}/tags`)
  });

  // 요약 조회
  const { data: summary } = useQuery<DocumentSummary>({
    queryKey: ["document-summary", id],
    queryFn: () => apiFetch<DocumentSummary>(`/api/v1/documents/${id}/summary`),
    enabled: document?.aiStatus === "COMPLETED"
  });

  // 삭제 뮤테이션 (Soft Delete)
  const deleteMutation = useMutation({
    mutationFn: () => apiFetch(`/api/v1/documents/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      router.push("/dashboard");
    }
  });

  if (isDocLoading) {
    return (
      <div className="space-y-8 animate-in fade-in duration-500">
        <div className="h-12 w-2/3 animate-pulse rounded-lg bg-white" />
        <div className="flex gap-4">
          <div className="h-4 w-24 animate-pulse rounded bg-[#e9ecef]" />
          <div className="h-4 w-24 animate-pulse rounded bg-[#e9ecef]" />
        </div>
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex h-[60vh] flex-col items-center justify-center text-[#adb5bd]">
        <AlertCircle className="mb-4 h-12 w-12 opacity-20" />
        <p>문서를 찾을 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-12 py-10 animate-in fade-in slide-in-from-bottom-4 duration-700">
      <header className="space-y-8">
        <h1 className="text-4xl font-extrabold tracking-tight text-[#212529] md:text-5xl">
          {document.title}
        </h1>

        <div className="flex flex-wrap items-center justify-between gap-6 text-sm text-[#495057]">
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
              className="flex items-center gap-1.5 text-[#868e96] hover:text-[#212529]"
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
              className="flex items-center gap-1.5 text-[#fa5252] hover:text-[#c92a2a]"
            >
              <Trash2 className="h-4 w-4" />
              삭제
            </button>
          </div>
        </div>

        {tags && tags.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {tags.map((tag) => (
              <span key={tag.id} className="rounded-full bg-[#f1f3f5] px-3 py-1 text-sm text-[#12b886] font-medium transition hover:bg-[#e9ecef]">
                # {tag.name}
              </span>
            ))}
          </div>
        )}
      </header>

      {document.aiStatus === "COMPLETED" && summary && (
        <section className="rounded-xl border border-[#e9ecef] bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center gap-2 text-[#12b886]">
            <Sparkles className="h-5 w-5" />
            <h2 className="text-sm font-bold uppercase tracking-wider">AI 요약</h2>
          </div>
          <p className="text-lg leading-relaxed text-[#495057] italic">
            &quot;{summary.content}&quot;
          </p>
        </section>
      )}

      <article className="min-h-[400px]">
        <MarkdownRenderer content={document.content || ""} />
      </article>

      <footer className="flex items-center justify-between border-t border-[#e9ecef] pt-8 text-sm text-[#868e96]">
        <div className="flex items-center gap-1.5">
          <Clock className="h-4 w-4" />
          <span>최근 수정: {new Date(document.updatedAt).toLocaleString()}</span>
        </div>
        <Link 
          href={`/documents/${id}/revisions`}
          className="flex items-center gap-1.5 font-bold text-[#12b886] hover:underline"
        >
          <History className="h-4 w-4" />
          변경 이력 보기
        </Link>
      </footer>
    </div>
  );
}
