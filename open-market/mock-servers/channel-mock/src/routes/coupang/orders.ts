import { Router } from 'express';
import { orderService } from '../../services/order.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';
import { OrderItem, ShippingAddress } from '../../types';

const router = Router();

router.get('/v2/orders', requireAuth('COUPANG', { allowMissing: true }), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const result = await orderService.list('COUPANG', 1, 0);
  const order = result.orders[0];

  if (!order) {
    return res.json({ code: 'SUCCESS', data: { orderId: 0, orderItems: [], nextToken: '' } });
  }

  const items = JSON.parse(order.itemsJson) as OrderItem[];
  const shipping = JSON.parse(order.shippingJson) as ShippingAddress;

  return res.json({
    code: 'SUCCESS',
    data: {
      orderId: Number(order.orderId.replace(/\D/g, '')) || 1234567890,
      orderItems: items.map((item, index) => ({
        shipmentBoxId: 9000000000 + index,
        orderId: Number(order.orderId.replace(/\D/g, '')) || 1234567890,
        vendorItemId: item.productId,
        vendorItemName: item.productName,
        quantity: item.quantity,
        shippingPrice: 0,
        orderPrice: item.price,
        receiverName: shipping.recipientName,
        receiverPhone: shipping.phone,
        postCode: shipping.zipCode,
        addr1: shipping.address,
        addr2: '',
        statusName: 'ACCEPT',
        orderedAt: order.createdAt,
      })),
      nextToken: '',
    },
  });
});

router.put('/v2/orders/:shipmentBoxId/invoice', requireAuth('COUPANG', { allowMissing: true }), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  return res.json({
    code: 'SUCCESS',
    message: '송장 등록 완료',
  });
});

export default router;
