import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type { ShippingTracking } from '@/types/shipping';

export const getShippingTracking = (shippingId: number) =>
  apiClient.get<ApiResponse<ShippingTracking>>(`/shippings/${shippingId}/tracking`);

export const confirmDelivery = (orderId: number) =>
  apiClient.post<ApiResponse<void>>(`/orders/${orderId}/confirm-delivery`);
