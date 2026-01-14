import { Router } from 'express';
import { webhookService } from '../services/webhook.service';
import { Channel } from '../types';

const router = Router();

router.post('/receiver', async (req, res) => {
  const headerChannel = req.header('X-Channel');
  const headerEvent = req.header('X-Event-Type');
  const bodyChannel = req.body?.channel as string | undefined;
  const bodyEvent = req.body?.eventType as string | undefined;

  const channel = (headerChannel || bodyChannel || 'ST11') as Channel;
  const eventType = headerEvent || bodyEvent || 'UNKNOWN_EVENT';

  await webhookService.storeReceived(channel, eventType, req.body || {});

  return res.json({
    code: '200',
    message: 'received',
  });
});

router.get('/received', async (req, res) => {
  const limit = Number(req.query.limit || 50);
  const events = await webhookService.listReceived(limit);

  return res.json({
    code: '200',
    data: events,
  });
});

export default router;
