import type { Product } from '@/types/product';
import ProductCard from './ProductCard';

interface ProductListProps {
  products: Product[];
  brandMap?: Record<number, string>;
}

export default function ProductList({ products, brandMap }: ProductListProps) {
  if (products.length === 0) {
    return (
      <div className="text-center py-16 text-gray-500">
        상품이 없습니다.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          brandName={brandMap?.[product.brandId]}
        />
      ))}
    </div>
  );
}
