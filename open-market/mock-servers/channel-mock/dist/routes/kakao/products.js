"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const product_service_1 = require("../../services/product.service");
const auth_1 = require("../../middleware/auth");
const scenario_1 = require("../../middleware/scenario");
const router = (0, express_1.Router)();
router.post('/', (0, auth_1.requireAuth)('KAKAO_STORE'), async (req, res) => {
    if (await (0, scenario_1.applyScenario)(req, res, ['error-stock', 'delay-5000'])) {
        return;
    }
    const name = req.body?.name || '테스트 상품';
    const price = Number(req.body?.price || 0);
    const stock = Number(req.body?.stock || 0);
    const product = await product_service_1.productService.create({
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
exports.default = router;
//# sourceMappingURL=products.js.map