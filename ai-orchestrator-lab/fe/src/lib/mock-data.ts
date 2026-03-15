import type { DocumentCard } from "@/lib/types";

export const documents: DocumentCard[] = [
  {
    id: 1,
    title: "AI Wiki MVP 정리",
    excerpt: "ACTIVE 전환 이후 summary -> tagger -> embedding 파이프라인을 비동기로 실행한다.",
    status: "ACTIVE",
    aiStatus: "PROCESSING",
    tags: ["prd", "pipeline"],
    updatedAt: "2026-03-14 11:00",
  },
  {
    id: 2,
    title: "검색 제약 메모",
    excerpt: "검색 대상은 본인 ACTIVE 문서만 허용한다.",
    status: "DRAFT",
    aiStatus: "NOT_STARTED",
    tags: ["search", "auth"],
    updatedAt: "2026-03-14 09:20",
  },
];
