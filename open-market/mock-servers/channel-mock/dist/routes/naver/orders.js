"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const order_service_1 = require("../../services/order.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.post('/search', (0, auth_1.requireAuth)('NAVER_STORE'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const pageIndex = Number(req.body?.pageIndex || 1);
    const pageSize = Number(req.body?.pageSize || 20);
    const offset = (pageIndex - 1) * pageSize;
    const result = await order_service_1.orderService.list('NAVER_STORE', pageSize, offset);
    const contents = result.orders.map((order) => {
        const items = JSON.parse(order.itemsJson);
        const shipping = JSON.parse(order.shippingJson);
        return {
            orderId: order.orderId,
            orderDate: order.createdAt,
            orderStatusType: order.status,
            totalPaymentAmount: order.totalAmount,
            orderItems: items.map((item, index) => ({
                productOrderId: `${order.orderId}-${index + 1}`,
                productId: Number(item.productId.replace(/\D/g, '')) || 1234567890,
                productName: item.productName,
                quantity: item.quantity,
                unitPrice: item.price,
                shippingAddress: {
                    name: shipping.recipientName,
                    tel1: shipping.phone,
                    baseAddress: shipping.address,
                    zipCode: shipping.zipCode,
                },
            })),
        };
    });
    return res.json({
        timestamp: new Date().toISOString(),
        data: {
            count: result.totalCount,
            moreSequence: result.totalCount > pageIndex * pageSize ? 'more' : '',
            contents,
        },
    });
});
router.post('/:productOrderId/ship', (0, auth_1.requireAuth)('NAVER_STORE'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['delay-5000'])) {
        return;
    }
    const orderId = req.params.productOrderId.replace(/-\d+$/, '');
    await order_service_1.orderService.updateStatus('NAVER_STORE', orderId, 'SHIPPING');
    return res.json({
        timestamp: new Date().toISOString(),
        data: {
            success: true,
        },
    });
});
exports.default = router;
//# sourceMappingURL=orders.js.map