"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const payment_service_1 = require("../../services/payment.service");
const router = (0, express_1.Router)();
// GET /mock/danal/checkout - Mock 결제창
router.get('/checkout', async (req, res) => {
    try {
        const { tid } = req.query;
        if (!tid) {
            return res.status(400).send('TID is required');
        }
        const paymentKey = `danal_${tid}`;
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).send('Payment not found');
        }
        res.render('danal-checkout', {
            tid,
            orderId: payment.orderId,
            orderName: payment.orderName,
            amount: payment.amount,
            returnUrl: payment.successUrl,
        });
    }
    catch (error) {
        console.error('Danal checkout error:', error);
        res.status(500).send('Internal server error');
    }
});
exports.default = router;
//# sourceMappingURL=checkout.js.map