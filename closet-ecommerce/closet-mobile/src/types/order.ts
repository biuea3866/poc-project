export interface Order {
  id: number;
  orderNumber: string;
  memberId: number;
  items: OrderItem[];
  totalPrice: number;
  discountPrice: number;
  deliveryFee: number;
  finalPrice: number;
  status: OrderStatus;
  shippingAddress: ShippingAddress;
  paymentMethod?: string;
  orderedAt: string;
  paidAt?: string;
  deliveredAt?: string;
}

export type OrderStatus =
  | 'PENDING'
  | 'PAID'
  | 'PREPARING'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURN_REQUESTED'
  | 'RETURNED'
  | 'EXCHANGE_REQUESTED'
  | 'EXCHANGED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  brandName: string;
  thumbnailUrl: string;
  optionId: number;
  size: string;
  color: string;
  quantity: number;
  price: number;
  status: OrderStatus;
}

export interface ShippingAddress {
  recipient: string;
  phoneNumber: string;
  zipCode: string;
  address: string;
  addressDetail: string;
}

export interface CreateOrderRequest {
  cartItemIds: number[];
  shippingAddress: ShippingAddress;
  paymentMethod: string;
  usedPoint?: number;
  couponId?: number;
}
