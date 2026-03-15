"use client";

import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { clearTokens } from "@/lib/auth";
import { Document } from "@/types/document";
import Link from "next/link";

export default function DashboardPage() {
  const router = useRouter();

  const { data: documents, isLoading } = useQuery<Document[]>({
    queryKey: ["documents"],
    queryFn: () => apiFetch<Document[]>("/api/v1/documents")
  });

  // Flatten tree to get all documents for the grid
  function flattenDocs(docs: Document[]): Document[] {
    const result: Document[] = [];
    for (const doc of docs) {
      result.push(doc);
      if (doc.children) {
        result.push(...flattenDocs(doc.children));
      }
    }
    return result;
  }

  const allDocs = documents ? flattenDocs(documents) : [];

  return (
    <div className="flex flex-col gap-6 animate-in">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-extrabold text-primary">문서</h1>
        <div className="flex items-center gap-3">
          <Link
            href="/search"
            className="rounded-lg border border-line px-4 py-2 text-sm font-medium text-secondary transition hover:border-accent hover:text-accent"
          >
            검색
          </Link>
          <Link
            href="/documents/new"
            className="rounded-lg bg-gradient-to-r from-accent to-accent-purple px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90"
          >
            새 문서
          </Link>
          <button
            type="button"
            onClick={() => {
              clearTokens();
              router.replace("/login");
            }}
            className="rounded-lg border border-line px-4 py-2 text-sm text-secondary transition hover:border-danger hover:text-danger"
          >
            로그아웃
          </button>
        </div>
      </header>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="h-36 animate-pulse rounded-xl bg-white shadow-sm" />
          ))}
        </div>
      ) : allDocs.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {allDocs.map((doc) => (
            <Link
              key={doc.id}
              href={`/documents/${doc.id}`}
              className="rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
            >
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold text-primary">{doc.title}</h3>
                {doc.status === "DRAFT" && (
                  <span className="flex-shrink-0 rounded bg-warning/10 px-1.5 py-0.5 text-[10px] font-semibold text-warning">
                    DRAFT
                  </span>
                )}
              </div>

              {doc.content && (
                <p className="mt-2 line-clamp-2 text-sm text-muted">
                  {doc.content.slice(0, 120)}
                </p>
              )}

              <div className="mt-4 flex items-center justify-between">
                {/* AI Status indicator */}
                <div className="flex items-center gap-1.5 text-xs">
                  {doc.aiStatus === "COMPLETED" && (
                    <span className="flex items-center gap-1 text-success">
                      <span className="h-1.5 w-1.5 rounded-full bg-success" />
                      AI 완료
                    </span>
                  )}
                  {doc.aiStatus === "PROCESSING" && (
                    <span className="flex items-center gap-1 text-accent">
                      <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-accent" />
                      처리 중
                    </span>
                  )}
                  {doc.aiStatus === "PENDING" && (
                    <span className="flex items-center gap-1 text-muted">
                      <span className="h-1.5 w-1.5 rounded-full bg-muted" />
                      대기 중
                    </span>
                  )}
                  {doc.aiStatus === "FAILED" && (
                    <span className="flex items-center gap-1 text-danger">
                      <span className="h-1.5 w-1.5 rounded-full bg-danger" />
                      실패
                    </span>
                  )}
                </div>
                <span className="text-xs text-muted">
                  {new Date(doc.updatedAt).toLocaleDateString()}
                </span>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="flex h-64 flex-col items-center justify-center rounded-xl border border-dashed border-line text-muted">
          <svg className="mb-4 h-12 w-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p>문서가 없습니다. 새 문서를 작성해보세요.</p>
        </div>
      )}
    </div>
  );
}
