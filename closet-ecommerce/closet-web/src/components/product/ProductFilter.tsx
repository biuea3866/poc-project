'use client';

interface ProductFilterProps {
  onFilterChange?: (filters: Record<string, string>) => void;
}

export default function ProductFilter({ onFilterChange }: ProductFilterProps) {
  return (
    <aside className="w-full">
      <h2 className="text-lg font-bold mb-4">필터</h2>

      {/* Category */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-2">카테고리</h3>
        <div className="space-y-2">
          <div className="h-4 bg-gray-200 rounded w-3/4 animate-pulse" />
          <div className="h-4 bg-gray-200 rounded w-2/3 animate-pulse" />
          <div className="h-4 bg-gray-200 rounded w-1/2 animate-pulse" />
        </div>
      </div>

      {/* Price Range */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-2">가격대</h3>
        <div className="flex gap-2">
          <input
            type="number"
            placeholder="최소"
            className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
          />
          <span className="text-gray-400">~</span>
          <input
            type="number"
            placeholder="최대"
            className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
          />
        </div>
      </div>

      {/* Brand */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-2">브랜드</h3>
        <div className="space-y-2">
          <div className="h-4 bg-gray-200 rounded w-2/3 animate-pulse" />
          <div className="h-4 bg-gray-200 rounded w-3/4 animate-pulse" />
          <div className="h-4 bg-gray-200 rounded w-1/2 animate-pulse" />
        </div>
      </div>
    </aside>
  );
}
