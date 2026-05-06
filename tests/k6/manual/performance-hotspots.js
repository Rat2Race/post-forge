import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, envNumber, envValue } from "../env.js";
import { createPerformanceSummary } from "../report.js";

const DURATION = envValue("HOTSPOT_DURATION", "2m");
const RAMP_DURATION = envValue("HOTSPOT_RAMP_DURATION", "20s");
const GRACEFUL_STOP = envValue("HOTSPOT_GRACEFUL_STOP", "30s");
const READ_VUS = envNumber("HOTSPOT_READ_VUS", 6);
const DETAIL_VUS = envNumber("HOTSPOT_DETAIL_VUS", 4);
const SEARCH_VUS = envNumber("HOTSPOT_SEARCH_VUS", 2);
const LOGIN_VUS = envNumber("HOTSPOT_LOGIN_VUS", 0);
const PAGE_SIZE = envNumber("HOTSPOT_PAGE_SIZE", 20);
const PAGE_SCAN_PAGES = envNumber("HOTSPOT_PAGE_SCAN_PAGES", 3);
const DISCOVERY_SIZE = envNumber("HOTSPOT_DISCOVERY_SIZE", 50);
const THINK_TIME_SECONDS = envNumber("HOTSPOT_THINK_TIME_SECONDS", 1);
const LOGIN_USER = envValue("LOGIN_USER", "");
const LOGIN_PASSWORD = envValue("LOGIN_PASSWORD", "");
const INCLUDE_LOGIN = envValue("INCLUDE_LOGIN", LOGIN_VUS > 0 ? "true" : "false") === "true";

const HOTSPOT_CANDIDATES = [
  "GET /posts: page query plus batch counts/cache lookups; catches list-query regressions",
  "GET /posts?keyword: LIKE '%keyword%' search can turn into a DB scan as data grows",
  "GET /posts/{id}: detail read plus Redis view-count cache path",
  "GET /posts/{id}/comments: comment page plus batched like-count lookup",
  "POST /auth/login: BCrypt/JWT CPU probe, disabled until credentials are supplied",
];

const DEFAULT_KEYWORDS = "spring,java,news,postforge";
const SEARCH_KEYWORDS = csv(envValue("SEARCH_KEYWORDS", DEFAULT_KEYWORDS));
const STATIC_POST_IDS = numberCsv(envValue("POST_IDS", ""));

function scenario(exec, target) {
  return {
    executor: "ramping-vus",
    exec,
    stages: [
      { duration: RAMP_DURATION, target },
      { duration: DURATION, target },
      { duration: RAMP_DURATION, target: 0 },
    ],
    gracefulRampDown: GRACEFUL_STOP,
    gracefulStop: GRACEFUL_STOP,
  };
}

function scenarioEntries() {
  const entries = {};
  if (READ_VUS > 0) entries.post_list_probe = scenario("postListScenario", READ_VUS);
  if (DETAIL_VUS > 0) entries.post_detail_probe = scenario("postDetailScenario", DETAIL_VUS);
  if (SEARCH_VUS > 0) entries.post_search_probe = scenario("postSearchScenario", SEARCH_VUS);
  if (INCLUDE_LOGIN && LOGIN_VUS > 0) entries.login_cpu_probe = scenario("loginScenario", LOGIN_VUS);
  return entries;
}

function thresholdEntries() {
  const thresholds = {
    checks: ["rate>0.99"],
    http_req_failed: ["rate<0.01"],
  };

  if (READ_VUS > 0) {
    thresholds["http_req_duration{name:GET /posts}"] = ["p(95)<700", "p(99)<1500"];
  }
  if (DETAIL_VUS > 0) {
    thresholds["http_req_duration{name:GET /posts/{id}}"] = ["p(95)<400", "p(99)<1000"];
    thresholds["http_req_duration{name:GET /posts/{id}/comments}"] = ["p(95)<400", "p(99)<1000"];
  }
  if (SEARCH_VUS > 0) {
    thresholds["http_req_duration{name:GET /posts?keyword}"] = ["p(95)<800", "p(99)<1600"];
  }
  if (INCLUDE_LOGIN && LOGIN_VUS > 0) {
    thresholds["http_req_duration{name:POST /auth/login}"] = ["p(95)<1200", "p(99)<2500"];
  }

  return thresholds;
}

export const options = {
  scenarios: scenarioEntries(),
  thresholds: thresholdEntries(),
  summaryTrendStats: ["avg", "min", "med", "p(90)", "p(95)", "p(99)", "max"],
};

