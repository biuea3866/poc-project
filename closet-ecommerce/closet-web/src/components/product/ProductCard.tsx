'use client';

import Link from 'next/link';
import type { Product } from '@/types/product';
import { formatPriceWithCurrency } from '@/lib/utils/format';

interface ProductCardProps {
  product: Product;
  brandName?: string;
}

const BRAND_COLORS = [
  'bg-rose-400', 'bg-sky-400', 'bg-amber-400', 'bg-emerald-400',
  'bg-violet-400', 'bg-pink-400', 'bg-teal-400', 'bg-orange-400',
];

function getBrandColor(brandId: number): string {
  return BRAND_COLORS[brandId % BRAND_COLORS.length];
}

export default function ProductCard({ product, brandName }: ProductCardProps) {
  const thumbnail = product.images?.[0]?.imageUrl;
  const displayBrandName = brandName || `Brand #${product.brandId}`;
  const brandInitial = displayBrandName.charAt(0).toUpperCase();

  return (
    <Link href={`/products/${product.id}`} className="group">
      <div className="aspect-[3/4] bg-gray-100 rounded-lg overflow-hidden mb-3">
        {thumbnail ? (
          <img
            src={thumbnail}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className={`w-full h-full flex items-center justify-center ${getBrandColor(product.brandId)} group-hover:scale-105 transition-transform duration-300`}>
            <span className="text-4xl font-bold text-white/80">{brandInitial}</span>
          </div>
        )}
      </div>
      <div>
        <p className="text-xs text-gray-500 mb-1">{displayBrandName}</p>
        <h3 className="text-sm font-medium text-gray-900 mb-1 line-clamp-2">{product.name}</h3>
        <div className="flex items-center gap-2">
          {product.discountRate > 0 ? (
            <>
              <span className="text-sm font-bold text-red-600">
                {product.discountRate}%
              </span>
              <span className="text-sm font-bold text-gray-900">
                {formatPriceWithCurrency(product.salePrice)}
              </span>
              <span className="text-xs text-gray-400 line-through">
                {formatPriceWithCurrency(product.basePrice)}
              </span>
            </>
          ) : (
            <span className="text-sm font-bold text-gray-900">
              {formatPriceWithCurrency(product.basePrice)}
            </span>
          )}
        </div>
      </div>
    </Link>
  );
}
