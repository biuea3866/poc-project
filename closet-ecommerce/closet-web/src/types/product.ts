export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  discountPrice: number | null;
  brandId: number;
  brandName: string;
  categoryId: number;
  categoryName: string;
  images: ProductImage[];
  options: ProductOption[];
  stockQuantity: number;
  status: ProductStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProductImage {
  id: number;
  url: string;
  sortOrder: number;
  isThumbnail: boolean;
}

export interface ProductOption {
  id: number;
  name: string;
  values: ProductOptionValue[];
}

export interface ProductOptionValue {
  id: number;
  value: string;
  additionalPrice: number;
  stockQuantity: number;
}

export interface Category {
  id: number;
  name: string;
  parentId: number | null;
  children: Category[];
}

export interface Brand {
  id: number;
  name: string;
  logoUrl: string | null;
}

export type ProductStatus = 'ACTIVE' | 'INACTIVE' | 'SOLD_OUT';

export interface ProductListParams {
  page?: number;
  size?: number;
  categoryId?: number;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  keyword?: string;
  sort?: string;
}
