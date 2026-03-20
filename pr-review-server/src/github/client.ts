import { Octokit } from '@octokit/rest';
import { config } from '../config.js';

let octokit: Octokit | null = null;
let authenticatedUser: string | null = null;

export function getOctokit(): Octokit {
  if (!octokit) {
    octokit = new Octokit({ auth: config.githubToken });
  }
  return octokit;
}

export async function getAuthenticatedUsername(): Promise<string> {
  if (authenticatedUser) return authenticatedUser;
  const { data } = await getOctokit().users.getAuthenticated();
  authenticatedUser = data.login;
  return authenticatedUser;
}
