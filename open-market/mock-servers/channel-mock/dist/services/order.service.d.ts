import { Channel, OrderRecord } from '../types';
interface OrderListResult {
    orders: OrderRecord[];
    totalCount: number;
}
export declare const orderService: {
    list(channel: Channel, limit?: number, offset?: number): Promise<OrderListResult>;
    count(channel: Channel): Promise<number>;
    updateStatus(channel: Channel, orderId: string, status: string): Promise<boolean>;
};
export {};
//# sourceMappingURL=order.service.d.ts.map