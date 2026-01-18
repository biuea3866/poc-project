"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const payment_service_1 = require("../../services/payment.service");
const router = (0, express_1.Router)();
// GET /mock/naver/checkout - Mock 결제창
router.get('/checkout', async (req, res) => {
    try {
        const { reserveId } = req.query;
        if (!reserveId) {
            return res.status(400).send('Reserve ID is required');
        }
        const paymentKey = `naver_${reserveId}`;
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).send('Payment not found');
        }
        res.render('naver-checkout', {
            reserveId,
            orderId: payment.orderId,
            orderName: payment.orderName,
            amount: payment.amount,
            returnUrl: payment.successUrl,
        });
    }
    catch (error) {
        console.error('Naver checkout error:', error);
        res.status(500).send('Internal server error');
    }
});
exports.default = router;
//# sourceMappingURL=checkout.js.map