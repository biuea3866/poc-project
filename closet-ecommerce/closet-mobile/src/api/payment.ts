import apiClient from './client';
import { ApiResponse } from '../types/common';
import { Payment, PaymentRequest, PaymentConfirmRequest } from '../types/payment';

export const paymentApi = {
  requestPayment: (data: PaymentRequest) =>
    apiClient.post<ApiResponse<Payment>>('/payments', data),

  confirmPayment: (data: PaymentConfirmRequest) =>
    apiClient.post<ApiResponse<Payment>>('/payments/confirm', data),

  getPayment: (paymentId: number) =>
    apiClient.get<ApiResponse<Payment>>(`/payments/${paymentId}`),

  cancelPayment: (paymentId: number, reason: string) =>
    apiClient.post<ApiResponse<Payment>>(`/payments/${paymentId}/cancel`, { reason }),
};
