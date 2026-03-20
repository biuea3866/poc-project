import express from 'express';
import { config, validateConfig } from './config.js';
import { verifySignature, filterPullRequestEvent } from './webhook/handler.js';
import { parsePullRequestPayload } from './webhook/parser.js';
import { fetchPRDiff } from './github/diff.js';
import { generateReview } from './review/generator.js';
import { loadReviewConfig } from './review/config.js';
import { loadPipelineContext } from './review/context/pipeline-loader.js';
import { initStore, saveReview, onPRMerged } from './review/store.js';
import uiRouter, { notifyNewReview } from './ui/routes.js';

validateConfig();

const app = express();

// Webhook route needs raw body for HMAC verification
app.post(
  '/webhook',
  express.raw({ type: 'application/json' }),
  verifySignature,
  filterPullRequestEvent,
  async (req, res) => {
    const payload = (req as any).webhookPayload;
    const isMerge = (req as any).isMergeEvent as boolean;
    const pr = parsePullRequestPayload(payload);

    // PR merged → delete .md file
    if (isMerge) {
      console.log(`[webhook] PR merged: ${pr.owner}/${pr.repo}#${pr.number}`);
      onPRMerged(pr.owner, pr.repo, pr.number);
      res.status(200).json({ message: 'Merged, md cleaned up' });
      return;
    }

    console.log(`[webhook] Received PR event: ${pr.owner}/${pr.repo}#${pr.number} (${pr.action})`);
    res.status(202).json({ message: 'Processing', pr: `${pr.owner}/${pr.repo}#${pr.number}` });

    // Process asynchronously
    try {
      const diff = await fetchPRDiff(pr);
      const reviewConfig = loadReviewConfig();
      const context = loadPipelineContext();

      const review = await generateReview(pr, diff, reviewConfig, context);
      const id = saveReview(review);

      notifyNewReview(id, pr.repo, pr.number);
      console.log(`[webhook] Review generated: ${id}`);
    } catch (error) {
      console.error(`[webhook] Failed to process PR:`, error);
    }
  },
);

// JSON body parser for UI API routes
app.use(express.json());

// UI routes
app.use(uiRouter);

// Initialize store and start server
initStore();
const pipelineCtx = loadPipelineContext();

app.listen(config.port, '127.0.0.1', () => {
  console.log(`\n  PR Review Server running at http://127.0.0.1:${config.port}`);
  console.log(`  Dashboard: http://127.0.0.1:${config.port}/`);
  console.log(`  Webhook:   http://127.0.0.1:${config.port}/webhook`);
  console.log(`  Results:   ${config.resultsDir}`);
  console.log(`  Design principles: ${pipelineCtx.designPrinciples.length} rules loaded\n`);
});
