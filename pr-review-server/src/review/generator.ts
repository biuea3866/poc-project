import { v4 as uuidv4 } from 'uuid';
import type {
  PRInfo, DiffData, ReviewResult, ReviewConfig, PipelineContext,
  ScopeAnalysis, ImpactAnalysis, QualityAnalysis, ReviewSummary,
  InlineComment, Severity, ReviewEvent, ImpactFinding, QualityFinding,
  ChangedFile, DesignPrinciple,
} from './types.js';
import { classifyPR } from './classifier.js';
import { analyzeDI } from './di-analyzer.js';
import { isLLMAvailable, analyzePRWithLLM, convertLLMResults } from './llm-analyzer.js';
import { loadBeImplementationContext, loadPRDContext, loadTicketContext } from './context/pipeline-loader.js';
import { resolveLinksFromPRBody } from './context/link-resolver.js';
import { matchDocuments } from './context/file-matcher.js';

// ── Stage 1: Scope Analysis ──────────────────────────────────────

function categorizeFileLayer(filename: string, layers: Record<string, string>): string {
  for (const [layerName, pathPrefix] of Object.entries(layers)) {
    if (filename.includes(pathPrefix)) return layerName;
  }
  if (filename.endsWith('.sql')) return 'migration';
  if (filename.includes('test/') || filename.includes('Test.kt') || filename.includes('.test.')) return 'test';
  if (filename.endsWith('.yml') || filename.endsWith('.yaml') || filename.endsWith('.properties')) return 'config';
  return 'other';
}

function calculateSize(additions: number, deletions: number, fileCount: number): ScopeAnalysis['size'] {
  const total = additions + deletions;
  if (total < 10 && fileCount <= 1) return 'XS';
  if (total < 50 && fileCount <= 3) return 'S';
  if (total < 200 && fileCount <= 10) return 'M';
  if (total < 500 && fileCount <= 20) return 'L';
  return 'XL';
}

function findMissingCompanionFiles(
  files: ChangedFile[],
  companionRules: Record<string, string[]>,
): string[] {
  const missing: string[] = [];
  const filenames = new Set(files.map(f => f.filename));

  for (const file of files) {
    if (file.status === 'removed') continue;
    const basename = file.filename.split('/').pop() ?? '';

    for (const [pattern, companions] of Object.entries(companionRules)) {
      if (!matchSimpleGlob(basename, pattern)) continue;

      for (const companionPattern of companions) {
        const expectedName = deriveCompanionName(basename, companionPattern);
        if (!expectedName) continue;

        const hasCompanion = Array.from(filenames).some(f => f.endsWith(expectedName));
        if (!hasCompanion) {
          missing.push(`${file.filename} → missing ${expectedName}`);
        }
      }
    }
  }

  return missing;
}

function matchSimpleGlob(name: string, pattern: string): boolean {
  if (pattern.startsWith('*')) return name.endsWith(pattern.slice(1));
  if (pattern.endsWith('*')) return name.startsWith(pattern.slice(0, -1));
  return name.includes(pattern.replace('*', ''));
}

function deriveCompanionName(basename: string, pattern: string): string | null {
  if (pattern === '*Test.kt' && basename.endsWith('.kt')) {
    return basename.replace('.kt', 'Test.kt');
  }
  if (pattern.endsWith('Test.kt') && basename.endsWith('.kt')) {
    const prefix = pattern.replace('Test.kt', '');
    const basePrefix = basename.replace('.kt', '');
    if (basename.endsWith(`${prefix}.kt`)) {
      return `${basePrefix}Test.kt`;
    }
  }
  return null;
}

function analyzeScope(
  pr: PRInfo,
  diff: DiffData,
  reviewConfig: ReviewConfig,
): ScopeAnalysis {
  const classification = classifyPR(pr, diff);

  const filesByLayer: Record<string, string[]> = {};
  for (const file of diff.files) {
    const layer = categorizeFileLayer(file.filename, reviewConfig.architecture.layers);
    if (!filesByLayer[layer]) filesByLayer[layer] = [];
    filesByLayer[layer].push(file.filename);
  }

  return {
    prType: classification.prType,
    filesByLayer,
    totalFiles: diff.files.length,
    totalAdditions: diff.totalAdditions,
    totalDeletions: diff.totalDeletions,
    size: calculateSize(diff.totalAdditions, diff.totalDeletions, diff.files.length),
    missingCompanionFiles: findMissingCompanionFiles(diff.files, reviewConfig.companionFiles),
  };
}

