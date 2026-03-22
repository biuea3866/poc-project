/**
 * Format number as Korean Won currency
 */
export const formatPrice = (price: number): string => {
  return `${price.toLocaleString('ko-KR')}원`;
};

/**
 * Format date string to Korean locale
 */
export const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
};

/**
 * Format date string with time
 */
export const formatDateTime = (dateStr: string): string => {
  const date = new Date(dateStr);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * Format phone number (010-1234-5678)
 */
export const formatPhoneNumber = (phone: string): string => {
  const cleaned = phone.replace(/\D/g, '');
  if (cleaned.length === 11) {
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-${cleaned.slice(7)}`;
  }
  return phone;
};

/**
 * Format discount rate
 */
export const formatDiscountRate = (rate: number): string => {
  return `${Math.round(rate)}%`;
};

/**
 * Translate order status to Korean
 */
export const formatOrderStatus = (status: string): string => {
  const statusMap: Record<string, string> = {
    PENDING: '주문 대기',
    PAID: '결제 완료',
    PREPARING: '상품 준비중',
    SHIPPING: '배송중',
    DELIVERED: '배송 완료',
    CANCELLED: '주문 취소',
    RETURN_REQUESTED: '반품 요청',
    RETURNED: '반품 완료',
    EXCHANGE_REQUESTED: '교환 요청',
    EXCHANGED: '교환 완료',
  };
  return statusMap[status] || status;
};
