"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.reserveToPaymentMap = exports.router = void 0;
const express_1 = require("express");
const uuid_1 = require("uuid");
const payment_service_1 = require("../../services/payment.service");
const types_1 = require("../../types");
const scenarios_1 = require("../../scenarios");
const router = (0, express_1.Router)();
exports.router = router;
// Store reserveId to paymentId mapping
const reserveToPaymentMap = new Map();
exports.reserveToPaymentMap = reserveToPaymentMap;
// Naver Auth middleware
function naverAuthMiddleware(req, res, next) {
    const clientId = req.headers['x-naver-client-id'];
    const clientSecret = req.headers['x-naver-client-secret'];
    if (!clientId || !clientSecret) {
        return res.status(401).json({
            code: 'Unauthorized',
            message: '인증이 필요합니다.',
        });
    }
    next();
}
// POST /api/naver/v1/payments/reserve - 결제 준비
router.post('/v1/payments/reserve', naverAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { merchantPayKey, productName, productCount, totalPayAmount, returnUrl } = req.body;
        if (!merchantPayKey || !productName || !totalPayAmount || !returnUrl) {
            return res.status(400).json({
                code: 'InvalidRequest',
                message: '필수 파라미터가 누락되었습니다.',
            });
        }
        const reserveId = `NAVER-RES-${(0, uuid_1.v4)()}`;
        const paymentKey = `naver_${reserveId}`;
        await payment_service_1.paymentService.createPayment({
            provider: types_1.PgProvider.NAVER_PAY,
            paymentKey,
            orderId: merchantPayKey,
            orderName: productName,
            amount: totalPayAmount,
            successUrl: returnUrl,
            failUrl: returnUrl,
        });
        res.json({
            code: 'Success',
            message: '성공',
            body: {
                reserveId,
                paymentUrl: `http://localhost:8081/mock/naver/checkout?reserveId=${reserveId}`,
            },
        });
    }
    catch (error) {
        console.error('Naver payment reserve error:', error);
        res.status(500).json({
            code: 'InternalServerError',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// POST /api/naver/v1/payments/:paymentId/apply - 결제 승인
router.post('/v1/payments/:paymentId/apply', naverAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { paymentId } = req.params;
        const paymentKey = `naver_${paymentId}`;
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).json({
                code: 'NotFound',
                message: '결제 건을 찾을 수 없습니다.',
            });
        }
        const approvedPayment = await payment_service_1.paymentService.approvePayment(paymentKey);
        res.json({
            code: 'Success',
            message: '성공',
            body: {
                paymentId,
                merchantPayKey: approvedPayment.orderId,
                merchantUserKey: 'USER-001',
                paymentResult: {
                    paymentMethod: 'CARD',
                    totalPayAmount: approvedPayment.amount,
                    cardCorpName: approvedPayment.cardCompany,
                    cardNo: approvedPayment.cardNumber,
                    admissionYmdt: approvedPayment.approvedAt.replace(/[-:TZ]/g, '').slice(0, 14),
                },
                detail: {
                    productName: approvedPayment.orderName,
                    productCount: 1,
                },
            },
        });
    }
    catch (error) {
        console.error('Naver payment apply error:', error);
        res.status(500).json({
            code: 'InternalServerError',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// POST /api/naver/v1/payments/:paymentId/cancel - 결제 취소
router.post('/v1/payments/:paymentId/cancel', naverAuthMiddleware, async (req, res) => {
    try {
        const { paymentId } = req.params;
        const { cancelReason, cancelAmount } = req.body;
        const paymentKey = `naver_${paymentId}`;
        const result = await payment_service_1.paymentService.cancelPayment(paymentKey, cancelReason, cancelAmount);
        res.json({
            code: 'Success',
            message: '성공',
            body: {
                paymentId,
                cancelId: `NAVER-CANCEL-${(0, uuid_1.v4)()}`,
                cancelAmount: cancelAmount || result.payment.amount,
                cancelledYmdt: result.cancel.canceledAt.replace(/[-:TZ]/g, '').slice(0, 14),
            },
        });
    }
    catch (error) {
        console.error('Naver payment cancel error:', error);
        res.status(400).json({
            code: 'CancelFailed',
            message: error.message || '결제 취소에 실패했습니다.',
        });
    }
});
//# sourceMappingURL=payments.js.map