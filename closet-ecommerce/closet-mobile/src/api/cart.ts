import apiClient from './client';
import { ApiResponse } from '../types/common';
import { Cart, AddCartItemRequest, UpdateCartItemRequest } from '../types/cart';

export const cartApi = {
  getCart: () =>
    apiClient.get<ApiResponse<Cart>>('/cart'),

  addItem: (data: AddCartItemRequest) =>
    apiClient.post<ApiResponse<Cart>>('/cart/items', data),

  updateItem: (cartItemId: number, data: UpdateCartItemRequest) =>
    apiClient.patch<ApiResponse<Cart>>(`/cart/items/${cartItemId}`, data),

  removeItem: (cartItemId: number) =>
    apiClient.delete<ApiResponse<Cart>>(`/cart/items/${cartItemId}`),

  removeItems: (cartItemIds: number[]) =>
    apiClient.post<ApiResponse<Cart>>('/cart/items/remove', { cartItemIds }),

  clearCart: () =>
    apiClient.delete<ApiResponse<void>>('/cart'),
};
