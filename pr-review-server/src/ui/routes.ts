import { Router } from 'express';
import type { Request, Response } from 'express';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';
import { config } from '../config.js';
import * as store from '../review/store.js';
import { postReviewToGitHub } from '../github/reviewer.js';
import type { InlineComment, ReviewEvent } from '../review/types.js';

const router = Router();

function paramId(req: Request): string {
  const id = req.params.id;
  return Array.isArray(id) ? id[0] : id;
}

function paramCommentId(req: Request): string {
  const id = req.params.commentId;
  return Array.isArray(id) ? id[0] : id;
}

// ── SSE ──────────────────────────────────────────────────────────

const sseClients: Set<Response> = new Set();

export function notifyNewReview(reviewId: string, repo: string, prNumber: number): void {
  const data = JSON.stringify({ type: 'new_review', id: reviewId, repo, pr: prNumber });
  console.log(`[sse] New review → ${repo}#${prNumber} (clients: ${sseClients.size})`);
  for (const client of sseClients) {
    client.write(`data: ${data}\n\n`);
  }
}

router.get('/api/events', (req: Request, res: Response) => {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.flushHeaders();

  sseClients.add(res);
  console.log(`[sse] Client connected (total: ${sseClients.size})`);

  const keepalive = setInterval(() => {
    res.write(': keepalive\n\n');
  }, 30000);

  req.on('close', () => {
    clearInterval(keepalive);
    sseClients.delete(res);
    console.log(`[sse] Client disconnected (total: ${sseClients.size})`);
  });
});

// ── REST API ─────────────────────────────────────────────────────

router.get('/api/reviews', (req: Request, res: Response) => {
  const repo = req.query.repo as string | undefined;
  const status = req.query.status as string | undefined;
  const reviews = store.listReviews({ repo, status });

  // Return lightweight list (no full diff data)
  const list = reviews.map(r => ({
    id: r.id,
    repo: `${r.prInfo.owner}/${r.prInfo.repo}`,
    prNumber: r.prInfo.number,
    title: r.prInfo.title,
    author: r.prInfo.author,
    branch: r.prInfo.branch,
    event: r.event,
    status: r.status,
    prType: r.scope.prType,
    size: r.scope.size,
    severityCounts: r.summary.severityCounts,
    createdAt: r.createdAt,
  }));

  res.json(list);
});

router.get('/api/reviews/:id', (req: Request, res: Response) => {
  const review = store.getReview(paramId(req));
  if (!review) {
    res.status(404).json({ error: 'Review not found' });
    return;
  }
  res.json(review);
});

router.patch('/api/reviews/:id', (req: Request, res: Response) => {
  try {
    const { reviewBody, event, inlineComments } = req.body;
    const patch: any = {};
    if (reviewBody !== undefined) patch.reviewBody = reviewBody;
    if (event !== undefined) patch.event = event as ReviewEvent;
    if (inlineComments !== undefined) patch.inlineComments = inlineComments;

    store.updateReview(paramId(req), patch);
    res.json({ success: true });
  } catch (error: any) {
    res.status(404).json({ error: error.message });
  }
});

router.post('/api/reviews/:id/comments', (req: Request, res: Response) => {
  const review = store.getReview(paramId(req));
  if (!review) {
    res.status(404).json({ error: 'Review not found' });
    return;
  }

  const { path: filePath, line, startLine, body, severity } = req.body;
  const comment: InlineComment = {
    id: uuidv4(),
    path: filePath,
    line,
    startLine,
    body,
    severity: severity ?? 'P4',
    source: 'user',
  };

  review.inlineComments.push(comment);
  store.updateReview(paramId(req), { inlineComments: review.inlineComments });
  res.json(comment);
});

router.delete('/api/reviews/:id/comments/:commentId', (req: Request, res: Response) => {
  const review = store.getReview(paramId(req));
  if (!review) {
    res.status(404).json({ error: 'Review not found' });
    return;
  }

  const idx = review.inlineComments.findIndex(c => c.id === paramCommentId(req));
  if (idx === -1) {
    res.status(404).json({ error: 'Comment not found' });
    return;
  }

  review.inlineComments.splice(idx, 1);
  store.updateReview(paramId(req), { inlineComments: review.inlineComments });
  res.json({ success: true });
});

router.post('/api/reviews/:id/publish', async (req: Request, res: Response) => {
  const review = store.getReview(paramId(req));
  if (!review) {
    res.status(404).json({ error: 'Review not found' });
    return;
  }

  if (review.status === 'posted') {
    res.status(400).json({ error: 'Review already posted' });
    return;
  }

  const result = await postReviewToGitHub(review.prInfo, review);

  if (result.posted) {
    store.updateReview(paramId(req), { status: 'posted' });
  }

  res.json(result);
});

router.get('/api/reviews/:id/review-request-path', (req: Request, res: Response) => {
  const review = store.getReview(paramId(req));
  if (!review) {
    res.status(404).json({ error: 'Review not found' });
    return;
  }
  // Build the review request file path
  const branch = review.prInfo.branch;
  const ticketMatch = branch.match(/(?:grt|GRT)-\d+/i);
  const ticket = ticketMatch ? ticketMatch[0].toLowerCase() : 'unknown';
  const requestPath = `${ticket}/${review.prInfo.repo}/pr${review.prInfo.number}-review-request.md`;
  res.json({ path: requestPath, fullPath: `${config.resultsDir}/${requestPath}` });
});

// ── Static files ─────────────────────────────────────────────────

router.get('/', (req: Request, res: Response) => {
  res.sendFile(path.join(import.meta.dirname, 'static', 'index.html'));
});

router.get('/static/:file', (req: Request, res: Response) => {
  const file = req.params.file;
  res.sendFile(path.join(import.meta.dirname, 'static', Array.isArray(file) ? file[0] : file));
});

export default router;
