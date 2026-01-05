import { Router, Request, Response } from 'express';
import { paymentService } from '../../services/payment.service';

const router = Router();

// GET /mock/danal/checkout - Mock 결제창
router.get('/checkout', async (req: Request, res: Response) => {
  try {
    const { tid } = req.query;

    if (!tid) {
      return res.status(400).send('TID is required');
    }

    const paymentKey = `danal_${tid}`;
    const payment = await paymentService.getPaymentByKey(paymentKey);

    if (!payment) {
      return res.status(404).send('Payment not found');
    }

    res.render('danal-checkout', {
      tid,
      orderId: payment.orderId,
      orderName: payment.orderName,
      amount: payment.amount,
      returnUrl: payment.successUrl,
    });
  } catch (error: any) {
    console.error('Danal checkout error:', error);
    res.status(500).send('Internal server error');
  }
});

export default router;