// ── Stage 2: Impact Analysis ─────────────────────────────────────

function analyzeImpact(
  pr: PRInfo,
  diff: DiffData,
  reviewConfig: ReviewConfig,
  scope: ScopeAnalysis,
): ImpactAnalysis {
  const findings: ImpactFinding[] = [];
  const affectedCallers: string[] = [];
  const crossRepoImpacts: { repo: string; description: string }[] = [];

  // Check cross-repo impact
  const relatedRepos = reviewConfig.crossRepo[pr.repo] ?? [];
  if (relatedRepos.length > 0) {
    const hasApiChanges = diff.files.some(f =>
      f.filename.includes('Controller') || f.filename.includes('api/')
    );
    const hasEntityChanges = diff.files.some(f =>
      f.filename.includes('entity/') || f.filename.includes('/domain/model/')
    );
    const hasKafkaChanges = diff.files.some(f =>
      f.filename.includes('kafka/') || (f.patch?.includes('KafkaTemplate') ?? false)
    );

    if (hasApiChanges) {
      for (const repo of relatedRepos) {
        crossRepoImpacts.push({
          repo,
          description: `API 변경 → ${repo}의 FeignClient/API 호출 확인 필요`,
        });
      }
      findings.push({
        type: 'cross-repo',
        severity: 'P3',
        description: `API 변경이 ${relatedRepos.length}개 연관 레포에 영향 가능: ${relatedRepos.join(', ')}`,
      });
    }

    if (hasEntityChanges) {
      findings.push({
        type: 'caller-impact',
        severity: 'P3',
        description: '도메인 모델 변경 → 관련 서비스의 DTO/매핑 확인 필요',
      });
    }

    if (hasKafkaChanges) {
      findings.push({
        type: 'cross-repo',
        severity: 'P2',
        description: 'Kafka 이벤트 변경 → Producer/Consumer 양쪽 호환성 확인 필요',
      });
    }
  }

  // Missing companion files — only flag non-test companions (e.g., migration without entity)
  for (const missing of scope.missingCompanionFiles) {
    // Skip test file companions (convention, not business risk)
    if (missing.includes('Test.kt')) continue;
    findings.push({
      type: 'missing-change',
      severity: 'P3',
      description: `동반 파일 누락: ${missing}`,
    });
  }

  // Architecture layer violation check
  for (const file of diff.files) {
    if (file.patch && file.filename.includes('domain/') && file.patch.includes('import') && file.patch.includes('adaptor')) {
      findings.push({
        type: 'architecture-violation',
        severity: 'P2',
        description: `도메인 레이어에서 어댑터 직접 참조: ${file.filename}`,
        file: file.filename,
      });
    }
  }

  return { findings, affectedCallers, crossRepoImpacts };
}

// ── Stage 3: Quality Analysis ────────────────────────────────────

function checkDesignPrinciples(
  diff: DiffData,
  principles: DesignPrinciple[],
): QualityFinding[] {
  const findings: QualityFinding[] = [];

  for (const file of diff.files) {
    if (!file.patch) continue;

    // Skip test files — test code is reviewed by LLM for scenarios/efficiency, not by regex rules
    if (file.filename.includes('/test/') || file.filename.includes('Test.kt')) continue;

    // Only check added/modified lines
    const addedLines = file.patch
      .split('\n')
      .filter(line => line.startsWith('+') && !line.startsWith('+++'))
      .map(line => line.slice(1))
      .join('\n');

    if (!addedLines.trim()) continue;

    for (const principle of principles) {
      if (!principle.filePattern.test(file.filename)) continue;
      if (!principle.violationPattern.test(addedLines)) continue;

      // Find the specific line number
      let lineNumber: number | undefined;
      const patchLines = file.patch.split('\n');
      let currentLine = 0;
      for (const patchLine of patchLines) {
        const hunkMatch = patchLine.match(/^@@\s+-\d+(?:,\d+)?\s+\+(\d+)/);
        if (hunkMatch) {
          currentLine = parseInt(hunkMatch[1], 10) - 1;
          continue;
        }
        if (patchLine.startsWith('-')) continue;
        currentLine++;
        if (patchLine.startsWith('+') && principle.violationPattern.test(patchLine.slice(1))) {
          lineNumber = currentLine;
          break;
        }
      }

      findings.push({
        type: 'design-principle',
        ruleId: principle.ruleId,
        severity: principle.severity,
        description: `[${principle.category}] ${principle.name}: ${principle.description}`,
        file: file.filename,
        line: lineNumber,
        suggestion: principle.suggestion,
      });
    }
  }

  return findings;
}

