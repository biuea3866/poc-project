import apiClient from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type { Product, Category, Brand, ProductListParams } from '@/types/product';

export const getProducts = (params?: ProductListParams) =>
  apiClient.get<ApiResponse<PageResponse<Product>>>('/products', { params });

export const getProduct = (id: number) =>
  apiClient.get<ApiResponse<Product>>(`/products/${id}`);

export const getCategories = () =>
  apiClient.get<ApiResponse<Category[]>>('/categories');

export const getBrands = () =>
  apiClient.get<ApiResponse<Brand[]>>('/brands');
