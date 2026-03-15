import type { DocumentCard } from "@/lib/types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const API = `${BASE_URL}/api/v1/documents`;

export async function createDocument(title: string, content: string): Promise<DocumentCard> {
  const res = await fetch(API, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content }),
  });
  if (!res.ok) throw new Error(`문서 생성 실패: ${res.status}`);
  return res.json();
}

export async function getDocument(id: number): Promise<DocumentCard> {
  const res = await fetch(`${API}/${id}`);
  if (!res.ok) throw new Error(`문서 조회 실패: ${res.status}`);
  return res.json();
}

export async function searchDocuments(query?: string): Promise<DocumentCard[]> {
  const url = new URL(API);
  if (query) url.searchParams.set("q", query);
  const res = await fetch(url.toString());
  if (!res.ok) throw new Error(`문서 검색 실패: ${res.status}`);
  return res.json();
}

export async function requestAnalysis(id: number): Promise<void> {
  const res = await fetch(`${API}/${id}/analyze`, { method: "POST" });
  if (!res.ok) throw new Error(`분석 요청 실패: ${res.status}`);
}

export async function activateDocument(id: number): Promise<void> {
  const res = await fetch(`${API}/${id}/activate`, { method: "POST" });
  if (!res.ok) throw new Error(`문서 활성화 실패: ${res.status}`);
}

export async function updateDocument(id: number, title: string, content: string): Promise<DocumentCard> {
  const res = await fetch(`${API}/${id}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title, content }),
  });
  if (!res.ok) throw new Error(`문서 수정 실패: ${res.status}`);
  return res.json();
}

export async function deleteDocument(id: number): Promise<DocumentCard> {
  const res = await fetch(`${API}/${id}`, { method: "DELETE" });
  if (!res.ok) throw new Error(`문서 삭제 실패: ${res.status}`);
  return res.json();
}
