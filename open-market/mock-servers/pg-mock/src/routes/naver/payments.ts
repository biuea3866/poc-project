import { Router, Request, Response } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { paymentService } from '../../services/payment.service';
import { PgProvider } from '../../types';
import { getScenario, applyScenario } from '../../scenarios';

const router = Router();

// Store reserveId to paymentId mapping
const reserveToPaymentMap: Map<string, string> = new Map();

// Naver Auth middleware
function naverAuthMiddleware(req: Request, res: Response, next: Function) {
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
router.post('/v1/payments/reserve', naverAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

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

    const reserveId = `NAVER-RES-${uuidv4()}`;
    const paymentKey = `naver_${reserveId}`;

    await paymentService.createPayment({
      provider: PgProvider.NAVER_PAY,
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
  } catch (error: any) {
    console.error('Naver payment reserve error:', error);
    res.status(500).json({
      code: 'InternalServerError',
      message: error.message || '서버 오류가 발생했습니다.',
    });
  }
});

// POST /api/naver/v1/payments/:paymentId/apply - 결제 승인
router.post('/v1/payments/:paymentId/apply', naverAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

    if (scenario.body) {
      return res.status(scenario.status || 400).json(scenario.body);
    }

    const { paymentId } = req.params;
    const paymentKey = `naver_${paymentId}`;

    const payment = await paymentService.getPaymentByKey(paymentKey);
    if (!payment) {
      return res.status(404).json({
        code: 'NotFound',
        message: '결제 건을 찾을 수 없습니다.',
      });
    }

    const approvedPayment = await paymentService.approvePayment(paymentKey);

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
          cardCorpName: approvedPayment.cardCompany!,
          cardNo: approvedPayment.cardNumber!,
          admissionYmdt: approvedPayment.approvedAt!.replace(/[-:TZ]/g, '').slice(0, 14),
        },
        detail: {
          productName: approvedPayment.orderName,
          productCount: 1,
        },
      },
    });
  } catch (error: any) {
    console.error('Naver payment apply error:', error);
    res.status(500).json({
      code: 'InternalServerError',
      message: error.message || '서버 오류가 발생했습니다.',
    });
  }
});

// POST /api/naver/v1/payments/:paymentId/cancel - 결제 취소
router.post('/v1/payments/:paymentId/cancel', naverAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const { paymentId } = req.params;
    const { cancelReason, cancelAmount } = req.body;

    const paymentKey = `naver_${paymentId}`;
    const result = await paymentService.cancelPayment(paymentKey, cancelReason, cancelAmount);

    res.json({
      code: 'Success',
      message: '성공',
      body: {
        paymentId,
        cancelId: `NAVER-CANCEL-${uuidv4()}`,
        cancelAmount: cancelAmount || result.payment.amount,
        cancelledYmdt: result.cancel.canceledAt.replace(/[-:TZ]/g, '').slice(0, 14),
      },
    });
  } catch (error: any) {
    console.error('Naver payment cancel error:', error);
    res.status(400).json({
      code: 'CancelFailed',
      message: error.message || '결제 취소에 실패했습니다.',
    });
  }
});

export { router, reserveToPaymentMap };
