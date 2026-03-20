import fs from 'fs';
import path from 'path';
import { config } from '../config.js';
import type { ReviewResult, InlineComment } from './types.js';

const index = new Map<string, string>();

function ensureDir(dir: string): void {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

/**
 * Extract ticket number from branch name.
 * e.g. "fix/grt-3381" → "grt-3381", "feature/grt-1234" → "grt-1234"
 * Falls back to "unknown" if no ticket pattern found.
 */
function extractTicket(branch: string): string {
  const match = branch.match(/(?:grt|GRT)-\d+/i);
  return match ? match[0].toLowerCase() : 'unknown';
}

/**
 * Directory structure: results/{ticket}/{repo}/
 * Files: pr{number}.json, pr{number}.md
 */
function buildDir(review: ReviewResult): string {
  const ticket = extractTicket(review.prInfo.branch);
  return path.join(config.resultsDir, ticket, review.prInfo.repo);
}

function buildJsonPath(review: ReviewResult): string {
  return path.join(buildDir(review), `pr${review.prInfo.number}.json`);
}

function buildMdPath(review: ReviewResult): string {
  return path.join(buildDir(review), `pr${review.prInfo.number}.md`);
}

function toMarkdown(review: ReviewResult): string {
  const { prInfo, scope, summary, impact, quality } = review;
  const ticket = extractTicket(prInfo.branch);

  const bySeverity: Record<string, typeof review.inlineComments> = {};
  for (const sev of ['P1','P2','P3','P4','P5','ASK']) {
    bySeverity[sev] = review.inlineComments.filter(c => c.severity === sev);
  }

  const formatComments = (comments: InlineComment[]): string => {
    if (comments.length === 0) return '> 없음\n';
    return comments.map((c, i) => {
      const loc = c.startLine ? `${c.path}:${c.startLine}-${c.line}` : `${c.path}:${c.line}`;
      return `**${i + 1}. [${c.severity}] ${loc}**\n${c.body}\n`;
    }).join('\n');
  };

  const crossRepoRows = impact.crossRepoImpacts.length > 0
    ? impact.crossRepoImpacts.map(cr => `| ${cr.repo} | ${cr.description} |`).join('\n')
    : '| - | 없음 |';

  return `# 코드 리뷰 보고서: ${ticket.toUpperCase()}

> 일시: ${review.createdAt}
> PR: ${prInfo.url}
> 레포: ${prInfo.owner}/${prInfo.repo}
> 티켓: ${ticket.toUpperCase()}
> 변경 파일: ${scope.totalFiles}개
> PR 유형: ${scope.prType}
> 크기: ${scope.size} (+${scope.totalAdditions} -${scope.totalDeletions})

---

## 1. 변경 요약

| 항목 | 내용 |
|------|------|
| 변경 유형 | ${scope.prType} |
| 핵심 변경 | ${prInfo.title} |
| 변경 파일 수 | ${scope.totalFiles} |
| 브랜치 | ${prInfo.branch} → ${prInfo.baseBranch} |

---

## 2. 영향 범위

### 크로스 레포
| 레포 | 영향 내용 |
|------|----------|
${crossRepoRows}

---

## 3. 리뷰 항목

${['P1','P2','P3','P4','P5','ASK'].map(sev => {
    const items = bySeverity[sev];
    if (items.length === 0) return '';
    return `### ${sev} (${items.length}건)\n${formatComments(items)}`;
  }).filter(Boolean).join('\n')}

---

## 4. 설계 원칙 검증

| 카테고리 | 위반 수 |
|---------|---------|
| 설계 원칙 | ${quality.designPrincipleViolations} |
| 보안 | ${quality.securityIssues} |
| 성능 | ${quality.performanceIssues} |

---

## 5. 심각도 요약

| P1 | P2 | P3 | P4 | P5 | ASK |
|----|----|----|----|----|-----|
| ${summary.severityCounts.P1 ?? 0} | ${summary.severityCounts.P2 ?? 0} | ${summary.severityCounts.P3 ?? 0} | ${summary.severityCounts.P4 ?? 0} | ${summary.severityCounts.P5 ?? 0} | ${summary.severityCounts.ASK ?? 0} |

**판정**: ${review.event}
`;
}

function buildReviewRequest(review: ReviewResult): string {
  const { prInfo, scope, impact, quality } = review;
  const ticket = extractTicket(prInfo.branch).toUpperCase();

  const staticFindings = [
    ...impact.findings.map(f => `- [${f.severity}] ${f.description}`),
    ...quality.findings.map(f => `- [${f.severity}] ${f.description}`),
  ].join('\n');

  const fileList = (review.changedFiles || [])
    .map(f => `- ${f.filename} (+${f.additions} -${f.deletions})`)
    .join('\n');

  return `# PR 리뷰 요청: ${ticket}

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr${prInfo.number}.json에 업데이트해주세요.

## PR 정보
- **제목**: ${prInfo.title}
- **URL**: ${prInfo.url}
- **브랜치**: ${prInfo.branch} → ${prInfo.baseBranch}
- **작성자**: ${prInfo.author}
- **유형**: ${scope.prType} | **크기**: ${scope.size}

## 변경 파일 (${scope.totalFiles}개, +${scope.totalAdditions} -${scope.totalDeletions})
${fileList}

