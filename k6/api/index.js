import { boardScenarios } from "./board.js";
import { catalogScenarios } from "./catalog.js";
import { priceScenarios } from "./price.js";

const scenarios = [
  ...boardScenarios,
  ...catalogScenarios,
  ...priceScenarios,
];

export function findScenario(key) {
  return scenarios.find((scenario) => scenario.key === key);
}

export function scenarioKeys() {
  return scenarios.map((scenario) => scenario.key);
}
