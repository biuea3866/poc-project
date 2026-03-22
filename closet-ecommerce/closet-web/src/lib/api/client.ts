import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach JWT + X-Member-Id
apiClient.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('accessToken');
    const memberId = localStorage.getItem('memberId');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (memberId) {
      config.headers['X-Member-Id'] = memberId;
    }
  }
  return config;
});

// Response interceptor: handle 401 -> refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // TODO: refresh token logic
      // 1. Get refreshToken from localStorage
      // 2. Call /members/refresh
      // 3. If success, update tokens and retry original request
      // 4. If fail, redirect to login
    }
    return Promise.reject(error);
  }
);

export default apiClient;
