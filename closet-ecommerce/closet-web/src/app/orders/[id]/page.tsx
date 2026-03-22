'use client';

export default function OrderDetailPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">Order Details</h1>

      {/* Order Info Skeleton */}
      <div className="space-y-6">
        <div className="bg-gray-50 rounded-lg p-6">
          <div className="flex justify-between items-center mb-4">
            <div className="h-5 bg-gray-200 rounded w-1/3 animate-pulse" />
            <div className="h-6 bg-gray-200 rounded w-20 animate-pulse" />
          </div>
          <div className="h-4 bg-gray-200 rounded w-1/4 animate-pulse" />
        </div>

        {/* Items Skeleton */}
        <div>
          <h2 className="text-lg font-bold mb-4">Items</h2>
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <div key={i} className="flex gap-4 py-4 border-b border-gray-200 animate-pulse">
                <div className="w-20 h-20 bg-gray-200 rounded" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-200 rounded w-1/2" />
                  <div className="h-3 bg-gray-200 rounded w-1/4" />
                </div>
                <div className="h-4 bg-gray-200 rounded w-20" />
              </div>
            ))}
          </div>
        </div>

        {/* Shipping Address Skeleton */}
        <div>
          <h2 className="text-lg font-bold mb-4">Shipping Address</h2>
          <div className="bg-gray-50 rounded-lg p-4 space-y-2 animate-pulse">
            <div className="h-4 bg-gray-200 rounded w-1/3" />
            <div className="h-4 bg-gray-200 rounded w-2/3" />
          </div>
        </div>
      </div>
    </div>
  );
}
