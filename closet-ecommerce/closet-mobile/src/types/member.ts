export interface Member {
  id: number;
  email: string;
  name: string;
  nickname?: string;
  phoneNumber?: string;
  grade: MemberGrade;
  point: number;
  profileImageUrl?: string;
  createdAt: string;
}

export type MemberGrade = 'NORMAL' | 'SILVER' | 'GOLD' | 'PLATINUM';

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
  nickname?: string;
  phoneNumber?: string;
}

export interface Address {
  id: number;
  memberId: number;
  name: string;
  recipient: string;
  phoneNumber: string;
  zipCode: string;
  address: string;
  addressDetail: string;
  isDefault: boolean;
}
