export interface Product {
  id: number;
  name: string;
  description?: string;
  brandId: number;
  categoryId: number;
  basePrice: number;
  salePrice: number;
  discountRate: number;
  status: string;
  season: string;
  fitType: string;
  gender: string;
  options?: ProductOption[];
  images?: ProductImage[];
  sizeGuides?: SizeGuide[];
}

export interface ProductOption {
  id: number;
  size: string;
  colorName: string;
  colorHex: string;
  skuCode: string;
  additionalPrice: number;
}

export interface ProductImage {
  id: number;
  imageUrl: string;
  type: string;
  sortOrder: number;
}

export interface SizeGuide {
  id: number;
  size: string;
  shoulderWidth: number;
  chestWidth: number;
  totalLength: number;
  sleeveLength: number;
}

export interface Category {
  id: number;
  name: string;
  depth: number;
  parentId: number | null;
  sortOrder: number;
  status: string;
  children: Category[];
}

export interface Brand {
  id: number;
  name: string;
  description: string;
  logoUrl: string | null;
}

export type ProductListParams = {
  page?: number;
  size?: number;
  categoryId?: number;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  status?: string;
  sort?: string;
}
