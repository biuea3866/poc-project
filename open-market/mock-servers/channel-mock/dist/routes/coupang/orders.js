"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const order_service_1 = require("../../services/order.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.get('/v2/orders', (0, auth_1.requireAuth)('COUPANG', { allowMissing: true }), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const result = await order_service_1.orderService.list('COUPANG', 1, 0);
    const order = result.orders[0];
    if (!order) {
        return res.json({ code: 'SUCCESS', data: { orderId: 0, orderItems: [], nextToken: '' } });
    }
    const items = JSON.parse(order.itemsJson);
    const shipping = JSON.parse(order.shippingJson);
    return res.json({
        code: 'SUCCESS',
        data: {
            orderId: Number(order.orderId.replace(/\D/g, '')) || 1234567890,
            orderItems: items.map((item, index) => ({
                shipmentBoxId: 9000000000 + index,
                orderId: Number(order.orderId.replace(/\D/g, '')) || 1234567890,
                vendorItemId: item.productId,
                vendorItemName: item.productName,
                quantity: item.quantity,
                shippingPrice: 0,
                orderPrice: item.price,
                receiverName: shipping.recipientName,
                receiverPhone: shipping.phone,
                postCode: shipping.zipCode,
                addr1: shipping.address,
                addr2: '',
                statusName: 'ACCEPT',
                orderedAt: order.createdAt,
            })),
            nextToken: '',
        },
    });
});
router.put('/v2/orders/:shipmentBoxId/invoice', (0, auth_1.requireAuth)('COUPANG', { allowMissing: true }), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    return res.json({
        code: 'SUCCESS',
        message: '송장 등록 완료',
    });
});
exports.default = router;
//# sourceMappingURL=orders.js.map