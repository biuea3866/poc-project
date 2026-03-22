import apiClient from './client';
import { ApiResponse, PageResponse, PageRequest } from '../types/common';
import { Product, Category, Brand, ProductListFilter } from '../types/product';

export const productApi = {
  getProducts: (params: PageRequest & ProductListFilter) =>
    apiClient.get<ApiResponse<PageResponse<Product>>>('/products', { params }),

  getProduct: (productId: number) =>
    apiClient.get<ApiResponse<Product>>(`/products/${productId}`),

  getCategories: () =>
    apiClient.get<ApiResponse<Category[]>>('/categories'),

  getBrands: (params?: PageRequest) =>
    apiClient.get<ApiResponse<PageResponse<Brand>>>('/brands', { params }),

  getBrand: (brandId: number) =>
    apiClient.get<ApiResponse<Brand>>(`/brands/${brandId}`),
};
