'use client';

import { Suspense, useEffect, useState, useCallback, useMemo } from 'react';
import { useSearchParams } from 'next/navigation';
import { getProducts, getCategories, getBrands } from '@/lib/api/product';
import ProductCard from '@/components/product/ProductCard';
import ProductFilter from '@/components/product/ProductFilter';
import type { Product, Category, Brand, ProductListParams } from '@/types/product';
import type { PageResponse } from '@/types/common';

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

function ProductsPageSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">상품 목록</h1>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
        {Array.from({ length: 6 }).map((_, i) => (
          <ProductSkeleton key={i} />
        ))}
      </div>
    </div>
  );
}

const SORT_OPTIONS = [
  { label: '신상품순', value: 'createdAt,desc' },
  { label: '가격 낮은순', value: 'salePrice,asc' },
  { label: '가격 높은순', value: 'salePrice,desc' },
  { label: '인기순', value: 'popularity,desc' },
];

const EMPTY_PAGE: PageResponse<Product> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  pageable: { pageNumber: 0, pageSize: 12 },
};

function ProductsContent() {
  const searchParams = useSearchParams();
  const initialCategoryId = searchParams.get('categoryId');

  const [products, setProducts] = useState<PageResponse<Product>>(EMPTY_PAGE);
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<ProductListParams>({
    page: 0,
    size: 12,
    sort: 'createdAt,desc',
    categoryId: initialCategoryId ? Number(initialCategoryId) : undefined,
  });

  const brandMap = useMemo(() => {
    const map: Record<number, string> = {};
    brands.forEach((b) => { map[b.id] = b.name; });
    return map;
  }, [brands]);

  const fetchProducts = useCallback(async (currentFilters: ProductListParams) => {
    setLoading(true);
    try {
      const cleanFilters = Object.fromEntries(
        Object.entries(currentFilters).filter(([, v]) => v !== undefined && v !== null && v !== '')
      );
      const res = await getProducts(cleanFilters as ProductListParams);
      setProducts(res.data.data || EMPTY_PAGE);
    } catch {
      setProducts(EMPTY_PAGE);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.all([getCategories(), getBrands()])
      .then(([catRes, brandRes]) => {
        setCategories(catRes.data.data || []);
        setBrands(brandRes.data.data || []);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    fetchProducts(filters);
  }, [filters, fetchProducts]);

  const handleFilterChange = (partial: Partial<ProductListParams>) => {
    setFilters((prev) => ({ ...prev, ...partial }));
  };

  const handleSortChange = (sort: string) => {
    setFilters((prev) => ({ ...prev, sort, page: 0 }));
  };

  const handlePageChange = (page: number) => {
    setFilters((prev) => ({ ...prev, page }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const currentPage = products.pageable.pageNumber;
  const isFirstPage = currentPage === 0;
  const isLastPage = currentPage >= products.totalPages - 1;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">상품 목록</h1>
      <div className="flex gap-8">
        {/* Sidebar Filter */}
        <div className="w-64 flex-shrink-0 hidden lg:block">
          <ProductFilter
            categories={categories}
            brands={brands}
            filters={filters}
            onFilterChange={handleFilterChange}
          />
        </div>

        {/* Product Grid */}
        <div className="flex-1">
          {/* Sort bar */}
          <div className="flex justify-between items-center mb-6">
            <p className="text-sm text-gray-500">
              {loading ? '상품을 불러오는 중...' : `총 ${products.totalElements.toLocaleString()}개 상품`}
            </p>
            <select
              className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm"
              value={filters.sort || 'createdAt,desc'}
              onChange={(e) => handleSortChange(e.target.value)}
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          {/* Product Grid */}
          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <ProductSkeleton key={i} />
              ))}
            </div>
          ) : products.content.length === 0 ? (
            <div className="text-center py-16 text-gray-500">
              <p className="text-lg">상품이 없습니다</p>
              <p className="text-sm mt-2">다른 필터를 시도해보세요.</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
              {products.content.map((product) => (
                <ProductCard key={product.id} product={product} brandName={brandMap[product.brandId]} />
              ))}
            </div>
          )}

          {/* Pagination */}
          {products.totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-12">
              <button
                disabled={isFirstPage}
                onClick={() => handlePageChange(currentPage - 1)}
                className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                이전
              </button>
              {Array.from({ length: products.totalPages }).map((_, i) => (
                <button
                  key={i}
                  onClick={() => handlePageChange(i)}
                  className={`w-10 h-10 text-sm rounded-lg ${
                    currentPage === i
                      ? 'bg-black text-white'
                      : 'border border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                disabled={isLastPage}
                onClick={() => handlePageChange(currentPage + 1)}
                className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                다음
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ProductsPage() {
  return (
    <Suspense fallback={<ProductsPageSkeleton />}>
      <ProductsContent />
    </Suspense>
  );
}
