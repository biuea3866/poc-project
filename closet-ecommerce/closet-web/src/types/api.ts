/**
 * API-related type definitions.
 * Re-exports from common.ts for backward compatibility,
 * plus additional API-specific types.
 */
export type { ApiResponse, ErrorResponse, PageResponse } from './common';

/**
 * Standard query parameters for paginated list endpoints.
 */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * API error that can be thrown from API client interceptors.
 */
export interface ApiError {
  status: number;
  code: string;
  message: string;
  details?: string[];
}
