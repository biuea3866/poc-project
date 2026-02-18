export type DocumentStatus = "ACTIVE" | "DELETED";
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
