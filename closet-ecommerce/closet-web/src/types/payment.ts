export interface Payment {
  id: number;
  orderId: number;
  paymentKey: string;
  method: PaymentMethod;
  amount: number;
  status: PaymentStatus;
  approvedAt: string | null;
  createdAt: string;
}

export type PaymentMethod = 'CARD' | 'BANK_TRANSFER' | 'VIRTUAL_ACCOUNT' | 'MOBILE';

export type PaymentStatus = 'PENDING' | 'APPROVED' | 'CANCELLED' | 'FAILED';

export interface ConfirmPaymentRequest {
  orderId: number;
  paymentKey: string;
  amount: number;
}
