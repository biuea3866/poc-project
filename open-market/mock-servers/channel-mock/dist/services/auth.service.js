"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.authService = void 0;
const uuid_1 = require("uuid");
const db_1 = require("../db");
exports.authService = {
    async createToken(channel, ttlSeconds = 3600) {
        const token = `mock-${channel.toLowerCase()}-${(0, uuid_1.v4)()}`;
        const expiresAt = Date.now() + ttlSeconds * 1000;
        await db_1.db.run('INSERT INTO tokens (channel, token, expires_at) VALUES (?, ?, ?)', [channel, token, expiresAt]);
        return { token, expiresIn: ttlSeconds };
    },
    async validateToken(channel, token) {
        const row = await db_1.db.get('SELECT token, expires_at FROM tokens WHERE channel = ? AND token = ? ORDER BY id DESC LIMIT 1', [channel, token]);
        if (!row) {
            return false;
        }
        return row.expires_at > Date.now();
    },
};
//# sourceMappingURL=auth.service.js.map