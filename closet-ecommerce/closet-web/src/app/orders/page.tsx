'use client';

export default function OrdersPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문 내역</h1>

      <div className="text-center py-16 text-gray-500">
        <p className="text-lg">주문 내역이 없습니다.</p>
        <p className="text-sm mt-2">주문하신 내역이 여기에 표시됩니다.</p>
      </div>
    </div>
  );
}
