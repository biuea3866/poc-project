"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const product_service_1 = require("../../services/product.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.post('/', (0, auth_1.requireAuth)('NAVER_STORE'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['error-stock', 'delay-5000'])) {
        return;
    }
    const name = req.body?.originProduct?.name || '테스트 상품';
    const price = Number(req.body?.originProduct?.salePrice || 0);
    const stock = Number(req.body?.originProduct?.stockQuantity || 0);
    const product = await product_service_1.productService.create({
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
router.get('/:productNo', (0, auth_1.requireAuth)('NAVER_STORE'), async (req, res) => {
    const product = await product_service_1.productService.getByExternalId('NAVER_STORE', req.params.productNo);
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
exports.default = router;
//# sourceMappingURL=products.js.map