export type ReturnReason =
  | 'CHANGE_OF_MIND'
  | 'WRONG_SIZE'
  | 'DEFECTIVE'
  | 'WRONG_ITEM'
  | 'DAMAGED_IN_DELIVERY'
  | 'OTHER';

export type ClaimStatus =
  | 'REQUESTED'
  | 'APPROVED'
  | 'COLLECTING'
  | 'COLLECTED'
  | 'COMPLETED'
  | 'REJECTED';

export interface ReturnRequest {
  orderId: number;
  orderItemId: number;
  reason: ReturnReason;
  reasonDetail?: string;
}

export interface ReturnResponse {
  id: number;
  orderId: number;
  orderItemId: number;
  reason: ReturnReason;
  reasonDetail: string | null;
  status: ClaimStatus;
  shippingFee: number;
  refundAmount: number;
  createdAt: string;
}

export interface ExchangeRequest {
  orderId: number;
  orderItemId: number;
  reason: ReturnReason;
  reasonDetail?: string;
  newProductOptionId: number;
}

export interface ExchangeResponse {
  id: number;
  orderId: number;
  orderItemId: number;
  reason: ReturnReason;
  reasonDetail: string | null;
  newProductOptionId: number;
  newOptionName: string;
  status: ClaimStatus;
  shippingFee: number;
  createdAt: string;
}

export interface RestockNotificationRequest {
  productId: number;
  productOptionId: number;
}

export interface RestockNotification {
  id: number;
  productId: number;
  productOptionId: number;
  productName: string;
  optionName: string;
  isNotified: boolean;
  createdAt: string;
}
