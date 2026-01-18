"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const order_service_1 = require("../../services/order.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.get('/v1/orders', (0, auth_1.requireAuth)('TOSS_STORE'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const page = Number(req.query.page || 1);
    const size = 20;
    const offset = (page - 1) * size;
    const result = await order_service_1.orderService.list('TOSS_STORE', size, offset);
    const orders = result.orders.map((order) => {
        const items = JSON.parse(order.itemsJson);
        const shipping = JSON.parse(order.shippingJson);
        return {
            orderId: order.orderId,
            orderDateTime: order.createdAt,
            status: 'PAYMENT_COMPLETED',
            totalAmount: order.totalAmount,
            buyer: {
                name: order.buyerName,
                phoneNumber: order.buyerPhone,
            },
            orderItems: items,
            shippingInfo: shipping,
        };
    });
    return res.json({
        result: 'SUCCESS',
        data: {
            orders,
            page: {
                number: page,
                size,
                totalElements: result.totalCount,
            },
        },
    });
});
exports.default = router;
//# sourceMappingURL=orders.js.map