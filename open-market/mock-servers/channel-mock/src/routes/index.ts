import { Router } from 'express';
import st11Auth from './st11/auth';
import st11Products from './st11/products';
import st11Orders from './st11/orders';
import naverAuth from './naver/auth';
import naverProducts from './naver/products';
import naverOrders from './naver/orders';
import kakaoAuth from './kakao/auth';
import kakaoProducts from './kakao/products';
import kakaoOrders from './kakao/orders';
import tossAuth from './toss/auth';
import tossProducts from './toss/products';
import tossOrders from './toss/orders';
import coupangProducts from './coupang/products';
import coupangOrders from './coupang/orders';
import webhookReceiver from './webhook-receiver';
import { createWebhookRouter } from './webhook-management';
import { requireAuth } from '../middleware/auth';
import openApiRouter from './openapi';

const router = Router();

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

router.use('/', openApiRouter);

router.use('/api/st11', st11Auth);
router.use('/api/st11/products', st11Products);
router.use('/api/st11/orders', st11Orders);
router.use('/api/st11/webhooks', createWebhookRouter('ST11', { requireAuth: requireAuth('ST11') }));

router.use('/api/naver', naverAuth);
router.use('/api/naver/products', naverProducts);
router.use('/api/naver/orders', naverOrders);
router.use('/api/naver/webhooks', createWebhookRouter('NAVER_STORE', { requireAuth: requireAuth('NAVER_STORE') }));

router.use('/api/kakao', kakaoAuth);
router.use('/api/kakao/products', kakaoProducts);
router.use('/api/kakao/orders', kakaoOrders);
router.use('/api/kakao/webhooks', createWebhookRouter('KAKAO_STORE', { requireAuth: requireAuth('KAKAO_STORE') }));

router.use('/api/toss', tossAuth);
router.use('/api/toss', tossProducts);
router.use('/api/toss', tossOrders);
router.use('/api/toss/webhooks', createWebhookRouter('TOSS_STORE', { requireAuth: requireAuth('TOSS_STORE') }));

router.use('/api/coupang', coupangProducts);
router.use('/api/coupang', coupangOrders);
router.use('/api/coupang/webhooks', createWebhookRouter('COUPANG', { requireAuth: requireAuth('COUPANG', { allowMissing: true }) }));

router.use('/webhooks', webhookReceiver);

export default router;
