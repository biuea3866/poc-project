export interface Cart {
  id: number;
  memberId: number;
  items: CartItem[];
  totalPrice: number;
}

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productImage: string;
  optionId: number | null;
  optionName: string | null;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface AddCartItemRequest {
  productId: number;
  optionId?: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}
