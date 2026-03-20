import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import os from 'os';
import type {
  PRInfo, DiffData, ReviewConfig, PipelineContext,
  InlineComment, QualityFinding, Severity,
} from './types.js';
import type { DIGraph } from './di-analyzer.js';
import { formatDIContext } from './di-analyzer.js';
import { v4 as uuidv4 } from 'uuid';

const CLAUDE_BIN = '/Users/biuea/.local/bin/claude';

export async function isLLMAvailable(): Promise<boolean> {
  // LLM은 Claude Code 대화에서 직접 수행. 서버에서는 정적 분석만.
  return false;
}

function runClaude(promptFile: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const proc = spawn(CLAUDE_BIN, [
      '-p',
      '--output-format', 'text',
      '--max-turns', '1',
    ], {
      timeout: 180_000,
      env: { ...process.env, CLAUDE_CODE_ENTRYPOINT: 'pr-review-server' },
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    let stdout = '';
    let stderr = '';

    proc.stdout.on('data', (data: Buffer) => { stdout += data.toString(); });
    proc.stderr.on('data', (data: Buffer) => { stderr += data.toString(); });

    proc.on('close', (code) => {
      if (code === 0) {
        resolve(stdout);
      } else {
        reject(new Error(`claude exited with code ${code}: ${stderr.slice(0, 500)}`));
      }
    });

    proc.on('error', reject);

    // Write prompt via stdin
    const content = fs.readFileSync(promptFile, 'utf-8');
    proc.stdin.write(content);
    proc.stdin.end();
  });
}

interface LLMComment {
  file: string;
  line: number | null;
  startLine: number | null;
  severity: string;
  body: string;
  category: string;
}

interface LLMReviewResult {
  comments: LLMComment[];
  summary: string;
  acVerification: string | null;
}

function buildDiffSummary(diff: DiffData): string {
  const fileSummaries = diff.files.map(f => {
    const patch = f.patch ?? '';
    const truncated = patch.length > 3000 ? patch.slice(0, 3000) + '\n... (truncated)' : patch;
    return `### ${f.filename} (${f.status}, +${f.additions} -${f.deletions})\n\`\`\`diff\n${truncated}\n\`\`\``;
  });
  return fileSummaries.join('\n\n');
}

function buildDesignPrinciplesContext(context: PipelineContext): string {
  const rules = context.designPrinciples.map(p =>
    `- [${p.category}] ${p.name}: ${p.description}`
  ).join('\n');
  return `## 리뷰 규칙\n${rules}`;
}

function buildPRDContext(prdContent: string | null, tddContent: string | null): string {
  if (!prdContent && !tddContent) return '';

  const parts: string[] = ['## 비즈니스 컨텍스트\n'];
  if (prdContent) {
    const truncated = prdContent.length > 5000 ? prdContent.slice(0, 5000) + '\n... (truncated)' : prdContent;
    parts.push(`### PRD\n${truncated}\n`);
  }
  if (tddContent) {
    const truncated = tddContent.length > 5000 ? tddContent.slice(0, 5000) + '\n... (truncated)' : tddContent;
    parts.push(`### TDD (기술 설계 문서)\n${truncated}\n`);
  }
  return parts.join('\n');
}

