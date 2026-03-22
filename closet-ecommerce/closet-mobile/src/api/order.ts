import apiClient from './client';
import { ApiResponse, PageResponse, PageRequest } from '../types/common';
import { Order, CreateOrderRequest } from '../types/order';

export const orderApi = {
  getOrders: (params?: PageRequest) =>
    apiClient.get<ApiResponse<PageResponse<Order>>>('/orders', { params }),

  getOrder: (orderId: number) =>
    apiClient.get<ApiResponse<Order>>(`/orders/${orderId}`),

  createOrder: (data: CreateOrderRequest) =>
    apiClient.post<ApiResponse<Order>>('/orders', data),

  cancelOrder: (orderId: number) =>
    apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/cancel`),

  requestReturn: (orderId: number, reason: string) =>
    apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/return`, { reason }),

  requestExchange: (orderId: number, reason: string) =>
    apiClient.post<ApiResponse<Order>>(`/orders/${orderId}/exchange`, { reason }),
};
