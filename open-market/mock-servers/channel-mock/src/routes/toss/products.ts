import { Router } from 'express';
import { productService } from '../../services/product.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';

const router = Router();

router.post('/v1/products', requireAuth('TOSS_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['error-stock', 'delay-5000'])) {
    return;
  }

  const name = req.body?.name || '테스트 상품';
  const price = Number(req.body?.sellingPrice || 0);
  const stock = Number(req.body?.stockQuantity || 0);

  const product = await productService.create({
    channel: 'TOSS_STORE',
    name,
    price,
    stock,
    rawData: req.body,
  });

  return res.json({
    result: 'SUCCESS',
    data: {
      productId: product.externalId,
      status: 'PENDING_APPROVAL',
    },
  });
});

export default router;
