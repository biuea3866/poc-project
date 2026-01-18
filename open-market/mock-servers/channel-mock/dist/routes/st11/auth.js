"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const auth_service_1 = require("../../services/auth.service");
const router = (0, express_1.Router)();
router.post('/auth/token', async (req, res) => {
    const { openapiKey, secretKey } = req.body;
    if (!openapiKey || !secretKey) {
        return res.status(400).json({
            code: '400',
            message: '인증 정보가 필요합니다',
        });
    }
    const token = await auth_service_1.authService.createToken('ST11', 3600);
    return res.json({
        code: '200',
        message: '성공',
        data: {
            accessToken: token.token,
            expiresIn: token.expiresIn,
        },
    });
});
exports.default = router;
//# sourceMappingURL=auth.js.map