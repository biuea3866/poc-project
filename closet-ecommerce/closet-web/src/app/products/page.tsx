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
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
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

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.categoryId) count++;
    if (filters.brandId) count++;
    if (filters.minPrice) count++;
    if (filters.maxPrice) count++;
    return count;
  }, [filters]);

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

  // Build pagination page numbers with ellipsis for large page counts
  const paginationPages = useMemo(() => {
    const total = products.totalPages;
    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }
    const pages: (number | 'ellipsis')[] = [];
    pages.push(0);
    if (currentPage > 2) pages.push('ellipsis');
    for (let i = Math.max(1, currentPage - 1); i <= Math.min(total - 2, currentPage + 1); i++) {
      pages.push(i);
    }
    if (currentPage < total - 3) pages.push('ellipsis');
    pages.push(total - 1);
    return pages;
  }, [products.totalPages, currentPage]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">상품 목록</h1>
      <div className="flex gap-8">
        {/* Sidebar Filter -- desktop */}
        <div className="w-64 flex-shrink-0 hidden lg:block">
          <ProductFilter
            categories={categories}
            brands={brands}
            filters={filters}
            onFilterChange={handleFilterChange}
          />
        </div>

        {/* Mobile Filter Overlay */}
        {mobileFilterOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            {/* Backdrop */}
            <div
              className="absolute inset-0 bg-black/40"
              onClick={() => setMobileFilterOpen(false)}
            />
            {/* Panel */}
            <div className="absolute right-0 top-0 bottom-0 w-80 max-w-[85vw] bg-white shadow-xl overflow-y-auto">
              <div className="flex items-center justify-between p-4 border-b border-gray-200">
                <h2 className="text-lg font-bold">필터</h2>
                <button
                  onClick={() => setMobileFilterOpen(false)}
                  className="p-1 text-gray-500 hover:text-gray-900"
                  aria-label="필터 닫기"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div className="p-4">
                <ProductFilter
                  categories={categories}
                  brands={brands}
                  filters={filters}
                  onFilterChange={(partial) => {
                    handleFilterChange(partial);
                    setMobileFilterOpen(false);
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Product Grid */}
        <div className="flex-1">
          {/* Sort bar */}
          <div className="flex justify-between items-center mb-6 gap-3">
            <p className="text-sm text-gray-500 flex-shrink-0">
              {loading ? '상품을 불러오는 중...' : `총 ${products.totalElements.toLocaleString()}개 상품`}
            </p>
            <div className="flex items-center gap-2">
              {/* Mobile filter button */}
              <button
                onClick={() => setMobileFilterOpen(true)}
                className="lg:hidden flex items-center gap-1.5 px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                </svg>
                필터
                {activeFilterCount > 0 && (
                  <span className="bg-black text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                    {activeFilterCount}
                  </span>
                )}
              </button>
              <select
                className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm bg-white"
                value={filters.sort || 'createdAt,desc'}
                onChange={(e) => handleSortChange(e.target.value)}
              >
                {SORT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Product Grid */}
          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <ProductSkeleton key={i} />
              ))}
            </div>
          ) : products.content.length === 0 ? (
            <div className="text-center py-16 text-gray-500">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mx-auto text-gray-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <p className="text-lg">상품이 없습니다</p>
              <p className="text-sm mt-2">다른 필터를 시도해보세요.</p>
              {activeFilterCount > 0 && (
                <button
                  onClick={() => handleFilterChange({ categoryId: undefined, brandId: undefined, minPrice: undefined, maxPrice: undefined, page: 0 })}
                  className="mt-4 text-sm text-black font-medium hover:underline"
                >
                  필터 초기화
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
              {products.content.map((product) => (
                <ProductCard key={product.id} product={product} brandName={brandMap[product.brandId]} />
              ))}
            </div>
          )}

          {/* Pagination */}
          {products.totalPages > 1 && (
            <div className="flex justify-center items-center gap-1 sm:gap-2 mt-12">
              <button
                disabled={isFirstPage}
                onClick={() => handlePageChange(currentPage - 1)}
                className="px-2 sm:px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                이전
              </button>
              {paginationPages.map((item, idx) =>
                item === 'ellipsis' ? (
                  <span key={`ellipsis-${idx}`} className="px-2 text-gray-400">...</span>
                ) : (
                  <button
                    key={item}
                    onClick={() => handlePageChange(item)}
                    className={`w-9 h-9 sm:w-10 sm:h-10 text-sm rounded-lg ${
                      currentPage === item
                        ? 'bg-black text-white'
                        : 'border border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    {item + 1}
                  </button>
                )
              )}
              <button
                disabled={isLastPage}
                onClick={() => handlePageChange(currentPage + 1)}
                className="px-2 sm:px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
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
