"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.webhookService = void 0;
const http_1 = __importDefault(require("http"));
const https_1 = __importDefault(require("https"));
const url_1 = require("url");
const db_1 = require("../db");
const DEFAULT_TIMEOUT_MS = 5000;
const postJson = (targetUrl, payload, headers, timeoutMs = DEFAULT_TIMEOUT_MS) => {
    return new Promise((resolve, reject) => {
        const url = new url_1.URL(targetUrl);
        const body = JSON.stringify(payload);
        const client = url.protocol === 'https:' ? https_1.default : http_1.default;
        const request = client.request({
            method: 'POST',
            hostname: url.hostname,
            port: url.port || (url.protocol === 'https:' ? 443 : 80),
            path: `${url.pathname}${url.search}`,
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(body).toString(),
                ...headers,
            },
        }, (response) => {
            response.resume();
            resolve({ status: response.statusCode || 0 });
        });
        request.on('error', reject);
        request.setTimeout(timeoutMs, () => {
            request.destroy(new Error('timeout'));
        });
        request.write(body);
        request.end();
    });
};
exports.webhookService = {
    async register(channel, eventTypes, targetUrl) {
        const createdAt = new Date().toISOString();
        for (const eventType of eventTypes) {
            await db_1.db.run('INSERT INTO webhooks (channel, event_type, target_url, created_at) VALUES (?, ?, ?, ?)', [channel, eventType, targetUrl, createdAt]);
        }
    },
    async list(channel, eventType) {
        if (eventType) {
            return db_1.db.all('SELECT id, channel, event_type, target_url, created_at FROM webhooks WHERE channel = ? AND event_type = ?', [channel, eventType]);
        }
        return db_1.db.all('SELECT id, channel, event_type, target_url, created_at FROM webhooks WHERE channel = ?', [channel]);
    },
    async trigger(channel, eventType, payload) {
        const targets = await exports.webhookService.list(channel, eventType);
        const results = await Promise.all(targets.map(async (target) => {
            try {
                const response = await postJson(target.target_url, { eventType, channel, data: payload }, {
                    'X-Channel': channel,
                    'X-Event-Type': eventType,
                });
                return { url: target.target_url, status: response.status };
            }
            catch (error) {
                return { url: target.target_url, status: 0, error: error.message };
            }
        }));
        return {
            delivered: results.filter((result) => result.status >= 200 && result.status < 300).length,
            failed: results.filter((result) => result.status < 200 || result.status >= 300).length,
            results,
        };
    },
    async storeReceived(channel, eventType, payload) {
        await db_1.db.run('INSERT INTO webhook_events (channel, event_type, payload_json, received_at) VALUES (?, ?, ?, ?)', [channel, eventType, JSON.stringify(payload), new Date().toISOString()]);
    },
    async listReceived(limit = 50) {
        return db_1.db.all('SELECT id, channel, event_type, payload_json, received_at FROM webhook_events ORDER BY id DESC LIMIT ?', [limit]);
    },
};
//# sourceMappingURL=webhook.service.js.map