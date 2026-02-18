"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, Tag, DocumentSummary } from "@/types/document";
import MarkdownRenderer from "@/components/MarkdownRenderer";
import { useAiStatus } from "@/hooks/useAiStatus";
import { 
  Calendar, 
  User, 
  Sparkles, 
  Tag as TagIcon, 
  Clock, 
  Loader2,
  AlertCircle
} from "lucide-react";
import { useParams } from "next/navigation";
import { cn } from "@/lib/utils";

export default function DocumentPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  
  // SSE 실시간 상태 구독
  const realTimeStatus = useAiStatus(id as string);

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
    enabled: (document?.aiStatus === "COMPLETED" || realTimeStatus === "COMPLETED")
  });

  const displayStatus = realTimeStatus || document?.aiStatus || "PENDING";

  if (isDocLoading) return <div className="p-20 text-center"><Loader2 className="animate-spin mx-auto text-[#12b886]" /></div>;
  if (!document) return <div className="p-20 text-center">문서를 찾을 수 없습니다.</div>;

  return (
    <div className="mx-auto max-w-3xl space-y-12 py-10 animate-in fade-in duration-700">
      <header className="space-y-6">
        <h1 className="text-4xl font-extrabold text-[#212529]">{document.title}</h1>
        
        <div className="flex items-center gap-6 text-sm text-[#495057]">
          <span className="font-bold">{document.createdBy}</span>
          <span>{new Date(document.createdAt).toLocaleDateString()}</span>
          
          {/* AI 상태 표시기 */}
          <div className={cn(
            "flex items-center gap-2 font-medium transition-colors duration-500",
            displayStatus === "COMPLETED" ? "text-[#12b886]" : 
            displayStatus === "PROCESSING" ? "text-[#f97316]" : "text-[#adb5bd]"
          )}>
            {displayStatus === "PROCESSING" ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Sparkles className="h-4 w-4" />
            )}
            <span>AI {
              displayStatus === "COMPLETED" ? "분석 완료" : 
              displayStatus === "PROCESSING" ? "분석 중..." : "대기 중"
            }</span>
          </div>
        </div>
      </header>

      {/* AI 요약 패널 - 상태에 따라 실시간 노출 */}
      {(displayStatus === "PROCESSING" || displayStatus === "COMPLETED") && (
        <section className={cn(
          "rounded-xl border p-6 transition-all duration-500",
          displayStatus === "COMPLETED" ? "border-[#12b886] bg-[#f1f3f5]/50" : "border-[#e9ecef] bg-white"
        )}>
          <div className="mb-4 flex items-center gap-2 text-[#12b886]">
            <Sparkles className="h-5 w-5" />
            <h2 className="text-sm font-bold uppercase tracking-wider">AI 요약</h2>
          </div>
          {displayStatus === "PROCESSING" ? (
            <div className="space-y-2">
              <div className="h-4 w-full animate-pulse rounded bg-[#e9ecef]" />
              <div className="h-4 w-2/3 animate-pulse rounded bg-[#e9ecef]" />
            </div>
          ) : summary ? (
            <p className="text-lg leading-relaxed text-[#495057] italic">
              &quot;{summary.content}&quot;
            </p>
          ) : null}
        </section>
      )}

      <article className="min-h-[400px]">
        <MarkdownRenderer content={document.content || ""} />
      </article>
    </div>
  );
}
