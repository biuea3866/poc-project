import { Router } from 'express';

// Toss Payments
import tossPayments from './toss/payments';
import tossCheckout from './toss/checkout';

// Kakao Pay
import { router as kakaoPayment } from './kakao/payment';
import kakaoCheckout from './kakao/checkout';

// Naver Pay
import { router as naverPayments } from './naver/payments';
import naverCheckout from './naver/checkout';

// Danal
import danalPayment from './danal/payment';
import danalCheckout from './danal/checkout';

const router = Router();

// ==================== API Routes ====================

// Toss Payments
router.use('/api/toss', tossPayments);

// Kakao Pay
router.use('/api/kakao', kakaoPayment);

// Naver Pay
router.use('/api/naver', naverPayments);

// Danal
router.use('/api/danal', danalPayment);

// ==================== Mock Checkout Pages ====================

// Toss
router.use('/mock/toss', tossCheckout);

// Kakao
router.use('/mock/kakao', kakaoCheckout);

// Naver
router.use('/mock/naver', naverCheckout);

// Danal
router.use('/mock/danal', danalCheckout);

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

export default router;
