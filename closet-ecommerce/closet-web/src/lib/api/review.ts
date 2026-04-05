import apiClient from './client';
import type { ApiResponse, PageResponse } from '@/types/common';
import type {
  Review,
  ReviewSummary,
  CreateReviewRequest,
  PresignedUrlRequest,
  PresignedUrlResponse,
  ReviewListParams,
} from '@/types/review';

export const getReviews = (params: ReviewListParams) =>
  apiClient.get<ApiResponse<PageResponse<Review>>>('/reviews', { params });

export const getReviewSummary = (productId: number) =>
  apiClient.get<ApiResponse<ReviewSummary>>(`/reviews/summary`, {
    params: { productId },
  });

export const createReview = (data: CreateReviewRequest) =>
  apiClient.post<ApiResponse<Review>>('/reviews', data);

export const getPresignedUrl = (data: PresignedUrlRequest) =>
  apiClient.post<ApiResponse<PresignedUrlResponse>>(
    '/reviews/images/presigned-url',
    data,
  );

export const markReviewHelpful = (reviewId: number) =>
  apiClient.post<ApiResponse<void>>(`/reviews/${reviewId}/helpful`);
