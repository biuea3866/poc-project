"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const auth_service_1 = require("../../services/auth.service");
const router = (0, express_1.Router)();
router.post('/oauth/token', async (req, res) => {
    const { client_id, client_secret } = req.body;
    if (!client_id || !client_secret) {
        return res.status(400).json({ error: 'invalid_client' });
    }
    const token = await auth_service_1.authService.createToken('NAVER_STORE', 43200);
    return res.json({
        access_token: token.token,
        token_type: 'Bearer',
        expires_in: token.expiresIn,
    });
});
exports.default = router;
//# sourceMappingURL=auth.js.map