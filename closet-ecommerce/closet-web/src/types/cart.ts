export interface Cart {
  id: number;
  items: CartItem[];
}

export interface CartItem {
  id: number;
  productId: number;
  productOptionId: number;
  quantity: number;
  unitPrice: number;
}

export interface AddCartItemRequest {
  productId: number;
  productOptionId: number;
  quantity: number;
  unitPrice: number;
}
