import { Router } from 'express';
import { productService } from '../../services/product.service';
import { requireAuth } from '../../middleware/auth';
import { applyScenario } from '../../middleware/scenario';

const router = Router();

router.post('/', requireAuth('NAVER_STORE'), async (req, res) => {
  if (await applyScenario(req, res, ['error-stock', 'delay-5000'])) {
    return;
  }

  const name = req.body?.originProduct?.name || '테스트 상품';
  const price = Number(req.body?.originProduct?.salePrice || 0);
  const stock = Number(req.body?.originProduct?.stockQuantity || 0);

  const product = await productService.create({
    channel: 'NAVER_STORE',
    name,
    price,
    stock,
    rawData: req.body,
  });

  return res.json({
    timestamp: new Date().toISOString(),
    data: {
      originProductNo: Number(product.externalId),
      smartstoreChannelProductNo: Number(product.externalId) + 1,
    },
  });
});

router.get('/:productNo', requireAuth('NAVER_STORE'), async (req, res) => {
  const product = await productService.getByExternalId('NAVER_STORE', req.params.productNo);

  if (!product) {
    return res.status(404).json({ message: '상품을 찾을 수 없습니다' });
  }

  const raw = JSON.parse(product.rawData || '{}');

  return res.json({
    timestamp: new Date().toISOString(),
    data: {
      originProduct: {
        statusType: 'SALE',
        name: product.name,
        salePrice: product.price,
        stockQuantity: product.stock,
        detailContent: raw.originProduct?.detailContent || '<p>상품 상세</p>',
        images: raw.originProduct?.images || {},
      },
    },
  });
});

export default router;
