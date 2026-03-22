import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type { Cart, AddCartItemRequest } from '@/types/cart';

const getMemberId = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('memberId');
};

const memberHeaders = () => {
  const memberId = getMemberId();
  return memberId ? { 'X-Member-Id': memberId } : {};
};

export const getCart = () =>
  apiClient.get<ApiResponse<Cart>>('/carts', { headers: memberHeaders() });

export const addCartItem = (data: AddCartItemRequest) =>
  apiClient.post<ApiResponse<Cart>>('/carts/items', data, { headers: memberHeaders() });

export const updateCartItem = (itemId: number, data: { quantity: number }) =>
  apiClient.patch<ApiResponse<Cart>>(`/carts/items/${itemId}`, data, { headers: memberHeaders() });

export const removeCartItem = (itemId: number) =>
  apiClient.delete<ApiResponse<Cart>>(`/carts/items/${itemId}`, { headers: memberHeaders() });

export const clearCart = () =>
  apiClient.delete<ApiResponse<void>>('/carts', { headers: memberHeaders() });
