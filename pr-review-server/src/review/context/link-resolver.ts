import { config } from '../../config.js';

export interface ResolvedLink {
  type: 'jira' | 'confluence' | 'notion' | 'github' | 'unknown';
  url: string;
  title: string;
  content: string;
}

interface ExtractedLink {
  type: ResolvedLink['type'];
  url: string;
  id: string;
}

// ── Link Extraction ──────────────────────────────────────────────

function extractLinks(prBody: string): ExtractedLink[] {
  const links: ExtractedLink[] = [];
  const seen = new Set<string>();

  // Jira: https://doodlin.atlassian.net/browse/GRT-1234
  const jiraRegex = /https?:\/\/[\w.-]*atlassian\.net\/browse\/([\w-]+)/gi;
  for (const match of prBody.matchAll(jiraRegex)) {
    if (seen.has(match[1])) continue;
    seen.add(match[1]);
    links.push({ type: 'jira', url: match[0], id: match[1] });
  }

  // Jira short: GRT-1234 (standalone, not in URL)
  const jiraShortRegex = /(?<![/\w])(GRT-\d+)(?!\w)/gi;
  for (const match of prBody.matchAll(jiraShortRegex)) {
    if (seen.has(match[1])) continue;
    seen.add(match[1]);
    links.push({
      type: 'jira',
      url: `${config.atlassianBaseUrl}/browse/${match[1]}`,
      id: match[1],
    });
  }

  // Confluence: https://doodlin.atlassian.net/wiki/spaces/XX/pages/12345/...
  // or https://doodlin.atlassian.net/wiki/x/XXXXX
  const confluencePageRegex = /https?:\/\/[\w.-]*atlassian\.net\/wiki\/(?:spaces\/[\w-]+\/pages\/(\d+)|x\/([\w-]+))/gi;
  for (const match of prBody.matchAll(confluencePageRegex)) {
    const id = match[1] || match[2];
    if (seen.has(match[0])) continue;
    seen.add(match[0]);
    links.push({ type: 'confluence', url: match[0], id });
  }

  // Notion: https://www.notion.so/... or https://notion.so/...
  const notionRegex = /https?:\/\/(?:www\.)?notion\.so\/([\w-]+)/gi;
  for (const match of prBody.matchAll(notionRegex)) {
    if (seen.has(match[0])) continue;
    seen.add(match[0]);
    links.push({ type: 'notion', url: match[0], id: match[1] });
  }

  // GitHub issues/PRs: #1234 or full URL
  const ghRegex = /https?:\/\/github\.com\/([\w-]+)\/([\w-]+)\/(issues|pull)\/(\d+)/gi;
  for (const match of prBody.matchAll(ghRegex)) {
    if (seen.has(match[0])) continue;
    seen.add(match[0]);
    links.push({ type: 'github', url: match[0], id: `${match[2]}#${match[4]}` });
  }

  return links;
}

// ── Atlassian API ────────────────────────────────────────────────

function atlassianHeaders(): Record<string, string> {
  const auth = Buffer.from(`${config.atlassianEmail}:${config.atlassianApiToken}`).toString('base64');
  return {
    'Authorization': `Basic ${auth}`,
    'Accept': 'application/json',
  };
}

function isAtlassianConfigured(): boolean {
  return !!(config.atlassianEmail && config.atlassianApiToken);
}

async function fetchJiraIssue(issueKey: string): Promise<ResolvedLink | null> {
  if (!isAtlassianConfigured()) return null;

  try {
    const url = `${config.atlassianBaseUrl}/rest/api/3/issue/${issueKey}`;
    const res = await fetch(url, { headers: atlassianHeaders() });
    if (!res.ok) {
      console.warn(`[link-resolver] Jira ${issueKey}: ${res.status}`);
      return null;
    }

    const data = await res.json() as any;
    const title = data.fields?.summary ?? issueKey;
    const description = extractTextFromADF(data.fields?.description);
    const acceptanceCriteria = data.fields?.customfield_10034
      ? extractTextFromADF(data.fields.customfield_10034)
      : '';

    const parts = [`# ${issueKey}: ${title}\n`];
    if (description) parts.push(`## 설명\n${description}\n`);
    if (acceptanceCriteria) parts.push(`## AC (Acceptance Criteria)\n${acceptanceCriteria}\n`);

    // Labels, components
    const labels = (data.fields?.labels ?? []).join(', ');
    const components = (data.fields?.components ?? []).map((c: any) => c.name).join(', ');
    if (labels) parts.push(`라벨: ${labels}`);
    if (components) parts.push(`컴포넌트: ${components}`);

    return {
      type: 'jira',
      url: `${config.atlassianBaseUrl}/browse/${issueKey}`,
      title,
      content: parts.join('\n'),
    };
  } catch (err) {
    console.error(`[link-resolver] Jira fetch error:`, err);
    return null;
  }
}

