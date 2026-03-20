export interface PRInfo {
  owner: string;
  repo: string;
  number: number;
  title: string;
  body: string;
  branch: string;
  baseBranch: string;
  author: string;
  action: string;
  url: string;
}

export interface ChangedFile {
  filename: string;
  status: 'added' | 'removed' | 'modified' | 'renamed' | 'copied' | 'changed' | 'unchanged';
  additions: number;
  deletions: number;
  changes: number;
  patch?: string;
  previousFilename?: string;
}

export interface DiffHunk {
  oldStart: number;
  oldLines: number;
  newStart: number;
  newLines: number;
  content: string;
}

export interface FileDiff {
  filename: string;
  hunks: DiffHunk[];
  rawPatch: string;
}

export interface DiffData {
  files: ChangedFile[];
  fileDiffs: FileDiff[];
  totalAdditions: number;
  totalDeletions: number;
}

export type PRType = 'simple' | 'feature' | 'refactor' | 'bugfix' | 'shared' | 'config' | 'docs';

export type Severity = 'P1' | 'P2' | 'P3' | 'P4' | 'P5' | 'ASK';

export type ReviewEvent = 'APPROVE' | 'COMMENT' | 'REQUEST_CHANGES';

export interface InlineComment {
  id: string;
  path: string;
  line: number;
  startLine?: number;
  body: string;
  severity: Severity;
  source: 'auto' | 'user';
  category?: string;
}

export interface ScopeAnalysis {
  prType: PRType;
  filesByLayer: Record<string, string[]>;
  totalFiles: number;
  totalAdditions: number;
  totalDeletions: number;
  size: 'XS' | 'S' | 'M' | 'L' | 'XL';
  missingCompanionFiles: string[];
}

export interface ImpactFinding {
  type: 'cross-repo' | 'caller-impact' | 'missing-change' | 'architecture-violation';
  severity: Severity;
  description: string;
  file?: string;
  line?: number;
}

export interface ImpactAnalysis {
  findings: ImpactFinding[];
  affectedCallers: string[];
  crossRepoImpacts: { repo: string; description: string }[];
}

export interface QualityFinding {
  type: 'design-principle' | 'security' | 'bug' | 'performance' | 'pattern' | 'test';
  ruleId?: string;
  severity: Severity;
  description: string;
  file: string;
  line?: number;
  suggestion?: string;
}

export interface QualityAnalysis {
  findings: QualityFinding[];
  designPrincipleViolations: number;
  securityIssues: number;
  performanceIssues: number;
}

export interface ReviewSummary {
  severityCounts: Record<Severity, number>;
  findings: Record<Severity, string[]>;
}

export interface ReviewChangedFile {
  filename: string;
  status: string;
  additions: number;
  deletions: number;
  patch?: string;
}

export interface ReviewResult {
  id: string;
  prInfo: PRInfo;
  scope: ScopeAnalysis;
  impact: ImpactAnalysis;
  quality: QualityAnalysis;
  summary: ReviewSummary;
  inlineComments: InlineComment[];
  changedFiles: ReviewChangedFile[];
  reviewBody: string;
  event: ReviewEvent;
  createdAt: string;
  status: 'pending' | 'posted';
}

export interface DesignPrinciple {
  category: string;
  ruleId: string;
  name: string;
  description: string;
  filePattern: RegExp;
  violationPattern: RegExp;
  severity: Severity;
  suggestion: string;
}

export interface ReviewConfig {
  architecture: {
    pattern: string;
    layers: Record<string, string>;
  };
  languageRules: Record<string, Record<string, boolean>>;
  crossRepo: Record<string, string[]>;
  companionFiles: Record<string, string[]>;
  ignorePaths: string[];
}

export interface PipelineContext {
  designPrinciples: DesignPrinciple[];
  reviewMethodology: string;
  implementationRules: string;
  verificationChecks: string;
}
