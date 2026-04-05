'use client';

import { useEffect, useState, useCallback } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { getReviews, getReviewSummary, markReviewHelpful } from '@/lib/api/review';
import { formatDate } from '@/lib/utils/format';
import type { Review, ReviewSummary, ReviewListParams, FitType } from '@/types/review';
import type { PageResponse } from '@/types/common';

interface ReviewListProps {
  productId: number;
}

const SORT_OPTIONS = [
  { label: '최신순', value: 'createdAt,desc' },
  { label: '별점 높은순', value: 'rating,desc' },
  { label: '도움순', value: 'helpfulCount,desc' },
];

const FIT_LABELS: Record<FitType, string> = {
  SMALL: '작아요',
  TRUE_TO_SIZE: '딱 맞아요',
  LARGE: '커요',
};

const EMPTY_PAGE: PageResponse<Review> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  pageable: { pageNumber: 0, pageSize: 10 },
};

function RatingBar({ rating, count, total }: { rating: number; count: number; total: number }) {
  const percentage = total > 0 ? (count / total) * 100 : 0;
  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="w-6 text-right text-gray-500">{rating}</span>
      <svg className="h-3.5 w-3.5 text-yellow-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
      </svg>
      <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
        <div
          className="h-full bg-yellow-400 rounded-full transition-all"
          style={{ width: `${percentage}%` }}
        />
      </div>
      <span className="w-8 text-right text-xs text-gray-400">{count}</span>
    </div>
  );
}

function FitBar({ label, count, total }: { label: string; count: number; total: number }) {
  const percentage = total > 0 ? (count / total) * 100 : 0;
  return (
    <div className="flex-1 text-center">
      <div className="text-sm font-medium text-gray-700 mb-1">{label}</div>
      <div className="h-2 bg-gray-200 rounded-full overflow-hidden mx-2">
        <div
          className="h-full bg-black rounded-full transition-all"
          style={{ width: `${percentage}%` }}
        />
      </div>
      <div className="text-xs text-gray-400 mt-1">{Math.round(percentage)}%</div>
    </div>
  );
}

