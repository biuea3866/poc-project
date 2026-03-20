import type { ChangedFile } from './types.js';

export interface ClassInfo {
  filename: string;
  className: string;
  packageName: string;
  layer: 'controller' | 'facade' | 'service' | 'repository' | 'entity' | 'event' | 'config' | 'other';
  boundedContext: string;
  injectedDependencies: string[];
  imports: string[];
  publishesEvents: boolean;
  usesRepository: boolean;
}

export interface DIViolation {
  file: string;
  className: string;
  type: 'cross-bc-dependency' | 'layer-violation' | 'facade-has-logic' | 'controller-injects-service' | 'facade-publishes-event';
  description: string;
  evidence: string;
}

export interface DIGraph {
  classes: ClassInfo[];
  violations: DIViolation[];
}

function detectLayer(filename: string, content: string): ClassInfo['layer'] {
  if (/Controller\.kt$/.test(filename)) return 'controller';
  if (/Facade\.kt$/.test(filename)) return 'facade';
  if (/Service(Impl)?\.kt$/.test(filename) || /UseCase\.kt$/.test(filename)) return 'service';
  if (/Repository\.kt$|Reader\.kt$|Store\.kt$/.test(filename)) return 'repository';
  if (/Entity\.kt$/.test(filename)) return 'entity';
  if (/Handler\.kt$|Listener\.kt$|Event\.kt$/.test(filename)) return 'event';
  if (/Config\.kt$|Configuration\.kt$/.test(filename)) return 'config';
  return 'other';
}

function extractBC(filename: string): string {
  // e.g. doodlin/greeting/candidate/... → candidate
  // e.g. doodlin/greeting/evaluation/... → evaluation
  const match = filename.match(/doodlin\/greeting\/(\w+)\//);
  return match ? match[1] : 'unknown';
}

function extractClassName(filename: string): string {
  const base = filename.split('/').pop() ?? '';
  return base.replace('.kt', '');
}

function extractPackage(content: string): string {
  const match = content.match(/^package\s+([\w.]+)/m);
  return match ? match[1] : '';
}

function extractImports(content: string): string[] {
  const imports: string[] = [];
  const regex = /^import\s+([\w.]+)/gm;
  let match;
  while ((match = regex.exec(content)) !== null) {
    imports.push(match[1]);
  }
  return imports;
}

function extractConstructorDeps(content: string): string[] {
  // Match Kotlin constructor parameters: private val fooService: FooService
  const deps: string[] = [];
  const ctorMatch = content.match(/class\s+\w+\s*\(([\s\S]*?)\)\s*[:{]/);
  if (!ctorMatch) return deps;

  const params = ctorMatch[1];
  const paramRegex = /(?:private\s+)?(?:val|var)\s+\w+\s*:\s*([\w<>]+)/g;
  let m;
  while ((m = paramRegex.exec(params)) !== null) {
    deps.push(m[1]);
  }
  return deps;
}

function analyzeClass(filename: string, addedContent: string): ClassInfo {
  const layer = detectLayer(filename, addedContent);
  const bc = extractBC(filename);
  const className = extractClassName(filename);
  const packageName = extractPackage(addedContent);
  const imports = extractImports(addedContent);
  const injectedDependencies = extractConstructorDeps(addedContent);
  const publishesEvents = /publishEvent|applicationEventPublisher|kafkaTemplate\.send/.test(addedContent);
  const usesRepository = /[Rr]epository|[Jj]paRepository|\.save\(|\.findBy|\.findAll/.test(addedContent);

  return {
    filename, className, packageName, layer, boundedContext: bc,
    injectedDependencies, imports, publishesEvents, usesRepository,
  };
}

function findViolations(classes: ClassInfo[]): DIViolation[] {
  const violations: DIViolation[] = [];

  for (const cls of classes) {
    // Controller → should only inject Facade
    if (cls.layer === 'controller') {
      const serviceInjections = cls.injectedDependencies.filter(d =>
        d.endsWith('Service') || d.endsWith('ServiceImpl') || d.endsWith('UseCase')
      );
      if (serviceInjections.length > 0) {
        violations.push({
          file: cls.filename,
          className: cls.className,
          type: 'controller-injects-service',
          description: `${cls.className}가 Service를 직접 주입하고 있어요. Facade만 주입해야 해요.`,
          evidence: `주입된 Service: ${serviceInjections.join(', ')}`,
        });
      }
    }

    // Facade → should not have business logic indicators
    if (cls.layer === 'facade') {
      if (cls.usesRepository) {
        violations.push({
          file: cls.filename,
          className: cls.className,
          type: 'facade-has-logic',
          description: `${cls.className}(Facade)에서 Repository를 직접 사용하고 있어요. Facade는 Service 호출만 해야 해요.`,
          evidence: 'Repository 사용 감지',
        });
      }
      if (cls.publishesEvents) {
        violations.push({
          file: cls.filename,
          className: cls.className,
          type: 'facade-publishes-event',
          description: `${cls.className}(Facade)에서 이벤트를 발행하고 있어요. 이벤트는 Service에서 발행해야 해요.`,
          evidence: 'publishEvent 또는 kafkaTemplate 사용 감지',
        });
      }
    }

    // Cross-BC dependency check
    if (cls.layer === 'service' || cls.layer === 'event') {
      const crossBcImports = cls.imports.filter(imp => {
        const impBC = imp.match(/doodlin\.greeting\.(\w+)\./)?.[1];
        return impBC && impBC !== cls.boundedContext && imp.includes('Repository');
      });

      if (crossBcImports.length > 0) {
        violations.push({
          file: cls.filename,
          className: cls.className,
          type: 'cross-bc-dependency',
          description: `${cls.className}(${cls.boundedContext})이 다른 도메인의 Repository를 직접 참조하고 있어요.`,
          evidence: crossBcImports.map(i => i.split('.').pop()).join(', '),
        });
      }
    }
  }

  return violations;
}

/**
 * Analyze DI relationships from PR changed files.
 * Only analyzes Kotlin files with patches.
 */
export function analyzeDI(files: ChangedFile[]): DIGraph {
  const classes: ClassInfo[] = [];

  for (const file of files) {
    if (!file.filename.endsWith('.kt') || !file.patch) continue;
    if (file.filename.includes('/test/')) continue;

    // Reconstruct added content from patch
    const addedLines = file.patch
      .split('\n')
      .filter(l => !l.startsWith('-') && !l.startsWith('@@'))
      .map(l => l.startsWith('+') ? l.slice(1) : l)
      .join('\n');

    const cls = analyzeClass(file.filename, addedLines);
    classes.push(cls);
  }

  const violations = findViolations(classes);

  return { classes, violations };
}

/**
 * Format DI graph as context string for Claude API prompt.
 */
export function formatDIContext(graph: DIGraph): string {
  if (graph.classes.length === 0) return '';

  const lines: string[] = ['## DI 의존 관계 분석\n'];

  for (const cls of graph.classes) {
    if (cls.injectedDependencies.length === 0 && !cls.publishesEvents) continue;
    lines.push(`- **${cls.className}** (${cls.layer}, ${cls.boundedContext})`);
    if (cls.injectedDependencies.length > 0) {
      lines.push(`  주입: ${cls.injectedDependencies.join(', ')}`);
    }
    if (cls.publishesEvents) lines.push(`  이벤트 발행: O`);
  }

  if (graph.violations.length > 0) {
    lines.push('\n### 구조적 문제 감지');
    for (const v of graph.violations) {
      lines.push(`- [${v.type}] ${v.description} (${v.evidence})`);
    }
  }

  return lines.join('\n');
}
