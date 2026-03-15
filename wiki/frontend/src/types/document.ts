export type DocumentStatus = "DRAFT" | "ACTIVE" | "DELETED";
export type AiStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface Document {
  id: number;
  title: string;
  content?: string;
  status: DocumentStatus;
  aiStatus: AiStatus;
  parentId?: number | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number;
  updatedBy: number;
  children?: Document[];
}

export interface DocumentRevision {
  id: number;
  documentId: number;
  data: string; // JSON string of document state
  createdAt: string;
  createdBy: number;
}

export interface DocumentSummary {
  id: number;
  content: string;
  documentId: number;
  documentRevisionId: number;
}

export interface Tag {
  id: number;
  name: string;
  tagType: string;
}

export interface SearchResult {
  id: number;
  title: string;
  content: string;
  summary?: string;
  tags: Tag[];
  relevanceScore?: number;
}

export interface VectorSearchResult {
  id: number;
  title: string;
  content: string;
  snippet?: string;
  similarityScore: number;
  tags?: Tag[];
}

export interface VectorSearchResponse {
  results: VectorSearchResult[];
  query: string;
  total: number;
}

export interface PaginatedResponse<T> {
  results: T[];
  page: number;
  size: number;
  total: number;
}

export interface DocumentListResponse {
  documents: Document[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
