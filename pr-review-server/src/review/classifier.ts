import type { PRInfo, DiffData, PRType, ChangedFile } from './types.js';

interface ClassificationResult {
  prType: PRType;
  runImpact: boolean;
  runQuality: boolean;
  crossRepoEmphasis: boolean;
}

function classifyByBranch(branch: string): PRType | null {
  if (branch.startsWith('fix/') || branch.startsWith('hotfix/')) return 'bugfix';
  if (branch.startsWith('feature/')) return 'feature';
  if (branch.startsWith('refactor/')) return 'refactor';
  if (branch.startsWith('docs/')) return 'docs';
  if (branch.startsWith('chore/') || branch.startsWith('ci/')) return 'config';
  return null;
}

function classifyByFiles(files: ChangedFile[]): PRType {
  const filenames = files.map(f => f.filename);

  const allDocs = filenames.every(f => f.endsWith('.md') || f.endsWith('.txt') || f.endsWith('.adoc'));
  if (allDocs) return 'docs';

  const allConfig = filenames.every(f =>
    f.endsWith('.yml') || f.endsWith('.yaml') || f.endsWith('.properties') ||
    f.endsWith('.json') || f.includes('Dockerfile') || f.includes('.github/')
  );
  if (allConfig) return 'config';

  const hasSharedLib = filenames.some(f =>
    f.includes('doodlin-commons') || f.includes('spring-kafka') || f.includes('doodlin-communication')
  );
  if (hasSharedLib) return 'shared';

  const totalLines = files.reduce((sum, f) => sum + f.additions + f.deletions, 0);
  if (files.length === 1 && totalLines < 50) return 'simple';

  return 'feature';
}

export function classifyPR(pr: PRInfo, diff: DiffData): ClassificationResult {
  const branchType = classifyByBranch(pr.branch);
  const fileType = classifyByFiles(diff.files);
  const prType = branchType ?? fileType;

  const hasMultiRepoIndicators = diff.files.some(f =>
    f.filename.includes('FeignClient') ||
    f.filename.includes('InternalController') ||
    f.filename.includes('adaptor/http')
  );

  const assignmentRules: Record<PRType, { impact: boolean; quality: boolean }> = {
    simple: { impact: false, quality: false },
    feature: { impact: true, quality: true },
    refactor: { impact: true, quality: false },
    bugfix: { impact: false, quality: true },
    shared: { impact: true, quality: true },
    config: { impact: true, quality: false },
    docs: { impact: false, quality: false },
  };

  const assignment = assignmentRules[prType];

  return {
    prType,
    runImpact: assignment.impact,
    runQuality: assignment.quality,
    crossRepoEmphasis: hasMultiRepoIndicators || prType === 'shared',
  };
}
