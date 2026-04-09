'use client';

import { Suspense, useState, useRef, useCallback } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Image from 'next/image';
import { createReview, getPresignedUrl } from '@/lib/api/review';
import { useAuthStore } from '@/stores/authStore';
import type { SizeFit } from '@/types/review';

const MAX_IMAGES = 5;
const MIN_CONTENT_LENGTH = 20;
const MAX_CONTENT_LENGTH = 1000;

const POINT_INFO = {
  text: 100,
  photo: 300,
  sizeInfo: 50,
};

const FIT_OPTIONS: { value: SizeFit; label: string }[] = [
  { value: 'SMALL', label: '작아요' },
  { value: 'TRUE_TO_SIZE', label: '딱 맞아요' },
  { value: 'LARGE', label: '커요' },
];

function ReviewWriteContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

  const productId = Number(searchParams.get('productId') || 0);
  const orderId = Number(searchParams.get('orderId') || 0);
  const orderItemId = Number(searchParams.get('orderItemId') || 0);
  const productName = searchParams.get('productName') || '';

  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [content, setContent] = useState('');
  const [images, setImages] = useState<{ file: File; preview: string }[]>([]);
  const [height, setHeight] = useState('');
  const [weight, setWeight] = useState('');
  const [fitType, setFitType] = useState<SizeFit | ''>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const estimatedPoints =
    POINT_INFO.text +
    (images.length > 0 ? POINT_INFO.photo : 0) +
    (height || weight || fitType ? POINT_INFO.sizeInfo : 0);

  const handleFileSelect = useCallback(
    (files: FileList | null) => {
      if (!files) return;
      const newFiles = Array.from(files).slice(0, MAX_IMAGES - images.length);
      const validFiles = newFiles.filter((f) => f.type.startsWith('image/'));
      const previews = validFiles.map((file) => ({
        file,
        preview: URL.createObjectURL(file),
      }));
      setImages((prev) => [...prev, ...previews].slice(0, MAX_IMAGES));
    },
    [images.length],
  );

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  };

  const removeImage = (index: number) => {
    setImages((prev) => {
      const removed = prev[index];
      URL.revokeObjectURL(removed.preview);
      return prev.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }

    if (rating === 0) {
      setError('별점을 선택해주세요.');
      return;
    }
    if (content.length < MIN_CONTENT_LENGTH) {
      setError(`리뷰는 최소 ${MIN_CONTENT_LENGTH}자 이상 입력해주세요.`);
      return;
    }

    setSubmitting(true);
    try {
      // Upload images
      const imageUrls: string[] = [];
      for (const img of images) {
        const presignedRes = await getPresignedUrl({
          fileName: img.file.name,
          contentType: img.file.type,
        });
        const presigned = presignedRes.data.data;
        if (presigned) {
          await fetch(presigned.uploadUrl, {
            method: 'PUT',
            body: img.file,
            headers: { 'Content-Type': img.file.type },
          });
          imageUrls.push(presigned.imageUrl);
        }
      }

      await createReview({
        productId,
        orderId,
        orderItemId,
        rating,
        content,
        imageUrls,
        height: height ? Number(height) : undefined,
        weight: weight ? Number(weight) : undefined,
        fitType: fitType || undefined,
      });

      alert(`리뷰가 등록되었습니다! ${estimatedPoints}P가 적립됩니다.`);
      router.push(`/products/${productId}`);
    } catch {
      setError('리뷰 등록에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">리뷰 작성</h1>
      {productName && <p className="text-sm text-gray-500 mb-8">{productName}</p>}

      {error && <div className="bg-red-50 text-red-600 text-sm p-4 rounded-lg mb-6">{error}</div>}

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Star Rating */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">별점</h2>
          <div className="flex gap-1">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                onClick={() => setRating(star)}
                onMouseEnter={() => setHoverRating(star)}
                onMouseLeave={() => setHoverRating(0)}
                className="p-1"
              >
                <svg
                  className={`h-8 w-8 transition-colors ${
                    star <= (hoverRating || rating) ? 'text-yellow-400' : 'text-gray-300'
                  }`}
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                </svg>
              </button>
            ))}
            {rating > 0 && (
              <span className="ml-2 text-sm text-gray-500 self-center">{rating}점</span>
            )}
          </div>
        </section>

        {/* Content */}
        <section>
          <div className="flex justify-between items-center mb-3">
            <h2 className="text-sm font-semibold text-gray-700">리뷰 내용</h2>
            <span
              className={`text-xs ${
                content.length < MIN_CONTENT_LENGTH
                  ? 'text-red-500'
                  : content.length > MAX_CONTENT_LENGTH
                    ? 'text-red-500'
                    : 'text-gray-400'
              }`}
            >
              {content.length}/{MAX_CONTENT_LENGTH}
            </span>
          </div>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value.slice(0, MAX_CONTENT_LENGTH))}
            rows={6}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black resize-none text-sm"
            placeholder={`상품에 대한 솔직한 리뷰를 남겨주세요. (최소 ${MIN_CONTENT_LENGTH}자)`}
          />
          {content.length > 0 && content.length < MIN_CONTENT_LENGTH && (
            <p className="text-xs text-red-500 mt-1">
              {MIN_CONTENT_LENGTH - content.length}자 더 입력해주세요.
            </p>
          )}
        </section>

        {/* Photo Upload */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">
            사진 ({images.length}/{MAX_IMAGES})
          </h2>

          {/* Drop zone */}
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
              dragOver ? 'border-black bg-gray-50' : 'border-gray-300 hover:border-gray-400'
            } ${images.length >= MAX_IMAGES ? 'opacity-50 pointer-events-none' : 'cursor-pointer'}`}
            onClick={() => fileInputRef.current?.click()}
          >
            <svg
              className="h-8 w-8 mx-auto text-gray-400 mb-2"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={1.5}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <p className="text-sm text-gray-500">사진을 드래그하거나 클릭하여 업로드</p>
            <p className="text-xs text-gray-400 mt-1">최대 {MAX_IMAGES}장, JPG/PNG</p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              className="hidden"
              onChange={(e) => handleFileSelect(e.target.files)}
            />
          </div>

          {/* Preview thumbnails */}
          {images.length > 0 && (
            <div className="flex gap-2 mt-3 overflow-x-auto pb-2">
              {images.map((img, idx) => (
                <div key={idx} className="relative flex-shrink-0">
                  <Image
                    src={img.preview}
                    alt={`리뷰 이미지 ${idx + 1}`}
                    width={80}
                    height={80}
                    className="w-20 h-20 object-cover rounded-lg border border-gray-200"
                    unoptimized
                  />
                  <button
                    type="button"
                    onClick={() => removeImage(idx)}
                    className="absolute -top-2 -right-2 w-5 h-5 bg-black text-white rounded-full flex items-center justify-center text-xs"
                  >
                    <svg
                      className="h-3 w-3"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2}
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Size Info */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">사이즈 정보 (선택)</h2>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs text-gray-500 mb-1">키 (cm)</label>
              <input
                type="number"
                value={height}
                onChange={(e) => setHeight(e.target.value)}
                placeholder="예: 175"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-black"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500 mb-1">몸무게 (kg)</label>
              <input
                type="number"
                value={weight}
                onChange={(e) => setWeight(e.target.value)}
                placeholder="예: 70"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-black"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-2">핏 평가</label>
            <div className="flex gap-2">
              {FIT_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setFitType(fitType === opt.value ? '' : opt.value)}
                  className={`flex-1 py-2 text-sm rounded-lg border transition-colors ${
                    fitType === opt.value
                      ? 'bg-black text-white border-black'
                      : 'border-gray-300 text-gray-700 hover:border-black'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Points Info */}
        <section className="bg-gray-50 rounded-lg p-4">
          <h2 className="text-sm font-semibold text-gray-700 mb-3">적립 포인트 안내</h2>
          <div className="space-y-1.5 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">텍스트 리뷰</span>
              <span className="font-medium">+{POINT_INFO.text}P</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">포토 리뷰</span>
              <span className={`font-medium ${images.length > 0 ? '' : 'text-gray-400'}`}>
                +{POINT_INFO.photo}P
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">사이즈 정보 입력</span>
              <span className={`font-medium ${height || weight || fitType ? '' : 'text-gray-400'}`}>
                +{POINT_INFO.sizeInfo}P
              </span>
            </div>
            <div className="border-t border-gray-200 pt-2 mt-2 flex justify-between font-bold">
              <span>예상 적립 포인트</span>
              <span className="text-black">{estimatedPoints}P</span>
            </div>
          </div>
        </section>

        {/* Submit */}
        <button
          type="submit"
          disabled={submitting || rating === 0 || content.length < MIN_CONTENT_LENGTH}
          className="w-full py-4 bg-black text-white rounded-lg text-base font-medium hover:bg-gray-800 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? '등록 중...' : '리뷰 등록'}
        </button>
      </form>
    </div>
  );
}

export default function ReviewWritePage() {
  return (
    <Suspense
      fallback={
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-8 animate-pulse" />
          <div className="space-y-6">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-24 bg-gray-200 rounded-lg animate-pulse" />
            ))}
          </div>
        </div>
      }
    >
      <ReviewWriteContent />
    </Suspense>
  );
}
