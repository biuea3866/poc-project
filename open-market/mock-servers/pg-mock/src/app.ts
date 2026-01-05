import express from 'express';
import cors from 'cors';
import bodyParser from 'body-parser';
import morgan from 'morgan';
import path from 'path';
import { db } from './db';
import routes from './routes';

const app = express();
const PORT = process.env.PORT || 8081;

// ==================== Middleware ====================
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(morgan('dev'));

// ==================== View Engine ====================
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// ==================== Routes ====================
app.use('/', routes);

// ==================== Error Handler ====================
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    code: err.code || 'INTERNAL_SERVER_ERROR',
    message: err.message || 'An unexpected error occurred',
  });
});

// ==================== 404 Handler ====================
app.use((req: express.Request, res: express.Response) => {
  res.status(404).json({
    code: 'NOT_FOUND',
    message: `Route ${req.method} ${req.path} not found`,
  });
});

// ==================== Start Server ====================
async function startServer() {
  try {
    // Connect to database
    await db.connect();
    await db.init();

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
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Handle shutdown
process.on('SIGINT', async () => {
  console.log('\n🛑 Shutting down server...');
  await db.close();
  process.exit(0);
});

process.on('SIGTERM', async () => {
  console.log('\n🛑 Shutting down server...');
  await db.close();
  process.exit(0);
});

// Start the server
startServer();

export default app;
