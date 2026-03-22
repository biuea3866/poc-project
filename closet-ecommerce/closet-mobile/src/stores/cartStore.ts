import { create } from 'zustand';
import { CartItem, Cart } from '../types/cart';
import { cartApi } from '../api/cart';

interface CartState {
  items: CartItem[];
  totalPrice: number;
  totalDiscountPrice: number;
  deliveryFee: number;
  finalPrice: number;
  isLoading: boolean;

  fetchCart: () => Promise<void>;
  addItem: (productId: number, optionId: number, quantity: number) => Promise<void>;
  updateItemQuantity: (cartItemId: number, quantity: number) => Promise<void>;
  removeItem: (cartItemId: number) => Promise<void>;
  clearCart: () => Promise<void>;
}

const applyCartData = (cart: Cart) => ({
  items: cart.items,
  totalPrice: cart.totalPrice,
  totalDiscountPrice: cart.totalDiscountPrice,
  deliveryFee: cart.deliveryFee,
  finalPrice: cart.finalPrice,
});

export const useCartStore = create<CartState>()((set) => ({
  items: [],
  totalPrice: 0,
  totalDiscountPrice: 0,
  deliveryFee: 0,
  finalPrice: 0,
  isLoading: false,

  fetchCart: async () => {
    set({ isLoading: true });
    try {
      const response = await cartApi.getCart();
      set({ ...applyCartData(response.data.data), isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  addItem: async (productId: number, optionId: number, quantity: number) => {
    set({ isLoading: true });
    try {
      const response = await cartApi.addItem({ productId, optionId, quantity });
      set({ ...applyCartData(response.data.data), isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  updateItemQuantity: async (cartItemId: number, quantity: number) => {
    try {
      const response = await cartApi.updateItem(cartItemId, { quantity });
      set(applyCartData(response.data.data));
    } catch {
      // silently fail
    }
  },

  removeItem: async (cartItemId: number) => {
    try {
      const response = await cartApi.removeItem(cartItemId);
      set(applyCartData(response.data.data));
    } catch {
      // silently fail
    }
  },

  clearCart: async () => {
    try {
      await cartApi.clearCart();
      set({ items: [], totalPrice: 0, totalDiscountPrice: 0, deliveryFee: 0, finalPrice: 0 });
    } catch {
      // silently fail
    }
  },
}));
