"use client";

import { useRouter } from "next/navigation";
import { clearTokens } from "@/lib/auth";

const mockDocuments = [
  {
    id: 1,
    title: "Product Roadmap 2026",
    excerpt: "Q1 목표와 핵심 마일스톤을 정리한 문서입니다.",
    date: "2026-03-15",
    tags: ["Product", "Planning"]
  },
  {
    id: 2,
    title: "Architecture Decision Records",
    excerpt: "시스템 아키텍처 관련 주요 결정 사항을 기록합니다.",
    date: "2026-03-14",
    tags: ["Engineering"]
  },
  {
    id: 3,
    title: "Release Checklist",
    excerpt: "배포 전 확인해야 할 체크리스트입니다.",
    date: "2026-03-13",
    tags: ["DevOps", "Release"]
  },
  {
    id: 4,
    title: "Customer Insights",
    excerpt: "고객 인터뷰 및 피드백 요약 문서입니다.",
    date: "2026-03-12",
    tags: ["Research"]
  },
  {
    id: 5,
    title: "API Design Guidelines",
    excerpt: "REST API 설계 가이드라인과 컨벤션을 정의합니다.",
    date: "2026-03-11",
    tags: ["Engineering", "API"]
  },
  {
    id: 6,
    title: "Onboarding Guide",
    excerpt: "신규 팀원을 위한 온보딩 가이드 문서입니다.",
    date: "2026-03-10",
    tags: ["HR"]
  }
];

export default function DashboardPage() {
  const router = useRouter();

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-extrabold text-primary">문서</h1>
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
      </header>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {mockDocuments.map((doc) => (
          <div
            key={doc.id}
            className="rounded-xl border border-line bg-white p-5 transition hover:shadow-card"
          >
            <h3 className="font-semibold text-primary">{doc.title}</h3>
            <p className="mt-2 text-sm text-muted">{doc.excerpt}</p>
            <div className="mt-4 flex items-center justify-between">
              <div className="flex flex-wrap gap-1.5">
                {doc.tags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full bg-accent-light px-2.5 py-0.5 text-xs font-medium text-accent"
                  >
                    {tag}
                  </span>
                ))}
              </div>
              <span className="text-xs text-muted">{doc.date}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