## 정적 분석 결과 (자동)
${staticFindings || '없음'}

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr${prInfo.number}.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: \`gh pr diff ${prInfo.number} --repo ${prInfo.owner}/${prInfo.repo}\`
- 정적 분석: 같은 디렉토리의 \`pr${prInfo.number}.json\`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
`;
}

/**
 * Recursively scan results directory for .json files and build index.
 */
function refreshIndex(): void {
  ensureDir(config.resultsDir);
  index.clear();
  scanDir(config.resultsDir);
}

function scanDir(dir: string): void {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      scanDir(fullPath);
    } else if (entry.name.endsWith('.json')) {
      try {
        const review: ReviewResult = JSON.parse(fs.readFileSync(fullPath, 'utf-8'));
        if (review.id) {
          index.set(review.id, fullPath);
        }
      } catch {
        // skip malformed json
      }
    }
  }
}

export function initStore(): void {
  refreshIndex();
  console.log(`[store] Loaded ${index.size} existing reviews from ${config.resultsDir}`);
}

export function saveReview(review: ReviewResult): string {
  // Remove existing review for the same PR (prevent duplicates)
  refreshIndex();
  for (const [existingId, existingPath] of index) {
    try {
      const existing: ReviewResult = JSON.parse(fs.readFileSync(existingPath, 'utf-8'));
      if (
        existing.prInfo.owner === review.prInfo.owner &&
        existing.prInfo.repo === review.prInfo.repo &&
        existing.prInfo.number === review.prInfo.number
      ) {
        fs.unlinkSync(existingPath);
        const existingMd = existingPath.replace('.json', '.md');
        if (fs.existsSync(existingMd)) fs.unlinkSync(existingMd);
        index.delete(existingId);
        // Clean up empty directory
        const dirPath = path.dirname(existingPath);
        if (dirPath !== config.resultsDir && fs.readdirSync(dirPath).length === 0) {
          fs.rmdirSync(dirPath);
        }
        console.log(`[store] Replaced existing review for ${review.prInfo.repo}#${review.prInfo.number}`);
      }
    } catch {
      // skip
    }
  }

  const ticketDir = buildDir(review);
  ensureDir(ticketDir);

  const jsonPath = buildJsonPath(review);
  const mdPath = buildMdPath(review);

  fs.writeFileSync(jsonPath, JSON.stringify(review, null, 2), 'utf-8');
  fs.writeFileSync(mdPath, toMarkdown(review), 'utf-8');

  // Save review-request.md for Claude Code to pick up
  const requestPath = path.join(ticketDir, `pr${review.prInfo.number}-review-request.md`);
  fs.writeFileSync(requestPath, buildReviewRequest(review), 'utf-8');

  index.set(review.id, jsonPath);

  const ticket = extractTicket(review.prInfo.branch);
  console.log(`[store] Saved → ${ticket}/${review.prInfo.repo}/pr${review.prInfo.number}.json`);
  console.log(`[store] Review request → ${ticket}/${review.prInfo.repo}/pr${review.prInfo.number}-review-request.md`);
  return review.id;
}

export function getReview(id: string): ReviewResult | null {
  refreshIndex();
  const filePath = index.get(id);
  if (!filePath || !fs.existsSync(filePath)) return null;
  return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
}

export function listReviews(filter?: { repo?: string; status?: string }): ReviewResult[] {
  refreshIndex();
  const reviews: ReviewResult[] = [];
  for (const [, filePath] of index) {
    if (!fs.existsSync(filePath)) continue;
    const review: ReviewResult = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    if (filter?.repo && review.prInfo.repo !== filter.repo) continue;
    if (filter?.status && review.status !== filter.status) continue;
    reviews.push(review);
  }
  return reviews.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function updateReview(id: string, patch: Partial<ReviewResult>): void {
  const existing = getReview(id);
  if (!existing) throw new Error(`Review ${id} not found`);

  const updated = { ...existing, ...patch };
  const filePath = index.get(id)!;
  fs.writeFileSync(filePath, JSON.stringify(updated, null, 2), 'utf-8');

  const mdFile = filePath.replace('.json', '.md');
  fs.writeFileSync(mdFile, toMarkdown(updated), 'utf-8');
}

/**
 * Called when a PR is merged. Deletes the .md file (keeps .json for history).
 */
export function onPRMerged(owner: string, repo: string, prNumber: number): void {
  refreshIndex();
  for (const [, filePath] of index) {
    try {
      const review: ReviewResult = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
      if (review.prInfo.owner === owner && review.prInfo.repo === repo && review.prInfo.number === prNumber) {
        const mdPath = filePath.replace('.json', '.md');
        if (fs.existsSync(mdPath)) {
          fs.unlinkSync(mdPath);
          console.log(`[store] PR merged → deleted ${path.basename(mdPath)}`);
        }
      }
    } catch {
      // skip
    }
  }
}

export function getReviewByPR(owner: string, repo: string, prNumber: number): ReviewResult | null {
  refreshIndex();
  for (const [, filePath] of index) {
    if (!fs.existsSync(filePath)) continue;
    const review: ReviewResult = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    if (review.prInfo.owner === owner && review.prInfo.repo === repo && review.prInfo.number === prNumber) {
      return review;
    }
  }
  return null;
}

export function getAllReviewIds(): string[] {
  return Array.from(index.keys());
}
