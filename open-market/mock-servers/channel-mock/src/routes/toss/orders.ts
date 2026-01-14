import { Router } from 'express';
import { orderService } from '../../services/order.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';
import { OrderItem, ShippingAddress } from '../../types';

const router = Router();

router.get('/v1/orders', requireAuth('TOSS_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const page = Number(req.query.page || 1);
  const size = 20;
  const offset = (page - 1) * size;

  const result = await orderService.list('TOSS_STORE', size, offset);

  const orders = result.orders.map((order) => {
    const items = JSON.parse(order.itemsJson) as OrderItem[];
    const shipping = JSON.parse(order.shippingJson) as ShippingAddress;

    return {
      orderId: order.orderId,
      orderDateTime: order.createdAt,
      status: 'PAYMENT_COMPLETED',
      totalAmount: order.totalAmount,
      buyer: {
        name: order.buyerName,
        phoneNumber: order.buyerPhone,
      },
      orderItems: items,
      shippingInfo: shipping,
    };
  });

  return res.json({
    result: 'SUCCESS',
    data: {
      orders,
      page: {
        number: page,
        size,
        totalElements: result.totalCount,
      },
    },
  });
});

export default router;
