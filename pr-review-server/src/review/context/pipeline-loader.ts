import fs from 'fs';
import path from 'path';
import { config } from '../../config.js';
import { DESIGN_PRINCIPLES } from './design-principles.js';
import type { PipelineContext } from '../types.js';

function loadMarkdown(relativePath: string): string {
  const fullPath = path.join(config.analysisDir, relativePath);
  if (!fs.existsSync(fullPath)) {
    console.warn(`[pipeline-loader] File not found: ${fullPath}`);
    return '';
  }
  return fs.readFileSync(fullPath, 'utf-8');
}

let cachedContext: PipelineContext | null = null;

export function loadPipelineContext(): PipelineContext {
  if (cachedContext) return cachedContext;

  const reviewMethodology = loadMarkdown('pr-review/PIPELINE.md');
  const implementationRules = loadMarkdown('implementation/PIPELINE.md');
  const verificationChecks = loadMarkdown('verification/PIPELINE.md');

  // Also load be-implementation for design principles reference
  const beImplementation = loadMarkdown('be-implementation/PIPELINE.md');

  cachedContext = {
    designPrinciples: DESIGN_PRINCIPLES,
    reviewMethodology,
    implementationRules,
    verificationChecks,
  };

  console.log(`[pipeline-loader] Loaded pipeline context:`);
  console.log(`  - Design principles: ${DESIGN_PRINCIPLES.length} rules`);
  console.log(`  - Review methodology: ${reviewMethodology.length > 0 ? 'OK' : 'MISSING'}`);
  console.log(`  - Implementation rules: ${implementationRules.length > 0 ? 'OK' : 'MISSING'}`);
  console.log(`  - Verification checks: ${verificationChecks.length > 0 ? 'OK' : 'MISSING'}`);

  return cachedContext;
}

export function reloadPipelineContext(): PipelineContext {
  cachedContext = null;
  return loadPipelineContext();
}

export function loadPRDContext(prdPath?: string): string | null {
  if (!prdPath) {
    const prdResultsDir = path.join(config.analysisDir, 'prd', 'results');
    if (!fs.existsSync(prdResultsDir)) return null;

    const dirs = fs.readdirSync(prdResultsDir)
      .filter(d => fs.statSync(path.join(prdResultsDir, d)).isDirectory())
      .sort()
      .reverse();

    if (dirs.length === 0) return null;

    const latestDir = path.join(prdResultsDir, dirs[0]);
    const files = fs.readdirSync(latestDir).filter(f => f.endsWith('.md'));
    return files.map(f => fs.readFileSync(path.join(latestDir, f), 'utf-8')).join('\n\n---\n\n');
  }

  const fullPath = path.resolve(config.analysisDir, prdPath);
  if (!fs.existsSync(fullPath)) return null;
  return fs.readFileSync(fullPath, 'utf-8');
}

export function loadBeImplementationContext(featureName?: string): string | null {
  const beResultsDir = path.join(config.analysisDir, 'be-implementation', 'results');
  if (!fs.existsSync(beResultsDir)) return null;

  const dirs = fs.readdirSync(beResultsDir)
    .filter(d => {
      const dirPath = path.join(beResultsDir, d);
      return fs.statSync(dirPath).isDirectory() && (!featureName || d.includes(featureName));
    })
    .sort()
    .reverse();

  if (dirs.length === 0) return null;

  const targetDir = path.join(beResultsDir, dirs[0]);
  const tddPath = path.join(targetDir, 'tdd.md');
  const gapPath = path.join(targetDir, 'gap_analysis.md');

  const parts: string[] = [];
  if (fs.existsSync(gapPath)) parts.push(fs.readFileSync(gapPath, 'utf-8'));
  if (fs.existsSync(tddPath)) parts.push(fs.readFileSync(tddPath, 'utf-8'));

  return parts.length > 0 ? parts.join('\n\n---\n\n') : null;
}

export function loadTicketContext(ticketNumber: string): string | null {
  const beResultsDir = path.join(config.analysisDir, 'be-implementation', 'results');
  if (!fs.existsSync(beResultsDir)) return null;

  const dirs = fs.readdirSync(beResultsDir)
    .filter(d => fs.statSync(path.join(beResultsDir, d)).isDirectory())
    .sort()
    .reverse();

  for (const dir of dirs) {
    const ticketsDir = path.join(beResultsDir, dir, 'tickets');
    if (!fs.existsSync(ticketsDir)) continue;

    const ticketFiles = fs.readdirSync(ticketsDir).filter(f =>
      f.includes(ticketNumber) || f.includes(`ticket-${ticketNumber}`)
    );

    if (ticketFiles.length > 0) {
      return ticketFiles.map(f =>
        fs.readFileSync(path.join(ticketsDir, f), 'utf-8')
      ).join('\n\n---\n\n');
    }
  }

  return null;
}