export default function ReviewList({ productId }: ReviewListProps) {
  const [reviews, setReviews] = useState<PageResponse<Review>>(EMPTY_PAGE);
  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<ReviewListParams>({
    productId,
    page: 0,
    size: 10,
    sort: 'createdAt,desc',
    photoOnly: false,
  });

  const fetchReviews = useCallback(async (params: ReviewListParams) => {
    setLoading(true);
    try {
      const [reviewRes, summaryRes] = await Promise.all([
        getReviews(params),
        getReviewSummary(productId),
      ]);
      setReviews(reviewRes.data.data || EMPTY_PAGE);
      setSummary(summaryRes.data.data || null);
    } catch {
      setReviews(EMPTY_PAGE);
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    fetchReviews(filters);
  }, [filters, fetchReviews]);

  const handleHelpful = async (reviewId: number) => {
    try {
      await markReviewHelpful(reviewId);
      setReviews((prev) => ({
        ...prev,
        content: prev.content.map((r) =>
          r.id === reviewId
            ? { ...r, helpfulCount: r.helpfulCount + 1, isHelpful: true }
            : r,
        ),
      }));
    } catch {
      // Silently fail
    }
  };

  const handlePageChange = (page: number) => {
    setFilters((prev) => ({ ...prev, page }));
  };

  const fitTotal = summary
    ? summary.fitDistribution.SMALL +
      summary.fitDistribution.TRUE_TO_SIZE +
      summary.fitDistribution.LARGE
    : 0;

  return (
    <div>
      {/* Summary */}
      {summary && (
        <div className="bg-gray-50 rounded-lg p-4 sm:p-6 mb-6">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            {/* Average Rating */}
            <div className="text-center sm:border-r sm:border-gray-200">
              <div className="text-4xl font-bold text-gray-900">
                {summary.averageRating.toFixed(1)}
              </div>
              <div className="flex justify-center gap-0.5 mt-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <svg
                    key={star}
                    className={`h-4 w-4 ${
                      star <= Math.round(summary.averageRating)
                        ? 'text-yellow-400'
                        : 'text-gray-300'
                    }`}
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                ))}
              </div>
              <div className="text-xs text-gray-400 mt-1">
                {summary.totalCount}개 리뷰
              </div>
            </div>

            {/* Rating Distribution */}
            <div className="space-y-1">
              {[5, 4, 3, 2, 1].map((star) => (
                <RatingBar
                  key={star}
                  rating={star}
                  count={summary.ratingDistribution[star] || 0}
                  total={summary.totalCount}
                />
              ))}
            </div>

            {/* Fit Distribution */}
            {fitTotal > 0 && (
              <div className="sm:border-l sm:border-gray-200 sm:pl-6">
                <p className="text-xs text-gray-500 mb-3 text-center">
                  사이즈 핏
                </p>
                <div className="flex">
                  <FitBar label="작아요" count={summary.fitDistribution.SMALL} total={fitTotal} />
                  <FitBar label="딱맞아요" count={summary.fitDistribution.TRUE_TO_SIZE} total={fitTotal} />
                  <FitBar label="커요" count={summary.fitDistribution.LARGE} total={fitTotal} />
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-2 mb-4">
        <select
          className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm bg-white"
          value={filters.sort || 'createdAt,desc'}
          onChange={(e) =>
            setFilters((prev) => ({ ...prev, sort: e.target.value, page: 0 }))
          }
        >
          {SORT_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <button
          onClick={() =>
            setFilters((prev) => ({
              ...prev,
              photoOnly: !prev.photoOnly,
              page: 0,
            }))
          }
          className={`px-3 py-1.5 text-sm rounded-lg border transition-colors ${
            filters.photoOnly
              ? 'bg-black text-white border-black'
              : 'border-gray-300 text-gray-700 hover:border-black'
          }`}
        >
          포토리뷰
          {summary?.photoReviewCount
            ? ` (${summary.photoReviewCount})`
            : ''}
        </button>
        <button
          onClick={() =>
            setFilters((prev) => ({
              ...prev,
              myBodyType: !prev.myBodyType,
              page: 0,
            }))
          }
          className={`px-3 py-1.5 text-sm rounded-lg border transition-colors ${
            filters.myBodyType
              ? 'bg-black text-white border-black'
              : 'border-gray-300 text-gray-700 hover:border-black'
          }`}
        >
          나와 비슷한 체형
        </button>
      </div>

      {/* Review Items */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="border-b border-gray-200 pb-4 animate-pulse">
              <div className="flex gap-2 mb-2">
                <div className="h-4 bg-gray-200 rounded w-20" />
                <div className="h-4 bg-gray-200 rounded w-24" />
              </div>
              <div className="h-4 bg-gray-200 rounded w-full mb-1" />
              <div className="h-4 bg-gray-200 rounded w-2/3" />
            </div>
          ))}
        </div>
      ) : reviews.content.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <p className="text-sm">아직 리뷰가 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-0 divide-y divide-gray-200">
          {reviews.content.map((review) => (
            <div key={review.id} className="py-4">
              {/* Review Header */}
              <div className="flex items-center gap-2 mb-2">
                <div className="flex gap-0.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <svg
                      key={star}
                      className={`h-3.5 w-3.5 ${
                        star <= review.rating
                          ? 'text-yellow-400'
                          : 'text-gray-300'
                      }`}
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
                <span className="text-sm font-medium text-gray-900">
                  {review.memberName}
                </span>
                <span className="text-xs text-gray-400">
                  {formatDate(review.createdAt)}
                </span>
              </div>

              {/* Body Info */}
              {(review.height || review.weight || review.fitType) && (
                <div className="flex flex-wrap gap-2 mb-2">
                  {review.height && (
                    <span className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full">
                      {review.height}cm
                    </span>
                  )}
                  {review.weight && (
                    <span className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full">
                      {review.weight}kg
                    </span>
                  )}
                  {review.fitType && (
                    <span className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full">
                      {FIT_LABELS[review.fitType]}
                    </span>
                  )}
                  {review.purchasedOption && (
                    <span className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded-full">
                      {review.purchasedOption}
                    </span>
                  )}
                </div>
              )}

              {/* Content */}
              <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-line mb-3">
                {review.content}
              </p>

              {/* Images */}
              {review.images.length > 0 && (
                <div className="flex gap-2 mb-3 overflow-x-auto pb-1">
                  {review.images
                    .sort((a, b) => a.sortOrder - b.sortOrder)
                    .map((img) => (
                      <div
                        key={img.id}
                        className="w-20 h-20 flex-shrink-0 rounded-lg overflow-hidden bg-gray-100"
                      >
                        <Image
                          src={img.imageUrl}
                          alt="리뷰 이미지"
                          width={80}
                          height={80}
                          className="w-full h-full object-cover"
                        />
                      </div>
                    ))}
                </div>
              )}

              {/* Helpful */}
              <button
                onClick={() => !review.isHelpful && handleHelpful(review.id)}
                disabled={review.isHelpful}
                className={`inline-flex items-center gap-1 px-3 py-1.5 text-xs rounded-full border transition-colors ${
                  review.isHelpful
                    ? 'bg-gray-100 text-gray-500 border-gray-200 cursor-default'
                    : 'border-gray-300 text-gray-600 hover:bg-gray-50 hover:border-gray-400'
                }`}
              >
                <svg
                  className="h-3.5 w-3.5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  strokeWidth={2}
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5"
                  />
                </svg>
                도움이 됐어요 {review.helpfulCount > 0 && `(${review.helpfulCount})`}
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {reviews.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2 mt-8">
          <button
            disabled={filters.page === 0}
            onClick={() => handlePageChange((filters.page || 0) - 1)}
            className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 hover:bg-gray-50"
          >
            이전
          </button>
          {Array.from({ length: Math.min(reviews.totalPages, 5) }).map(
            (_, i) => (
              <button
                key={i}
                onClick={() => handlePageChange(i)}
                className={`w-9 h-9 text-sm rounded-lg ${
                  (filters.page || 0) === i
                    ? 'bg-black text-white'
                    : 'border border-gray-300 hover:bg-gray-50'
                }`}
              >
                {i + 1}
              </button>
            ),
          )}
          <button
            disabled={(filters.page || 0) >= reviews.totalPages - 1}
            onClick={() => handlePageChange((filters.page || 0) + 1)}
            className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 hover:bg-gray-50"
          >
            다음
          </button>
        </div>
      )}

      {/* Write Review Link */}
      <div className="text-center mt-6">
        <Link
          href={`/reviews/write?productId=${productId}`}
          className="inline-flex items-center gap-2 px-6 py-3 bg-black text-white rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors"
        >
          <svg
            className="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"
            />
          </svg>
          리뷰 작성하기
        </Link>
      </div>
    </div>
  );
}
