export interface SearchProductsParams {
  keyword: string;
  page?: number;
  size?: number;
  sort?: string;
  categoryId?: number;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  colorName?: string;
  size_?: string;
}

export interface SearchProduct {
  id: number;
  name: string;
  brandId: number;
  brandName: string;
  categoryId: number;
  categoryName: string;
  basePrice: number;
  salePrice: number;
  discountRate: number;
  status: string;
  thumbnailUrl: string | null;
  reviewCount: number;
  averageRating: number;
}

export interface SearchFacets {
  categories: FacetItem[];
  brands: FacetItem[];
  colors: FacetItem[];
  sizes: FacetItem[];
  priceRanges: PriceRangeFacet[];
}

export interface FacetItem {
  key: string;
  id?: number;
  count: number;
}

export interface PriceRangeFacet {
  min: number;
  max: number;
  count: number;
}

export interface SearchResponse {
  products: SearchProduct[];
  totalElements: number;
  totalPages: number;
  facets: SearchFacets;
  spellCorrection: string | null;
}

export interface AutocompleteResponse {
  suggestions: AutocompleteSuggestion[];
}

export interface AutocompleteSuggestion {
  keyword: string;
  type: 'PRODUCT' | 'BRAND' | 'CATEGORY';
}

export type RankChange = 'NEW' | 'UP' | 'DOWN' | 'SAME';

export interface PopularKeyword {
  rank: number;
  keyword: string;
  change: RankChange;
  changeAmount?: number;
}