function analyzeQuality(
  diff: DiffData,
  context: PipelineContext,
): QualityAnalysis {
  const principleFindings = checkDesignPrinciples(diff, context.designPrinciples);

  let designPrincipleViolations = 0;
  let securityIssues = 0;
  let performanceIssues = 0;

  for (const f of principleFindings) {
    if (f.ruleId?.startsWith('security.')) securityIssues++;
    else if (f.ruleId?.startsWith('perf.')) performanceIssues++;
    else designPrincipleViolations++;
  }

  return {
    findings: principleFindings,
    designPrincipleViolations,
    securityIssues,
    performanceIssues,
  };
}

// ── Stage 4: Summary ─────────────────────────────────────────────

function generateSummary(
  impact: ImpactAnalysis,
  quality: QualityAnalysis,
): ReviewSummary {
  const severityCounts: Record<Severity, number> = { P1: 0, P2: 0, P3: 0, P4: 0, P5: 0, ASK: 0 };
  const findings: Record<Severity, string[]> = { P1: [], P2: [], P3: [], P4: [], P5: [], ASK: [] };

  const allFindings = [
    ...impact.findings.map(f => ({ severity: f.severity, desc: f.description })),
    ...quality.findings.map(f => ({ severity: f.severity, desc: f.description })),
  ];

  for (const finding of allFindings) {
    severityCounts[finding.severity]++;
    findings[finding.severity].push(finding.desc);
  }

  return { severityCounts, findings };
}

function determineEvent(summary: ReviewSummary): ReviewEvent {
  if (summary.severityCounts.P1 > 0 || summary.severityCounts.P2 > 0) return 'REQUEST_CHANGES';
  if (summary.severityCounts.P3 > 0) return 'COMMENT';
  return 'APPROVE';
}

function buildInlineComments(
  impact: ImpactAnalysis,
  quality: QualityAnalysis,
): InlineComment[] {
  const comments: InlineComment[] = [];

  for (const finding of quality.findings) {
    const body = finding.suggestion
      ? `${finding.description}\n\n💡 ${finding.suggestion}`
      : finding.description;

    comments.push({
      id: uuidv4(),
      path: finding.file,
      line: finding.line ?? 1,
      body,
      severity: finding.severity,
      source: 'auto',
      category: finding.ruleId ?? finding.type,
    });
  }

  for (const finding of impact.findings) {
    if (finding.file) {
      comments.push({
        id: uuidv4(),
        path: finding.file,
        line: finding.line ?? 1,
        body: finding.description,
        severity: finding.severity,
        source: 'auto',
        category: finding.type,
      });
    }
  }

  return comments;
}

