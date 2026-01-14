import { db } from './index';

async function initDatabase() {
  try {
    await db.connect();
    await db.init();
    console.log('Database initialized successfully');
    await db.close();
    process.exit(0);
  } catch (error) {
    console.error('Failed to initialize database:', error);
    process.exit(1);
  }
}

initDatabase();
