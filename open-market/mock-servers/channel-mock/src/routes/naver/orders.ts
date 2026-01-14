import { Router } from 'express';
import { orderService } from '../../services/order.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';
import { OrderItem, ShippingAddress } from '../../types';

const router = Router();

router.post('/search', requireAuth('NAVER_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const pageIndex = Number(req.body?.pageIndex || 1);
  const pageSize = Number(req.body?.pageSize || 20);
  const offset = (pageIndex - 1) * pageSize;

  const result = await orderService.list('NAVER_STORE', pageSize, offset);

  const contents = result.orders.map((order) => {
    const items = JSON.parse(order.itemsJson) as OrderItem[];
    const shipping = JSON.parse(order.shippingJson) as ShippingAddress;

    return {
      orderId: order.orderId,
      orderDate: order.createdAt,
      orderStatusType: order.status,
      totalPaymentAmount: order.totalAmount,
      orderItems: items.map((item, index) => ({
        productOrderId: `${order.orderId}-${index + 1}`,
        productId: Number(item.productId.replace(/\D/g, '')) || 1234567890,
        productName: item.productName,
        quantity: item.quantity,
        unitPrice: item.price,
        shippingAddress: {
          name: shipping.recipientName,
          tel1: shipping.phone,
          baseAddress: shipping.address,
          zipCode: shipping.zipCode,
        },
      })),
    };
  });

  return res.json({
    timestamp: new Date().toISOString(),
    data: {
      count: result.totalCount,
      moreSequence: result.totalCount > pageIndex * pageSize ? 'more' : '',
      contents,
    },
  });
});

router.post('/:productOrderId/ship', requireAuth('NAVER_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['delay-5000'])) {
    return;
  }

  const orderId = req.params.productOrderId.replace(/-\d+$/, '');
  await orderService.updateStatus('NAVER_STORE', orderId, 'SHIPPING');

  return res.json({
    timestamp: new Date().toISOString(),
    data: {
      success: true,
    },
  });
});

export default router;
