"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const auth_service_1 = require("../../services/auth.service");
const router = (0, express_1.Router)();
router.post('/v1/auth/token', async (req, res) => {
    const { clientId, clientSecret } = req.body;
    if (!clientId || !clientSecret) {
        return res.status(400).json({ result: 'FAIL', message: '인증 정보가 필요합니다' });
    }
    const token = await auth_service_1.authService.createToken('TOSS_STORE', 3600);
    return res.json({
        accessToken: token.token,
        expiresAt: new Date(Date.now() + token.expiresIn * 1000).toISOString(),
    });
});
exports.default = router;
//# sourceMappingURL=auth.js.map