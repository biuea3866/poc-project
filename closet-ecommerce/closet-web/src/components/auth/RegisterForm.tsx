'use client';

import { useState } from 'react';
import Link from 'next/link';

interface RegisterFormProps {
  onSubmit: (data: { email: string; password: string; name: string; phone?: string }) => void;
  isLoading?: boolean;
  error?: string | null;
}

export default function RegisterForm({ onSubmit, isLoading, error }: RegisterFormProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  // Terms agreement state
  const [agreeAll, setAgreeAll] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeMarketing, setAgreeMarketing] = useState(false);

  const handleAgreeAll = (checked: boolean) => {
    setAgreeAll(checked);
    setAgreeTerms(checked);
    setAgreePrivacy(checked);
    setAgreeMarketing(checked);
  };

  const handleIndividualChange = (
    setter: (v: boolean) => void,
    value: boolean,
    others: boolean[],
  ) => {
    setter(value);
    const allChecked = [value, ...others].every(Boolean);
    setAgreeAll(allChecked);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setValidationError('비밀번호가 일치하지 않습니다');
      return;
    }
    if (!agreeTerms || !agreePrivacy) {
      setValidationError('필수 약관에 동의해주세요');
      return;
    }
    setValidationError(null);
    onSubmit({ email, password, name, phone: phone || undefined });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {(error || validationError) && (
        <div className="bg-red-50 text-red-600 text-sm p-3 rounded-lg">
          {error || validationError}
        </div>
      )}
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
          이름
        </label>
        <input
          id="name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          placeholder="이름을 입력해주세요"
        />
      </div>
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
          minLength={8}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          placeholder="8자 이상 입력해주세요"
        />
      </div>
      <div>
        <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
          비밀번호 확인
        </label>
        <input
          id="confirmPassword"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          placeholder="비밀번호를 다시 입력해주세요"
        />
      </div>
      <div>
        <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-1">
          전화번호 <span className="text-gray-400">(선택)</span>
        </label>
        <input
          id="phone"
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
          placeholder="010-0000-0000"
        />
      </div>

      {/* Terms Agreement */}
      <div className="border border-gray-200 rounded-lg p-4 space-y-3">
        <label className="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            checked={agreeAll}
            onChange={(e) => handleAgreeAll(e.target.checked)}
            className="w-5 h-5 accent-black rounded"
          />
          <span className="text-sm font-semibold">전체 동의하기</span>
        </label>
        <div className="border-t border-gray-200 pt-3 space-y-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={agreeTerms}
              onChange={(e) =>
                handleIndividualChange(setAgreeTerms, e.target.checked, [agreePrivacy, agreeMarketing])
              }
              className="w-4 h-4 accent-black rounded"
            />
            <span className="text-sm text-gray-700">
              <span className="text-red-500">[필수]</span> 이용약관 동의
            </span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={agreePrivacy}
              onChange={(e) =>
                handleIndividualChange(setAgreePrivacy, e.target.checked, [agreeTerms, agreeMarketing])
              }
              className="w-4 h-4 accent-black rounded"
            />
            <span className="text-sm text-gray-700">
              <span className="text-red-500">[필수]</span> 개인정보 수집 및 이용 동의
            </span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={agreeMarketing}
              onChange={(e) =>
                handleIndividualChange(setAgreeMarketing, e.target.checked, [agreeTerms, agreePrivacy])
              }
              className="w-4 h-4 accent-black rounded"
            />
            <span className="text-sm text-gray-700">
              <span className="text-gray-400">[선택]</span> 마케팅 정보 수신 동의
            </span>
          </label>
        </div>
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full bg-black text-white py-3 rounded-lg font-medium hover:bg-gray-800 disabled:bg-gray-400 transition-colors"
      >
        {isLoading ? '가입 중...' : '회원가입'}
      </button>
      <p className="text-center text-sm text-gray-600">
        이미 계정이 있으신가요?{' '}
        <Link href="/auth/login" className="text-black font-medium hover:underline">
          로그인
        </Link>
      </p>
    </form>
  );
}
