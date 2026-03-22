'use client';

import { useEffect, useState } from 'react';
import { getAddresses, addAddress, deleteAddress } from '@/lib/api/member';
import type { ShippingAddress } from '@/types/member';

interface AddressForm {
  name: string;
  phone: string;
  zipCode: string;
  address: string;
  detailAddress: string;
  isDefault: boolean;
}

const EMPTY_FORM: AddressForm = {
  name: '',
  phone: '',
  zipCode: '',
  address: '',
  detailAddress: '',
  isDefault: false,
};

export default function AddressesPage() {
  const [addresses, setAddresses] = useState<ShippingAddress[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<AddressForm>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

  const fetchAddresses = () => {
    setLoading(true);
    getAddresses()
      .then((res) => {
        setAddresses(res.data.data || []);
      })
      .catch(() => {
        setAddresses([]);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAddresses();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.phone || !form.zipCode || !form.address) {
      alert('필수 항목을 모두 입력해주세요.');
      return;
    }
    setSubmitting(true);
    try {
      await addAddress(form);
      setForm(EMPTY_FORM);
      setShowForm(false);
      fetchAddresses();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || '배송지 추가에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (addressId: number) => {
    if (!confirm('이 배송지를 삭제하시겠습니까?')) return;
    try {
      await deleteAddress(addressId);
      fetchAddresses();
    } catch (err: any) {
      alert(err.response?.data?.error?.message || '배송지 삭제에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-2xl font-bold text-gray-900">배송지 관리</h1>
        <button
          onClick={() => {
            setForm(EMPTY_FORM);
            setShowForm(!showForm);
          }}
          className="bg-black text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors"
        >
          {showForm ? '취소' : '배송지 추가'}
        </button>
      </div>

      {/* Add Address Form */}
      {showForm && (
        <form onSubmit={handleSubmit} className="mb-8 border border-gray-200 rounded-lg p-6">
          <h2 className="text-lg font-bold mb-4">새 배송지 추가</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">수령인 이름 *</label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="이름을 입력하세요"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">연락처 *</label>
              <input
                type="tel"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="01012345678"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">우편번호 *</label>
              <input
                type="text"
                value={form.zipCode}
                onChange={(e) => setForm({ ...form, zipCode: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="06035"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">주소 *</label>
              <input
                type="text"
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="서울특별시 강남구"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">상세주소</label>
              <input
                type="text"
                value={form.detailAddress}
                onChange={(e) => setForm({ ...form, detailAddress: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-black focus:border-black"
                placeholder="상세주소를 입력하세요"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isDefault"
                checked={form.isDefault}
                onChange={(e) => setForm({ ...form, isDefault: e.target.checked })}
                className="rounded"
              />
              <label htmlFor="isDefault" className="text-sm text-gray-700">기본 배송지로 설정</label>
            </div>
            <button
              type="submit"
              disabled={submitting}
              className={`w-full py-3 rounded-lg font-medium transition-colors ${
                submitting
                  ? 'bg-gray-300 text-white cursor-not-allowed'
                  : 'bg-black text-white hover:bg-gray-800'
              }`}
            >
              {submitting ? '등록 중...' : '배송지 등록'}
            </button>
          </div>
        </form>
      )}

      {/* Address List */}
      {loading ? (
        <div className="space-y-4">
          {Array.from({ length: 2 }).map((_, i) => (
            <div key={i} className="border border-gray-200 rounded-lg p-6 animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-1/3 mb-3" />
              <div className="h-3 bg-gray-200 rounded w-1/2 mb-2" />
              <div className="h-3 bg-gray-200 rounded w-2/3" />
            </div>
          ))}
        </div>
      ) : addresses.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <p className="text-lg">저장된 배송지가 없습니다.</p>
          <p className="text-sm mt-2">배송지를 추가하면 더 빠르게 주문할 수 있습니다.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {addresses.map((addr) => (
            <div key={addr.id} className="border border-gray-200 rounded-lg p-6">
              <div className="flex justify-between items-start">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-medium text-gray-900">{addr.name}</p>
                    {addr.isDefault && (
                      <span className="text-xs bg-black text-white px-1.5 py-0.5 rounded">기본</span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600">{addr.phone}</p>
                  <p className="text-sm text-gray-600 mt-1">
                    [{addr.zipCode}] {addr.address} {addr.detailAddress}
                  </p>
                </div>
                <button
                  onClick={() => handleDelete(addr.id)}
                  className="text-sm text-red-500 hover:text-red-700 transition-colors"
                >
                  삭제
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
