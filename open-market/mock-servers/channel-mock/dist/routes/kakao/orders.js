"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const order_service_1 = require("../../services/order.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.get('/', (0, auth_1.requireAuth)('KAKAO_STORE'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const page = Number(req.query.page || 1);
    const limit = Number(req.query.limit || 20);
    const offset = (page - 1) * limit;
    const result = await order_service_1.orderService.list('KAKAO_STORE', limit, offset);
    const orders = result.orders.map((order) => {
        const items = JSON.parse(order.itemsJson);
        const shipping = JSON.parse(order.shippingJson);
        return {
            orderId: order.orderId,
            orderDate: order.createdAt,
            status: 'PAID',
            buyer: {
                name: order.buyerName,
                phone: order.buyerPhone,
            },
            items,
            totalAmount: order.totalAmount,
            delivery: {
                name: shipping.recipientName,
                phone: shipping.phone,
                address: shipping.address,
            },
        };
    });
    return res.json({
        code: 0,
        data: {
            orders,
            hasNext: result.totalCount > page * limit,
        },
    });
});
exports.default = router;
//# sourceMappingURL=orders.js.map