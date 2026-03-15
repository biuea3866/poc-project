"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import {
  SearchResult,
  VectorSearchResult,
  VectorSearchResponse,
  PaginatedResponse,
} from "@/types/document";
import Link from "next/link";

type SearchMode = "keyword" | "semantic" | "hybrid";

const tabs: { key: SearchMode; label: string; description: string }[] = [
  { key: "keyword", label: "키워드", description: "제목과 내용에서 키워드를 검색합니다." },
  { key: "semantic", label: "시맨틱", description: "AI가 의미 기반으로 관련 문서를 찾습니다." },
  { key: "hybrid", label: "하이브리드", description: "키워드 + 시맨틱 통합 랭킹 결과입니다." },
];

function SimilarityBar({ score }: { score: number }) {
  const percent = Math.round(score * 100);
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-20 overflow-hidden rounded-full bg-surface">
        <div
          className="h-full rounded-full bg-gradient-to-r from-accent to-accent-purple transition-all duration-500"
          style={{ width: `${percent}%` }}
        />
      </div>
      <span className="text-xs font-semibold text-accent">{percent}%</span>
    </div>
  );
}

function HighlightSnippet({ text, query }: { text: string; query: string }) {
  if (!query.trim()) return <span>{text}</span>;

  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi");
  const parts = text.split(regex);

  return (
    <span>
      {parts.map((part, i) =>
        regex.test(part) ? (
          <mark key={i} className="rounded bg-warning/20 px-0.5 text-primary">
            {part}
          </mark>
        ) : (
          <span key={i}>{part}</span>
        )
      )}
    </span>
  );
}

export default function SearchPage() {
  const [mode, setMode] = useState<SearchMode>("keyword");
  const [query, setQuery] = useState("");
  const [searchTerm, setSearchTerm] = useState("");

  // Keyword search (integrated)
  const keywordQuery = useQuery<PaginatedResponse<SearchResult>>({
    queryKey: ["search", "keyword", searchTerm],
    queryFn: () =>
      apiFetch<PaginatedResponse<SearchResult>>(
        `/api/v1/search/integrated?q=${encodeURIComponent(searchTerm)}&page=0&size=20`
      ),
    enabled: searchTerm.length > 0 && mode === "keyword",
  });

  // Semantic (vector) search
  const semanticQuery = useQuery<VectorSearchResponse>({
    queryKey: ["search", "semantic", searchTerm],
    queryFn: () =>
      apiFetch<VectorSearchResponse>("/api/v1/search/vector", {
        method: "POST",
        body: JSON.stringify({ query: searchTerm }),
      }),
    enabled: searchTerm.length > 0 && mode === "semantic",
  });

  // Hybrid search (both queries)
  const hybridKeywordQuery = useQuery<PaginatedResponse<SearchResult>>({
    queryKey: ["search", "hybrid-keyword", searchTerm],
    queryFn: () =>
      apiFetch<PaginatedResponse<SearchResult>>(
        `/api/v1/search/integrated?q=${encodeURIComponent(searchTerm)}&page=0&size=10`
      ),
    enabled: searchTerm.length > 0 && mode === "hybrid",
  });

  const hybridSemanticQuery = useQuery<VectorSearchResponse>({
    queryKey: ["search", "hybrid-semantic", searchTerm],
    queryFn: () =>
      apiFetch<VectorSearchResponse>("/api/v1/search/vector", {
        method: "POST",
        body: JSON.stringify({ query: searchTerm }),
      }),
    enabled: searchTerm.length > 0 && mode === "hybrid",
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      setSearchTerm(query.trim());
    }
  };

  const isLoading =
    (mode === "keyword" && keywordQuery.isLoading) ||
    (mode === "semantic" && semanticQuery.isLoading) ||
    (mode === "hybrid" && (hybridKeywordQuery.isLoading || hybridSemanticQuery.isLoading));

  const isFetching =
    (mode === "keyword" && keywordQuery.isFetching) ||
    (mode === "semantic" && semanticQuery.isFetching) ||
    (mode === "hybrid" && (hybridKeywordQuery.isFetching || hybridSemanticQuery.isFetching));

  const activeTab = tabs.find((t) => t.key === mode)!;

  return (
    <div className="mx-auto max-w-3xl space-y-6 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold text-primary">검색</h1>
        <p className="mt-2 text-secondary">{activeTab.description}</p>
      </header>

      {/* Tabs */}
      <div className="flex gap-1 rounded-xl bg-surface p-1">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => {
              setMode(tab.key);
              if (searchTerm) {
                // Re-trigger search on tab switch (React Query handles caching)
              }
            }}
            className={`flex-1 rounded-lg px-4 py-2.5 text-sm font-semibold transition ${
              mode === tab.key
                ? "bg-white text-accent shadow-sm"
                : "text-muted hover:text-secondary"
            }`}
          >
            {tab.label}
            {tab.key === "semantic" && (
              <span className="ml-1.5 rounded-full bg-gradient-to-r from-accent to-accent-purple px-1.5 py-0.5 text-[10px] font-bold text-white">
                AI
              </span>
            )}
          </button>
        ))}
      </div>

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
          placeholder={
            mode === "semantic"
              ? "자연어로 검색하세요... (예: 인증 처리 방법)"
              : "검색어를 입력하세요..."
          }
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
      {searchTerm && !isLoading && (
        <div className="space-y-4">
          {/* Keyword results */}
          {mode === "keyword" && keywordQuery.data && (
            <KeywordResults data={keywordQuery.data} searchTerm={searchTerm} />
          )}

          {/* Semantic results */}
          {mode === "semantic" && semanticQuery.data && (
            <SemanticResults data={semanticQuery.data} searchTerm={searchTerm} />
          )}

          {/* Hybrid results */}
          {mode === "hybrid" && (
            <HybridResults
              keywordData={hybridKeywordQuery.data}
              semanticData={hybridSemanticQuery.data}
              searchTerm={searchTerm}
            />
          )}
        </div>
      )}

      {/* Loading skeleton */}
      {searchTerm && isLoading && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-28 animate-pulse rounded-xl border border-line bg-white" />
          ))}
        </div>
      )}

      {/* Empty state (no search) */}
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

