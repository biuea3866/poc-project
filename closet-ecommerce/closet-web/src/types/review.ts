export type FitType = 'SMALL' | 'TRUE_TO_SIZE' | 'LARGE';

export interface Review {
  id: number;
  productId: number;
  memberId: number;
  memberName: string;
  rating: number;
  content: string;
  images: ReviewImage[];
  height: number | null;
  weight: number | null;
  fitType: FitType | null;
  purchasedOption: string | null;
  helpfulCount: number;
  isHelpful: boolean;
  createdAt: string;
}

export interface ReviewImage {
  id: number;
  imageUrl: string;
  sortOrder: number;
}

export interface ReviewSummary {
  averageRating: number;
  totalCount: number;
  ratingDistribution: Record<number, number>;
  fitDistribution: {
    SMALL: number;
    TRUE_TO_SIZE: number;
    LARGE: number;
  };
  photoReviewCount: number;
}

export interface CreateReviewRequest {
  productId: number;
  orderId: number;
  orderItemId: number;
  rating: number;
  content: string;
  imageUrls: string[];
  height?: number;
  weight?: number;
  fitType?: FitType;
}

export interface PresignedUrlRequest {
  fileName: string;
  contentType: string;
}

export interface PresignedUrlResponse {
  uploadUrl: string;
  imageUrl: string;
}

export interface ReviewListParams {
  productId: number;
  page?: number;
  size?: number;
  sort?: string;
  photoOnly?: boolean;
  myBodyType?: boolean;
}
