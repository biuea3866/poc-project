"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, DocumentRevision } from "@/types/document";
import { 
  History, 
  ChevronLeft, 
  User, 
  Clock, 
  FileCode,
  ArrowRight
} from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import MarkdownRenderer from "@/components/MarkdownRenderer";

export default function RevisionHistoryPage() {
  const { id } = useParams();
  const [selectedRevision, setSelectedRevision] = useState<DocumentRevision | null>(null);

  // 문서 정보 조회 (제목용)
  const { data: document } = useQuery<Document>({
    queryKey: ["document", id],
    queryFn: () => apiFetch<Document>(`/api/v1/documents/${id}`)
  });

  // 변경 이력 목록 조회
  const { data: revisions, isLoading } = useQuery<DocumentRevision[]>({
    queryKey: ["document-revisions", id],
    queryFn: () => apiFetch<DocumentRevision[]>(`/api/v1/documents/${id}/revisions`)
  });

  return (
    <div className="space-y-10 py-6 animate-in fade-in duration-500">
      <header className="flex items-center gap-4">
        <Link 
          href={`/documents/${id}`}
          className="rounded-full p-2 text-[#868e96] hover:bg-[#f1f3f5] hover:text-[#212529]"
        >
          <ChevronLeft className="h-6 w-6" />
        </Link>
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-[#212529]">변경 이력</h1>
          <p className="mt-1 text-[#495057]">{document?.title}의 과거 기록입니다.</p>
        </div>
      </header>

      <div className="grid gap-8 lg:grid-cols-[350px_1fr]">
        {/* Revision List */}
        <aside className="space-y-4">
          <h2 className="text-sm font-bold uppercase tracking-widest text-[#868e96] px-2">버전 목록</h2>
          <div className="space-y-2">
            {isLoading ? (
              [1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-16 w-full animate-pulse rounded-lg bg-white shadow-sm" />
              ))
            ) : revisions && revisions.length > 0 ? (
              revisions.map((rev, index) => {
                const isSelected = selectedRevision?.id === rev.id;
                // rev.data는 전체 정보를 담은 JSON 문자열이므로 파싱이 필요할 수 있음
                const revData = JSON.parse(rev.data);
                
                return (
                  <button
                    key={rev.id}
                    onClick={() => setSelectedRevision(rev)}
                    className={cn(
                      "w-full flex flex-col gap-1 rounded-lg p-4 text-left transition shadow-sm",
                      isSelected 
                        ? "bg-[#12b886] text-white shadow-md" 
                        : "bg-white text-[#495057] hover:border-[#dee2e6] border border-transparent"
                    )}
                  >
                    <div className="flex items-center justify-between font-bold">
                      <span>버전 {revisions.length - index}</span>
                      {index === 0 && (
                        <span className={cn(
                          "text-[10px] px-1.5 py-0.5 rounded-full uppercase tracking-tighter",
                          isSelected ? "bg-white/20 text-white" : "bg-[#f1f3f5] text-[#12b886]"
                        )}>
                          현재
                        </span>
                      )}
                    </div>
                    <div className={cn(
                      "flex items-center gap-2 text-xs",
                      isSelected ? "text-white/80" : "text-[#868e96]"
                    )}>
                      <Clock className="h-3 w-3" />
                      {new Date(rev.createdAt).toLocaleString()}
                    </div>
                  </button>
                );
              })
            ) : (
              <p className="text-sm text-[#adb5bd] px-2 italic">변경 이력이 없습니다.</p>
            )}
          </div>
        </aside>

        {/* Content Preview */}
        <main className="min-h-[600px] rounded-lg border border-[#e9ecef] bg-white p-8">
          {selectedRevision ? (
            <div className="space-y-8 animate-in fade-in slide-in-from-right-4 duration-300">
              <div className="flex items-center justify-between border-b border-[#f1f3f5] pb-6">
                <div>
                  <h3 className="text-2xl font-bold text-[#212529]">
                    {JSON.parse(selectedRevision.data).title}
                  </h3>
                  <div className="mt-2 flex items-center gap-4 text-sm text-[#868e96]">
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
                <button className="flex items-center gap-2 rounded-md border border-[#12b886] px-4 py-2 text-sm font-bold text-[#12b886] hover:bg-[#f1f3f5]">
                  이 버전으로 복구
                </button>
              </div>
              <article className="prose prose-slate max-w-none">
                <MarkdownRenderer content={JSON.parse(selectedRevision.data).content || ""} />
              </article>
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center text-[#adb5bd]">
              <History className="mb-4 h-12 w-12 opacity-10" />
              <p>이력을 확인하려면 왼쪽 목록에서 버전을 선택하세요.</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

// cn 유틸리티가 필요함
function cn(...inputs: any[]) {
  return inputs.filter(Boolean).join(" ");
}
