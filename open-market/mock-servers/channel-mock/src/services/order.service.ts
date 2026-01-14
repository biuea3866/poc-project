import { db } from '../db';
import { Channel, OrderItem, OrderRecord, ShippingAddress } from '../types';

interface OrderListResult {
  orders: OrderRecord[];
  totalCount: number;
}

const seedOrderForChannel = (channel: Channel): { status: string; totalAmount: number; items: OrderItem[]; shipping: ShippingAddress } => {
  const items: OrderItem[] = [
    {
      productId: `${channel}-ITEM-00001`,
      productName: '테스트 상품',
      optionName: '기본',
      quantity: 1,
      price: 10000,
    },
  ];

  const shipping: ShippingAddress = {
    recipientName: '홍길동',
    phone: '010-1234-5678',
    address: '서울시 강남구',
    zipCode: '06000',
  };

  return {
    status: 'PAY_COMPLETE',
    totalAmount: 10000,
    items,
    shipping,
  };
};

export const orderService = {
  async list(channel: Channel, limit = 20, offset = 0): Promise<OrderListResult> {
    const existing = await db.all<OrderRecord>(
      'SELECT id, channel, order_id as orderId, status, total_amount as totalAmount, buyer_name as buyerName, buyer_phone as buyerPhone, items_json as itemsJson, shipping_json as shippingJson, created_at as createdAt FROM orders WHERE channel = ? ORDER BY id DESC LIMIT ? OFFSET ?',
      [channel, limit, offset]
    );

    if (existing.length === 0) {
      const seed = seedOrderForChannel(channel);
      const createdAt = new Date().toISOString();
      const orderId = `${channel}-ORD-00001`;

      await db.run(
        'INSERT INTO orders (channel, order_id, status, total_amount, buyer_name, buyer_phone, items_json, shipping_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
        [
          channel,
          orderId,
          seed.status,
          seed.totalAmount,
          '홍길동',
          '010-1234-5678',
          JSON.stringify(seed.items),
          JSON.stringify(seed.shipping),
          createdAt,
        ]
      );

      const seeded = await db.all<OrderRecord>(
        'SELECT id, channel, order_id as orderId, status, total_amount as totalAmount, buyer_name as buyerName, buyer_phone as buyerPhone, items_json as itemsJson, shipping_json as shippingJson, created_at as createdAt FROM orders WHERE channel = ? ORDER BY id DESC LIMIT ? OFFSET ?',
        [channel, limit, offset]
      );

      const totalCount = await orderService.count(channel);
      return { orders: seeded, totalCount };
    }

    const totalCount = await orderService.count(channel);
    return { orders: existing, totalCount };
  },

  async count(channel: Channel): Promise<number> {
    const row = await db.get<{ count: number }>('SELECT COUNT(*) as count FROM orders WHERE channel = ?', [
      channel,
    ]);
    return row?.count ?? 0;
  },

  async updateStatus(channel: Channel, orderId: string, status: string): Promise<boolean> {
    const result = await db.run('UPDATE orders SET status = ? WHERE channel = ? AND order_id = ?', [
      status,
      channel,
      orderId,
    ]);
    return result.changes > 0;
  },
};