function buildReviewBody(
  pr: PRInfo,
  scope: ScopeAnalysis,
  summary: ReviewSummary,
  impact: ImpactAnalysis,
): string {
  const lines: string[] = [];

  lines.push(`## PR Review: ${pr.title}`);
  lines.push('');
  lines.push(`**유형**: ${scope.prType} | **크기**: ${scope.size} (+${scope.totalAdditions} -${scope.totalDeletions}) | **파일**: ${scope.totalFiles}`);
  lines.push('');

  // Severity summary
  const counts = summary.severityCounts;
  const parts: string[] = [];
  if (counts.P1) parts.push(`P1: ${counts.P1}`);
  if (counts.P2) parts.push(`P2: ${counts.P2}`);
  if (counts.P3) parts.push(`P3: ${counts.P3}`);
  if (counts.P4) parts.push(`P4: ${counts.P4}`);
  if (counts.P5) parts.push(`P5: ${counts.P5}`);
  if (counts.ASK) parts.push(`ASK: ${counts.ASK}`);
  if (parts.length > 0) {
    lines.push(`**심각도**: ${parts.join(' | ')}`);
    lines.push('');
  }

  for (const sev of ['P1', 'P2', 'P3', 'P4', 'P5', 'ASK'] as Severity[]) {
    const items = summary.findings[sev];
    if (items.length === 0) continue;
    lines.push(`### ${sev} (${items.length}건)`);
    for (const item of items) {
      lines.push(`- ${item}`);
    }
    lines.push('');
  }

  if (impact.crossRepoImpacts.length > 0) {
    lines.push('### Cross-Repo');
    for (const cr of impact.crossRepoImpacts) {
      lines.push(`- **${cr.repo}**: ${cr.description}`);
    }
    lines.push('');
  }

  lines.push('---');
  lines.push('*Generated by PR Review Server (pr-review pipeline)*');

  return lines.join('\n');
}

// ── Main Pipeline ────────────────────────────────────────────────

function extractTicketFromBranch(branch: string): string | null {
  const match = branch.match(/(?:grt|GRT)-(\d+)/i);
  return match ? match[1] : null;
}

