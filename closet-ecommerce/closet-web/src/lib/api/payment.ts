import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type { Payment, ConfirmPaymentRequest } from '@/types/payment';

export const confirmPayment = (data: ConfirmPaymentRequest) =>
  apiClient.post<ApiResponse<Payment>>('/payments/confirm', data);
