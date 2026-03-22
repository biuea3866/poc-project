'use client';

export default function OrdersPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">My Orders</h1>

      <div className="text-center py-16 text-gray-500">
        <p className="text-lg">No orders yet.</p>
        <p className="text-sm mt-2">Your order history will appear here.</p>
      </div>
    </div>
  );
}
