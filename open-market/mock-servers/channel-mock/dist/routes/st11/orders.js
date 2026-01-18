"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const order_service_1 = require("../../services/order.service");
const scenario_1 = require("../../middleware/scenario");
const auth_1 = require("../../middleware/auth");
const router = (0, express_1.Router)();
router.get('/', (0, auth_1.requireAuth)('ST11'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const page = Number(req.query.page || 1);
    const size = Number(req.query.size || 20);
    const offset = (page - 1) * size;
    const result = await order_service_1.orderService.list('ST11', size, offset);
    const totalPages = Math.ceil(result.totalCount / size) || 1;
    const orders = result.orders.map((order) => {
        const items = JSON.parse(order.itemsJson);
        const shipping = JSON.parse(order.shippingJson);
        return {
            orderNo: order.orderId,
            orderDate: order.createdAt,
            buyerName: order.buyerName,
            buyerPhone: order.buyerPhone,
            totalAmount: order.totalAmount,
            status: order.status,
            items,
            shippingAddress: shipping,
        };
    });
    return res.json({
        code: '200',
        data: {
            orders,
            pagination: {
                page,
                size,
                totalCount: result.totalCount,
                totalPages,
            },
        },
    });
});
router.post('/:orderNo/ship', (0, auth_1.requireAuth)('ST11'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const updated = await order_service_1.orderService.updateStatus('ST11', req.params.orderNo, 'SHIPPING');
    if (!updated) {
        return res.status(404).json({ code: '404', message: '주문을 찾을 수 없습니다' });
    }
    return res.json({
        code: '200',
        message: '발송 처리 완료',
        data: {
            orderNo: req.params.orderNo,
            status: 'SHIPPING',
        },
    });
});
exports.default = router;
//# sourceMappingURL=orders.js.map