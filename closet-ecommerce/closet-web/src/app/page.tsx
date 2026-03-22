'use client';

import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { getProducts, getCategories, getBrands } from '@/lib/api/product';
import ProductCard from '@/components/product/ProductCard';
import type { Product, Category, Brand } from '@/types/product';

function ProductSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="aspect-[3/4] bg-gray-200 rounded-lg mb-3" />
      <div className="h-3 bg-gray-200 rounded w-1/3 mb-2" />
      <div className="h-4 bg-gray-200 rounded w-2/3 mb-2" />
      <div className="h-4 bg-gray-200 rounded w-1/4" />
    </div>
  );
}

export default function HomePage() {
  const [popularProducts, setPopularProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const brandMap = useMemo(() => {
    const map: Record<number, string> = {};
    brands.forEach((b) => { map[b.id] = b.name; });
    return map;
  }, [brands]);

  useEffect(() => {
    Promise.all([
      getProducts({ page: 0, size: 8 }),
      getCategories(),
      getBrands(),
    ])
      .then(([prodRes, catRes, brandRes]) => {
        setPopularProducts(prodRes.data.data?.content || []);
        setCategories(catRes.data.data || []);
        setBrands(brandRes.data.data || []);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, []);

  const defaultCategories: Category[] = [
    { id: 0, name: '상의', depth: 1, parentId: null, sortOrder: 1, status: 'ACTIVE', children: [] },
    { id: 1, name: '하의', depth: 1, parentId: null, sortOrder: 2, status: 'ACTIVE', children: [] },
    { id: 2, name: '아우터', depth: 1, parentId: null, sortOrder: 3, status: 'ACTIVE', children: [] },
    { id: 3, name: '액세서리', depth: 1, parentId: null, sortOrder: 4, status: 'ACTIVE', children: [] },
  ];

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 text-center">
          <h1 className="text-5xl font-bold text-gray-900 mb-4">Closet</h1>
          <p className="text-xl text-gray-600 mb-8">
            나만의 스타일을 발견하세요. 엄선된 프리미엄 패션.
          </p>
          <Link
            href="/products"
            className="inline-block bg-black text-white px-8 py-3 rounded-lg font-medium hover:bg-gray-800 transition-colors"
          >
            쇼핑하기
          </Link>
        </div>
      </section>

      {/* Featured Products */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="flex justify-between items-center mb-8">
          <h2 className="text-2xl font-bold text-gray-900">인기 상품</h2>
          <Link href="/products" className="text-sm text-gray-500 hover:text-gray-900">
            전체보기 &rarr;
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
              <ProductSkeleton key={i} />
            ))}
          </div>
        ) : error ? (
          <div className="text-center py-16 text-gray-500">
            <p>상품을 불러오는 데 실패했습니다.</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 text-sm text-black underline"
            >
              다시 시도
            </button>
          </div>
        ) : popularProducts.length === 0 ? (
          <div className="text-center py-16 text-gray-500">
            <p>등록된 상품이 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {popularProducts.map((product) => (
              <ProductCard key={product.id} product={product} brandName={brandMap[product.brandId]} />
            ))}
          </div>
        )}
      </section>

      {/* Categories */}
      <section className="bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <h2 className="text-2xl font-bold text-gray-900 mb-8">카테고리별 쇼핑</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {(categories.length > 0 ? categories : defaultCategories).map((category) => (
              <Link
                key={category.id}
                href={`/products?categoryId=${category.id}`}
                className="aspect-square bg-gray-200 rounded-lg flex items-center justify-center hover:bg-gray-300 transition-colors cursor-pointer"
              >
                <span className="text-lg font-medium text-gray-600">{category.name}</span>
              </Link>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
