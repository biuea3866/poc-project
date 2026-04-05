'use client';

import { useState } from 'react';
import Link from 'next/link';

interface LoginFormProps {
  onSubmit: (email: string, password: string) => void;
  isLoading?: boolean;
  error?: string | null;
}

export default function LoginForm({ onSubmit, isLoading, error }: LoginFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(email, password);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="bg-red-50 text-red-600 text-sm p-3 rounded-lg">{error}</div>
      )}
      <div>
        <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
          이메일
        </label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          placeholder="이메일을 입력해주세요"
        />
      </div>
      <div>
        <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
          비밀번호
        </label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          placeholder="비밀번호를 입력해주세요"
        />
      </div>
      <button
        type="submit"
        disabled={isLoading}
        className="w-full bg-black text-white py-3 rounded-lg font-medium hover:bg-gray-800 disabled:bg-gray-400 transition-colors"
      >
        {isLoading ? '로그인 중...' : '로그인'}
      </button>

      {/* Divider */}
      <div className="relative my-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-gray-300" />
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="bg-white px-4 text-gray-500">또는</span>
        </div>
      </div>

      {/* Social Login Buttons */}
      <div className="space-y-3">
        <button
          type="button"
          className="w-full flex items-center justify-center gap-3 py-3 rounded-lg font-medium transition-colors bg-[#FEE500] text-[#191919] hover:bg-[#FDD800]"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M9 1C4.58 1 1 3.79 1 7.21c0 2.17 1.45 4.08 3.64 5.18l-.93 3.44c-.08.3.26.54.52.37l4.12-2.74c.21.02.43.03.65.03 4.42 0 8-2.79 8-6.28C17 3.79 13.42 1 9 1z" fill="#191919"/>
          </svg>
          카카오로 시작하기
        </button>
        <button
          type="button"
          className="w-full flex items-center justify-center gap-3 py-3 rounded-lg font-medium transition-colors bg-[#03C75A] text-white hover:bg-[#02B350]"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M9 1C4.58 1 1 4.13 1 8s3.58 7 8 7c.34 0 .67-.02 1-.06V18l3.5-3.5C15.44 12.84 17 10.56 17 8c0-3.87-3.58-7-8-7z" fill="white"/>
            <text x="5" y="12" fill="#03C75A" fontSize="10" fontWeight="bold">N</text>
          </svg>
          네이버로 시작하기
        </button>
        <button
          type="button"
          className="w-full flex items-center justify-center gap-3 py-3 rounded-lg font-medium transition-colors border border-gray-300 bg-white text-gray-700 hover:bg-gray-50"
        >
          <svg width="18" height="18" viewBox="0 0 18 18">
            <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 01-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
            <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z" fill="#34A853"/>
            <path d="M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/>
            <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
          </svg>
          Google로 시작하기
        </button>
      </div>

      <p className="text-center text-sm text-gray-600 mt-6">
        계정이 없으신가요?{' '}
        <Link href="/auth/register" className="text-black font-medium hover:underline">
          회원가입
        </Link>
      </p>
    </form>
  );
}
