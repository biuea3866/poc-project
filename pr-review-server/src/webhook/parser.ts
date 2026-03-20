import type { PRInfo } from '../review/types.js';

export function parsePullRequestPayload(payload: any): PRInfo {
  const pr = payload.pull_request;
  const repo = payload.repository;

  return {
    owner: repo.owner.login,
    repo: repo.name,
    number: pr.number,
    title: pr.title,
    body: pr.body ?? '',
    branch: pr.head.ref,
    baseBranch: pr.base.ref,
    author: pr.user.login,
    action: payload.action,
    url: pr.html_url,
  };
}
