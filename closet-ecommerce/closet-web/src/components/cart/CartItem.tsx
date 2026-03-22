'use client';

import type { CartItem as CartItemType } from '@/types/cart';
import { formatPriceWithCurrency } from '@/lib/utils/format';

interface CartItemProps {
  item: CartItemType;
  onUpdateQuantity: (itemId: number, quantity: number) => void;
  onRemove: (itemId: number) => void;
}

export default function CartItem({ item, onUpdateQuantity, onRemove }: CartItemProps) {
  return (
    <div className="flex gap-4 py-4 border-b border-gray-200">
      <div className="w-24 h-24 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
        {item.productImage ? (
          <img src={item.productImage} alt={item.productName} className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">
            No Image
          </div>
        )}
      </div>
      <div className="flex-1">
        <h3 className="text-sm font-medium text-gray-900">{item.productName}</h3>
        {item.optionName && (
          <p className="text-xs text-gray-500 mt-1">{item.optionName}</p>
        )}
        <div className="flex items-center gap-2 mt-2">
          <button
            onClick={() => onUpdateQuantity(item.id, Math.max(1, item.quantity - 1))}
            className="w-8 h-8 border border-gray-300 rounded flex items-center justify-center text-sm"
          >
            -
          </button>
          <span className="text-sm w-8 text-center">{item.quantity}</span>
          <button
            onClick={() => onUpdateQuantity(item.id, item.quantity + 1)}
            className="w-8 h-8 border border-gray-300 rounded flex items-center justify-center text-sm"
          >
            +
          </button>
        </div>
      </div>
      <div className="flex flex-col items-end justify-between">
        <button
          onClick={() => onRemove(item.id)}
          className="text-xs text-gray-400 hover:text-gray-600"
        >
          Remove
        </button>
        <span className="text-sm font-bold">{formatPriceWithCurrency(item.totalPrice)}</span>
      </div>
    </div>
  );
}
