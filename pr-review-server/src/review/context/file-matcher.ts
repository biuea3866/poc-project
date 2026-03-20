import fs from 'fs';
import path from 'path';
import { config } from '../../config.js';

export interface MatchedDocument {
  filePath: string;
  type: 'ticket' | 'tdd' | 'gap_analysis' | 'prd';
  projectName: string;
  score: number;
  matchedFiles: string[];
}

interface ParsedTicket {
  filePath: string;
  projectName: string;
  mentionedFiles: string[];
  content: string;
}

/**
 * Scan all .analysis/ documents and find the ones most relevant to the PR's changed files.
 */
export function matchDocuments(changedFiles: string[]): {
  prd: string | null;
  tdd: string | null;
  tickets: string | null;
  matches: MatchedDocument[];
} {
  if (changedFiles.length === 0) return { prd: null, tdd: null, tickets: null, matches: [] };

  // Normalize changed file paths — extract just the meaningful parts
  const changedParts = changedFiles.map(f => extractFileSignature(f));

  // Scan all analysis results
  const allDocs = scanAnalysisResults();
  const scored = scoreDocuments(allDocs, changedParts, changedFiles);

  // Sort by score descending
  scored.sort((a, b) => b.score - a.score);

  const topMatches = scored.filter(m => m.score > 0);
  if (topMatches.length === 0) {
    return { prd: null, tdd: null, tickets: null, matches: [] };
  }

  console.log(`[file-matcher] Found ${topMatches.length} matching documents:`);
  for (const m of topMatches.slice(0, 5)) {
    console.log(`  [${m.type}] ${path.basename(m.filePath)} (score: ${m.score}, matched: ${m.matchedFiles.length} files)`);
  }

  // Collect content by type
  let prd: string | null = null;
  let tdd: string | null = null;
  const ticketContents: string[] = [];

  // Use the best matching project
  const bestProject = topMatches[0].projectName;

  for (const match of topMatches) {
    if (match.projectName !== bestProject) continue;

    const content = fs.readFileSync(match.filePath, 'utf-8');
    switch (match.type) {
      case 'prd':
      case 'gap_analysis':
        prd = prd ? `${prd}\n\n---\n\n${content}` : content;
        break;
      case 'tdd':
        tdd = tdd ? `${tdd}\n\n---\n\n${content}` : content;
        break;
      case 'ticket':
        ticketContents.push(content);
        break;
    }
  }

  const tickets = ticketContents.length > 0 ? ticketContents.join('\n\n---\n\n') : null;

  return { prd, tdd, tickets, matches: topMatches };
}

// ── Internal ─────────────────────────────────────────────────────

/**
 * Extract a recognizable signature from a file path.
 * e.g. "domain/src/main/.../EvaluationService.kt" → ["EvaluationService", "evaluation"]
 */
function extractFileSignature(filepath: string): string[] {
  const parts: string[] = [];

  // Class/file name without extension
  const basename = filepath.split('/').pop()?.replace(/\.\w+$/, '') ?? '';
  if (basename) parts.push(basename.toLowerCase());

  // Domain/package segments
  const domainMatch = filepath.match(/doodlin\/greeting\/(\w+)\//);
  if (domainMatch) parts.push(domainMatch[1].toLowerCase());

  // Key path segments
  const segments = filepath.split('/');
  for (const seg of segments) {
    if (['service', 'entity', 'controller', 'facade', 'repository', 'event', 'adaptor'].includes(seg)) {
      parts.push(seg.toLowerCase());
    }
  }

  return parts;
}

function scanAnalysisResults(): ParsedTicket[] {
  const docs: ParsedTicket[] = [];
  const analysisDir = config.analysisDir;

  // Scan be-implementation/results/*/
  scanDirectory(path.join(analysisDir, 'be-implementation', 'results'), docs);
  // Scan implementation/results/*/
  scanDirectory(path.join(analysisDir, 'implementation', 'results'), docs);
  // Scan prd/results/*/
  scanDirectory(path.join(analysisDir, 'prd', 'results'), docs);

  return docs;
}

function scanDirectory(dir: string, docs: ParsedTicket[]): void {
  if (!fs.existsSync(dir)) return;

  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);

    if (entry.isDirectory()) {
      const projectName = entry.name;
      scanProjectDir(fullPath, projectName, docs);
    } else if (entry.name.endsWith('.md')) {
      const content = fs.readFileSync(fullPath, 'utf-8');
      const mentionedFiles = extractMentionedFiles(content);
      docs.push({
        filePath: fullPath,
        projectName: 'root',
        mentionedFiles,
        content,
      });
    }
  }
}

