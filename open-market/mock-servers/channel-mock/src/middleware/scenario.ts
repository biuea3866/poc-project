import { Request, Response } from 'express';
import { scenarios } from '../scenarios';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export async function applyScenario(
  req: Request,
  res: Response,
  allowedKeys: string[] = []
): Promise<boolean> {
  const scenarioKey = (req.headers['x-mock-scenario'] as string | undefined) || 'success';
  const scenario = scenarios[scenarioKey];

  if (!scenario) {
    return false;
  }

  if (allowedKeys.length > 0 && !allowedKeys.includes(scenarioKey)) {
    return false;
  }

  if (scenario.delay) {
    await sleep(scenario.delay);
  }

  if (scenario.status !== 200) {
    res.status(scenario.status).json(scenario.body);
    return true;
  }

  return false;
}