async function fetchConfluencePage(pageIdOrKey: string, originalUrl: string): Promise<ResolvedLink | null> {
  if (!isAtlassianConfigured()) return null;

  try {
    // Try page ID first, then content key
    let url: string;
    if (/^\d+$/.test(pageIdOrKey)) {
      url = `${config.atlassianBaseUrl}/wiki/api/v2/pages/${pageIdOrKey}?body-format=storage`;
    } else {
      // Short link (x/XXXXX) — need to resolve
      const resolveRes = await fetch(originalUrl, {
        headers: atlassianHeaders(),
        redirect: 'manual',
      });
      const location = resolveRes.headers.get('location') ?? '';
      const idMatch = location.match(/pages\/(\d+)/);
      if (!idMatch) return null;
      url = `${config.atlassianBaseUrl}/wiki/api/v2/pages/${idMatch[1]}?body-format=storage`;
    }

    const res = await fetch(url, { headers: atlassianHeaders() });
    if (!res.ok) {
      console.warn(`[link-resolver] Confluence ${pageIdOrKey}: ${res.status}`);
      return null;
    }

    const data = await res.json() as any;
    const title = data.title ?? 'Confluence Page';
    const htmlBody = data.body?.storage?.value ?? '';
    const textContent = stripHtml(htmlBody);

    return {
      type: 'confluence',
      url: originalUrl,
      title,
      content: `# ${title}\n\n${textContent}`,
    };
  } catch (err) {
    console.error(`[link-resolver] Confluence fetch error:`, err);
    return null;
  }
}

// ── ADF / HTML Helpers ───────────────────────────────────────────

function extractTextFromADF(adf: any): string {
  if (!adf) return '';
  if (typeof adf === 'string') return adf;

  const texts: string[] = [];

  function walk(node: any) {
    if (!node) return;
    if (node.type === 'text' && node.text) {
      texts.push(node.text);
    }
    if (node.type === 'hardBreak') texts.push('\n');
    if (node.type === 'paragraph') texts.push('\n');
    if (node.type === 'heading') texts.push('\n## ');
    if (node.type === 'listItem') texts.push('\n- ');
    if (node.content) {
      for (const child of node.content) walk(child);
    }
  }

  walk(adf);
  return texts.join('').trim();
}

function stripHtml(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<\/li>/gi, '\n')
    .replace(/<\/h[1-6]>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

// ── Main Resolver ────────────────────────────────────────────────

export async function resolveLinksFromPRBody(prBody: string): Promise<{
  prd: string | null;
  tdd: string | null;
  tickets: string | null;
  allResolved: ResolvedLink[];
}> {
  const links = extractLinks(prBody);
  if (links.length === 0) {
    return { prd: null, tdd: null, tickets: null, allResolved: [] };
  }

  console.log(`[link-resolver] Found ${links.length} links in PR body:`);
  for (const l of links) console.log(`  [${l.type}] ${l.id}`);

  const resolved: ResolvedLink[] = [];

  for (const link of links) {
    let result: ResolvedLink | null = null;

    switch (link.type) {
      case 'jira':
        result = await fetchJiraIssue(link.id);
        break;
      case 'confluence':
        result = await fetchConfluencePage(link.id, link.url);
        break;
      case 'notion':
        // Notion API requires separate token — skip for now, log the link
        console.log(`  [notion] ${link.url} (Notion API 미지원, 링크만 기록)`);
        result = { type: 'notion', url: link.url, title: 'Notion Link', content: `Notion: ${link.url}` };
        break;
      default:
        break;
    }

    if (result) resolved.push(result);
  }

  console.log(`[link-resolver] Resolved ${resolved.length}/${links.length} links`);

  // Categorize: Jira tickets → tickets context, Confluence → PRD or TDD
  const jiraContents = resolved.filter(r => r.type === 'jira').map(r => r.content);
  const confluenceContents = resolved.filter(r => r.type === 'confluence');

  // Heuristic: confluence page with "TDD" or "설계" in title → tdd, else → prd
  let prd: string | null = null;
  let tdd: string | null = null;

  for (const page of confluenceContents) {
    const isTdd = /tdd|설계|기술|architecture|design/i.test(page.title);
    if (isTdd) {
      tdd = tdd ? `${tdd}\n\n---\n\n${page.content}` : page.content;
    } else {
      prd = prd ? `${prd}\n\n---\n\n${page.content}` : page.content;
    }
  }

  const tickets = jiraContents.length > 0 ? jiraContents.join('\n\n---\n\n') : null;

  return { prd, tdd, tickets, allResolved: resolved };
}
