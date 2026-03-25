import { getOctokit } from '../github/client.js';
import { fetchPRDiff } from '../github/diff.js';
import { generateReview } from './generator.js';
import { loadReviewConfig } from './config.js';
import { loadPipelineContext } from './context/pipeline-loader.js';
import { saveReview, getReviewByPR } from './store.js';
import { notifyNewReview } from '../ui/routes.js';
import type { PRInfo } from './types.js';

let polling = false;

async function pollRepo(owner: string, repo: string): Promise<number> {
  const octokit = getOctokit();
  let reviewed = 0;

  const { data: prs } = await octokit.pulls.list({
    owner,
    repo,
    state: 'open',
    sort: 'created',
    direction: 'desc',
    per_page: 10,
  });

  for (const pr of prs) {
    // Skip drafts
    if (pr.draft) continue;

    // Skip if already reviewed
    const existing = getReviewByPR(owner, repo, pr.number);
    if (existing) continue;

    console.log(`[poller] New PR found: ${owner}/${repo}#${pr.number} "${pr.title}"`);

    try {
      const prInfo: PRInfo = {
        owner,
        repo,
        number: pr.number,
        title: pr.title,
        body: pr.body ?? '',
        branch: pr.head.ref,
        baseBranch: pr.base.ref,
        author: pr.user?.login ?? '',
        action: 'opened',
        url: pr.html_url,
      };

      const diff = await fetchPRDiff(prInfo);
      const reviewConfig = loadReviewConfig();
      const context = loadPipelineContext();
      const review = await generateReview(prInfo, diff, reviewConfig, context);
      const id = saveReview(review);

      notifyNewReview(id, repo, pr.number);
      reviewed++;

      console.log(`[poller] Reviewed: ${owner}/${repo}#${pr.number}`);
    } catch (err) {
      console.error(`[poller] Failed: ${owner}/${repo}#${pr.number}`, err);
    }
  }

  return reviewed;
}

async function pollAll(): Promise<void> {
  if (polling) return;
  polling = true;

  const reviewConfig = loadReviewConfig();
  const repos = (reviewConfig as any).watchRepos ?? [];

  let total = 0;
  for (const { owner, repo } of repos) {
    try {
      total += await pollRepo(owner, repo);
    } catch (err) {
      console.error(`[poller] Error polling ${owner}/${repo}:`, err);
    }
  }

  if (total > 0) {
    console.log(`[poller] Cycle done: ${total} new review(s)`);
  }

  polling = false;
}

let intervalId: ReturnType<typeof setInterval> | null = null;

export function startPoller(intervalMs: number = 60_000): void {
  const reviewConfig = loadReviewConfig();
  const repos = (reviewConfig as any).watchRepos ?? [];
  console.log(`[poller] Started (every ${intervalMs / 1000}s, ${repos.length} repos: ${repos.map((r: any) => r.repo).join(', ')})`);

  // Initial poll
  pollAll();

  intervalId = setInterval(pollAll, intervalMs);
}

export function stopPoller(): void {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
    console.log('[poller] Stopped');
  }
}

export function getPollerRepos(): { owner: string; repo: string }[] {
  const reviewConfig = loadReviewConfig();
  return (reviewConfig as any).watchRepos ?? [];
}
