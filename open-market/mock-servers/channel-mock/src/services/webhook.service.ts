import http from 'http';
import https from 'https';
import { URL } from 'url';
import { db } from '../db';
import { Channel } from '../types';

interface WebhookRecord {
  id: number;
  channel: Channel;
  event_type: string;
  target_url: string;
  created_at: string;
}

interface WebhookEventRecord {
  id: number;
  channel: Channel;
  event_type: string;
  payload_json: string;
  received_at: string;
}

const DEFAULT_TIMEOUT_MS = 5000;

const postJson = (targetUrl: string, payload: Record<string, unknown>, headers: Record<string, string>, timeoutMs = DEFAULT_TIMEOUT_MS) => {
  return new Promise<{ status: number }>((resolve, reject) => {
    const url = new URL(targetUrl);
    const body = JSON.stringify(payload);
    const client = url.protocol === 'https:' ? https : http;

    const request = client.request(
      {
        method: 'POST',
        hostname: url.hostname,
        port: url.port || (url.protocol === 'https:' ? 443 : 80),
        path: `${url.pathname}${url.search}`,
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(body).toString(),
          ...headers,
        },
      },
      (response) => {
        response.resume();
        resolve({ status: response.statusCode || 0 });
      }
    );

    request.on('error', reject);
    request.setTimeout(timeoutMs, () => {
      request.destroy(new Error('timeout'));
    });

    request.write(body);
    request.end();
  });
};

export const webhookService = {
  async register(channel: Channel, eventTypes: string[], targetUrl: string): Promise<void> {
    const createdAt = new Date().toISOString();

    for (const eventType of eventTypes) {
      await db.run(
        'INSERT INTO webhooks (channel, event_type, target_url, created_at) VALUES (?, ?, ?, ?)',
        [channel, eventType, targetUrl, createdAt]
      );
    }
  },

  async list(channel: Channel, eventType?: string): Promise<WebhookRecord[]> {
    if (eventType) {
      return db.all<WebhookRecord>(
        'SELECT id, channel, event_type, target_url, created_at FROM webhooks WHERE channel = ? AND event_type = ?',
        [channel, eventType]
      );
    }

    return db.all<WebhookRecord>(
      'SELECT id, channel, event_type, target_url, created_at FROM webhooks WHERE channel = ?',
      [channel]
    );
  },

  async trigger(channel: Channel, eventType: string, payload: Record<string, unknown>) {
    const targets = await webhookService.list(channel, eventType);

    const results = await Promise.all(
      targets.map(async (target) => {
        try {
          const response = await postJson(
            target.target_url,
            { eventType, channel, data: payload },
            {
              'X-Channel': channel,
              'X-Event-Type': eventType,
            }
          );

          return { url: target.target_url, status: response.status };
        } catch (error) {
          return { url: target.target_url, status: 0, error: (error as Error).message };
        }
      })
    );

    return {
      delivered: results.filter((result) => result.status >= 200 && result.status < 300).length,
      failed: results.filter((result) => result.status < 200 || result.status >= 300).length,
      results,
    };
  },

  async storeReceived(channel: Channel, eventType: string, payload: Record<string, unknown>) {
    await db.run(
      'INSERT INTO webhook_events (channel, event_type, payload_json, received_at) VALUES (?, ?, ?, ?)',
      [channel, eventType, JSON.stringify(payload), new Date().toISOString()]
    );
  },

  async listReceived(limit = 50): Promise<WebhookEventRecord[]> {
    return db.all<WebhookEventRecord>(
      'SELECT id, channel, event_type, payload_json, received_at FROM webhook_events ORDER BY id DESC LIMIT ?',
      [limit]
    );
  },
};
