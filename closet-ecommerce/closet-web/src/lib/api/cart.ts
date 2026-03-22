import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type { Cart, AddCartItemRequest, UpdateCartItemRequest } from '@/types/cart';

export const getCart = () =>
  apiClient.get<ApiResponse<Cart>>('/cart');

export const addCartItem = (data: AddCartItemRequest) =>
  apiClient.post<ApiResponse<Cart>>('/cart/items', data);

export const updateCartItem = (itemId: number, data: UpdateCartItemRequest) =>
  apiClient.patch<ApiResponse<Cart>>(`/cart/items/${itemId}`, data);

export const removeCartItem = (itemId: number) =>
  apiClient.delete<ApiResponse<Cart>>(`/cart/items/${itemId}`);

export const clearCart = () =>
  apiClient.delete<ApiResponse<void>>('/cart');
