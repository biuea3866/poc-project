"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const webhook_service_1 = require("../services/webhook.service");
const router = (0, express_1.Router)();
router.post('/receiver', async (req, res) => {
    const headerChannel = req.header('X-Channel');
    const headerEvent = req.header('X-Event-Type');
    const bodyChannel = req.body?.channel;
    const bodyEvent = req.body?.eventType;
    const channel = (headerChannel || bodyChannel || 'ST11');
    const eventType = headerEvent || bodyEvent || 'UNKNOWN_EVENT';
    await webhook_service_1.webhookService.storeReceived(channel, eventType, req.body || {});
    return res.json({
        code: '200',
        message: 'received',
    });
});
router.get('/received', async (req, res) => {
    const limit = Number(req.query.limit || 50);
    const events = await webhook_service_1.webhookService.listReceived(limit);
    return res.json({
        code: '200',
        data: events,
    });
});
exports.default = router;
//# sourceMappingURL=webhook-receiver.js.map