import apiClient from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { Order, CreateOrderRequest } from '@/types/order';

export const createOrder = (data: CreateOrderRequest) =>
  apiClient.post<ApiResponse<Order>>('/orders', data);

export const getOrders = (params?: { page?: number; size?: number }) =>
  apiClient.get<ApiResponse<PageResponse<Order>>>('/orders', { params });

export const getOrder = (id: number) =>
  apiClient.get<ApiResponse<Order>>(`/orders/${id}`);

export const cancelOrder = (id: number) =>
  apiClient.post<ApiResponse<Order>>(`/orders/${id}/cancel`);
