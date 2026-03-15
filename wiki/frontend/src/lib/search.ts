import { apiFetch } from "@/lib/api";

export type SearchResult = {
  id: number;
  title: string;
  content?: string;
  score?: number;
  type?: string;
};

export type VectorSearchResult = {
  id: number;
  title: string;
  content: string;
  similarity: number;
};

export async function searchIntegrated(query: string): Promise<SearchResult[]> {
  const data = await apiFetch<{ results: SearchResult[] } | SearchResult[]>(
    `/api/v1/search/integrated?query=${encodeURIComponent(query)}`
  );
  return Array.isArray(data) ? data : data.results ?? [];
}

export async function searchVector(query: string, limit = 10): Promise<VectorSearchResult[]> {
  const data = await apiFetch<VectorSearchResult[] | { results: VectorSearchResult[] }>(
    "/api/v1/search/vector",
    {
      method: "POST",
      body: JSON.stringify({ query, limit }),
    }
  );
  return Array.isArray(data) ? data : data.results ?? [];
}
