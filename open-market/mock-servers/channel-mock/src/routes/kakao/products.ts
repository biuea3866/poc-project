import { Router } from 'express';
import { productService } from '../../services/product.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';

const router = Router();

router.post('/', requireAuth('KAKAO_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['error-stock', 'delay-5000'])) {
    return;
  }

  const name = req.body?.name || '테스트 상품';
  const price = Number(req.body?.price || 0);
  const stock = Number(req.body?.stock || 0);

  const product = await productService.create({
    channel: 'KAKAO_STORE',
    name,
    price,
    stock,
    rawData: req.body,
  });

  return res.json({
    code: 0,
    message: 'success',
    data: {
      productId: product.externalId,
    },
  });
});

export default router;
