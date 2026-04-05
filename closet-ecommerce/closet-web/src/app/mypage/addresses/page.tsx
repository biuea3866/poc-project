'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAddresses, addAddress, deleteAddress } from '@/lib/api/member';
import { useAuthStore } from '@/stores/authStore';
import type { ShippingAddress } from '@/types/member';

export default function AddressesPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [addresses, setAddresses] = useState<ShippingAddress[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: '',
    phone: '',
    zipCode: '',
    address: '',
    detailAddress: '',
    isDefault: false,
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }
    fetchAddresses();
  }, [isAuthenticated, router]);

  const fetchAddresses = () => {
    setLoading(true);
    getAddresses()
      .then((res) => setAddresses(res.data.data || []))
      .catch(() => setAddresses([]))
      .finally(() => setLoading(false));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.phone || !form.address) return;

    setSaving(true);
    try {
      await addAddress(form);
      setShowForm(false);
      setForm({ name: '', phone: '', zipCode: '', address: '', detailAddress: '', isDefault: false });
      fetchAddresses();
    } catch {
      alert('배송지 추가에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (addressId: number) => {
    if (!confirm('이 배송지를 삭제하시겠습니까?')) return;
    try {
      await deleteAddress(addressId);
      fetchAddresses();
    } catch {
      alert('배송지 삭제에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-2xl font-bold text-gray-900">배송지 관리</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-black text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-800 transition-colors"
        >
          {showForm ? '취소' : '배송지 추가'}
        </button>
      </div>

      {/* Add Address Form */}
      {showForm && (
        <form onSubmit={handleSubmit} className="border border-gray-200 rounded-lg p-6 mb-6 space-y-4">
          <h2 className="text-lg font-bold mb-2">새 배송지</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">수령인</label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                placeholder="수령인 이름"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">연락처</label>
              <input
                type="tel"
                value={form.phone}
                onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                placeholder="010-0000-0000"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">우편번호</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={form.zipCode}
                onChange={(e) => setForm((p) => ({ ...p, zipCode: e.target.value }))}
                className="w-32 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                placeholder="우편번호"
              />
              <button type="button" className="px-4 py-2 bg-gray-100 text-sm rounded-lg hover:bg-gray-200 transition-colors">
                주소 검색
              </button>
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">주소</label>
            <input
              type="text"
              value={form.address}
              onChange={(e) => setForm((p) => ({ ...p, address: e.target.value }))}
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
              placeholder="기본 주소"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">상세 주소</label>
            <input
              type="text"
              value={form.detailAddress}
              onChange={(e) => setForm((p) => ({ ...p, detailAddress: e.target.value }))}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
              placeholder="상세 주소 입력"
            />
          </div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={form.isDefault}
              onChange={(e) => setForm((p) => ({ ...p, isDefault: e.target.checked }))}
              className="w-4 h-4 accent-black rounded"
            />
            <span className="text-sm text-gray-700">기본 배송지로 설정</span>
          </label>
          <button
            type="submit"
            disabled={saving}
            className="w-full bg-black text-white py-3 rounded-lg font-medium hover:bg-gray-800 disabled:bg-gray-400 transition-colors"
          >
            {saving ? '저장 중...' : '배송지 저장'}
          </button>
        </form>
      )}

      {/* Address List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2].map((i) => (
            <div key={i} className="border border-gray-200 rounded-lg p-6 animate-pulse">
              <div className="h-5 bg-gray-200 rounded w-1/4 mb-2" />
              <div className="h-4 bg-gray-200 rounded w-1/3 mb-1" />
              <div className="h-4 bg-gray-200 rounded w-2/3" />
            </div>
          ))}
        </div>
      ) : addresses.length === 0 && !showForm ? (
        <div className="text-center py-16 text-gray-500">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-16 w-16 mx-auto text-gray-300 mb-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={1}
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
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
                    <span className="text-sm font-bold text-gray-900">{addr.name}</span>
                    {addr.isDefault && (
                      <span className="text-xs bg-black text-white px-2 py-0.5 rounded-full">
                        기본
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600">{addr.phone}</p>
                  <p className="text-sm text-gray-600 mt-1">
                    ({addr.zipCode}) {addr.address} {addr.detailAddress}
                  </p>
                </div>
                <button
                  onClick={() => handleDelete(addr.id)}
                  className="text-sm text-gray-400 hover:text-red-500 transition-colors"
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
