"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createWebhookRouter = createWebhookRouter;
const express_1 = require("express");
const webhook_service_1 = require("../services/webhook.service");
const sample_events_1 = require("../webhooks/sample-events");
function createWebhookRouter(channel, options = {}) {
    const router = (0, express_1.Router)();
    if (options.requireAuth) {
        router.use(options.requireAuth);
    }
    router.post('/register', async (req, res) => {
        const targetUrl = req.body?.url;
        const eventTypes = req.body?.events;
        if (!targetUrl || !Array.isArray(eventTypes) || eventTypes.length === 0) {
            return res.status(400).json({ code: '400', message: 'url 과 events가 필요합니다' });
        }
        const allowedEvents = (0, sample_events_1.getEventTypes)(channel);
        const invalidEvents = eventTypes.filter((eventType) => !allowedEvents.includes(eventType));
        if (invalidEvents.length > 0) {
            return res.status(400).json({
                code: '400',
                message: '지원하지 않는 이벤트입니다',
                data: { invalidEvents, allowedEvents },
            });
        }
        await webhook_service_1.webhookService.register(channel, eventTypes, targetUrl);
        return res.json({
            code: '200',
            message: 'registered',
            data: {
                channel,
                eventTypes,
                url: targetUrl,
            },
        });
    });
    router.get('/list', async (_req, res) => {
        const hooks = await webhook_service_1.webhookService.list(channel);
        return res.json({
            code: '200',
            data: hooks,
        });
    });
    router.get('/events', async (_req, res) => {
        return res.json({
            code: '200',
            data: (0, sample_events_1.getEventTypes)(channel),
        });
    });
    router.post('/trigger', async (req, res) => {
        const eventType = req.body?.eventType;
        const payload = req.body?.data || {};
        if (!eventType) {
            return res.status(400).json({ code: '400', message: 'eventType이 필요합니다' });
        }
        const allowedEvents = (0, sample_events_1.getEventTypes)(channel);
        if (!allowedEvents.includes(eventType)) {
            return res.status(400).json({
                code: '400',
                message: '지원하지 않는 이벤트입니다',
                data: { allowedEvents },
            });
        }
        const finalPayload = Object.keys(payload).length > 0 ? payload : (0, sample_events_1.getSamplePayload)(channel, eventType);
        const result = await webhook_service_1.webhookService.trigger(channel, eventType, finalPayload);
        return res.json({
            code: '200',
            data: result,
        });
    });
    return router;
}
//# sourceMappingURL=webhook-management.js.map