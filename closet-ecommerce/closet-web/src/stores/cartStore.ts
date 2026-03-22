import { create } from 'zustand';
import type { CartItem } from '@/types/cart';

interface CartState {
  items: CartItem[];
  setItems: (items: CartItem[]) => void;
  addItem: (item: CartItem) => void;
  removeItem: (itemId: number) => void;
  updateQuantity: (itemId: number, quantity: number) => void;
  clearCart: () => void;
  totalPrice: () => number;
  totalCount: () => number;
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],

  setItems: (items) => set({ items }),

  addItem: (item) =>
    set((state) => {
      const existing = state.items.find((i) => i.id === item.id);
      if (existing) {
        return {
          items: state.items.map((i) =>
            i.id === item.id ? { ...i, quantity: i.quantity + item.quantity } : i
          ),
        };
      }
      return { items: [...state.items, item] };
    }),

  removeItem: (itemId) =>
    set((state) => ({
      items: state.items.filter((i) => i.id !== itemId),
    })),

  updateQuantity: (itemId, quantity) =>
    set((state) => ({
      items: state.items.map((i) =>
        i.id === itemId ? { ...i, quantity, totalPrice: i.unitPrice * quantity } : i
      ),
    })),

  clearCart: () => set({ items: [] }),

  totalPrice: () => get().items.reduce((sum, item) => sum + item.totalPrice, 0),

  totalCount: () => get().items.reduce((sum, item) => sum + item.quantity, 0),
}));
