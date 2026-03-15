"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { DocumentRevision } from "@/types/document";
import Link from "next/link";

export default function RevisionsPage() {
  const { data: revisions, isLoading } = useQuery<DocumentRevision[]>({
    queryKey: ["all-revisions"],
    queryFn: () => apiFetch<DocumentRevision[]>("/api/v1/documents/revisions?page=0&size=50")
  });

  return (
    <div className="space-y-8 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold text-primary">변경 이력</h1>
        <p className="mt-2 text-secondary">전체 문서의 변경 이력을 확인합니다.</p>
      </header>

      <section className="space-y-3">
        {isLoading ? (
          [1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-16 w-full animate-pulse rounded-lg bg-white shadow-sm" />
          ))
        ) : revisions && revisions.length > 0 ? (
          revisions.map((rev) => {
            let revData: { title?: string } = {};
            try {
              revData = JSON.parse(rev.data);
            } catch {}

            return (
              <Link
                key={rev.id}
                href={`/documents/${rev.documentId}/revisions`}
                className="flex items-center justify-between rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
              >
                <div className="flex items-center gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent-light text-accent">
                    <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <div>
                    <h3 className="font-semibold text-primary">{revData.title || `문서 #${rev.documentId}`}</h3>
                    <p className="mt-0.5 text-xs text-muted">
                      {new Date(rev.createdAt).toLocaleString()} · 작성자 ID: {rev.createdBy}
                    </p>
                  </div>
                </div>
                <svg className="h-4 w-4 text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </Link>
            );
          })
        ) : (
          <div className="flex h-64 flex-col items-center justify-center rounded-xl border border-dashed border-line text-muted">
            <svg className="mb-4 h-12 w-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p>변경 이력이 없습니다.</p>
          </div>
        )}
      </section>
    </div>
  );
}
