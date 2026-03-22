export interface CartItem {
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
  salePrice?: number;
  additionalPrice: number;
}

export interface Cart {
  items: CartItem[];
  totalPrice: number;
  totalDiscountPrice: number;
  deliveryFee: number;
  finalPrice: number;
}

export interface AddCartItemRequest {
  productId: number;
  optionId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}
