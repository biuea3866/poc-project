import { Router, Request, Response } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { paymentService } from '../../services/payment.service';
import { tidToPgToken } from './payment';

const router = Router();

// GET /mock/kakao/checkout - Mock 결제창
router.get('/checkout', async (req: Request, res: Response) => {
  try {
    const { tid } = req.query;

    if (!tid) {
      return res.status(400).send('TID is required');
    }

    const paymentKey = `kakao_${tid}`;
    const payment = await paymentService.getPaymentByKey(paymentKey);

    if (!payment) {
      return res.status(404).send('Payment not found');
    }

    res.render('kakao-checkout', {
      tid,
      orderId: payment.orderId,
      orderName: payment.orderName,
      amount: payment.amount,
      approvalUrl: payment.successUrl,
      cancelUrl: payment.failUrl,
    });
  } catch (error: any) {
    console.error('Kakao checkout error:', error);
    res.status(500).send('Internal server error');
  }
});

export default router;
