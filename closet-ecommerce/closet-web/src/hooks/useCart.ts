'use client';

import { useCartStore } from '@/stores/cartStore';
import * as cartApi from '@/lib/api/cart';
import type { AddCartItemRequest } from '@/types/cart';

export function useCart() {
  const store = useCartStore();

  const fetchCart = async () => {
    const response = await cartApi.getCart();
    store.setItems(response.data.data.items);
    return response.data.data;
  };

  const addItem = async (data: AddCartItemRequest) => {
    const response = await cartApi.addCartItem(data);
    store.setItems(response.data.data.items);
    return response.data.data;
  };

  const updateQuantity = async (itemId: number, quantity: number) => {
    const response = await cartApi.updateCartItem(itemId, { quantity });
    store.setItems(response.data.data.items);
    return response.data.data;
  };

  const removeItem = async (itemId: number) => {
    const response = await cartApi.removeCartItem(itemId);
    store.setItems(response.data.data.items);
    return response.data.data;
  };

  return {
    items: store.items,
    totalPrice: store.totalPrice,
    totalCount: store.totalCount,
    fetchCart,
    addItem,
    updateQuantity,
    removeItem,
    clearCart: store.clearCart,
  };
}
