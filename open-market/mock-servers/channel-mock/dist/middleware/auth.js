"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.requireAuth = requireAuth;
const auth_service_1 = require("../services/auth.service");
function parseAuthToken(headerValue) {
    if (!headerValue) {
        return null;
    }
    if (headerValue.startsWith('Bearer ')) {
        return headerValue.replace('Bearer ', '').trim();
    }
    return headerValue.trim();
}
function requireAuth(channel, options = {}) {
    return async (req, res, next) => {
        const scenario = req.headers['x-mock-scenario'];
        if (scenario === 'error-auth') {
            return res.status(401).json({ code: '401', message: '인증 실패' });
        }
        const token = parseAuthToken(req.headers.authorization);
        if (!token) {
            if (options.allowMissing) {
                return next();
            }
            return res.status(401).json({ code: '401', message: '인증 토큰이 필요합니다' });
        }
        const isValid = await auth_service_1.authService.validateToken(channel, token);
        if (!isValid && !options.allowMissing) {
            return res.status(401).json({ code: '401', message: '유효하지 않은 토큰입니다' });
        }
        return next();
    };
}
//# sourceMappingURL=auth.js.map