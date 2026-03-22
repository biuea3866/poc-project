import apiClient from './client';
import type { ApiResponse } from '@/types/common';
import type { LoginRequest, LoginResponse, Member, RegisterRequest, ShippingAddress } from '@/types/member';

export const login = (data: LoginRequest) =>
  apiClient.post<ApiResponse<LoginResponse>>('/members/login', data);

export const register = (data: RegisterRequest) =>
  apiClient.post<ApiResponse<Member>>('/members/register', data);

export const getMe = () =>
  apiClient.get<ApiResponse<Member>>('/members/me');

export const getAddresses = () =>
  apiClient.get<ApiResponse<ShippingAddress[]>>('/members/me/addresses');

export const addAddress = (data: Omit<ShippingAddress, 'id' | 'memberId'>) =>
  apiClient.post<ApiResponse<ShippingAddress>>('/members/me/addresses', data);

export const deleteAddress = (addressId: number) =>
  apiClient.delete<ApiResponse<void>>(`/members/me/addresses/${addressId}`);
