import { Router } from 'express';
import { productService } from '../../services/product.service';
import { applyScenario } from '../../middleware/scenario';
import { requireAuth } from '../../middleware/auth';

const router = Router();

router.post('/', requireAuth('ST11'), async (req, res) => {
  if (await applyScenario(req, res, ['error-stock', 'delay-5000'])) {
    return;
  }

  const { productName, categoryCode, sellingPrice, stockQuantity } = req.body;

  if (!productName || !categoryCode || typeof sellingPrice !== 'number') {
    return res.status(400).json({
      code: '400',
      message: '필수 값 누락',
      errors: [
        {
          field: 'productName',
          message: '상품명 또는 카테고리 정보가 필요합니다',
        },
      ],
    });
  }

  const product = await productService.create({
    channel: 'ST11',
    name: productName,
    price: sellingPrice,
    stock: typeof stockQuantity === 'number' ? stockQuantity : 0,
    rawData: req.body,
  });

  return res.json({
    code: '200',
    message: '성공',
    data: {
      productNo: product.externalId,
      status: 'WAIT_APPROVAL',
      createdAt: product.createdAt,
    },
  });
});

router.get('/:productNo', requireAuth('ST11'), async (req, res) => {
  const product = await productService.getByExternalId('ST11', req.params.productNo);

  if (!product) {
    return res.status(404).json({ code: '404', message: '상품을 찾을 수 없습니다' });
  }

  const raw = JSON.parse(product.rawData || '{}');

  return res.json({
    code: '200',
    data: {
      productNo: product.externalId,
      productName: product.name,
      categoryCode: raw.categoryCode || '001001001',
      sellingPrice: product.price,
      status: 'ON_SALE',
      stockQuantity: product.stock,
      options: raw.options || [],
      images: raw.images || [],
    },
  });
});

export default router;