const SYSTEM_PROMPT = `당신은 Greeting 채용 플랫폼의 시니어 백엔드 개발자입니다.
PR 코드 리뷰를 수행합니다. 반드시 아래 JSON 형식으로만 응답하세요. JSON 외 다른 텍스트를 포함하지 마세요.

{
  "comments": [
    {
      "file": "파일 경로",
      "line": 라인번호 또는 null,
      "startLine": 시작라인 또는 null,
      "severity": "P1|P2|P3|P4|P5|ASK",
      "body": "리뷰 코멘트 (한국어, 대화체)",
      "category": "버그|성능|보안|비즈니스|누락|스타일"
    }
  ],
  "summary": "전체 리뷰 요약 (2-3줄)",
  "acVerification": "PRD/TDD AC 검증 결과 또는 null"
}

## 심각도
- P1: 런타임 에러, 데이터 손상/유실, 보안 취약점
- P2: 성능 저하, 동시성 이슈, 외부 장애 전파
- P3: 에러 핸들링 부족, 엣지 케이스 누락
- P4: 코드 스타일, 네이밍, 리팩토링 제안
- P5: 포매팅, import 정리, 사소한 개선
- ASK: 비즈니스 의도 확인 필요

## 리뷰 우선순위
1. 비즈니스 로직: PRD/TDD AC 충족 여부, 빠진 엣지 케이스, 상태 전이 누락
2. 버그: 동시성, 리소스 누수, 에러 삼킴, 데이터 유실 (!!은 논리적으로 타당하면 무시)
3. 성능: N+1, 페이지네이션 없음, 트랜잭션 내 외부 호출
4. 보안: SQL Injection, 권한 누락, 시크릿 노출
5. PRD/TDD 누락: 명시된 기능 미구현, API 스펙 불일치
6. 스타일 (P4-P5): 컨벤션, 네이밍, 포매팅 — 있으면 좋지만 강제 아님

## 테스트 코드 리뷰
테스트 코드(*Test.kt)는 아래 관점만 봐주세요:
- 비즈니스 요구사항 대비 빠진 시나리오가 있는지
- 효율적인 테스트인지 (중복, 불필요한 케이스)
- 놓친 엣지 케이스 (경계값, 동시성, 에러)
- 프레임워크 선택, 네이밍, 코드 스타일은 지적하지 마세요

## 문체
- 대화체 ("~해요", "~해주세요")
- 왜 문제인지 한 줄 + 어떻게 고치면 되는지 한 줄
- 불확실하면 ASK
- 확신 없는 추측은 하지 마세요`;

export async function analyzePRWithLLM(
  pr: PRInfo,
  diff: DiffData,
  reviewConfig: ReviewConfig,
  context: PipelineContext,
  diGraph: DIGraph,
  prdContent: string | null,
  tddContent: string | null,
): Promise<LLMReviewResult> {
  const userPrompt = `# PR 리뷰 요청

## PR 정보
- 제목: ${pr.title}
- 브랜치: ${pr.branch} → ${pr.baseBranch}
- 작성자: ${pr.author}
- 파일 수: ${diff.files.length}, +${diff.totalAdditions} -${diff.totalDeletions}

${buildDesignPrinciplesContext(context)}

${formatDIContext(diGraph)}

${buildPRDContext(prdContent, tddContent)}

## 변경 코드

${buildDiffSummary(diff)}`;

  const tmpDir = os.tmpdir();
  const promptFile = path.join(tmpDir, `pr-review-${pr.number}-${Date.now()}.md`);
  const fullPrompt = `${SYSTEM_PROMPT}\n\n---\n\n${userPrompt}`;
  fs.writeFileSync(promptFile, fullPrompt, 'utf-8');

  console.log(`[llm] Calling claude CLI via stdin (${(fullPrompt.length / 1024).toFixed(0)}KB prompt)...`);

  try {
    const stdout = await runClaude(promptFile);

    console.log(`[llm] Response received (${stdout.length} chars)`);
    fs.unlinkSync(promptFile);

    const jsonMatch = stdout.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      console.error('[llm] No JSON found in response');
      return { comments: [], summary: 'Claude 응답에서 JSON을 찾을 수 없음', acVerification: null };
    }
    return JSON.parse(jsonMatch[0]) as LLMReviewResult;
  } catch (err: any) {
    if (fs.existsSync(promptFile)) fs.unlinkSync(promptFile);
    console.error(`[llm] Claude CLI failed:`, err.message ?? err);
    throw err;
  }
}

/**
 * Convert LLM comments to InlineComment[] and QualityFinding[].
 */
export function convertLLMResults(
  llmResult: LLMReviewResult,
): { comments: InlineComment[]; findings: QualityFinding[] } {
  const comments: InlineComment[] = [];
  const findings: QualityFinding[] = [];

  for (const c of llmResult.comments) {
    const severity = (['P1', 'P2', 'P3', 'P4', 'P5', 'ASK'].includes(c.severity) ? c.severity : 'P3') as Severity;

    comments.push({
      id: uuidv4(),
      path: c.file,
      line: c.line ?? 1,
      startLine: c.startLine ?? undefined,
      body: c.body,
      severity,
      source: 'auto',
      category: c.category,
    });

    findings.push({
      type: 'design-principle',
      ruleId: `llm.${c.category}`,
      severity,
      description: c.body.split('\n')[0],
      file: c.file,
      line: c.line ?? undefined,
      suggestion: c.body.includes('\n') ? c.body.split('\n').slice(1).join('\n').trim() : undefined,
    });
  }

  return { comments, findings };
}
