"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const body_parser_1 = __importDefault(require("body-parser"));
const morgan_1 = __importDefault(require("morgan"));
const path_1 = __importDefault(require("path"));
const db_1 = require("./db");
const routes_1 = __importDefault(require("./routes"));
const app = (0, express_1.default)();
const PORT = process.env.PORT || 8081;
// ==================== Middleware ====================
app.use((0, cors_1.default)());
app.use(body_parser_1.default.json());
app.use(body_parser_1.default.urlencoded({ extended: true }));
app.use((0, morgan_1.default)('dev'));
// ==================== View Engine ====================
app.set('view engine', 'ejs');
app.set('views', path_1.default.join(__dirname, 'views'));
// ==================== Routes ====================
app.use('/', routes_1.default);
// ==================== Error Handler ====================
app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(err.status || 500).json({
        code: err.code || 'INTERNAL_SERVER_ERROR',
        message: err.message || 'An unexpected error occurred',
    });
});
// ==================== 404 Handler ====================
app.use((req, res) => {
    res.status(404).json({
        code: 'NOT_FOUND',
        message: `Route ${req.method} ${req.path} not found`,
    });
});
// ==================== Start Server ====================
async function startServer() {
    try {
        // Connect to database
        await db_1.db.connect();
        await db_1.db.init();
        // Start server
        app.listen(PORT, () => {
            console.log('🚀 PG Mock Server started');
            console.log(`📍 Server running on http://localhost:${PORT}`);
            console.log(`📄 API Documentation: http://localhost:${PORT}/`);
            console.log(`💚 Health Check: http://localhost:${PORT}/health`);
            console.log('');
            console.log('Available PG Providers:');
            console.log('  - Toss Payments: /api/toss');
            console.log('  - Kakao Pay: /api/kakao');
            console.log('  - Naver Pay: /api/naver');
            console.log('  - Danal: /api/danal');
        });
    }
    catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}
// Handle shutdown
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
// Start the server
startServer();
exports.default = app;
//# sourceMappingURL=app.js.map