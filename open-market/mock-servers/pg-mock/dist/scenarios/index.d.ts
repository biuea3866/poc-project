import { ScenarioConfig, ScenarioType } from '../types';
export declare const scenarios: Record<ScenarioType, ScenarioConfig>;
export declare function getScenario(scenarioName?: string): ScenarioConfig;
export declare function applyScenario(scenario: ScenarioConfig): Promise<void>;
//# sourceMappingURL=index.d.ts.map