'use client';

export default function AddressesPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-2xl font-bold text-gray-900">배송지 관리</h1>
        <button className="bg-black text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors">
          배송지 추가
        </button>
      </div>

      <div className="text-center py-16 text-gray-500">
        <p className="text-lg">저장된 배송지가 없습니다.</p>
        <p className="text-sm mt-2">배송지를 추가하면 더 빠르게 주문할 수 있습니다.</p>
      </div>
    </div>
  );
}
