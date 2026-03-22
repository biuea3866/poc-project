export interface Payment {
  id: number;
  orderId: number;
  orderNumber: string;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  pgTransactionId?: string;
  paidAt?: string;
  cancelledAt?: string;
}

export type PaymentMethod = 'CARD' | 'BANK_TRANSFER' | 'VIRTUAL_ACCOUNT' | 'KAKAO_PAY' | 'NAVER_PAY' | 'TOSS_PAY';

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED' | 'FAILED' | 'REFUNDED' | 'PARTIAL_REFUNDED';

export interface PaymentRequest {
  orderId: number;
  method: PaymentMethod;
  amount: number;
}

export interface PaymentConfirmRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}
