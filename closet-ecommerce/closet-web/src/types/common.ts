export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ErrorResponse | null;
}

export interface ErrorResponse {
  code: string;
  message: string;
  details: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
}
