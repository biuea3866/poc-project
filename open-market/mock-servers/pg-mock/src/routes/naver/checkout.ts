import { Router, Request, Response } from 'express';
import { paymentService } from '../../services/payment.service';
import { reserveToPaymentMap } from './payments';

const router = Router();

// GET /mock/naver/checkout - Mock 결제창
router.get('/checkout', async (req: Request, res: Response) => {
  try {
    const { reserveId } = req.query;

    if (!reserveId) {
      return res.status(400).send('Reserve ID is required');
    }

    const paymentKey = `naver_${reserveId}`;
    const payment = await paymentService.getPaymentByKey(paymentKey);

    if (!payment) {
      return res.status(404).send('Payment not found');
    }

    res.render('naver-checkout', {
      reserveId,
      orderId: payment.orderId,
      orderName: payment.orderName,
      amount: payment.amount,
      returnUrl: payment.successUrl,
    });
  } catch (error: any) {
    console.error('Naver checkout error:', error);
    res.status(500).send('Internal server error');
  }
});

export default router;