function scanProjectDir(dir: string, projectName: string, docs: ParsedTicket[]): void {
  const entries = fs.readdirSync(dir, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);

    if (entry.isDirectory() && entry.name === 'tickets') {
      // Scan tickets subdirectory
      const ticketFiles = fs.readdirSync(fullPath).filter(f => f.endsWith('.md'));
      for (const tf of ticketFiles) {
        const ticketPath = path.join(fullPath, tf);
        const content = fs.readFileSync(ticketPath, 'utf-8');
        docs.push({
          filePath: ticketPath,
          projectName,
          mentionedFiles: extractMentionedFiles(content),
          content,
        });
      }
    } else if (entry.name.endsWith('.md')) {
      const content = fs.readFileSync(fullPath, 'utf-8');
      docs.push({
        filePath: fullPath,
        projectName,
        mentionedFiles: extractMentionedFiles(content),
        content,
      });
    }
  }
}

/**
 * Extract file paths mentioned in a markdown document.
 * Looks for:
 * - Kotlin class names: SomethingService, SomethingController, etc.
 * - File paths in tables: | repo | module | path |
 * - Code blocks with file references
 * - Package names: doodlin.greeting.candidate.xxx
 */
function extractMentionedFiles(content: string): string[] {
  const mentioned: Set<string> = new Set();

  // Kotlin class names (PascalCase ending with known suffixes)
  const classRegex = /\b([A-Z]\w+(?:Service|Controller|Facade|Repository|Entity|UseCase|Handler|Listener|Event|Tasklet|Config|Adaptor|Reader|Store))\b/g;
  for (const match of content.matchAll(classRegex)) {
    mentioned.add(match[1].toLowerCase());
  }

  // File paths in backticks or table cells
  const pathRegex = /[`|]\s*([\w/.-]+\.(?:kt|java|sql|yml|yaml|xml|json))\s*[`|]/g;
  for (const match of content.matchAll(pathRegex)) {
    const basename = match[1].split('/').pop()?.replace(/\.\w+$/, '') ?? '';
    if (basename) mentioned.add(basename.toLowerCase());
  }

  // Package references: doodlin.greeting.xxx
  const pkgRegex = /doodlin\.greeting\.(\w+)/g;
  for (const match of content.matchAll(pkgRegex)) {
    mentioned.add(match[1].toLowerCase());
  }

  // Table rows with file paths
  const tablePathRegex = /\|\s*[\w-]+\s*\|\s*[\w-]+\s*\|\s*([\w/.]+\.kt)\s*\|/g;
  for (const match of content.matchAll(tablePathRegex)) {
    const basename = match[1].split('/').pop()?.replace('.kt', '') ?? '';
    if (basename) mentioned.add(basename.toLowerCase());
  }

  return Array.from(mentioned);
}

function scoreDocuments(
  docs: ParsedTicket[],
  changedSignatures: string[][],
  changedFiles: string[],
): MatchedDocument[] {
  return docs.map(doc => {
    const matchedFiles: string[] = [];
    let score = 0;

    const docMentioned = new Set(doc.mentionedFiles);

    for (let i = 0; i < changedSignatures.length; i++) {
      const sigs = changedSignatures[i];
      for (const sig of sigs) {
        if (docMentioned.has(sig)) {
          score++;
          matchedFiles.push(changedFiles[i]);
          break;
        }
      }
    }

    // Bonus: document content mentions domain paths from changed files
    const contentLower = doc.content.toLowerCase();
    for (const file of changedFiles) {
      const domain = file.match(/doodlin\/greeting\/(\w+)\//)?.[1]?.toLowerCase();
      if (domain && contentLower.includes(domain)) {
        score += 0.5;
      }
    }

    // Determine document type
    const basename = path.basename(doc.filePath).toLowerCase();
    let type: MatchedDocument['type'] = 'ticket';
    if (basename.includes('tdd')) type = 'tdd';
    else if (basename.includes('gap_analysis')) type = 'gap_analysis';
    else if (basename.includes('prd') || doc.filePath.includes('/prd/')) type = 'prd';
    else if (basename.startsWith('ticket')) type = 'ticket';
    else if (basename.includes('overview')) type = 'ticket';
    else type = 'tdd'; // Default for non-ticket docs in be-implementation

    return {
      filePath: doc.filePath,
      type,
      projectName: doc.projectName,
      score,
      matchedFiles,
    };
  });
}
