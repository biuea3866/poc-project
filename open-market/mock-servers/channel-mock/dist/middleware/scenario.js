"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.applyScenario = applyScenario;
const scenarios_1 = require("../scenarios");
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
async function applyScenario(req, res, allowedKeys = []) {
    const scenarioKey = req.headers['x-mock-scenario'] || 'success';
    const scenario = scenarios_1.scenarios[scenarioKey];
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
//# sourceMappingURL=scenario.js.map