'use client';

import Link from 'next/link';
import { useAuthStore } from '@/stores/authStore';
import { useCartStore } from '@/stores/cartStore';

export default function Header() {
  const { isAuthenticated, logout } = useAuthStore();
  const totalCount = useCartStore((s) => s.totalCount);

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="text-2xl font-bold text-gray-900">
            Closet
          </Link>

          {/* Search */}
          <div className="flex-1 max-w-lg mx-8">
            <input
              type="text"
              placeholder="Search products..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black focus:border-transparent"
            />
          </div>

          {/* Navigation */}
          <nav className="flex items-center gap-6">
            <Link href="/products" className="text-sm text-gray-600 hover:text-gray-900">
              Products
            </Link>
            <Link href="/cart" className="relative text-sm text-gray-600 hover:text-gray-900">
              Cart
              {totalCount() > 0 && (
                <span className="absolute -top-2 -right-3 bg-black text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {totalCount()}
                </span>
              )}
            </Link>
            {isAuthenticated ? (
              <>
                <Link href="/mypage" className="text-sm text-gray-600 hover:text-gray-900">
                  My Page
                </Link>
                <button
                  onClick={logout}
                  className="text-sm text-gray-600 hover:text-gray-900"
                >
                  Logout
                </button>
              </>
            ) : (
              <Link href="/login" className="text-sm text-gray-600 hover:text-gray-900">
                Login
              </Link>
            )}
          </nav>
        </div>
      </div>
    </header>
  );
}
