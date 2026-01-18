"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const uuid_1 = require("uuid");
const payment_service_1 = require("../../services/payment.service");
const types_1 = require("../../types");
const scenarios_1 = require("../../scenarios");
const router = (0, express_1.Router)();
// Danal Auth middleware
function danalAuthMiddleware(req, res, next) {
    const cpid = req.headers['cpid'];
    const cpPassword = req.headers['cppassword'];
    if (!cpid || !cpPassword) {
        return res.status(401).json({
            result: '9999',
            message: '인증이 필요합니다.',
        });
    }
    next();
}
// POST /api/danal/v1/payment/ready - 결제 준비
router.post('/v1/payment/ready', danalAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { amount, orderNo, itemName, returnUrl } = req.body;
        if (!amount || !orderNo || !itemName || !returnUrl) {
            return res.status(400).json({
                result: '9001',
                message: '필수 파라미터가 누락되었습니다.',
            });
        }
        const tid = `DANAL-TID-${(0, uuid_1.v4)()}`;
        const paymentKey = `danal_${tid}`;
        await payment_service_1.paymentService.createPayment({
            provider: types_1.PgProvider.DANAL,
            paymentKey,
            orderId: orderNo,
            orderName: itemName,
            amount: parseInt(amount, 10),
            successUrl: returnUrl,
            failUrl: returnUrl,
        });
        res.json({
            result: '0000',
            message: '성공',
            data: {
                tid,
                paymentUrl: `http://localhost:8081/mock/danal/checkout?tid=${tid}`,
            },
        });
    }
    catch (error) {
        console.error('Danal payment ready error:', error);
        res.status(500).json({
            result: '9999',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// POST /api/danal/v1/payment/confirm - 결제 승인
router.post('/v1/payment/confirm', danalAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { tid, orderNo, amount } = req.body;
        if (!tid || !orderNo || !amount) {
            return res.status(400).json({
                result: '9001',
                message: '필수 파라미터가 누락되었습니다.',
            });
        }
        const paymentKey = `danal_${tid}`;
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).json({
                result: '9002',
                message: '결제 건을 찾을 수 없습니다.',
            });
        }
        const approvedPayment = await payment_service_1.paymentService.approvePayment(paymentKey);
        res.json({
            result: '0000',
            message: '성공',
            data: {
                tid,
                orderNo,
                amount,
                payMethod: 'CARD',
                cardName: approvedPayment.cardCompany,
                cardNo: approvedPayment.cardNumber.replace(/-/g, ''),
                installMonth: '00',
                authNo: approvedPayment.approveNo,
                transDate: approvedPayment.approvedAt.replace(/[-:TZ]/g, '').slice(0, 14),
            },
        });
    }
    catch (error) {
        console.error('Danal payment confirm error:', error);
        res.status(500).json({
            result: '9999',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// POST /api/danal/v1/payment/cancel - 결제 취소
router.post('/v1/payment/cancel', danalAuthMiddleware, async (req, res) => {
    try {
        const { tid, cancelReason, cancelAmount } = req.body;
        const paymentKey = `danal_${tid}`;
        const result = await payment_service_1.paymentService.cancelPayment(paymentKey, cancelReason, cancelAmount ? parseInt(cancelAmount, 10) : undefined);
        res.json({
            result: '0000',
            message: '취소 완료',
            data: {
                tid,
                cancelTid: `DANAL-CANCEL-${(0, uuid_1.v4)()}`,
                cancelAmount: cancelAmount || result.payment.amount.toString(),
                cancelDate: result.cancel.canceledAt.replace(/[-:TZ]/g, '').slice(0, 14),
            },
        });
    }
    catch (error) {
        console.error('Danal payment cancel error:', error);
        res.status(400).json({
            result: '9003',
            message: error.message || '결제 취소에 실패했습니다.',
        });
    }
});
exports.default = router;
//# sourceMappingURL=payment.js.map