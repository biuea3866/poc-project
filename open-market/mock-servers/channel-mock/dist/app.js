"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const body_parser_1 = __importDefault(require("body-parser"));
const morgan_1 = __importDefault(require("morgan"));
const db_1 = require("./db");
const routes_1 = __importDefault(require("./routes"));
const app = (0, express_1.default)();
const PORT = process.env.PORT || 8082;
app.use((0, cors_1.default)());
app.use(body_parser_1.default.json());
app.use(body_parser_1.default.urlencoded({ extended: true }));
app.use((0, morgan_1.default)('dev'));
app.use('/', routes_1.default);
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(err.status || 500).json({
        code: err.code || 'INTERNAL_SERVER_ERROR',
        message: err.message || 'An unexpected error occurred',
    });
});
app.use((req, res) => {
    res.status(404).json({
        code: 'NOT_FOUND',
        message: `Route ${req.method} ${req.path} not found`,
    });
});
async function startServer() {
    try {
        await db_1.db.connect();
        await db_1.db.init();
        app.listen(PORT, () => {
            console.log('🚀 Channel Mock Server started');
            console.log(`📍 Server running on http://localhost:${PORT}`);
            console.log(`💚 Health Check: http://localhost:${PORT}/health`);
            console.log('');
            console.log('Available Channels:');
            console.log('  - ST11: /api/st11');
            console.log('  - Naver Store: /api/naver');
            console.log('  - Kakao Store: /api/kakao');
            console.log('  - Toss Store: /api/toss');
            console.log('  - Coupang: /api/coupang');
        });
    }
    catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}
process.on('SIGINT', async () => {
    console.log('\n🛑 Shutting down server...');
    await db_1.db.close();
    process.exit(0);
});
process.on('SIGTERM', async () => {
    console.log('\n🛑 Shutting down server...');
    await db_1.db.close();
    process.exit(0);
});
startServer();
exports.default = app;
//# sourceMappingURL=app.js.map