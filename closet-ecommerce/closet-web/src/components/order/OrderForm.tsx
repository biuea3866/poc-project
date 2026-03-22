'use client';

export default function OrderForm() {
  return (
    <div className="space-y-6">
      {/* Shipping Address */}
      <div>
        <h3 className="text-lg font-bold mb-3">배송지</h3>
        <div className="bg-gray-50 rounded-lg p-4">
          <p className="text-sm text-gray-500">배송지를 선택해주세요.</p>
        </div>
      </div>

      {/* Payment Method */}
      <div>
        <h3 className="text-lg font-bold mb-3">결제 수단</h3>
        <div className="space-y-2">
          {['신용/체크카드', '무통장입금', '간편결제'].map((method) => (
            <label key={method} className="flex items-center gap-3 p-3 border border-gray-200 rounded-lg cursor-pointer hover:border-gray-400">
              <input type="radio" name="payment" className="accent-black" />
              <span className="text-sm">{method}</span>
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}
