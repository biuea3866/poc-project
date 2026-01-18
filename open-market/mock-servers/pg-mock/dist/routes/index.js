"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
// Toss Payments
const payments_1 = __importDefault(require("./toss/payments"));
const checkout_1 = __importDefault(require("./toss/checkout"));
// Kakao Pay
const payment_1 = require("./kakao/payment");
const checkout_2 = __importDefault(require("./kakao/checkout"));
// Naver Pay
const payments_2 = require("./naver/payments");
const checkout_3 = __importDefault(require("./naver/checkout"));
// Danal
const payment_2 = __importDefault(require("./danal/payment"));
const checkout_4 = __importDefault(require("./danal/checkout"));
const router = (0, express_1.Router)();
// ==================== API Routes ====================
// Toss Payments
router.use('/api/toss', payments_1.default);
// Kakao Pay
router.use('/api/kakao', payment_1.router);
// Naver Pay
router.use('/api/naver', payments_2.router);
// Danal
router.use('/api/danal', payment_2.default);
// ==================== Mock Checkout Pages ====================
// Toss
router.use('/mock/toss', checkout_1.default);
// Kakao
router.use('/mock/kakao', checkout_2.default);
// Naver
router.use('/mock/naver', checkout_3.default);
// Danal
router.use('/mock/danal', checkout_4.default);
// ==================== Health Check ====================
router.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        timestamp: new Date().toISOString(),
        service: 'PG Mock Server',
        version: '1.0.0',
    });
});
// ==================== API Documentation ====================
router.get('/', (req, res) => {
    res.json({
        service: 'PG Mock Server',
        version: '1.0.0',
        providers: [
            {
                name: 'Toss Payments',
                apis: [
                    'POST /api/toss/v1/payments',
                    'POST /api/toss/v1/payments/confirm',
                    'GET /api/toss/v1/payments/:paymentKey',
                    'POST /api/toss/v1/payments/:paymentKey/cancel',
                ],
                checkout: 'GET /mock/toss/checkout?paymentKey={paymentKey}',
            },
            {
                name: 'Kakao Pay',
                apis: [
                    'POST /api/kakao/v1/payment/ready',
                    'POST /api/kakao/v1/payment/approve',
                    'POST /api/kakao/v1/payment/cancel',
                ],
                checkout: 'GET /mock/kakao/checkout?tid={tid}',
            },
            {
                name: 'Naver Pay',
                apis: [
                    'POST /api/naver/v1/payments/reserve',
                    'POST /api/naver/v1/payments/:paymentId/apply',
                    'POST /api/naver/v1/payments/:paymentId/cancel',
                ],
                checkout: 'GET /mock/naver/checkout?reserveId={reserveId}',
            },
            {
                name: 'Danal',
                apis: [
                    'POST /api/danal/v1/payment/ready',
                    'POST /api/danal/v1/payment/confirm',
                    'POST /api/danal/v1/payment/cancel',
                ],
                checkout: 'GET /mock/danal/checkout?tid={tid}',
            },
        ],
        scenarios: [
            'success (default)',
            'error-card-declined',
            'error-insufficient-balance',
            'error-invalid-card',
            'error-expired-card',
            'timeout',
        ],
        usage: 'Set X-Mock-Scenario header to trigger different scenarios',
    });
});
exports.default = router;
//# sourceMappingURL=index.js.map