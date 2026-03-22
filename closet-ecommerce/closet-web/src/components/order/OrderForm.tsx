'use client';

export default function OrderForm() {
  return (
    <div className="space-y-6">
      {/* Shipping Address */}
      <div>
        <h3 className="text-lg font-bold mb-3">Shipping Address</h3>
        <div className="bg-gray-50 rounded-lg p-4">
          <p className="text-sm text-gray-500">Select a shipping address to continue.</p>
        </div>
      </div>

      {/* Payment Method */}
      <div>
        <h3 className="text-lg font-bold mb-3">Payment Method</h3>
        <div className="space-y-2">
          {['Card', 'Bank Transfer', 'Mobile Payment'].map((method) => (
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
