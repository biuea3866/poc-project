'use client';

import { useState } from 'react';

interface SellerApplyForm {
  brandName: string;
  businessNumber: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  bankName: string;
  bankAccount: string;
  bankHolder: string;
}

const EMPTY_FORM: SellerApplyForm = {
  brandName: '',
  businessNumber: '',
  contactName: '',
  contactEmail: '',
  contactPhone: '',
  bankName: '',
  bankAccount: '',
  bankHolder: '',
};

export default function SellerApplyPage() {
  const [form, setForm] = useState<SellerApplyForm>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

  const updateField = (field: keyof SellerApplyForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!form.brandName || !form.businessNumber || !form.contactName || !form.contactEmail) {
      alert('필수 항목을 모두 입력해주세요.');
      return;
    }

    setSubmitting(true);

    // BFF does not have seller endpoints yet — show placeholder message
    setTimeout(() => {
      setSubmitting(false);
      alert('셀러 입점 신청 기능을 준비 중입니다. 빠른 시일 내에 오픈 예정입니다.');
    }, 500);
  };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">셀러 입점 신청</h1>
      <p className="text-sm text-gray-500 mb-8">
        Closet에 브랜드를 입점하고 상품을 판매해보세요.
      </p>

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Brand Info */}
        <div>
          <h2 className="text-lg font-bold mb-4">브랜드 정보</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">브랜드명 *</label>
              <input
                type="text"
                value={form.brandName}
                onChange={(e) => updateField('brandName', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="브랜드명을 입력하세요"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">사업자등록번호 *</label>
              <input
                type="text"
                value={form.businessNumber}
                onChange={(e) => updateField('businessNumber', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="000-00-00000"
              />
            </div>
          </div>
        </div>

        {/* Contact Info */}
        <div>
          <h2 className="text-lg font-bold mb-4">담당자 정보</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">담당자명 *</label>
              <input
                type="text"
                value={form.contactName}
                onChange={(e) => updateField('contactName', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="담당자 이름"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">이메일 *</label>
              <input
                type="email"
                value={form.contactEmail}
                onChange={(e) => updateField('contactEmail', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="email@example.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">연락처</label>
              <input
                type="tel"
                value={form.contactPhone}
                onChange={(e) => updateField('contactPhone', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="01012345678"
              />
            </div>
          </div>
        </div>

        {/* Bank Info */}
        <div>
          <h2 className="text-lg font-bold mb-4">정산 계좌 정보</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">은행명</label>
              <input
                type="text"
                value={form.bankName}
                onChange={(e) => updateField('bankName', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="은행명"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">계좌번호</label>
              <input
                type="text"
                value={form.bankAccount}
                onChange={(e) => updateField('bankAccount', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="계좌번호"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">예금주</label>
              <input
                type="text"
                value={form.bankHolder}
                onChange={(e) => updateField('bankHolder', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="예금주명"
              />
            </div>
          </div>
        </div>

        <button
          type="submit"
          disabled={submitting}
          className={`w-full py-4 rounded-lg font-medium text-lg transition-colors ${
            submitting
              ? 'bg-gray-300 text-white cursor-not-allowed'
              : 'bg-black text-white hover:bg-gray-800'
          }`}
        >
          {submitting ? '처리 중...' : '입점 신청하기'}
        </button>
      </form>
    </div>
  );
}
