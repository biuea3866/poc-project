'use client';

export default function AddressesPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Shipping Addresses</h1>
        <button className="bg-black text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors">
          Add Address
        </button>
      </div>

      <div className="text-center py-16 text-gray-500">
        <p className="text-lg">No saved addresses.</p>
        <p className="text-sm mt-2">Add a shipping address for faster checkout.</p>
      </div>
    </div>
  );
}
