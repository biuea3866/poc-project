import { Router } from 'express';
import { productService } from '../../services/product.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';

const router = Router();

router.post('/v2/products', requireAuth('COUPANG', { allowMissing: true }), async (req, res) => {
  if (await applyScenario(req, res, ['error-stock', 'delay-5000'])) {
    return;
  }

  const name = req.body?.sellerProductName || '테스트 상품';
  const price = Number(req.body?.items?.[0]?.salePrice || 0);
  const stock = Number(req.body?.items?.[0]?.maximumBuyCount || 0);

  const product = await productService.create({
    channel: 'COUPANG',
    name,
    price,
    stock,
    rawData: req.body,
  });

  return res.json({
    code: 'SUCCESS',
    message: '',
    data: {
      sellerProductId: Number(product.externalId),
    },
  });
});

export default router;
