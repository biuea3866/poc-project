import { v4 as uuidv4 } from 'uuid';
import { db } from '../db';
import { Channel } from '../types';

interface TokenRecord {
  token: string;
  expires_at: number;
}

export const authService = {
  async createToken(channel: Channel, ttlSeconds = 3600): Promise<{ token: string; expiresIn: number }> {
    const token = `mock-${channel.toLowerCase()}-${uuidv4()}`;
    const expiresAt = Date.now() + ttlSeconds * 1000;

    await db.run(
      'INSERT INTO tokens (channel, token, expires_at) VALUES (?, ?, ?)',
      [channel, token, expiresAt]
    );

    return { token, expiresIn: ttlSeconds };
  },

  async validateToken(channel: Channel, token: string): Promise<boolean> {
    const row = await db.get<TokenRecord>(
      'SELECT token, expires_at FROM tokens WHERE channel = ? AND token = ? ORDER BY id DESC LIMIT 1',
      [channel, token]
    );

    if (!row) {
      return false;
    }

    return row.expires_at > Date.now();
  },
};
