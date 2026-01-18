"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const auth_1 = __importDefault(require("./st11/auth"));
const products_1 = __importDefault(require("./st11/products"));
const orders_1 = __importDefault(require("./st11/orders"));
const auth_2 = __importDefault(require("./naver/auth"));
const products_2 = __importDefault(require("./naver/products"));
const orders_2 = __importDefault(require("./naver/orders"));
const auth_3 = __importDefault(require("./kakao/auth"));
const products_3 = __importDefault(require("./kakao/products"));
const orders_3 = __importDefault(require("./kakao/orders"));
const auth_4 = __importDefault(require("./toss/auth"));
const products_4 = __importDefault(require("./toss/products"));
const orders_4 = __importDefault(require("./toss/orders"));
const products_5 = __importDefault(require("./coupang/products"));
const orders_5 = __importDefault(require("./coupang/orders"));
const webhook_receiver_1 = __importDefault(require("./webhook-receiver"));
const webhook_management_1 = require("./webhook-management");
const auth_5 = require("../middleware/auth");
const openapi_1 = __importDefault(require("./openapi"));
const router = (0, express_1.Router)();
router.get('/', (_req, res) => {
    res.json({
        service: 'Channel Mock Server',
        version: '1.0.0',
        channels: ['ST11', 'NAVER_STORE', 'KAKAO_STORE', 'TOSS_STORE', 'COUPANG'],
    });
});
router.get('/health', (_req, res) => {
    res.json({
        status: 'ok',
        timestamp: new Date().toISOString(),
        service: 'Channel Mock Server',
        version: '1.0.0',
    });
});
router.use('/', openapi_1.default);
router.use('/api/st11', auth_1.default);
router.use('/api/st11/products', products_1.default);
router.use('/api/st11/orders', orders_1.default);
router.use('/api/st11/webhooks', (0, webhook_management_1.createWebhookRouter)('ST11', { requireAuth: (0, auth_5.requireAuth)('ST11') }));
router.use('/api/naver', auth_2.default);
router.use('/api/naver/products', products_2.default);
router.use('/api/naver/orders', orders_2.default);
router.use('/api/naver/webhooks', (0, webhook_management_1.createWebhookRouter)('NAVER_STORE', { requireAuth: (0, auth_5.requireAuth)('NAVER_STORE') }));
router.use('/api/kakao', auth_3.default);
router.use('/api/kakao/products', products_3.default);
router.use('/api/kakao/orders', orders_3.default);
router.use('/api/kakao/webhooks', (0, webhook_management_1.createWebhookRouter)('KAKAO_STORE', { requireAuth: (0, auth_5.requireAuth)('KAKAO_STORE') }));
router.use('/api/toss', auth_4.default);
router.use('/api/toss', products_4.default);
router.use('/api/toss', orders_4.default);
router.use('/api/toss/webhooks', (0, webhook_management_1.createWebhookRouter)('TOSS_STORE', { requireAuth: (0, auth_5.requireAuth)('TOSS_STORE') }));
router.use('/api/coupang', products_5.default);
router.use('/api/coupang', orders_5.default);
router.use('/api/coupang/webhooks', (0, webhook_management_1.createWebhookRouter)('COUPANG', { requireAuth: (0, auth_5.requireAuth)('COUPANG', { allowMissing: true }) }));
router.use('/webhooks', webhook_receiver_1.default);
exports.default = router;
//# sourceMappingURL=index.js.map