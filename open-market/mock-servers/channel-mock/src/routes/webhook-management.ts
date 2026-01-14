import { Router } from 'express';
import { webhookService } from '../services/webhook.service';
import { Channel } from '../types';
import { getEventTypes, getSamplePayload } from '../webhooks/sample-events';

interface WebhookRouterOptions {
  requireAuth?: (req: any, res: any, next: any) => void;
}

export function createWebhookRouter(channel: Channel, options: WebhookRouterOptions = {}) {
  const router = Router();

  if (options.requireAuth) {
    router.use(options.requireAuth);
  }

  router.post('/register', async (req, res) => {
    const targetUrl = req.body?.url as string | undefined;
    const eventTypes = req.body?.events as string[] | undefined;

    if (!targetUrl || !Array.isArray(eventTypes) || eventTypes.length === 0) {
      return res.status(400).json({ code: '400', message: 'url 과 events가 필요합니다' });
    }

    const allowedEvents = getEventTypes(channel);
    const invalidEvents = eventTypes.filter((eventType) => !allowedEvents.includes(eventType));
    if (invalidEvents.length > 0) {
      return res.status(400).json({
        code: '400',
        message: '지원하지 않는 이벤트입니다',
        data: { invalidEvents, allowedEvents },
      });
    }

    await webhookService.register(channel, eventTypes, targetUrl);

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
    const hooks = await webhookService.list(channel);
    return res.json({
      code: '200',
      data: hooks,
    });
  });

  router.get('/events', async (_req, res) => {
    return res.json({
      code: '200',
      data: getEventTypes(channel),
    });
  });

  router.post('/trigger', async (req, res) => {
    const eventType = req.body?.eventType as string | undefined;
    const payload = (req.body?.data as Record<string, unknown> | undefined) || {};

    if (!eventType) {
      return res.status(400).json({ code: '400', message: 'eventType이 필요합니다' });
    }

    const allowedEvents = getEventTypes(channel);
    if (!allowedEvents.includes(eventType)) {
      return res.status(400).json({
        code: '400',
        message: '지원하지 않는 이벤트입니다',
        data: { allowedEvents },
      });
    }

    const finalPayload = Object.keys(payload).length > 0 ? payload : getSamplePayload(channel, eventType);
    const result = await webhookService.trigger(channel, eventType, finalPayload);

    return res.json({
      code: '200',
      data: result,
    });
  });

  return router;
}
