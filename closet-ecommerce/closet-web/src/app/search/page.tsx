'use client';

import { Suspense, useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import Image from 'next/image';
import { searchProducts, getAutocomplete, getPopularKeywords } from '@/lib/api/search';
import SearchFilterSidebar from '@/components/search/SearchFilterSidebar';
import { formatPriceWithCurrency } from '@/lib/utils/format';
import type { SearchProductsParams, SearchResponse, AutocompleteSuggestion, PopularKeyword, RankChange } from '@/types/search';

const SORT_OPTIONS = [
  { label: '관련도순', value: 'relevance' },
  { label: '최신순', value: 'createdAt,desc' },
  { label: '가격 낮은순', value: 'salePrice,asc' },
  { label: '가격 높은순', value: 'salePrice,desc' },
  { label: '인기순', value: 'popularity,desc' },
];

const BRAND_COLORS = [
  'bg-rose-400', 'bg-sky-400', 'bg-amber-400', 'bg-emerald-400',
  'bg-violet-400', 'bg-pink-400', 'bg-teal-400', 'bg-orange-400',
];

function SearchSkeleton() {
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="animate-pulse">
          <div className="aspect-[3/4] bg-gray-200 rounded-lg mb-3" />
          <div className="h-3 bg-gray-200 rounded w-1/3 mb-2" />
          <div className="h-4 bg-gray-200 rounded w-2/3 mb-2" />
          <div className="h-4 bg-gray-200 rounded w-1/4" />
        </div>
      ))}
    </div>
  );
}

function RankChangeIndicator({ change, changeAmount }: { change: RankChange; changeAmount?: number }) {
  switch (change) {
    case 'NEW':
      return <span className="text-xs font-bold text-red-500 ml-1">NEW</span>;
    case 'UP':
      return (
        <span className="text-xs text-red-500 ml-1">
          <svg className="inline h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M5.293 9.707a1 1 0 010-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 01-1.414 1.414L10 6.414l-3.293 3.293a1 1 0 01-1.414 0z" clipRule="evenodd" />
          </svg>
          {changeAmount}
        </span>
      );
    case 'DOWN':
      return (
        <span className="text-xs text-blue-500 ml-1">
          <svg className="inline h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L10 13.586l3.293-3.293a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
          {changeAmount}
        </span>
      );
    case 'SAME':
      return <span className="text-xs text-gray-400 ml-1">-</span>;
    default:
      return null;
  }
}

function SearchContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const initialKeyword = searchParams.get('q') || '';

  const [keyword, setKeyword] = useState(initialKeyword);
  const [inputValue, setInputValue] = useState(initialKeyword);
  const [suggestions, setSuggestions] = useState<AutocompleteSuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [popularKeywords, setPopularKeywords] = useState<PopularKeyword[]>([]);
  const [result, setResult] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [filters, setFilters] = useState<SearchProductsParams>({
    keyword: initialKeyword,
    page: 0,
    size: 12,
    sort: 'relevance',
  });

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  // Load popular keywords on mount
  useEffect(() => {
    getPopularKeywords()
      .then((res) => setPopularKeywords(res.data.data || []))
      .catch(() => {});
  }, []);

  // Autocomplete with debounce
  useEffect(() => {
    if (!inputValue.trim() || inputValue === keyword) {
      setSuggestions([]);
      return;
    }
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      getAutocomplete(inputValue)
        .then((res) => {
          const items = res.data.data?.suggestions || [];
          setSuggestions(items.slice(0, 10));
          setShowSuggestions(items.length > 0);
        })
        .catch(() => setSuggestions([]));
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [inputValue, keyword]);

  // Close suggestions on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(e.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // Search function
  const doSearch = useCallback(
    async (currentFilters: SearchProductsParams) => {
      if (!currentFilters.keyword.trim()) {
        setResult(null);
        return;
      }
      setLoading(true);
      try {
        const cleanFilters = Object.fromEntries(
          Object.entries(currentFilters).filter(
            ([, v]) => v !== undefined && v !== null && v !== '',
          ),
        );
        const res = await searchProducts(cleanFilters as SearchProductsParams);
        setResult(res.data.data);
      } catch {
        setResult(null);
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  // Perform search when filters or keyword change
  useEffect(() => {
    if (filters.keyword.trim()) {
      doSearch(filters);
    }
  }, [filters, doSearch]);

  const handleSearch = (searchKeyword: string) => {
    const q = searchKeyword.trim();
    if (!q) return;
    setKeyword(q);
    setInputValue(q);
    setShowSuggestions(false);
    setFilters((prev) => ({ ...prev, keyword: q, page: 0 }));
    router.replace(`/search?q=${encodeURIComponent(q)}`, { scroll: false });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSearch(inputValue);
  };

  const handleFilterChange = (partial: Partial<SearchProductsParams>) => {
    setFilters((prev) => ({ ...prev, ...partial }));
  };

  const handleSortChange = (sort: string) => {
    setFilters((prev) => ({ ...prev, sort, page: 0 }));
  };

  const handlePageChange = (page: number) => {
    setFilters((prev) => ({ ...prev, page }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (filters.categoryId) count++;
    if (filters.brandId) count++;
    if (filters.minPrice) count++;
    if (filters.maxPrice) count++;
    if (filters.colorName) count++;
    if (filters.size_) count++;
    return count;
  }, [filters]);

  const totalPages = result?.totalPages || 0;
  const currentPage = filters.page || 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Search Bar */}
      <div className="relative max-w-2xl mx-auto mb-8">
        <form onSubmit={handleSubmit} className="relative">
          <input
            ref={inputRef}
            type="text"
            value={inputValue}
            onChange={(e) => {
              setInputValue(e.target.value);
              if (!e.target.value.trim()) setShowSuggestions(false);
            }}
            onFocus={() => {
              if (suggestions.length > 0) setShowSuggestions(true);
            }}
            placeholder="상품명, 브랜드, 카테고리 검색..."
            className="w-full px-4 py-3 pr-12 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-black focus:border-transparent text-base"
          />
          <button
            type="submit"
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-900"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </button>
        </form>

        {/* Autocomplete Dropdown */}
        {showSuggestions && suggestions.length > 0 && (
          <div
            ref={suggestionsRef}
            className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-xl shadow-lg overflow-hidden"
          >
            {suggestions.map((s, i) => (
              <button
                key={`${s.keyword}-${i}`}
                onClick={() => handleSearch(s.keyword)}
                className="w-full px-4 py-2.5 text-left text-sm hover:bg-gray-50 flex items-center gap-2"
              >
                <svg
                  className="h-4 w-4 text-gray-400 flex-shrink-0"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                  />
                </svg>
                <span className="flex-1 truncate">{s.keyword}</span>
                <span className="text-xs text-gray-400 flex-shrink-0">
                  {s.type === 'BRAND' ? '브랜드' : s.type === 'CATEGORY' ? '카테고리' : ''}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* No keyword: show popular keywords */}
      {!keyword.trim() && (
        <div className="max-w-2xl mx-auto">
          <h2 className="text-lg font-bold text-gray-900 mb-4">
            인기 검색어
          </h2>
          {popularKeywords.length === 0 ? (
            <div className="text-center py-8 text-gray-400 text-sm">
              인기 검색어를 불러오는 중...
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
              {popularKeywords.slice(0, 10).map((kw) => (
                <button
                  key={kw.rank}
                  onClick={() => handleSearch(kw.keyword)}
                  className="flex items-center gap-3 py-3 border-b border-gray-100 hover:bg-gray-50 px-2 rounded transition-colors text-left"
                >
                  <span
                    className={`w-6 text-center font-bold text-sm ${
                      kw.rank <= 3 ? 'text-black' : 'text-gray-400'
                    }`}
                  >
                    {kw.rank}
                  </span>
                  <span className="flex-1 text-sm text-gray-900">
                    {kw.keyword}
                  </span>
                  <RankChangeIndicator
                    change={kw.change}
                    changeAmount={kw.changeAmount}
                  />
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Search Results */}
      {keyword.trim() && (
        <>
          <h1 className="text-xl font-bold text-gray-900 mb-6">
            &ldquo;{keyword}&rdquo; 검색 결과
          </h1>

          <div className="flex gap-8">
            {/* Desktop Filter Sidebar */}
            <div className="w-64 flex-shrink-0 hidden lg:block">
              <SearchFilterSidebar
                facets={result?.facets || null}
                filters={filters}
                onFilterChange={handleFilterChange}
              />
            </div>

            {/* Mobile Filter Bottom Sheet */}
            {mobileFilterOpen && (
              <div className="fixed inset-0 z-50 lg:hidden">
                <div
                  className="absolute inset-0 bg-black/40"
                  onClick={() => setMobileFilterOpen(false)}
                />
                <div className="absolute bottom-0 left-0 right-0 max-h-[80vh] bg-white rounded-t-2xl shadow-xl overflow-y-auto">
                  <div className="flex items-center justify-between p-4 border-b border-gray-200 sticky top-0 bg-white">
                    <h2 className="text-lg font-bold">필터</h2>
                    <button
                      onClick={() => setMobileFilterOpen(false)}
                      className="p-1 text-gray-500 hover:text-gray-900"
                      aria-label="필터 닫기"
                    >
                      <svg
                        className="h-6 w-6"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth={2}
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M6 18L18 6M6 6l12 12"
                        />
                      </svg>
                    </button>
                  </div>
                  <div className="p-4">
                    <SearchFilterSidebar
                      facets={result?.facets || null}
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

            {/* Results Grid */}
            <div className="flex-1">
              {/* Sort bar */}
              <div className="flex justify-between items-center mb-6 gap-3">
                <p className="text-sm text-gray-500 flex-shrink-0">
                  {loading
                    ? '검색 중...'
                    : `총 ${(result?.totalElements || 0).toLocaleString()}개 상품`}
                </p>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setMobileFilterOpen(true)}
                    className="lg:hidden flex items-center gap-1.5 px-3 py-1.5 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
                  >
                    <svg
                      className="h-4 w-4"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"
                      />
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
                    value={filters.sort || 'relevance'}
                    onChange={(e) => handleSortChange(e.target.value)}
                  >
                    {SORT_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Spell Correction */}
              {result?.spellCorrection && (
                <div className="mb-6 p-3 bg-yellow-50 rounded-lg text-sm">
                  <span className="text-gray-600">혹시 </span>
                  <button
                    onClick={() => handleSearch(result.spellCorrection!)}
                    className="text-blue-600 font-medium hover:underline"
                  >
                    {result.spellCorrection}
                  </button>
                  <span className="text-gray-600">
                    을(를) 찾으시나요?
                  </span>
                </div>
              )}

              {loading ? (
                <SearchSkeleton />
              ) : !result || result.products.length === 0 ? (
                <div className="text-center py-16 text-gray-500">
                  <svg
                    className="h-16 w-16 mx-auto text-gray-300 mb-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={1}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    />
                  </svg>
                  <p className="text-lg">
                    &ldquo;{keyword}&rdquo;에 대한 검색 결과가 없습니다
                  </p>
                  <p className="text-sm mt-2">
                    다른 검색어를 입력하거나 필터를 변경해보세요.
                  </p>
                  {result?.spellCorrection && (
                    <button
                      onClick={() =>
                        handleSearch(result.spellCorrection!)
                      }
                      className="mt-4 text-sm text-black font-medium hover:underline"
                    >
                      &ldquo;{result.spellCorrection}&rdquo;(으)로 검색
                    </button>
                  )}
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
                  {result.products.map((product) => (
                    <Link
                      key={product.id}
                      href={`/products/${product.id}`}
                      className="group"
                    >
                      <div className="aspect-[3/4] bg-gray-100 rounded-lg overflow-hidden mb-3 relative">
                        {product.thumbnailUrl ? (
                          <Image
                            src={product.thumbnailUrl}
                            alt={product.name}
                            fill
                            sizes="(max-width: 768px) 50vw, (max-width: 1024px) 33vw, 25vw"
                            className="object-cover group-hover:scale-105 transition-transform duration-300"
                          />
                        ) : (
                          <div
                            className={`w-full h-full flex items-center justify-center ${BRAND_COLORS[product.brandId % BRAND_COLORS.length]} group-hover:scale-105 transition-transform duration-300`}
                          >
                            <span className="text-3xl sm:text-4xl font-bold text-white/80">
                              {product.brandName.charAt(0).toUpperCase()}
                            </span>
                          </div>
                        )}
                        {product.status === 'SOLD_OUT' && (
                          <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                            <span className="text-white font-bold text-sm">
                              SOLD OUT
                            </span>
                          </div>
                        )}
                        {product.discountRate > 0 && (
                          <span className="absolute top-2 left-2 bg-red-600 text-white text-xs font-bold px-2 py-1 rounded">
                            {product.discountRate}%
                          </span>
                        )}
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">
                          {product.brandName}
                        </p>
                        <h3 className="text-sm font-medium text-gray-900 mb-1 line-clamp-2">
                          {product.name}
                        </h3>
                        <div className="flex items-center gap-2 flex-wrap">
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
                        {product.reviewCount > 0 && (
                          <div className="flex items-center gap-1 mt-1">
                            <svg
                              className="h-3.5 w-3.5 text-yellow-400"
                              fill="currentColor"
                              viewBox="0 0 20 20"
                            >
                              <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                            </svg>
                            <span className="text-xs text-gray-500">
                              {product.averageRating.toFixed(1)} ({product.reviewCount})
                            </span>
                          </div>
                        )}
                      </div>
                    </Link>
                  ))}
                </div>
              )}

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex justify-center items-center gap-1 sm:gap-2 mt-12">
                  <button
                    disabled={currentPage === 0}
                    onClick={() => handlePageChange(currentPage - 1)}
                    className="px-2 sm:px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    이전
                  </button>
                  {Array.from({ length: Math.min(totalPages, 7) }).map(
                    (_, i) => {
                      let pageNum = i;
                      if (totalPages > 7) {
                        if (currentPage < 4) {
                          pageNum = i;
                        } else if (currentPage > totalPages - 4) {
                          pageNum = totalPages - 7 + i;
                        } else {
                          pageNum = currentPage - 3 + i;
                        }
                      }
                      return (
                        <button
                          key={pageNum}
                          onClick={() => handlePageChange(pageNum)}
                          className={`w-9 h-9 sm:w-10 sm:h-10 text-sm rounded-lg ${
                            currentPage === pageNum
                              ? 'bg-black text-white'
                              : 'border border-gray-300 hover:bg-gray-50'
                          }`}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    },
                  )}
                  <button
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => handlePageChange(currentPage + 1)}
                    className="px-2 sm:px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
                  >
                    다음
                  </button>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="max-w-2xl mx-auto mb-8">
            <div className="h-12 bg-gray-200 rounded-xl animate-pulse" />
          </div>
          <SearchSkeleton />
        </div>
      }
    >
      <SearchContent />
    </Suspense>
  );
}
