"use client";

import { History } from "lucide-react";

export default function RevisionsPage() {
  return (
    <div className="space-y-8 py-6 animate-in">
      <header>
        <h1 className="text-3xl font-extrabold tracking-tight text-primary">변경 이력</h1>
        <p className="mt-2 text-secondary">전체 문서의 변경 이력을 확인합니다.</p>
      </header>

      <div className="flex h-64 flex-col items-center justify-center rounded-xl border border-dashed border-border-line text-disabled">
        <History className="mb-4 h-12 w-12 opacity-10" />
        <p>버전 히스토리 브라우저가 곧 연결됩니다.</p>
      </div>
    </div>
  );
}
