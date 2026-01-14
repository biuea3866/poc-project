import { Router } from 'express';
import { orderService } from '../../services/order.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';
import { OrderItem, ShippingAddress } from '../../types';

const router = Router();

router.get('/', requireAuth('KAKAO_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const page = Number(req.query.page || 1);
  const limit = Number(req.query.limit || 20);
  const offset = (page - 1) * limit;

  const result = await orderService.list('KAKAO_STORE', limit, offset);

  const orders = result.orders.map((order) => {
    const items = JSON.parse(order.itemsJson) as OrderItem[];
    const shipping = JSON.parse(order.shippingJson) as ShippingAddress;

    return {
      orderId: order.orderId,
      orderDate: order.createdAt,
      status: 'PAID',
      buyer: {
        name: order.buyerName,
        phone: order.buyerPhone,
      },
      items,
      totalAmount: order.totalAmount,
      delivery: {
        name: shipping.recipientName,
        phone: shipping.phone,
        address: shipping.address,
      },
    };
  });

  return res.json({
    code: 0,
    data: {
      orders,
      hasNext: result.totalCount > page * limit,
    },
  });
});

export default router;
