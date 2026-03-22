export interface Member {
  id: number;
  email: string;
  name: string;
  phone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ShippingAddress {
  id: number;
  memberId: number;
  name: string;
  recipientName: string;
  phone: string;
  zipCode: string;
  address: string;
  addressDetail: string;
  isDefault: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  member: Member;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  phone?: string;
}
