"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const uuid_1 = require("uuid");
const payment_service_1 = require("../../services/payment.service");
const types_1 = require("../../types");
const scenarios_1 = require("../../scenarios");
const router = (0, express_1.Router)();
// Basic Auth middleware
function basicAuthMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Basic ')) {
        return res.status(401).json({
            code: 'UNAUTHORIZED',
            message: '인증이 필요합니다.',
        });
    }
    // In production, validate the credentials
    // For mock, we accept any valid Basic auth format
    next();
}
// POST /api/toss/v1/payments - 결제 준비
router.post('/v1/payments', basicAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { amount, orderId, orderName, successUrl, failUrl, customerEmail, customerName, customerMobilePhone } = req.body;
        // Validation
        if (!amount || !orderId || !orderName || !successUrl || !failUrl) {
            return res.status(400).json({
                code: 'INVALID_REQUEST',
                message: '필수 파라미터가 누락되었습니다.',
            });
        }
        const paymentKey = `toss_payment_key_${(0, uuid_1.v4)()}`;
        // Save to DB
        await payment_service_1.paymentService.createPayment({
            provider: types_1.PgProvider.TOSS_PAYMENTS,
            paymentKey,
            orderId,
            orderName,
            amount,
            successUrl,
            failUrl,
            customerEmail,
            customerName,
            customerPhone: customerMobilePhone,
        });
        const response = {
            paymentKey,
            orderId,
            status: 'READY',
            requestedAt: new Date().toISOString(),
            checkout: {
                url: `http://localhost:8081/mock/toss/checkout?paymentKey=${paymentKey}`,
            },
        };
        res.json(response);
    }
    catch (error) {
        console.error('Toss payment prepare error:', error);
        res.status(500).json({
            code: 'INTERNAL_SERVER_ERROR',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// POST /api/toss/v1/payments/confirm - 결제 승인
router.post('/v1/payments/confirm', basicAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { paymentKey, orderId, amount } = req.body;
        if (!paymentKey || !orderId || !amount) {
            return res.status(400).json({
                code: 'INVALID_REQUEST',
                message: '필수 파라미터가 누락되었습니다.',
            });
        }
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).json({
                code: 'NOT_FOUND',
                message: '결제 건을 찾을 수 없습니다.',
            });
        }
        if (payment.orderId !== orderId) {
            return res.status(400).json({
                code: 'INVALID_REQUEST',
                message: '주문번호가 일치하지 않습니다.',
            });
        }
        if (payment.amount !== amount) {
            return res.status(400).json({
                code: 'INVALID_REQUEST',
                message: '결제 금액이 일치하지 않습니다.',
            });
        }
        const approvedPayment = await payment_service_1.paymentService.approvePayment(paymentKey);
        const response = {
            paymentKey: approvedPayment.paymentKey,
            orderId: approvedPayment.orderId,
            status: 'DONE',
            totalAmount: approvedPayment.amount,
            balanceAmount: approvedPayment.balanceAmount,
            method: approvedPayment.method,
            approvedAt: approvedPayment.approvedAt,
            card: {
                company: approvedPayment.cardCompany,
                number: approvedPayment.cardNumber,
                installmentPlanMonths: 0,
                isInterestFree: false,
                approveNo: approvedPayment.approveNo,
            },
            receipt: {
                url: `http://localhost:8081/mock/toss/receipt/${paymentKey}`,
            },
        };
        res.json(response);
    }
    catch (error) {
        console.error('Toss payment confirm error:', error);
        res.status(500).json({
            code: 'INTERNAL_SERVER_ERROR',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// GET /api/toss/v1/payments/:paymentKey - 결제 조회
router.get('/v1/payments/:paymentKey', basicAuthMiddleware, async (req, res) => {
    try {
        const { paymentKey } = req.params;
        const payment = await payment_service_1.paymentService.getPaymentByKey(paymentKey);
        if (!payment) {
            return res.status(404).json({
                code: 'NOT_FOUND',
                message: '결제 건을 찾을 수 없습니다.',
            });
        }
        const response = {
            paymentKey: payment.paymentKey,
            orderId: payment.orderId,
            status: payment.status,
            totalAmount: payment.amount,
            balanceAmount: payment.balanceAmount,
            method: payment.method,
        };
        if (payment.approvedAt) {
            response.approvedAt = payment.approvedAt;
            response.card = {
                company: payment.cardCompany,
                number: payment.cardNumber,
                installmentPlanMonths: 0,
                approveNo: payment.approveNo,
            };
        }
        res.json(response);
    }
    catch (error) {
        console.error('Toss payment get error:', error);
        res.status(500).json({
            code: 'INTERNAL_SERVER_ERROR',
            message: error.message || '서버 오류가 발생했습니다.',
        });
    }
});
// POST /api/toss/v1/payments/:paymentKey/cancel - 결제 취소
router.post('/v1/payments/:paymentKey/cancel', basicAuthMiddleware, async (req, res) => {
    try {
        const scenario = (0, scenarios_1.getScenario)(req.headers['x-mock-scenario']);
        await (0, scenarios_1.applyScenario)(scenario);
        if (scenario.body) {
            return res.status(scenario.status || 400).json(scenario.body);
        }
        const { paymentKey } = req.params;
        const { cancelReason, cancelAmount } = req.body;
        if (!cancelReason) {
            return res.status(400).json({
                code: 'INVALID_REQUEST',
                message: '취소 사유가 필요합니다.',
            });
        }
        const result = await payment_service_1.paymentService.cancelPayment(paymentKey, cancelReason, cancelAmount);
        const cancels = await payment_service_1.paymentService.getCancelsByPaymentId(result.payment.id);
        const response = {
            paymentKey: result.payment.paymentKey,
            orderId: result.payment.orderId,
            status: result.payment.status,
            totalAmount: result.payment.amount,
            balanceAmount: result.payment.balanceAmount,
            cancels: cancels.map((cancel) => ({
                transactionKey: cancel.transactionKey,
                cancelReason: cancel.cancelReason,
                canceledAt: cancel.canceledAt,
                cancelAmount: cancel.cancelAmount,
            })),
        };
        res.json(response);
    }
    catch (error) {
        console.error('Toss payment cancel error:', error);
        res.status(400).json({
            code: 'CANCEL_FAILED',
            message: error.message || '결제 취소에 실패했습니다.',
        });
    }
});
exports.default = router;
//# sourceMappingURL=payments.js.map