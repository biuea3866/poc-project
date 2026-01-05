import { Router, Request, Response } from 'express';
import { paymentService } from '../../services/payment.service';

const router = Router();

// GET /mock/toss/checkout - Mock 결제창
router.get('/checkout', async (req: Request, res: Response) => {
  try {
    const { paymentKey } = req.query;

    if (!paymentKey) {
      return res.status(400).send('Payment key is required');
    }

    const payment = await paymentService.getPaymentByKey(paymentKey as string);

    if (!payment) {
      return res.status(404).send('Payment not found');
    }

    res.render('toss-checkout', {
      paymentKey: payment.paymentKey,
      orderId: payment.orderId,
      orderName: payment.orderName,
      amount: payment.amount,
      successUrl: payment.successUrl,
      failUrl: payment.failUrl,
    });
  } catch (error: any) {
    console.error('Toss checkout error:', error);
    res.status(500).send('Internal server error');
  }
});

// GET /mock/toss/receipt/:paymentKey - 영수증 페이지
router.get('/receipt/:paymentKey', async (req: Request, res: Response) => {
  try {
    const { paymentKey } = req.params;

    const payment = await paymentService.getPaymentByKey(paymentKey);

    if (!payment) {
      return res.status(404).send('Payment not found');
    }

    res.render('receipt', {
      provider: '토스페이먼츠',
      payment,
    });
  } catch (error: any) {
    console.error('Toss receipt error:', error);
    res.status(500).send('Internal server error');
  }
});

export default router;
