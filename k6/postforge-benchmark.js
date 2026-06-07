import http from "k6/http";
import { check, fail } from "k6";
import { Counter, Trend } from "k6/metrics";
import { findScenario, scenarioKeys } from "./api/index.js";
import { authMode, resolveAuthHeader } from "./lib/auth.js";
import { env, envNumber } from "./lib/env.js";
import { createSeedContext } from "./lib/fixtures.js";

const BASE_URL = env("BASE_URL", "http://127.0.0.1:8080");
const TARGET_ENDPOINT_KEY = env("TARGET_ENDPOINT_KEY", "board.post.list");
const TARGET_RPS = envNumber("K6_TARGET_RPS", 1);
const DURATION_SECONDS = Math.max(1, envNumber("K6_DURATION_SECONDS", 1));
const SETUP_ITERATIONS = envNumber("K6_SETUP_ITERATIONS", 0);
const API_VUS = Math.max(1, envNumber("API_VUS", 1));
const SCENARIO = findScenario(TARGET_ENDPOINT_KEY);
const AUTH_MODE = authMode();
const EXPECT_AUTH_DENIAL = AUTH_MODE === "GUEST" && SCENARIO?.requiresAuth;

if (!SCENARIO) {
  fail(`Unknown TARGET_ENDPOINT_KEY=${TARGET_ENDPOINT_KEY}. Known scenarios: ${scenarioKeys().join(", ")}`);
}

const targetTransactions = new Counter("postforge_target_transactions");
const successfulTransactions = new Counter("postforge_target_successful_transactions");
const targetDuration = new Trend("postforge_target_duration", true);

const JSON_HEADERS = {
  Accept: "application/json",
  "Content-Type": "application/json",
};

export const options = {
  scenarios: {
    target: {
      executor: "constant-arrival-rate",
      rate: TARGET_RPS,
      timeUnit: "1s",
      duration: `${DURATION_SECONDS}s`,
      preAllocatedVUs: API_VUS,
      maxVUs: Math.max(API_VUS * 2, API_VUS + 10),
      gracefulStop: "10s",
    },
  },
  thresholds: {
    checks: ["rate>0.90"],
    http_req_failed: [EXPECT_AUTH_DENIAL ? "rate<=1" : "rate<0.25"],
    postforge_target_transactions: ["count>=0"],
    postforge_target_successful_transactions: ["count>=0"],
    postforge_target_duration: ["max>=0"],
  },
  summaryTrendStats: ["avg", "med", "p(90)", "p(95)", "p(99)", "max"],
};

export function setup() {
  const setupRequiresAuth = SETUP_ITERATIONS > 0 || SCENARIO.seedPost || SCENARIO.seedComment;
  const authHeader = resolveAuthHeader(BASE_URL, JSON_HEADERS, {
    targetRequiresAuth: SCENARIO.requiresAuth,
    setupRequiresAuth,
  });
  if (AUTH_MODE !== "GUEST" && (SCENARIO.requiresAuth || AUTH_MODE === "USER") && !authHeader) {
    fail("Target scenario requires auth. Set PERF_ACCESS_TOKEN, PERF_AUTH_HEADER, or PERF_USER_ID/PERF_PASSWORD.");
  }

  return createSeedContext({
    baseUrl: BASE_URL,
    authHeader,
    jsonHeaders: JSON_HEADERS,
    scenario: SCENARIO,
    setupIterations: SETUP_ITERATIONS,
  });
}

export default function (seed) {
  const targetSeed = prepareTargetSeed(seed);
  const started = Date.now();
  const res = sendTargetRequest(targetSeed);
  const elapsed = Date.now() - started;
  const ok = isExpectedStatus(res.status);

  targetTransactions.add(1);
  targetDuration.add(elapsed);
  if (ok) {
    successfulTransactions.add(1);
  }

  check(res, {
    "target status is expected": () => ok,
  });
}

function prepareTargetSeed(seed) {
  if (!SCENARIO.prepareTarget) {
    return seed;
  }
  return SCENARIO.prepareTarget({
    baseUrl: BASE_URL,
    authHeader: seed.authHeader,
    jsonHeaders: JSON_HEADERS,
    seed,
  });
}

function isExpectedStatus(status) {
  if (EXPECT_AUTH_DENIAL) {
    return status === 401 || status === 403;
  }
  return status >= 200 && status < 400;
}

function sendTargetRequest(seed) {
  const url = `${BASE_URL}${SCENARIO.resolvePath(seed)}`;
  const params = {
    headers: headers(seed),
    tags: {
      name: SCENARIO.key,
      endpoint: SCENARIO.key,
      mode: env("BENCHMARK_MODE", "UNKNOWN"),
      cache_state: env("CACHE_STATE", "UNKNOWN"),
    },
  };

  switch (SCENARIO.method) {
    case "GET":
      return http.get(url, params);
    case "POST":
      return http.post(url, SCENARIO.requestBody(seed), params);
    case "PUT":
      return http.put(url, SCENARIO.requestBody(seed), params);
    case "PATCH":
      return http.patch(url, SCENARIO.requestBody(seed), params);
    case "DELETE":
      return http.del(url, null, params);
    default:
      fail(`Unsupported method for ${SCENARIO.key}: ${SCENARIO.method}`);
  }
}

function headers(seed) {
  const value = {
    ...JSON_HEADERS,
    "X-PostForge-Benchmark-Run": env("RUN_GROUP", "manual"),
    "X-PostForge-Benchmark-Mode": env("BENCHMARK_MODE", "UNKNOWN"),
    "X-PostForge-Cache-State": env("CACHE_STATE", "UNKNOWN"),
  };
  if (seed.authHeader) {
    value.Authorization = seed.authHeader;
  }
  return value;
}
