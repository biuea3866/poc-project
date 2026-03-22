import { formatPriceWithCurrency } from '@/lib/utils/format';

interface CartSummaryProps {
  totalPrice: number;
  itemCount: number;
  onCheckout: () => void;
}

export default function CartSummary({ totalPrice, itemCount, onCheckout }: CartSummaryProps) {
  const shippingFee = totalPrice >= 50000 ? 0 : 3000;
  const finalTotal = totalPrice + shippingFee;

  return (
    <div className="bg-gray-50 rounded-lg p-6">
      <h3 className="text-lg font-bold mb-4">주문 요약</h3>
      <div className="space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-600">상품 금액 ({itemCount}개)</span>
          <span>{formatPriceWithCurrency(totalPrice)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-600">배송비</span>
          <span>{shippingFee === 0 ? '무료' : formatPriceWithCurrency(shippingFee)}</span>
        </div>
        <div className="border-t border-gray-200 pt-2 mt-2">
          <div className="flex justify-between font-bold text-base">
            <span>합계</span>
            <span>{formatPriceWithCurrency(finalTotal)}</span>
          </div>
        </div>
      </div>
      {totalPrice > 0 && totalPrice < 50000 && (
        <p className="text-xs text-gray-500 mt-2">
          {formatPriceWithCurrency(50000 - totalPrice)} 더 구매 시 무료배송
        </p>
      )}
      <button
        onClick={onCheckout}
        disabled={itemCount === 0}
        className="w-full mt-4 bg-black text-white py-3 rounded-lg font-medium hover:bg-gray-800 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
      >
        주문하기
      </button>
    </div>
  );
}
