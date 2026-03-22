import Link from 'next/link';
import type { Product } from '@/types/product';
import { formatPriceWithCurrency } from '@/lib/utils/format';

interface ProductCardProps {
  product: Product;
}

export default function ProductCard({ product }: ProductCardProps) {
  const thumbnail = product.images.find((img) => img.isThumbnail)?.url || product.images[0]?.url;

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
          <div className="w-full h-full flex items-center justify-center text-gray-400">
            이미지 없음
          </div>
        )}
      </div>
      <div>
        <p className="text-xs text-gray-500 mb-1">{product.brandName}</p>
        <h3 className="text-sm font-medium text-gray-900 mb-1 line-clamp-2">{product.name}</h3>
        <div className="flex items-center gap-2">
          {product.discountPrice ? (
            <>
              <span className="text-sm font-bold text-red-600">
                {formatPriceWithCurrency(product.discountPrice)}
              </span>
              <span className="text-xs text-gray-400 line-through">
                {formatPriceWithCurrency(product.price)}
              </span>
            </>
          ) : (
            <span className="text-sm font-bold text-gray-900">
              {formatPriceWithCurrency(product.price)}
            </span>
          )}
        </div>
      </div>
    </Link>
  );
}
