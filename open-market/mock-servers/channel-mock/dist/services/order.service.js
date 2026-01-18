"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.orderService = void 0;
const db_1 = require("../db");
const seedOrderForChannel = (channel) => {
    const items = [
        {
            productId: `${channel}-ITEM-00001`,
            productName: '테스트 상품',
            optionName: '기본',
            quantity: 1,
            price: 10000,
        },
    ];
    const shipping = {
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
exports.orderService = {
    async list(channel, limit = 20, offset = 0) {
        const existing = await db_1.db.all('SELECT id, channel, order_id as orderId, status, total_amount as totalAmount, buyer_name as buyerName, buyer_phone as buyerPhone, items_json as itemsJson, shipping_json as shippingJson, created_at as createdAt FROM orders WHERE channel = ? ORDER BY id DESC LIMIT ? OFFSET ?', [channel, limit, offset]);
        if (existing.length === 0) {
            const seed = seedOrderForChannel(channel);
            const createdAt = new Date().toISOString();
            const orderId = `${channel}-ORD-00001`;
            await db_1.db.run('INSERT INTO orders (channel, order_id, status, total_amount, buyer_name, buyer_phone, items_json, shipping_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)', [
                channel,
                orderId,
                seed.status,
                seed.totalAmount,
                '홍길동',
                '010-1234-5678',
                JSON.stringify(seed.items),
                JSON.stringify(seed.shipping),
                createdAt,
            ]);
            const seeded = await db_1.db.all('SELECT id, channel, order_id as orderId, status, total_amount as totalAmount, buyer_name as buyerName, buyer_phone as buyerPhone, items_json as itemsJson, shipping_json as shippingJson, created_at as createdAt FROM orders WHERE channel = ? ORDER BY id DESC LIMIT ? OFFSET ?', [channel, limit, offset]);
            const totalCount = await exports.orderService.count(channel);
            return { orders: seeded, totalCount };
        }
        const totalCount = await exports.orderService.count(channel);
        return { orders: existing, totalCount };
    },
    async count(channel) {
        const row = await db_1.db.get('SELECT COUNT(*) as count FROM orders WHERE channel = ?', [
            channel,
        ]);
        return row?.count ?? 0;
    },
    async updateStatus(channel, orderId, status) {
        const result = await db_1.db.run('UPDATE orders SET status = ? WHERE channel = ? AND order_id = ?', [
            status,
            channel,
            orderId,
        ]);
        return result.changes > 0;
    },
};
//# sourceMappingURL=order.service.js.map