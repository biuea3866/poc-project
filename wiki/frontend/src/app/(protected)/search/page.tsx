"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { SearchResult, PaginatedResponse } from "@/types/document";
import Link from "next/link";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [searchTerm, setSearchTerm] = useState("");

  const { data, isLoading, isFetching } = useQuery<PaginatedResponse<SearchResult>>({
    queryKey: ["search", searchTerm],
    queryFn: () =>
      apiFetch<PaginatedResponse<SearchResult>>(
        `/api/v1/search/integrated?q=${encodeURIComponent(searchTerm)}&page=0&size=20`
      ),
    enabled: searchTerm.length > 0
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      setSearchTerm(query.trim());
    }
  };

  return (
    <div className="mx-auto max-w-3xl space-y-8 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold text-primary">검색</h1>
        <p className="mt-2 text-secondary">제목과 내용에서 키워드를 검색합니다.</p>
      </header>

      {/* Search input */}
      <form onSubmit={handleSubmit} className="relative">
        <svg
          className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="검색어를 입력하세요..."
          className="w-full rounded-xl border border-line bg-white py-4 pl-12 pr-4 text-base text-primary shadow-sm placeholder:text-muted focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
        />
        {isFetching && (
          <svg className="absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 animate-spin text-accent" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        )}
      </form>

      {/* Results */}
      {searchTerm && !isLoading && data && (
        <div className="space-y-4">
          <p className="text-sm text-muted">
            &quot;{searchTerm}&quot; 검색 결과 {data.total}건
          </p>

          {data.results.length > 0 ? (
            <div className="space-y-3">
              {data.results.map((result) => (
                <Link
                  key={result.id}
                  href={`/documents/${result.id}`}
                  className="block rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
                >
                  <h3 className="font-semibold text-primary">{result.title}</h3>

                  {result.summary && (
                    <p className="mt-1 text-sm italic text-accent">{result.summary}</p>
                  )}

                  <p className="mt-2 text-sm text-muted line-clamp-2">{result.content}</p>

                  {result.tags && result.tags.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {result.tags.map((tag) => (
                        <span
                          key={tag.id}
                          className="rounded-full bg-accent-light px-2.5 py-0.5 text-xs font-medium text-accent"
                        >
                          {tag.name}
                        </span>
                      ))}
                    </div>
                  )}
                </Link>
              ))}
            </div>
          ) : (
            <div className="flex h-48 flex-col items-center justify-center rounded-xl border border-dashed border-line text-muted">
              <svg className="mb-3 h-10 w-10 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <p>검색 결과가 없습니다.</p>
            </div>
          )}
        </div>
      )}

      {/* Empty state */}
      {!searchTerm && (
        <div className="flex h-48 flex-col items-center justify-center text-muted">
          <svg className="mb-3 h-10 w-10 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p>검색어를 입력하면 결과가 표시됩니다.</p>
        </div>
      )}
    </div>
  );
}
