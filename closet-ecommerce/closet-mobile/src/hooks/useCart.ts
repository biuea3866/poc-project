import { useEffect, useCallback } from 'react';
import { useCartStore } from '../stores/cartStore';
import { useAuth } from './useAuth';

export const useCart = () => {
  const { isAuthenticated } = useAuth();
  const {
    items,
    totalPrice,
    totalDiscountPrice,
    deliveryFee,
    finalPrice,
    isLoading,
    fetchCart,
    addItem,
    updateItemQuantity,
    removeItem,
    clearCart,
  } = useCartStore();

  useEffect(() => {
    if (isAuthenticated) {
      fetchCart();
    }
  }, [isAuthenticated, fetchCart]);

  const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);

  const handleAddItem = useCallback(
    async (productId: number, optionId: number, quantity: number = 1) => {
      await addItem(productId, optionId, quantity);
    },
    [addItem],
  );

  return {
    items,
    itemCount,
    totalPrice,
    totalDiscountPrice,
    deliveryFee,
    finalPrice,
    isLoading,
    addItem: handleAddItem,
    updateItemQuantity,
    removeItem,
    clearCart,
    refreshCart: fetchCart,
  };
};
