import { apiFetch } from "./api";

export type NotificationType = "COMMENT" | "DOCUMENT_UPDATED" | "AI_COMPLETED" | "AI_FAILED";

export type Notification = {
  id: number;
  type: NotificationType;
  targetUserId: number;
  refId: number;
  message: string;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
};

export type NotificationPage = {
  content: Notification[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type UnreadCountResponse = {
  count: number;
};

export const getNotifications = (page = 0, unreadOnly = false) =>
  apiFetch<NotificationPage>(
    `/api/v1/notifications?page=${page}&size=20&unreadOnly=${unreadOnly}`
  );

export const getUnreadCount = () =>
  apiFetch<UnreadCountResponse>("/api/v1/notifications/unread-count");

export const markAsRead = (id: number) =>
  apiFetch<void>(`/api/v1/notifications/${id}/read`, { method: "PATCH" });

export const markAllAsRead = () =>
  apiFetch<void>("/api/v1/notifications/read-all", { method: "PATCH" });
