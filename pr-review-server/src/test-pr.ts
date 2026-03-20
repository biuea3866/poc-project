/**
 * Manual PR review test script.
 * Usage: npx tsx src/test-pr.ts <owner> <repo> <pr_number>
 */
import { config, validateConfig } from './config.js';
import { getOctokit } from './github/client.js';
import { fetchPRDiff } from './github/diff.js';
import { generateReview } from './review/generator.js';
import { loadReviewConfig } from './review/config.js';
import { loadPipelineContext } from './review/context/pipeline-loader.js';
import { initStore, saveReview } from './review/store.js';
import type { PRInfo } from './review/types.js';

validateConfig();

const [owner, repo, prNumberStr] = process.argv.slice(2);
if (!owner || !repo || !prNumberStr) {
  console.error('Usage: npx tsx src/test-pr.ts <owner> <repo> <pr_number>');
  process.exit(1);
}

const prNumber = parseInt(prNumberStr, 10);

async function main() {
  console.log(`\nFetching PR: ${owner}/${repo}#${prNumber}...\n`);

  const octokit = getOctokit();
  const { data: pr } = await octokit.pulls.get({ owner, repo, pull_number: prNumber });

  const prInfo: PRInfo = {
    owner,
    repo,
    number: prNumber,
    title: pr.title,
    body: pr.body ?? '',
    branch: pr.head.ref,
    baseBranch: pr.base.ref,
    author: pr.user?.login ?? '',
    action: 'opened',
    url: pr.html_url,
  };

  console.log(`PR: ${prInfo.title}`);
  console.log(`Branch: ${prInfo.branch} → ${prInfo.baseBranch}`);
  console.log(`Author: ${prInfo.author}\n`);

  const diff = await fetchPRDiff(prInfo);
  console.log(`Files: ${diff.files.length}, +${diff.totalAdditions} -${diff.totalDeletions}\n`);

  const reviewConfig = loadReviewConfig();
  const context = loadPipelineContext();

  const review = await generateReview(prInfo, diff, reviewConfig, context);

  initStore();
  const id = saveReview(review);

  console.log(`\n${'='.repeat(60)}`);
  console.log(`Review saved: ${id}`);
  console.log(`Event: ${review.event}`);
  console.log(`Inline comments: ${review.inlineComments.length}`);
  console.log(`Severity: P1=${review.summary.severityCounts.P1} P2=${review.summary.severityCounts.P2} P3=${review.summary.severityCounts.P3} P4=${review.summary.severityCounts.P4} P5=${review.summary.severityCounts.P5} ASK=${review.summary.severityCounts.ASK}`);
  console.log(`${'='.repeat(60)}\n`);
  console.log(review.reviewBody);
  console.log(`\nResults saved to: ${config.resultsDir}`);
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
