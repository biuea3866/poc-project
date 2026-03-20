import { getOctokit, getAuthenticatedUsername } from './client.js';
import type { PRInfo, ReviewResult, InlineComment, ReviewEvent } from '../review/types.js';

interface GitHubComment {
  path: string;
  line: number;
  start_line?: number;
  body: string;
}

function mapToGitHubComments(comments: InlineComment[]): GitHubComment[] {
  return comments
    .filter(c => c.path && c.line > 0)
    .map(c => {
      const ghComment: GitHubComment = {
        path: c.path,
        line: c.line,
        body: c.body,
      };
      if (c.startLine && c.startLine !== c.line) {
        ghComment.start_line = c.startLine;
      }
      return ghComment;
    });
}

export async function postReviewToGitHub(
  pr: PRInfo,
  review: ReviewResult,
): Promise<{ posted: boolean; message: string }> {
  const octokit = getOctokit();

  let event: ReviewEvent = review.event;

  // Self-PR: cannot APPROVE own PR → fallback to COMMENT
  const authenticatedUser = await getAuthenticatedUsername();
  if (pr.author === authenticatedUser && event === 'APPROVE') {
    event = 'COMMENT';
    console.log(`[reviewer] Self-PR detected, falling back APPROVE → COMMENT`);
  }

  const ghComments = mapToGitHubComments(review.inlineComments);

  try {
    await octokit.pulls.createReview({
      owner: pr.owner,
      repo: pr.repo,
      pull_number: pr.number,
      body: review.reviewBody,
      event,
      comments: ghComments,
    });

    const commentCount = ghComments.length;
    const message = `Review posted: ${event} with ${commentCount} inline comment(s)`;
    console.log(`[reviewer] ${message}`);
    return { posted: true, message };
  } catch (error: any) {
    const message = `Failed to post review: ${error.message}`;
    console.error(`[reviewer] ${message}`);

    // If comments fail (e.g., line out of range), retry without inline comments
    if (ghComments.length > 0 && error.status === 422) {
      console.log('[reviewer] Retrying without inline comments...');
      try {
        await octokit.pulls.createReview({
          owner: pr.owner,
          repo: pr.repo,
          pull_number: pr.number,
          body: review.reviewBody + '\n\n> Note: Inline comments could not be posted (line mapping issue)',
          event,
          comments: [],
        });
        return { posted: true, message: 'Review posted without inline comments (line mapping issue)' };
      } catch (retryError: any) {
        return { posted: false, message: `Retry also failed: ${retryError.message}` };
      }
    }

    return { posted: false, message };
  }
}
