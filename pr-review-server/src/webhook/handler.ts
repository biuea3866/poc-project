import crypto from 'crypto';
import type { Request, Response, NextFunction } from 'express';
import { config } from '../config.js';

export function verifySignature(req: Request, res: Response, next: NextFunction): void {
  const signature = req.headers['x-hub-signature-256'] as string | undefined;
  if (!signature) {
    res.status(401).json({ error: 'Missing X-Hub-Signature-256 header' });
    return;
  }

  const body = req.body as Buffer;
  const expected = 'sha256=' + crypto
    .createHmac('sha256', config.webhookSecret)
    .update(body)
    .digest('hex');

  if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expected))) {
    res.status(401).json({ error: 'Invalid signature' });
    return;
  }

  next();
}

export function filterPullRequestEvent(req: Request, res: Response, next: NextFunction): void {
  const event = req.headers['x-github-event'] as string | undefined;
  if (event !== 'pull_request') {
    res.status(200).json({ message: `Ignored event: ${event}` });
    return;
  }

  const payload = JSON.parse((req.body as Buffer).toString('utf-8'));
  const action = payload.action as string;
  const reviewActions = ['opened', 'synchronize', 'reopened', 'ready_for_review', 'edited'];
  const isMerge = action === 'closed' && payload.pull_request?.merged === true;

  if (!reviewActions.includes(action) && !isMerge) {
    res.status(200).json({ message: `Ignored action: ${action}` });
    return;
  }

  (req as any).webhookPayload = payload;
  (req as any).isMergeEvent = isMerge;
  next();
}
