'use client';

import { useCartStore } from '@/stores/cartStore';
import * as cartApi from '@/lib/api/cart';
import type { AddCartItemRequest } from '@/types/cart';

export function useCart() {
  const store = useCartStore();

  const fetchCart = async () => {
    const response = await cartApi.getCart();
    const cart = response.data.data;
    if (cart) {
      store.setItems(cart.items);
    }
    return cart;
  };

  const addItem = async (data: AddCartItemRequest) => {
    const response = await cartApi.addCartItem(data);
    const cart = response.data.data;
    if (cart) {
      store.setItems(cart.items);
    }
    return cart;
  };

  const updateQuantity = async (itemId: number, quantity: number) => {
    const response = await cartApi.updateCartItem(itemId, { quantity });
    const cart = response.data.data;
    if (cart) {
      store.setItems(cart.items);
    }
    return cart;
  };

  const removeItem = async (itemId: number) => {
    const response = await cartApi.removeCartItem(itemId);
    const cart = response.data.data;
    if (cart) {
      store.setItems(cart.items);
    }
    return cart;
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
