import apiClient from './client';
import { ApiResponse } from '../types/common';
import { LoginRequest, LoginResponse, RegisterRequest, Member, Address } from '../types/member';

export const memberApi = {
  login: (data: LoginRequest) =>
    apiClient.post<ApiResponse<LoginResponse>>('/members/login', data),

  register: (data: RegisterRequest) =>
    apiClient.post<ApiResponse<Member>>('/members/register', data),

  getMyInfo: () =>
    apiClient.get<ApiResponse<Member>>('/members/me'),

  updateMyInfo: (data: Partial<Member>) =>
    apiClient.patch<ApiResponse<Member>>('/members/me', data),

  getAddresses: () =>
    apiClient.get<ApiResponse<Address[]>>('/members/me/addresses'),

  addAddress: (data: Omit<Address, 'id' | 'memberId'>) =>
    apiClient.post<ApiResponse<Address>>('/members/me/addresses', data),

  updateAddress: (addressId: number, data: Partial<Address>) =>
    apiClient.patch<ApiResponse<Address>>(`/members/me/addresses/${addressId}`, data),

  deleteAddress: (addressId: number) =>
    apiClient.delete(`/members/me/addresses/${addressId}`),
};
