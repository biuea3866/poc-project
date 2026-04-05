import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type {
  SearchProductsParams,
  SearchResponse,
  AutocompleteResponse,
  PopularKeyword,
} from '@/types/search';

export const searchProducts = (params: SearchProductsParams) =>
  apiClient.get<ApiResponse<SearchResponse>>('/search/products', { params });

export const getAutocomplete = (keyword: string) =>
  apiClient.get<ApiResponse<AutocompleteResponse>>('/search/autocomplete', {
    params: { keyword },
  });

export const getPopularKeywords = () =>
  apiClient.get<ApiResponse<PopularKeyword[]>>('/search/popular-keywords');
