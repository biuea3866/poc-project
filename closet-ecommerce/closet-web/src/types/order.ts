export interface Order {
  id: number;
  orderNumber: string;
  memberId: number;
  items: OrderItem[];
  totalAmount: number;
  discountAmount: number;
  shippingFee: number;
  paymentAmount: number;
  status: OrderStatus;
  shippingAddress: string;
  shippingAddressDetail: string;
  recipientName: string;
  recipientPhone: string;
  zipCode: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productImage: string;
  optionName: string | null;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  PREPARING = 'PREPARING',
  SHIPPING = 'SHIPPING',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  REFUNDED = 'REFUNDED',
}

export interface CreateOrderRequest {
  items: CreateOrderItemRequest[];
  shippingAddressId: number;
  paymentMethod: string;
  couponId?: number;
}

export interface CreateOrderItemRequest {
  productId: number;
  optionId?: number;
  quantity: number;
}
