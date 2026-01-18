"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const product_service_1 = require("../../services/product.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.post('/v2/products', (0, auth_1.requireAuth)('COUPANG', { allowMissing: true }), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['error-stock', 'delay-5000'])) {
        return;
    }
    const name = req.body?.sellerProductName || '테스트 상품';
    const price = Number(req.body?.items?.[0]?.salePrice || 0);
    const stock = Number(req.body?.items?.[0]?.maximumBuyCount || 0);
    const product = await product_service_1.productService.create({
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
exports.default = router;
//# sourceMappingURL=products.js.map