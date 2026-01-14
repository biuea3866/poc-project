import { Router } from 'express';
import { orderService } from '../../services/order.service';
import { applyScenario } from '../../middleware/scenario';
import { requireAuth } from '../../middleware/auth';
import { OrderItem, ShippingAddress } from '../../types';

const router = Router();

router.get('/', requireAuth('ST11'), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const page = Number(req.query.page || 1);
  const size = Number(req.query.size || 20);
  const offset = (page - 1) * size;

  const result = await orderService.list('ST11', size, offset);
  const totalPages = Math.ceil(result.totalCount / size) || 1;

  const orders = result.orders.map((order) => {
    const items = JSON.parse(order.itemsJson) as OrderItem[];
    const shipping = JSON.parse(order.shippingJson) as ShippingAddress;

    return {
      orderNo: order.orderId,
      orderDate: order.createdAt,
      buyerName: order.buyerName,
      buyerPhone: order.buyerPhone,
      totalAmount: order.totalAmount,
      status: order.status,
      items,
      shippingAddress: shipping,
    };
  });

  return res.json({
    code: '200',
    data: {
      orders,
      pagination: {
        page,
        size,
        totalCount: result.totalCount,
        totalPages,
      },
    },
  });
});

router.post('/:orderNo/ship', requireAuth('ST11'), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const updated = await orderService.updateStatus('ST11', req.params.orderNo, 'SHIPPING');

  if (!updated) {
    return res.status(404).json({ code: '404', message: '주문을 찾을 수 없습니다' });
  }

  return res.json({
    code: '200',
    message: '발송 처리 완료',
    data: {
      orderNo: req.params.orderNo,
      status: 'SHIPPING',
    },
  });
});

export default router;
