import { apiFetch } from "./api";

export type Comment = {
  id: number;
  authorId: number;
  content: string;
  parentId: number | null;
  isDeleted: boolean;
  createdAt: string;
  replies: Comment[];
};

export const getComments = (documentId: number) =>
  apiFetch<Comment[]>(`/api/v1/documents/${documentId}/comments`);

export const createComment = (documentId: number, content: string, parentId?: number) =>
  apiFetch<Comment>(`/api/v1/documents/${documentId}/comments`, {
    method: "POST",
    body: JSON.stringify({ content, parentId: parentId ?? null }),
  });

export const updateComment = (commentId: number, content: string) =>
  apiFetch<Comment>(`/api/v1/comments/${commentId}`, {
    method: "PUT",
    body: JSON.stringify({ content }),
  });

export const deleteComment = (commentId: number) =>
  apiFetch<void>(`/api/v1/comments/${commentId}`, { method: "DELETE" });
