import { config as dotenvConfig } from 'dotenv';
import path from 'path';

dotenvConfig();

export const config = {
  githubToken: process.env.GITHUB_TOKEN ?? '',
  webhookSecret: process.env.WEBHOOK_SECRET_PR_REVIEW ?? '',
  atlassianEmail: process.env.ATLASSIAN_EMAIL ?? '',
  atlassianApiToken: process.env.ATLASSIAN_API_TOKEN ?? '',
  atlassianBaseUrl: process.env.ATLASSIAN_BASE_URL ?? 'https://doodlin.atlassian.net',
  port: parseInt(process.env.PORT ?? '3847', 10),
  analysisDir: process.env.ANALYSIS_DIR || path.resolve(import.meta.dirname, '../../.analysis'),
  resultsDir: '',
};

config.resultsDir = path.join(config.analysisDir, 'pr-review', 'results');

export function validateConfig(): void {
  if (!config.githubToken) {
    console.error('GITHUB_TOKEN is required. Set it in .env file.');
    process.exit(1);
  }
  if (!config.webhookSecret) {
    console.error('WEBHOOK_SECRET_PR_REVIEW is required. Set it in .env file.');
    process.exit(1);
  }
}
