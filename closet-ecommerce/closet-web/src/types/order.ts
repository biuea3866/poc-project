export interface Order {
  id: number;
  orderNumber: string;
  memberId: number;
  sellerId: number;
  totalAmount: number;
  discountAmount: number;
  shippingFee: number;
  paymentAmount: number;
  status: string;
  receiverName: string;
  receiverPhone: string;
  orderedAt: string;
  items: OrderItem[];
}

export interface OrderItem {
  id: number;
  productId: number;
  productOptionId: number;
  productName: string;
  optionName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
}

export interface CreateOrderRequest {
  memberId: number;
  sellerId: number;
  items: CreateOrderItemRequest[];
  receiverName: string;
  receiverPhone: string;
  zipCode: string;
  address: string;
  detailAddress: string;
  shippingFee: number;
}

export interface CreateOrderItemRequest {
  productId: number;
  productOptionId: number;
  quantity: number;
  unitPrice: number;
}
