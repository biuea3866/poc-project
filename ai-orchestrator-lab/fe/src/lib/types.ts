export type DocumentStatus = "DRAFT" | "ACTIVE" | "DELETED";
export type AiStatus = "NOT_STARTED" | "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";

export interface DocumentCard {
  id: number;
  title: string;
  excerpt: string;
  status: DocumentStatus;
  aiStatus: AiStatus;
  tags: string[];
  updatedAt: string;
}

export interface SseStatusEvent {
  status: AiStatus;
}
