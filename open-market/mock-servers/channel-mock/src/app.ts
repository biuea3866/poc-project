import express from 'express';
import cors from 'cors';
import bodyParser from 'body-parser';
import morgan from 'morgan';
import { db } from './db';
import routes from './routes';

const app = express();
const PORT = process.env.PORT || 8082;

app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(morgan('dev'));

app.use('/', routes);

app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    code: err.code || 'INTERNAL_SERVER_ERROR',
    message: err.message || 'An unexpected error occurred',
  });
});

app.use((req: express.Request, res: express.Response) => {
  res.status(404).json({
    code: 'NOT_FOUND',
    message: `Route ${req.method} ${req.path} not found`,
  });
});

async function startServer() {
  try {
    await db.connect();
    await db.init();

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
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

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

startServer();

export default app;
