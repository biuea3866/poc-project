'use client';

import { useState } from 'react';
import type { SearchFacets, SearchProductsParams } from '@/types/search';

interface SearchFilterSidebarProps {
  facets: SearchFacets | null;
  filters: SearchProductsParams;
  onFilterChange: (filters: Partial<SearchProductsParams>) => void;
}

const PRICE_RANGES = [
  { label: '1만원 이하', min: 0, max: 10000 },
  { label: '1만원 ~ 3만원', min: 10000, max: 30000 },
  { label: '3만원 ~ 5만원', min: 30000, max: 50000 },
  { label: '5만원 ~ 10만원', min: 50000, max: 100000 },
  { label: '10만원 이상', min: 100000, max: undefined },
];

export default function SearchFilterSidebar({
  facets,
  filters,
  onFilterChange,
}: SearchFilterSidebarProps) {
  const [openSections, setOpenSections] = useState<Record<string, boolean>>({
    category: true,
    brand: true,
    price: true,
    color: false,
    size: false,
  });

  const toggleSection = (key: string) => {
    setOpenSections((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const [priceMin, setPriceMin] = useState<string>(
    filters.minPrice?.toString() || '',
  );
  const [priceMax, setPriceMax] = useState<string>(
    filters.maxPrice?.toString() || '',
  );

  const handlePriceApply = () => {
    onFilterChange({
      minPrice: priceMin ? Number(priceMin) : undefined,
      maxPrice: priceMax ? Number(priceMax) : undefined,
      page: 0,
    });
  };

  const handlePriceRangeClick = (min: number, max: number | undefined) => {
    setPriceMin(String(min));
    setPriceMax(max !== undefined ? String(max) : '');
    onFilterChange({
      minPrice: min,
      maxPrice: max,
      page: 0,
    });
  };

  return (
    <aside className="w-full space-y-1">
      <h2 className="text-lg font-bold mb-4">필터</h2>

      {/* Category Accordion */}
      <div className="border-b border-gray-200 pb-4 mb-4">
        <button
          onClick={() => toggleSection('category')}
          className="flex items-center justify-between w-full text-sm font-semibold py-2"
        >
          카테고리
          <svg
            className={`h-4 w-4 transition-transform ${openSections.category ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
        {openSections.category && (
          <div className="space-y-1 mt-2">
            <button
              onClick={() =>
                onFilterChange({ categoryId: undefined, page: 0 })
              }
              className={`block text-sm w-full text-left px-2 py-1.5 rounded ${
                !filters.categoryId
                  ? 'bg-black text-white'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              전체
            </button>
            {facets?.categories.map((cat) => (
              <button
                key={cat.key}
                onClick={() =>
                  onFilterChange({ categoryId: cat.id, page: 0 })
                }
                className={`flex justify-between items-center text-sm w-full text-left px-2 py-1.5 rounded ${
                  filters.categoryId === cat.id
                    ? 'bg-black text-white'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                <span>{cat.key}</span>
                <span
                  className={`text-xs ${filters.categoryId === cat.id ? 'text-gray-300' : 'text-gray-400'}`}
                >
                  {cat.count}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Brand Checkbox */}
      <div className="border-b border-gray-200 pb-4 mb-4">
        <button
          onClick={() => toggleSection('brand')}
          className="flex items-center justify-between w-full text-sm font-semibold py-2"
        >
          브랜드
          <svg
            className={`h-4 w-4 transition-transform ${openSections.brand ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
        {openSections.brand && (
          <div className="space-y-1.5 mt-2 max-h-48 overflow-y-auto">
            {facets?.brands.map((brand) => (
              <label
                key={brand.key}
                className="flex items-center justify-between gap-2 text-sm px-2 py-1 cursor-pointer hover:bg-gray-50 rounded"
              >
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={filters.brandId === brand.id}
                    onChange={() =>
                      onFilterChange({
                        brandId:
                          filters.brandId === brand.id
                            ? undefined
                            : brand.id,
                        page: 0,
                      })
                    }
                    className="w-4 h-4 accent-black rounded"
                  />
                  <span className="text-gray-700">{brand.key}</span>
                </div>
                <span className="text-xs text-gray-400">{brand.count}</span>
              </label>
            ))}
          </div>
        )}
      </div>

      {/* Price Range */}
      <div className="border-b border-gray-200 pb-4 mb-4">
        <button
          onClick={() => toggleSection('price')}
          className="flex items-center justify-between w-full text-sm font-semibold py-2"
        >
          가격
          <svg
            className={`h-4 w-4 transition-transform ${openSections.price ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
        {openSections.price && (
          <div className="mt-2 space-y-3">
            <div className="space-y-1">
              {PRICE_RANGES.map((range) => (
                <button
                  key={range.label}
                  onClick={() =>
                    handlePriceRangeClick(range.min, range.max)
                  }
                  className={`block text-sm w-full text-left px-2 py-1.5 rounded ${
                    filters.minPrice === range.min &&
                    filters.maxPrice === range.max
                      ? 'bg-black text-white'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  {range.label}
                </button>
              ))}
            </div>
            <div className="flex gap-2 items-center">
              <input
                type="number"
                placeholder="최소"
                value={priceMin}
                onChange={(e) => setPriceMin(e.target.value)}
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              />
              <span className="text-gray-400 flex-shrink-0">~</span>
              <input
                type="number"
                placeholder="최대"
                value={priceMax}
                onChange={(e) => setPriceMax(e.target.value)}
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              />
              <button
                onClick={handlePriceApply}
                className="px-3 py-1.5 bg-gray-100 text-sm rounded hover:bg-gray-200 transition-colors flex-shrink-0"
              >
                적용
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Color Circles */}
      <div className="border-b border-gray-200 pb-4 mb-4">
        <button
          onClick={() => toggleSection('color')}
          className="flex items-center justify-between w-full text-sm font-semibold py-2"
        >
          색상
          <svg
            className={`h-4 w-4 transition-transform ${openSections.color ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
        {openSections.color && (
          <div className="flex flex-wrap gap-2 mt-2">
            {facets?.colors.map((color) => {
              const isSelected = filters.colorName === color.key;
              return (
                <button
                  key={color.key}
                  onClick={() =>
                    onFilterChange({
                      colorName: isSelected ? undefined : color.key,
                      page: 0,
                    })
                  }
                  className="flex flex-col items-center gap-1"
                  title={`${color.key} (${color.count})`}
                >
                  <span
                    className={`inline-block w-8 h-8 rounded-full border-2 transition-colors ${
                      isSelected ? 'border-black scale-110' : 'border-gray-200'
                    }`}
                    style={{
                      backgroundColor:
                        COLOR_HEX_MAP[color.key] || '#cccccc',
                    }}
                  />
                  <span className="text-[10px] text-gray-500">
                    {color.count}
                  </span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Size Chips */}
      <div className="pb-4 mb-4">
        <button
          onClick={() => toggleSection('size')}
          className="flex items-center justify-between w-full text-sm font-semibold py-2"
        >
          사이즈
          <svg
            className={`h-4 w-4 transition-transform ${openSections.size ? 'rotate-180' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
        {openSections.size && (
          <div className="flex flex-wrap gap-2 mt-2">
            {facets?.sizes.map((size) => {
              const isSelected = filters.size_ === size.key;
              return (
                <button
                  key={size.key}
                  onClick={() =>
                    onFilterChange({
                      size_: isSelected ? undefined : size.key,
                      page: 0,
                    })
                  }
                  className={`px-3 py-1.5 text-sm rounded-full border transition-colors ${
                    isSelected
                      ? 'bg-black text-white border-black'
                      : 'border-gray-300 text-gray-700 hover:border-black'
                  }`}
                >
                  {size.key}
                  <span
                    className={`ml-1 text-xs ${isSelected ? 'text-gray-300' : 'text-gray-400'}`}
                  >
                    ({size.count})
                  </span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Reset */}
      <button
        onClick={() =>
          onFilterChange({
            categoryId: undefined,
            brandId: undefined,
            minPrice: undefined,
            maxPrice: undefined,
            colorName: undefined,
            size_: undefined,
            page: 0,
          })
        }
        className="w-full text-sm text-gray-500 border border-gray-300 rounded py-2 hover:bg-gray-50 transition-colors"
      >
        필터 초기화
      </button>
    </aside>
  );
}

const COLOR_HEX_MAP: Record<string, string> = {
  블랙: '#000000',
  화이트: '#FFFFFF',
  그레이: '#808080',
  네이비: '#000080',
  베이지: '#F5F5DC',
  브라운: '#8B4513',
  레드: '#FF0000',
  핑크: '#FFC0CB',
  오렌지: '#FFA500',
  옐로우: '#FFFF00',
  그린: '#008000',
  블루: '#0000FF',
  퍼플: '#800080',
  카키: '#BDB76B',
  와인: '#722F37',
  아이보리: '#FFFFF0',
};
