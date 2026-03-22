export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  code?: string;
}

export interface ErrorResponse {
  success: false;
  message: string;
  code: string;
  errors?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}
