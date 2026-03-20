import fs from 'fs';
import path from 'path';
import type { ReviewConfig } from './types.js';

const CONFIG_PATH = path.resolve(import.meta.dirname, '../../review.config.json');

let cachedConfig: ReviewConfig | null = null;

export function loadReviewConfig(): ReviewConfig {
  if (cachedConfig) return cachedConfig;

  if (!fs.existsSync(CONFIG_PATH)) {
    console.warn(`[config] review.config.json not found at ${CONFIG_PATH}, using defaults`);
    cachedConfig = {
      architecture: { pattern: 'hexagonal', layers: {} },
      languageRules: {},
      crossRepo: {},
      companionFiles: {},
      ignorePaths: ['node_modules/', '.gradle/', 'build/', '.idea/'],
    };
    return cachedConfig;
  }

  cachedConfig = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf-8'));
  console.log(`[config] Loaded review config from ${CONFIG_PATH}`);
  return cachedConfig!;
}

export function reloadReviewConfig(): ReviewConfig {
  cachedConfig = null;
  return loadReviewConfig();
}
