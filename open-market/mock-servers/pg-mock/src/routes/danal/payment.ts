import { Router, Request, Response } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { paymentService } from '../../services/payment.service';
import { PgProvider } from '../../types';
import { getScenario, applyScenario } from '../../scenarios';

const router = Router();

// Danal Auth middleware
function danalAuthMiddleware(req: Request, res: Response, next: Function) {
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
router.post('/v1/payment/ready', danalAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

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

    const tid = `DANAL-TID-${uuidv4()}`;
    const paymentKey = `danal_${tid}`;

    await paymentService.createPayment({
      provider: PgProvider.DANAL,
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
  } catch (error: any) {
    console.error('Danal payment ready error:', error);
    res.status(500).json({
      result: '9999',
      message: error.message || '서버 오류가 발생했습니다.',
    });
  }
});

// POST /api/danal/v1/payment/confirm - 결제 승인
router.post('/v1/payment/confirm', danalAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

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
    const payment = await paymentService.getPaymentByKey(paymentKey);

    if (!payment) {
      return res.status(404).json({
        result: '9002',
        message: '결제 건을 찾을 수 없습니다.',
      });
    }

    const approvedPayment = await paymentService.approvePayment(paymentKey);

    res.json({
      result: '0000',
      message: '성공',
      data: {
        tid,
        orderNo,
        amount,
        payMethod: 'CARD',
        cardName: approvedPayment.cardCompany!,
        cardNo: approvedPayment.cardNumber!.replace(/-/g, ''),
        installMonth: '00',
        authNo: approvedPayment.approveNo!,
        transDate: approvedPayment.approvedAt!.replace(/[-:TZ]/g, '').slice(0, 14),
      },
    });
  } catch (error: any) {
    console.error('Danal payment confirm error:', error);
    res.status(500).json({
      result: '9999',
      message: error.message || '서버 오류가 발생했습니다.',
    });
  }
});

// POST /api/danal/v1/payment/cancel - 결제 취소
router.post('/v1/payment/cancel', danalAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const { tid, cancelReason, cancelAmount } = req.body;

    const paymentKey = `danal_${tid}`;
    const result = await paymentService.cancelPayment(
      paymentKey,
      cancelReason,
      cancelAmount ? parseInt(cancelAmount, 10) : undefined
    );

    res.json({
      result: '0000',
      message: '취소 완료',
      data: {
        tid,
        cancelTid: `DANAL-CANCEL-${uuidv4()}`,
        cancelAmount: cancelAmount || result.payment.amount.toString(),
        cancelDate: result.cancel.canceledAt.replace(/[-:TZ]/g, '').slice(0, 14),
      },
    });
  } catch (error: any) {
    console.error('Danal payment cancel error:', error);
    res.status(400).json({
      result: '9003',
      message: error.message || '결제 취소에 실패했습니다.',
    });
  }
});

export default router;
