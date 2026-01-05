import { Router, Request, Response } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { paymentService } from '../../services/payment.service';
import { PgProvider } from '../../types';
import { getScenario, applyScenario } from '../../scenarios';

const router = Router();

// Store TID to PG_TOKEN mapping (in-memory for mock)
const tidToPgToken: Map<string, string> = new Map();

// KakaoAK Auth middleware
function kakaoAuthMiddleware(req: Request, res: Response, next: Function) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('KakaoAK ')) {
    return res.status(401).json({
      code: -401,
      msg: '인증이 필요합니다.',
    });
  }

  next();
}

// POST /api/kakao/v1/payment/ready - 결제 준비
router.post('/v1/payment/ready', kakaoAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

    if (scenario.body) {
      return res.status(scenario.status || 400).json(scenario.body);
    }

    const {
      cid,
      partner_order_id,
      partner_user_id,
      item_name,
      quantity,
      total_amount,
      tax_free_amount,
      approval_url,
      cancel_url,
      fail_url,
    } = req.body;

    // Validation
    if (!cid || !partner_order_id || !partner_user_id || !item_name || !total_amount) {
      return res.status(400).json({
        code: -400,
        msg: '필수 파라미터가 누락되었습니다.',
      });
    }

    const tid = `T${Date.now()}${Math.floor(Math.random() * 1000)}`;
    const paymentKey = `kakao_${tid}`;

    // Save to DB
    await paymentService.createPayment({
      provider: PgProvider.KAKAO_PAY,
      paymentKey,
      orderId: partner_order_id,
      orderName: item_name,
      amount: total_amount,
      successUrl: approval_url,
      failUrl: fail_url,
    });

    const response = {
      tid,
      next_redirect_app_url: 'kakaotalk://...',
      next_redirect_mobile_url: 'https://mockpay.kakao.com/...',
      next_redirect_pc_url: `http://localhost:8081/mock/kakao/checkout?tid=${tid}`,
      android_app_scheme: 'kakaotalk://...',
      ios_app_scheme: 'kakaotalk://...',
      created_at: new Date().toISOString(),
    };

    res.json(response);
  } catch (error: any) {
    console.error('Kakao payment ready error:', error);
    res.status(500).json({
      code: -500,
      msg: error.message || '서버 오류가 발생했습니다.',
    });
  }
});

// POST /api/kakao/v1/payment/approve - 결제 승인
router.post('/v1/payment/approve', kakaoAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

    if (scenario.body) {
      return res.status(scenario.status || 400).json(scenario.body);
    }

    const { cid, tid, partner_order_id, partner_user_id, pg_token } = req.body;

    if (!cid || !tid || !partner_order_id || !partner_user_id || !pg_token) {
      return res.status(400).json({
        code: -400,
        msg: '필수 파라미터가 누락되었습니다.',
      });
    }

    // Validate pg_token (skip validation in mock environment for testing)
    // In production mock, you would validate against tidToPgToken
    // For now, we accept any pg_token for easy testing

    const paymentKey = `kakao_${tid}`;
    const payment = await paymentService.getPaymentByKey(paymentKey);

    if (!payment) {
      return res.status(404).json({
        code: -404,
        msg: '결제 건을 찾을 수 없습니다.',
      });
    }

    const approvedPayment = await paymentService.approvePayment(paymentKey);

    const response = {
      aid: `A${Date.now()}${Math.floor(Math.random() * 1000)}`,
      tid,
      cid,
      partner_order_id,
      partner_user_id,
      payment_method_type: 'CARD',
      item_name: approvedPayment.orderName,
      quantity: 1,
      amount: {
        total: approvedPayment.amount,
        tax_free: 0,
        vat: Math.floor(approvedPayment.amount / 11),
        point: 0,
        discount: 0,
      },
      card_info: {
        purchase_corp: approvedPayment.cardCompany!,
        purchase_corp_code: 'SHINHAN',
        issuer_corp: approvedPayment.cardCompany!,
        issuer_corp_code: 'SHINHAN',
        bin: '432112',
        card_type: '신용',
        install_month: '00',
        approved_id: approvedPayment.approveNo!,
        card_mid: approvedPayment.cardNumber!.slice(-4),
      },
      created_at: payment.createdAt,
      approved_at: approvedPayment.approvedAt,
    };

    res.json(response);
  } catch (error: any) {
    console.error('Kakao payment approve error:', error);
    res.status(500).json({
      code: -500,
      msg: error.message || '서버 오류가 발생했습니다.',
    });
  }
});

// POST /api/kakao/v1/payment/cancel - 결제 취소
router.post('/v1/payment/cancel', kakaoAuthMiddleware, async (req: Request, res: Response) => {
  try {
    const scenario = getScenario(req.headers['x-mock-scenario'] as string);
    await applyScenario(scenario);

    if (scenario.body) {
      return res.status(scenario.status || 400).json(scenario.body);
    }

    const { cid, tid, cancel_amount, cancel_tax_free_amount } = req.body;

    if (!cid || !tid || !cancel_amount) {
      return res.status(400).json({
        code: -400,
        msg: '필수 파라미터가 누락되었습니다.',
      });
    }

    const paymentKey = `kakao_${tid}`;
    const result = await paymentService.cancelPayment(paymentKey, '고객 요청', cancel_amount);

    const response = {
      aid: `A${Date.now()}${Math.floor(Math.random() * 1000)}`,
      tid,
      status: result.payment.status === 'CANCELED' ? 'CANCEL_PAYMENT' : 'PARTIAL_CANCEL_PAYMENT',
      approved_cancel_amount: {
        total: cancel_amount,
        tax_free: cancel_tax_free_amount || 0,
        vat: Math.floor(cancel_amount / 11),
      },
      canceled_at: result.cancel.canceledAt,
    };

    res.json(response);
  } catch (error: any) {
    console.error('Kakao payment cancel error:', error);
    res.status(400).json({
      code: -450,
      msg: error.message || '결제 취소에 실패했습니다.',
    });
  }
});

export { router, tidToPgToken };
