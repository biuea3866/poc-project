import { ScenarioConfig, ScenarioType } from '../types';

export const scenarios: Record<ScenarioType, ScenarioConfig> = {
  success: {
    status: 200,
    delay: 0,
  },
  'error-card-declined': {
    status: 400,
    delay: 0,
    body: {
      code: 'CARD_DECLINED',
      message: '카드가 거절되었습니다.',
    },
  },
  'error-insufficient-balance': {
    status: 400,
    delay: 0,
    body: {
      code: 'INSUFFICIENT_BALANCE',
      message: '잔액이 부족합니다.',
    },
  },
  'error-invalid-card': {
    status: 400,
    delay: 0,
    body: {
      code: 'INVALID_CARD',
      message: '유효하지 않은 카드입니다.',
    },
  },
  'error-expired-card': {
    status: 400,
    delay: 0,
    body: {
      code: 'EXPIRED_CARD',
      message: '만료된 카드입니다.',
    },
  },
  timeout: {
    status: 504,
    delay: 30000,
    body: {
      code: 'TIMEOUT',
      message: '요청 시간이 초과되었습니다.',
    },
  },
};

export function getScenario(scenarioName?: string): ScenarioConfig {
  if (!scenarioName) {
    return scenarios.success;
  }

  const scenario = scenarios[scenarioName as ScenarioType];
  return scenario || scenarios.success;
}

export async function applyScenario(scenario: ScenarioConfig): Promise<void> {
  if (scenario.delay && scenario.delay > 0) {
    await new Promise((resolve) => setTimeout(resolve, scenario.delay));
  }
}
