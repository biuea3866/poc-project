"use client";

import { Search } from "lucide-react";

export default function SearchPage() {
  return (
    <div className="space-y-8 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold tracking-tight text-primary">검색</h1>
        <p className="mt-2 text-secondary">키워드와 AI 기반 통합 검색 결과를 확인하세요.</p>
      </header>

      <div className="relative">
        <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted" />
        <input
          type="text"
          placeholder="문서 제목, 내용, 태그 검색..."
          className="w-full rounded-xl border border-border-line bg-surface py-4 pl-12 pr-4 text-lg text-primary placeholder:text-disabled transition focus:border-accent focus:ring-2 focus:ring-accent/20 focus:outline-none"
        />
      </div>

      <div className="flex h-64 flex-col items-center justify-center text-disabled">
        <Search className="mb-4 h-12 w-12 opacity-10" />
        <p>검색어를 입력하면 결과가 여기에 표시됩니다.</p>
      </div>
    </div>
  );
}