export async function generateReview(
  pr: PRInfo,
  diff: DiffData,
  reviewConfig: ReviewConfig,
  context: PipelineContext,
): Promise<ReviewResult> {
  console.log(`[generator] Starting review for ${pr.owner}/${pr.repo}#${pr.number}`);

  // Stage 1: Scope
  const scope = analyzeScope(pr, diff, reviewConfig);
  console.log(`[generator] Stage 1 (Scope): ${scope.prType}, ${scope.size}, ${scope.totalFiles} files`);

  // Stage 2: Impact (static)
  const impact = analyzeImpact(pr, diff, reviewConfig, scope);
  console.log(`[generator] Stage 2 (Impact): ${impact.findings.length} findings`);

  // Stage 3a: DI 정적 분석
  const diGraph = analyzeDI(diff.files);
  console.log(`[generator] Stage 3a (DI): ${diGraph.classes.length} classes, ${diGraph.violations.length} violations`);

  // Stage 3b: 비즈니스 컨텍스트 — 3단계 폴백
  //   1순위: 변경 파일 기반 .analysis/ 문서 자동 매칭
  //   2순위: 브랜치 티켓번호 기반 로컬 파일 탐색
  //   3순위: PR 본문 링크 (Jira/Confluence) 에서 가져옴
  const ticketNum = extractTicketFromBranch(pr.branch);
  let prdContent: string | null = null;
  let tddContent: string | null = null;
  let ticketsContent: string | null = null;

  // 1순위: 변경 파일 목록 기반 자동 매칭
  const changedFilenames = diff.files.map(f => f.filename);
  const matched = matchDocuments(changedFilenames);
  if (matched.prd || matched.tdd || matched.tickets) {
    prdContent = matched.prd;
    tddContent = matched.tdd;
    ticketsContent = matched.tickets;
    console.log(`[generator] Stage 3b: 파일 매칭으로 ${matched.matches.length}개 문서 발견`);
  }

  // 2순위: 브랜치 티켓번호 기반 로컬 파일
  if (!prdContent && !tddContent && !ticketsContent && ticketNum) {
    tddContent = loadBeImplementationContext();
    prdContent = loadPRDContext();
    ticketsContent = loadTicketContext(ticketNum);
    if (prdContent || tddContent || ticketsContent) {
      console.log(`[generator] Stage 3b: 티켓번호(${ticketNum}) 기반 로컬 파일 발견`);
    }
  }

  // 3순위: PR 본문 링크에서 가져옴
  if (!prdContent && !tddContent && !ticketsContent && pr.body) {
    console.log(`[generator] Stage 3b: 로컬 컨텍스트 없음 → PR 본문 링크에서 가져오는 중...`);
    try {
      const resolved = await resolveLinksFromPRBody(pr.body);
      if (resolved.prd) prdContent = resolved.prd;
      if (resolved.tdd) tddContent = resolved.tdd;
      if (resolved.tickets) ticketsContent = resolved.tickets;
    } catch (err) {
      console.warn(`[generator] Link resolution failed:`, err);
    }
  }

  if (ticketsContent) {
    tddContent = tddContent
      ? `${tddContent}\n\n---\n\n## 관련 티켓\n${ticketsContent}`
      : ticketsContent;
  }

  const contextSources: string[] = [];
  if (prdContent) contextSources.push('PRD');
  if (tddContent) contextSources.push('TDD');
  if (ticketsContent) contextSources.push('Tickets');
  console.log(`[generator] Stage 3b (Context): ${contextSources.length > 0 ? contextSources.join(', ') : 'none'}`);

  // Stage 3c: Quality — LLM or regex fallback
  let quality: QualityAnalysis;
  let llmComments: InlineComment[] = [];

  if (await isLLMAvailable()) {
    console.log(`[generator] Stage 3c (LLM): Using Claude API for analysis...`);
    try {
      const llmResult = await analyzePRWithLLM(pr, diff, reviewConfig, context, diGraph, prdContent, tddContent);
      const converted = convertLLMResults(llmResult);
      llmComments = converted.comments;

      quality = {
        findings: converted.findings,
        designPrincipleViolations: converted.findings.filter(f => !f.ruleId?.startsWith('llm.security') && !f.ruleId?.startsWith('llm.perf')).length,
        securityIssues: converted.findings.filter(f => f.ruleId?.includes('security') || f.ruleId?.includes('보안')).length,
        performanceIssues: converted.findings.filter(f => f.ruleId?.includes('perf') || f.ruleId?.includes('성능')).length,
      };

      // Append AC verification if available
      if (llmResult.acVerification) {
        console.log(`[generator] AC Verification: ${llmResult.acVerification.slice(0, 100)}...`);
      }

      console.log(`[generator] Stage 3c (LLM): ${quality.findings.length} findings, ${llmComments.length} comments`);
    } catch (error) {
      console.error(`[generator] LLM analysis failed, falling back to regex:`, error);
      quality = analyzeQuality(diff, context);
      // Add DI violations as additional findings
      for (const v of diGraph.violations) {
        quality.findings.push({
          type: 'design-principle',
          ruleId: `di.${v.type}`,
          severity: v.type === 'cross-bc-dependency' ? 'ASK' : 'P2',
          description: v.description,
          file: v.file,
          suggestion: v.evidence,
        });
      }
    }
  } else {
    console.log(`[generator] Stage 3c (Regex): LLM not available, using regex fallback`);
    quality = analyzeQuality(diff, context);
    // Add DI violations
    for (const v of diGraph.violations) {
      quality.findings.push({
        type: 'design-principle',
        ruleId: `di.${v.type}`,
        severity: v.type === 'cross-bc-dependency' ? 'ASK' : 'P2',
        description: v.description,
        file: v.file,
        suggestion: v.evidence,
      });
    }
  }

  // Stage 4: Summary
  const summary = generateSummary(impact, quality);
  const event = determineEvent(summary);
  const inlineComments = llmComments.length > 0
    ? llmComments
    : buildInlineComments(impact, quality);
  const reviewBody = buildReviewBody(pr, scope, summary, impact);
  console.log(`[generator] Stage 4 (Summary): ${event}, ${inlineComments.length} inline comments`);

  const changedFiles = diff.files.map(f => ({
    filename: f.filename,
    status: f.status,
    additions: f.additions,
    deletions: f.deletions,
    patch: f.patch,
  }));

  return {
    id: uuidv4(),
    prInfo: pr,
    scope,
    impact,
    quality,
    summary,
    inlineComments,
    changedFiles,
    reviewBody,
    event,
    createdAt: new Date().toISOString(),
    status: 'pending',
  };
}
