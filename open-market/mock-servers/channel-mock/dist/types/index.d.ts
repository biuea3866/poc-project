export type Channel = 'ST11' | 'NAVER_STORE' | 'KAKAO_STORE' | 'TOSS_STORE' | 'COUPANG';
export interface ProductRecord {
    id: number;
    channel: Channel;
    externalId: string;
    name: string;
    price: number;
    stock: number;
    rawData: string;
    createdAt: string;
}
export interface OrderItem {
    productId: string;
    productName: string;
    optionName?: string;
    quantity: number;
    price: number;
}
export interface ShippingAddress {
    recipientName: string;
    phone: string;
    address: string;
    zipCode: string;
}
export interface OrderRecord {
    id: number;
    channel: Channel;
    orderId: string;
    status: string;
    totalAmount: number;
    buyerName: string;
    buyerPhone: string;
    itemsJson: string;
    shippingJson: string;
    createdAt: string;
}
//# sourceMappingURL=index.d.ts.map