/* ── Keyword Results ── */
function KeywordResults({
  data,
  searchTerm,
}: {
  data: PaginatedResponse<SearchResult>;
  searchTerm: string;
}) {
  return (
    <>
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
              <h3 className="font-semibold text-primary">
                <HighlightSnippet text={result.title} query={searchTerm} />
              </h3>
              {result.summary && (
                <p className="mt-1 text-sm italic text-accent">{result.summary}</p>
              )}
              <p className="mt-2 text-sm text-muted line-clamp-2">
                <HighlightSnippet text={result.content} query={searchTerm} />
              </p>
              {result.tags && result.tags.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {result.tags.map((tag) => (
                    <span key={tag.id} className="rounded-full bg-accent-light px-2.5 py-0.5 text-xs font-medium text-accent">
                      {tag.name}
                    </span>
                  ))}
                </div>
              )}
            </Link>
          ))}
        </div>
      ) : (
        <EmptyResult />
      )}
    </>
  );
}

/* ── Semantic Results ── */
function SemanticResults({
  data,
  searchTerm,
}: {
  data: VectorSearchResponse;
  searchTerm: string;
}) {
  return (
    <>
      <p className="text-sm text-muted">
        &quot;{searchTerm}&quot; 시맨틱 검색 결과 {data.total}건
      </p>
      {data.results.length > 0 ? (
        <div className="space-y-3">
          {data.results.map((result) => (
            <Link
              key={result.id}
              href={`/documents/${result.id}`}
              className="block rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
            >
              <div className="flex items-start justify-between gap-4">
                <h3 className="font-semibold text-primary">{result.title}</h3>
                <SimilarityBar score={result.similarityScore} />
              </div>
              {result.snippet && (
                <p className="mt-2 text-sm text-secondary line-clamp-3">
                  <HighlightSnippet text={result.snippet} query={searchTerm} />
                </p>
              )}
              {!result.snippet && result.content && (
                <p className="mt-2 text-sm text-muted line-clamp-2">
                  {result.content.slice(0, 200)}
                </p>
              )}
              {result.tags && result.tags.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {result.tags.map((tag) => (
                    <span key={tag.id} className="rounded-full bg-accent-light px-2.5 py-0.5 text-xs font-medium text-accent">
                      {tag.name}
                    </span>
                  ))}
                </div>
              )}
            </Link>
          ))}
        </div>
      ) : (
        <EmptyResult />
      )}
    </>
  );
}

/* ── Hybrid Results ── */
function HybridResults({
  keywordData,
  semanticData,
  searchTerm,
}: {
  keywordData?: PaginatedResponse<SearchResult>;
  semanticData?: VectorSearchResponse;
  searchTerm: string;
}) {
  const keywordResults = keywordData?.results ?? [];
  const semanticResults = semanticData?.results ?? [];

  if (keywordResults.length === 0 && semanticResults.length === 0) {
    return <EmptyResult />;
  }

  return (
    <div className="space-y-8">
      {/* Semantic section */}
      {semanticResults.length > 0 && (
        <section>
          <div className="mb-3 flex items-center gap-2">
            <span className="rounded-full bg-gradient-to-r from-accent to-accent-purple px-2 py-0.5 text-[10px] font-bold text-white">
              AI
            </span>
            <h2 className="text-sm font-bold text-secondary">
              의미 기반 결과 ({semanticResults.length})
            </h2>
          </div>
          <div className="space-y-3">
            {semanticResults.slice(0, 5).map((result) => (
              <Link
                key={`sem-${result.id}`}
                href={`/documents/${result.id}`}
                className="block rounded-xl border border-accent/20 bg-accent-light/30 p-5 transition hover:shadow-card"
              >
                <div className="flex items-start justify-between gap-4">
                  <h3 className="font-semibold text-primary">{result.title}</h3>
                  <SimilarityBar score={result.similarityScore} />
                </div>
                {result.snippet && (
                  <p className="mt-2 text-sm text-secondary line-clamp-2">
                    <HighlightSnippet text={result.snippet} query={searchTerm} />
                  </p>
                )}
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Keyword section */}
      {keywordResults.length > 0 && (
        <section>
          <h2 className="mb-3 text-sm font-bold text-secondary">
            키워드 결과 ({keywordResults.length})
          </h2>
          <div className="space-y-3">
            {keywordResults.slice(0, 5).map((result) => (
              <Link
                key={`kw-${result.id}`}
                href={`/documents/${result.id}`}
                className="block rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
              >
                <h3 className="font-semibold text-primary">
                  <HighlightSnippet text={result.title} query={searchTerm} />
                </h3>
                <p className="mt-2 text-sm text-muted line-clamp-2">
                  <HighlightSnippet text={result.content} query={searchTerm} />
                </p>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

/* ── Empty State ── */
function EmptyResult() {
  return (
    <div className="flex h-48 flex-col items-center justify-center rounded-xl border border-dashed border-line text-muted">
      <svg className="mb-3 h-10 w-10 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      <p>관련 문서를 찾지 못했습니다.</p>
      <p className="mt-1 text-xs">다른 검색어나 검색 모드를 시도해보세요.</p>
    </div>
  );
}
