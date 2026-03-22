export interface Product {
  id: number;
  name: string;
  brandId: number;
  brandName: string;
  categoryId: number;
  categoryName: string;
  price: number;
  salePrice?: number;
  discountRate?: number;
  description: string;
  thumbnailUrl: string;
  imageUrls: string[];
  options: ProductOption[];
  reviewCount: number;
  averageRating: number;
  status: ProductStatus;
  season?: Season;
  fit?: Fit;
  material?: string;
  createdAt: string;
}

export type ProductStatus = 'ON_SALE' | 'SOLD_OUT' | 'DISCONTINUED' | 'HIDDEN';
export type Season = 'SS' | 'FW' | 'ALL';
export type Fit = 'OVERFIT' | 'REGULAR' | 'SLIM';

export interface ProductOption {
  id: number;
  productId: number;
  size: string;
  color: string;
  stockQuantity: number;
  additionalPrice: number;
}

export interface Category {
  id: number;
  name: string;
  parentId?: number;
  depth: number;
  children?: Category[];
}

export interface Brand {
  id: number;
  name: string;
  logoUrl?: string;
  description?: string;
}

export interface ProductListFilter {
  categoryId?: number;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  size?: string;
  color?: string;
  season?: Season;
  fit?: Fit;
  sort?: 'LATEST' | 'PRICE_ASC' | 'PRICE_DESC' | 'POPULAR' | 'REVIEW';
}
