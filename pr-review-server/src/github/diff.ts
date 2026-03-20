import { getOctokit } from './client.js';
import type { PRInfo, ChangedFile, FileDiff, DiffHunk, DiffData } from '../review/types.js';

function parseHunks(patch: string): DiffHunk[] {
  const hunks: DiffHunk[] = [];
  const hunkRegex = /^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@(.*)$/gm;
  let match: RegExpExecArray | null;
  const lines = patch.split('\n');
  let currentHunk: DiffHunk | null = null;
  const hunkContentLines: string[] = [];

  for (const line of lines) {
    const hunkMatch = line.match(/^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@/);
    if (hunkMatch) {
      if (currentHunk) {
        currentHunk.content = hunkContentLines.join('\n');
        hunks.push(currentHunk);
        hunkContentLines.length = 0;
      }
      currentHunk = {
        oldStart: parseInt(hunkMatch[1], 10),
        oldLines: parseInt(hunkMatch[2] ?? '1', 10),
        newStart: parseInt(hunkMatch[3], 10),
        newLines: parseInt(hunkMatch[4] ?? '1', 10),
        content: '',
      };
    } else if (currentHunk) {
      hunkContentLines.push(line);
    }
  }

  if (currentHunk) {
    currentHunk.content = hunkContentLines.join('\n');
    hunks.push(currentHunk);
  }

  return hunks;
}

export async function fetchPRDiff(pr: PRInfo): Promise<DiffData> {
  const octokit = getOctokit();

  const allFiles: ChangedFile[] = [];
  let page = 1;
  while (true) {
    const { data: files } = await octokit.pulls.listFiles({
      owner: pr.owner,
      repo: pr.repo,
      pull_number: pr.number,
      per_page: 100,
      page,
    });

    if (files.length === 0) break;

    for (const f of files) {
      allFiles.push({
        filename: f.filename,
        status: f.status as ChangedFile['status'],
        additions: f.additions,
        deletions: f.deletions,
        changes: f.changes,
        patch: f.patch,
        previousFilename: f.previous_filename,
      });
    }

    if (files.length < 100) break;
    page++;
  }

  const fileDiffs: FileDiff[] = allFiles
    .filter(f => f.patch)
    .map(f => ({
      filename: f.filename,
      hunks: parseHunks(f.patch!),
      rawPatch: f.patch!,
    }));

  const totalAdditions = allFiles.reduce((sum, f) => sum + f.additions, 0);
  const totalDeletions = allFiles.reduce((sum, f) => sum + f.deletions, 0);

  return { files: allFiles, fileDiffs, totalAdditions, totalDeletions };
}