if (!BASE_URL) {
  throw new Error("BASE_URL is empty. Set BASE_URL=http://localhost:8080 before running this script.");
}

if (INCLUDE_LOGIN && (!LOGIN_USER || !LOGIN_PASSWORD)) {
  throw new Error("INCLUDE_LOGIN=true requires LOGIN_USER and LOGIN_PASSWORD.");
}

function csv(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function numberCsv(value) {
  return csv(value)
    .map((item) => Number(item))
    .filter((item) => Number.isInteger(item) && item > 0);
}

function uniqueNumbers(values) {
  return [...new Set(values)];
}

function pick(items, fallback = null) {
  if (!items || items.length === 0) return fallback;
  return items[__ITER % items.length];
}

function getJson(res) {
  try {
    return res.json();
  } catch (_) {
    return null;
  }
}

function request(name, method, path, body = null, params = {}) {
  const requestParams = {
    ...params,
    tags: {
      ...(params.tags || {}),
      name,
    },
  };

  return http.request(method, `${BASE_URL}${path}`, body, requestParams);
}

function discoverPostIds() {
  const res = request("GET /posts", "GET", `/posts?page=0&size=${DISCOVERY_SIZE}&sort=createdAt,desc`);
  const body = getJson(res);
  const discovered = Array.isArray(body?.content)
    ? body.content.map((post) => Number(post.id)).filter((id) => Number.isInteger(id) && id > 0)
    : [];

  return uniqueNumbers([...STATIC_POST_IDS, ...discovered]);
}

export function setup() {
  const postIds = discoverPostIds();

  if (DETAIL_VUS > 0 && postIds.length === 0) {
    throw new Error(
      "No post ids were discovered. Seed posts, set POST_IDS=1,2,3, or set HOTSPOT_DETAIL_VUS=0.",
    );
  }

  return {
    postIds,
    keywords: SEARCH_KEYWORDS,
    candidates: HOTSPOT_CANDIDATES,
  };
}

export function postListScenario() {
  const page = __ITER % Math.max(PAGE_SCAN_PAGES, 1);
  const path = `/posts?page=${page}&size=${PAGE_SIZE}&sort=createdAt,desc`;
  const res = request("GET /posts", "GET", path);

  check(res, {
    "GET /posts returns 200": (r) => r.status === 200,
    "GET /posts returns a page body": (r) => Array.isArray(getJson(r)?.content),
  });

  sleep(THINK_TIME_SECONDS);
}

export function postSearchScenario(data) {
  const keyword = encodeURIComponent(pick(data.keywords, "postforge"));
  const page = __ITER % Math.max(PAGE_SCAN_PAGES, 1);
  const path = `/posts?keyword=${keyword}&page=${page}&size=${PAGE_SIZE}&sort=createdAt,desc`;
  const res = request("GET /posts?keyword", "GET", path);

  check(res, {
    "GET /posts?keyword returns 200": (r) => r.status === 200,
    "GET /posts?keyword returns a page body": (r) => Array.isArray(getJson(r)?.content),
  });

  sleep(THINK_TIME_SECONDS);
}

export function postDetailScenario(data) {
  const postId = pick(data.postIds);

  const detail = request("GET /posts/{id}", "GET", `/posts/${postId}`);
  check(detail, {
    "GET /posts/{id} returns 200": (r) => r.status === 200,
    "GET /posts/{id} returns the requested id": (r) => Number(getJson(r)?.id) === postId,
  });

  const comments = request(
    "GET /posts/{id}/comments",
    "GET",
    `/posts/${postId}/comments?page=0&size=50&sort=createdAt,asc`,
  );
  check(comments, {
    "GET /posts/{id}/comments returns 200": (r) => r.status === 200,
    "GET /posts/{id}/comments returns a page body": (r) => Array.isArray(getJson(r)?.content),
  });

  sleep(THINK_TIME_SECONDS);
}

export function loginScenario() {
  const res = request(
    "POST /auth/login",
    "POST",
    "/auth/login",
    JSON.stringify({ userId: LOGIN_USER, password: LOGIN_PASSWORD }),
    { headers: { "Content-Type": "application/json" } },
  );

  check(res, {
    "POST /auth/login returns 200": (r) => r.status === 200,
    "POST /auth/login returns an access token": (r) => Boolean(getJson(r)?.accessToken),
  });

  sleep(THINK_TIME_SECONDS);
}

export function handleSummary(data) {
  return createPerformanceSummary(data, {
    purpose: "performance hotspot detection for post list/search/detail/comment reads and optional login CPU load",
    script: "tests/k6/manual/performance-hotspots.js",
    executor: "ramping-vus",
  });
}
