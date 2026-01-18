"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.scenarios = void 0;
exports.scenarios = {
    success: {
        status: 200,
        delay: 0,
    },
    'error-auth': {
        status: 401,
        body: { code: '401', message: '인증 실패' },
    },
    'error-stock': {
        status: 400,
        body: { code: 'INSUFFICIENT_STOCK', message: '재고 부족' },
    },
    'delay-5000': {
        status: 200,
        delay: 5000,
    },
};
//# sourceMappingURL=index.js.map