import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type {
  ReturnRequest,
  ReturnResponse,
  ExchangeRequest,
  ExchangeResponse,
  RestockNotificationRequest,
  RestockNotification,
} from '@/types/claim';

export const createReturn = (data: ReturnRequest) =>
  apiClient.post<ApiResponse<ReturnResponse>>('/returns', data);

export const getReturn = (returnId: number) =>
  apiClient.get<ApiResponse<ReturnResponse>>(`/returns/${returnId}`);

export const createExchange = (data: ExchangeRequest) =>
  apiClient.post<ApiResponse<ExchangeResponse>>('/exchanges', data);

export const getExchange = (exchangeId: number) =>
  apiClient.get<ApiResponse<ExchangeResponse>>(`/exchanges/${exchangeId}`);

export const createRestockNotification = (data: RestockNotificationRequest) =>
  apiClient.post<ApiResponse<RestockNotification>>(
    '/restock-notifications',
    data,
  );

export const getRestockNotifications = () =>
  apiClient.get<ApiResponse<RestockNotification[]>>(
    '/restock-notifications/me',
  );

export const deleteRestockNotification = (id: number) =>
  apiClient.delete<ApiResponse<void>>(`/restock-notifications/${id}`);
