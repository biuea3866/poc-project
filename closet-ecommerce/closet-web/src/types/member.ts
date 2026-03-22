export interface Member {
  id: number;
  email: string;
  name: string;
  phone: string;
  grade: string;
  pointBalance: number;
  status: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  memberId: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  phone?: string;
}

export interface ShippingAddress {
  id: number;
  name: string;
  phone: string;
  zipCode: string;
  address: string;
  detailAddress: string;
  isDefault: boolean;
}
