"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const payment_service_1 = require("../../services/payment.service");
const router = (0, express_1.Router)();
// GET /mock/toss/checkout - Mock 결제창
router.get('/checkout', async (req, res) => {
    try {
        const { paymentKey } = req.query;
        if (!paymentKey) {
            return res.status(400).send('Payment key is required');
        }
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).send('Payment not found');
        }
        res.render('toss-checkout', {
            paymentKey: payment.paymentKey,
            orderId: payment.orderId,
            orderName: payment.orderName,
            amount: payment.amount,
            successUrl: payment.successUrl,
            failUrl: payment.failUrl,
        });
    }
    catch (error) {
        console.error('Toss checkout error:', error);
        res.status(500).send('Internal server error');
    }
});
// GET /mock/toss/receipt/:paymentKey - 영수증 페이지
router.get('/receipt/:paymentKey', async (req, res) => {
    try {
        const { paymentKey } = req.params;
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).send('Payment not found');
        }
        res.render('receipt', {
            provider: '토스페이먼츠',
            payment,
        });
    }
    catch (error) {
        console.error('Toss receipt error:', error);
        res.status(500).send('Internal server error');
    }
});
exports.default = router;
//# sourceMappingURL=checkout.js.map