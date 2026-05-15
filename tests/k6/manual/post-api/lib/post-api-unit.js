import { BASE_URL, envNumber, envValue, REPORT_DIR, SUMMARY_DIR } from "../../../env.js";
import http from "k6/http";
import { check, fail, group, sleep } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";

const TEST_SECONDS = envNumber("TEST_SECONDS", 60);
const API_VUS = envNumber("API_VUS", envNumber("VUS_COUNT", 1));
const READ_PAGE_SIZE = envNumber("READ_PAGE_SIZE", 20);
const COMMENT_PAGE_SIZE = envNumber("COMMENT_PAGE_SIZE", 50);
const SEARCH_KEYWORD = envValue("SEARCH_KEYWORD", "k6");
const API_SLEEP_SECONDS = envNumber("API_SLEEP_SECONDS", 0.1);
const LIKE_SLEEP_SECONDS = envNumber("LIKE_SLEEP_SECONDS", 2.2);
const UNLIKE_SLEEP_SECONDS = envNumber("UNLIKE_SLEEP_SECONDS", 4.3);
const PERF_USER_ID = envValue("PERF_USER_ID", firstCsvValue(envValue("PERF_USER_IDS", "")));
const PERF_PASSWORD = envValue("PERF_PASSWORD", "");
const PERF_ACCESS_TOKEN = envValue("PERF_ACCESS_TOKEN", "");
const PERF_AUTH_HEADER = envValue("PERF_AUTH_HEADER", "");
const PERF_POST_ID = envValue("PERF_POST_ID", "");
const PERF_COMMENT_ID = envValue("PERF_COMMENT_ID", "");
const REPORT_NAME = envValue("K6_REPORT_NAME", "");

const targetTransactions = new Counter("post_api_unit_target_transactions");
const successfulTransactions = new Counter("post_api_unit_successful_transactions");

const JSON_HEADERS = {
  Accept: "application/json",
  "Content-Type": "application/json",
};

const API_CONFIGS = {
  "01_GET_POSTS_LIST": {
    label: "GET /posts",
    operation: "listPosts",
    seedPost: "optional",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "02_GET_POSTS_SEARCH": {
    label: "GET /posts?keyword",
    operation: "searchPosts",
    seedPost: "optional",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "03_GET_POST_DETAIL": {
    label: "GET /posts/{id}",
    operation: "getPost",
    seedPost: "required",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "04_GET_POST_COMMENTS": {
    label: "GET /posts/{id}/comments",
    operation: "getComments",
    seedPost: "required",
    seedComment: "optional",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "05_POST_POSTS_CREATE": {
    label: "POST /posts",
    operation: "createPost",
    auth: true,
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "06_PUT_POSTS_UPDATE": {
    label: "PUT /posts/{id}",
    operation: "updatePost",
    auth: true,
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "07_DELETE_POSTS_DELETE": {
    label: "DELETE /posts/{id}",
    operation: "deletePost",
    auth: true,
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "08_POST_POST_LIKE": {
    label: "POST /posts/{id}/like",
    operation: "likePost",
    auth: true,
    seedPost: "required",
    sleepSeconds: LIKE_SLEEP_SECONDS,
  },
  "09_DELETE_POST_LIKE": {
    label: "DELETE /posts/{id}/like",
    operation: "unlikePost",
    auth: true,
    seedPost: "required",
    sleepSeconds: UNLIKE_SLEEP_SECONDS,
  },
  "10_POST_COMMENT_CREATE": {
    label: "POST /posts/{id}/comments",
    operation: "createComment",
    auth: true,
    seedPost: "required",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "11_PUT_COMMENT_UPDATE": {
    label: "PUT /posts/{id}/comments/{commentId}",
    operation: "updateComment",
    auth: true,
    seedPost: "required",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "12_DELETE_COMMENT_DELETE": {
    label: "DELETE /posts/{id}/comments/{commentId}",
    operation: "deleteComment",
    auth: true,
    seedPost: "required",
    sleepSeconds: API_SLEEP_SECONDS,
  },
  "13_POST_COMMENT_LIKE": {
    label: "POST /posts/{id}/comments/{commentId}/like",
    operation: "likeComment",
    auth: true,
    seedPost: "required",
    seedComment: "required",
    sleepSeconds: LIKE_SLEEP_SECONDS,
  },
  "14_DELETE_COMMENT_LIKE": {
    label: "DELETE /posts/{id}/comments/{commentId}/like",
    operation: "unlikeComment",
    auth: true,
    seedPost: "required",
    seedComment: "required",
    sleepSeconds: UNLIKE_SLEEP_SECONDS,
  },
};

export function createPostApiUnitTest(apiName) {
  const config = API_CONFIGS[apiName];
  if (!config) {
    throw new Error(`Unknown post API unit test: ${apiName}`);
  }

  const options = {
    scenarios: {
      [apiName]: {
        executor: "constant-vus",
        exec: "runTarget",
        vus: API_VUS,
        duration: `${TEST_SECONDS}s`,
        gracefulStop: "10s",
      },
    },
    thresholds: {
      checks: ["rate>0.95"],
      http_req_failed: ["rate<0.20"],
      [`http_req_duration{name:${apiName}}`]: ["max>=0"],
      [`post_api_unit_target_transactions{api:${apiName}}`]: ["count>=0"],
      [`post_api_unit_successful_transactions{api:${apiName}}`]: ["count>=0"],
    },
    summaryTrendStats: ["avg", "med", "p(90)", "p(95)", "p(99)", "max"],
  };

  return {
    options,
    setup: () => setupUnit(config),
    teardown: (data) => teardownUnit(data),
    runTarget: (data) => runUnit(config, data),
    handleSummary: (data) => handleUnitSummary(config, data),
  };
}

function setupUnit(config) {
  if (!BASE_URL) {
    fail("BASE_URL is empty. Set BASE_URL=http://localhost:8080.");
  }

  const seedPostId = numericId(PERF_POST_ID);
  const seedCommentId = numericId(PERF_COMMENT_ID);
  const hasAuthMaterial = Boolean(PERF_AUTH_HEADER || PERF_ACCESS_TOKEN || (PERF_USER_ID && PERF_PASSWORD));
  const wantsPostSeed = Boolean(config.seedPost === "required" || (config.seedPost === "optional" && hasAuthMaterial));
  const wantsCommentSeed = Boolean(config.seedComment === "required" || (config.seedComment === "optional" && hasAuthMaterial));
  const needsPostSeed = Boolean(wantsPostSeed && !seedPostId);
  const needsCommentSeed = Boolean(wantsCommentSeed && !seedCommentId);
  const needsAuth = Boolean(config.auth || needsPostSeed || needsCommentSeed);
  const authHeader = needsAuth ? login() : "";
  const data = {
    authHeader,
    seedPostId,
    seedCommentId,
    ownsSeedPost: false,
    ownsSeedComment: false,
  };

  if (!data.seedPostId && config.seedPost && authHeader) {
    const required = config.seedPost === "required";
    const seedPost = createPostResource(authHeader, "setup-seed-post", required);
    data.seedPostId = seedPost?.id || null;
    data.ownsSeedPost = Boolean(seedPost?.id);
  }

  if (!data.seedCommentId && config.seedComment && data.seedPostId && authHeader) {
    const required = config.seedComment === "required";
    const seedComment = createCommentResource(authHeader, data.seedPostId, "setup-seed-comment", required);
    data.seedCommentId = seedComment?.id || null;
    data.ownsSeedComment = Boolean(seedComment?.id);
  }

  if (config.seedPost === "required" && !data.seedPostId) {
    fail(`${config.label} requires PERF_POST_ID or local credentials to create a seed post.`);
  }

  if (config.seedComment === "required" && !data.seedCommentId) {
    fail(`${config.label} requires PERF_COMMENT_ID or local credentials to create a seed comment.`);
  }

  return data;
}

function teardownUnit(data) {
  if (!data?.authHeader) return;

  if (data.ownsSeedComment && data.seedPostId && data.seedCommentId) {
    http.del(`${BASE_URL}/posts/${data.seedPostId}/comments/${data.seedCommentId}`, null, {
      headers: data.authHeader,
      tags: { name: "TEARDOWN_DELETE_SEED_COMMENT" },
    });
  }

  if (data.ownsSeedPost && data.seedPostId) {
    http.del(`${BASE_URL}/posts/${data.seedPostId}`, null, {
      headers: data.authHeader,
      tags: { name: "TEARDOWN_DELETE_SEED_POST" },
    });
  }
}

function runUnit(config, data) {
  const success = runOperation(config, data);
  if (success !== undefined) {
    sleep(config.sleepSeconds);
  }
}

function runOperation(config, data) {
  switch (config.operation) {
    case "listPosts":
      return target(config, () => {
        const res = http.get(`${BASE_URL}/posts?page=0&size=${READ_PAGE_SIZE}`, targetParams(config));
        const body = parseJson(res);
        return record(config, res, res.status === 200 && Array.isArray(body?.content));
      });
    case "searchPosts":
      return target(config, () => {
        const res = http.get(
          `${BASE_URL}/posts?keyword=${encodeURIComponent(SEARCH_KEYWORD)}&page=0&size=${READ_PAGE_SIZE}`,
          targetParams(config),
        );
        return record(config, res, res.status === 200);
      });
    case "getPost":
      return target(config, () => {
        const res = http.get(`${BASE_URL}/posts/${data.seedPostId}`, targetParams(config));
        return record(config, res, res.status === 200);
      });
    case "getComments":
      return target(config, () => {
        const res = http.get(
          `${BASE_URL}/posts/${data.seedPostId}/comments?page=0&size=${COMMENT_PAGE_SIZE}`,
          targetParams(config),
        );
        return record(config, res, res.status === 200);
      });
    case "createPost":
      return target(config, () => {
        const res = http.post(`${BASE_URL}/posts`, JSON.stringify(postPayload("create")), targetParams(config, data.authHeader));
        const postId = numericId(parseJson(res)?.id);
        const ok = record(config, res, res.status === 201 && Boolean(postId));
        cleanupPost(data.authHeader, postId, "CLEANUP_CREATE_POST");
        return ok;
      });
    case "updatePost":
      return withPostFixture(data.authHeader, "setup-update-post", (postId) => target(config, () => {
        const res = http.put(
          `${BASE_URL}/posts/${postId}`,
          JSON.stringify(postPayload("update")),
          targetParams(config, data.authHeader),
        );
        return record(config, res, res.status === 200);
      }));
    case "deletePost":
      return withPostFixture(data.authHeader, "setup-delete-post", (postId) => target(config, () => {
        const res = http.del(`${BASE_URL}/posts/${postId}`, null, targetParams(config, data.authHeader));
        return record(config, res, res.status === 200);
      }), false);
    case "likePost":
      return target(config, () => {
        const res = http.post(`${BASE_URL}/posts/${data.seedPostId}/like`, null, targetParams(config, data.authHeader));
        return record(config, res, res.status === 200);
      });
    case "unlikePost":
      setupPostLike(data.authHeader, data.seedPostId);
      return target(config, () => {
        const res = http.del(`${BASE_URL}/posts/${data.seedPostId}/like`, null, targetParams(config, data.authHeader));
        return record(config, res, res.status === 200);
      });
    case "createComment":
      return target(config, () => {
        const res = http.post(
          `${BASE_URL}/posts/${data.seedPostId}/comments`,
          JSON.stringify(commentPayload("create")),
          targetParams(config, data.authHeader),
        );
        const commentId = numericId(parseJson(res)?.id);
        const ok = record(config, res, res.status === 201 && Boolean(commentId));
        cleanupComment(data.authHeader, data.seedPostId, commentId, "CLEANUP_CREATE_COMMENT");
        return ok;
      });
    case "updateComment":
      return withCommentFixture(data.authHeader, data.seedPostId, "setup-update-comment", (commentId) => target(config, () => {
        const res = http.put(
          `${BASE_URL}/posts/${data.seedPostId}/comments/${commentId}`,
          JSON.stringify(commentPayload("update")),
          targetParams(config, data.authHeader),
        );
        return record(config, res, res.status === 200);
      }));
    case "deleteComment":
      return withCommentFixture(data.authHeader, data.seedPostId, "setup-delete-comment", (commentId) => target(config, () => {
        const res = http.del(`${BASE_URL}/posts/${data.seedPostId}/comments/${commentId}`, null, targetParams(config, data.authHeader));
        return record(config, res, res.status === 200);
      }), false);
    case "likeComment":
      return target(config, () => {
        const res = http.post(
          `${BASE_URL}/posts/${data.seedPostId}/comments/${data.seedCommentId}/like`,
          null,
          targetParams(config, data.authHeader),
        );
        return record(config, res, res.status === 200);
      });
    case "unlikeComment":
      setupCommentLike(data.authHeader, data.seedPostId, data.seedCommentId);
      return target(config, () => {
        const res = http.del(
          `${BASE_URL}/posts/${data.seedPostId}/comments/${data.seedCommentId}/like`,
          null,
          targetParams(config, data.authHeader),
        );
        return record(config, res, res.status === 200);
      });
    default:
      fail(`Unsupported operation: ${config.operation}`);
      return false;
  }
}

function target(config, fn) {
  let result = false;
  group(config.id || config.label, () => {
    result = fn();
  });
  return result;
}

function record(config, res, success) {
  targetTransactions.add(1, { api: config.id });
  if (success) {
    successfulTransactions.add(1, { api: config.id });
  }

  check(res, {
    [`${config.id} success`]: () => success,
  });

  return success;
}

function login() {
  if (PERF_AUTH_HEADER) {
    return {
      ...JSON_HEADERS,
      Authorization: PERF_AUTH_HEADER,
    };
  }

  if (PERF_ACCESS_TOKEN) {
    return {
      ...JSON_HEADERS,
      Authorization: `Bearer ${PERF_ACCESS_TOKEN}`,
    };
  }

  if (!PERF_USER_ID || !PERF_PASSWORD) {
    fail("Set PERF_ACCESS_TOKEN, PERF_AUTH_HEADER, PERF_USER_ID/PERF_PASSWORD, or PERF_USER_IDS/PERF_PASSWORD for post API unit scripts.");
  }

  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ userId: PERF_USER_ID, password: PERF_PASSWORD }),
    {
      headers: JSON_HEADERS,
      tags: { name: "SETUP_LOGIN" },
    },
  );
  const body = parseJson(res);

  check(res, {
    "SETUP_LOGIN returns 200": (r) => r.status === 200,
    "SETUP_LOGIN returns access token": () => typeof body?.accessToken === "string" && body.accessToken.length > 0,
  });

  if (!body?.accessToken) {
    fail(`Login failed for PERF_USER_ID=${PERF_USER_ID}.`);
  }

  return {
    ...JSON_HEADERS,
    Authorization: `${body.grantType || "Bearer"} ${body.accessToken}`,
  };
}

function targetParams(config, headers = undefined) {
  return {
    headers,
    tags: { name: config.id },
  };
}

function createPostResource(headers, name, required = true) {
  if (!headers) {
    if (required) fail(`${name} requires auth headers.`);
    return null;
  }

  const res = http.post(`${BASE_URL}/posts`, JSON.stringify(postPayload(name)), {
    headers,
    tags: { name: resourceTag(name) },
  });
  const id = numericId(parseJson(res)?.id);
  if (!id && required) {
    fail(`Failed to create post resource for ${name}: status=${res.status}`);
  }
  return id ? { id } : null;
}

function withPostFixture(headers, name, fn, cleanup = true) {
  const fixture = createPostResource(headers, name, true);
  const result = fn(fixture.id);
  if (cleanup) {
    cleanupPost(headers, fixture.id, `CLEANUP_${name.toUpperCase().replace(/-/g, "_")}`);
  }
  return result;
}

function cleanupPost(headers, postId, name) {
  if (!headers || !postId) return;
  http.del(`${BASE_URL}/posts/${postId}`, null, {
    headers,
    tags: { name },
  });
}

function createCommentResource(headers, postId, name, required = true) {
  if (!headers || !postId) {
    if (required) fail(`${name} requires auth headers and postId.`);
    return null;
  }

  const res = http.post(`${BASE_URL}/posts/${postId}/comments`, JSON.stringify(commentPayload(name)), {
    headers,
    tags: { name: resourceTag(name) },
  });
  const id = numericId(parseJson(res)?.id);
  if (!id && required) {
    fail(`Failed to create comment resource for ${name}: status=${res.status}`);
  }
  return id ? { id } : null;
}

function withCommentFixture(headers, postId, name, fn, cleanup = true) {
  const fixture = createCommentResource(headers, postId, name, true);
  const result = fn(fixture.id);
  if (cleanup) {
    cleanupComment(headers, postId, fixture.id, `CLEANUP_${name.toUpperCase().replace(/-/g, "_")}`);
  }
  return result;
}

function cleanupComment(headers, postId, commentId, name) {
  if (!headers || !postId || !commentId) return;
  http.del(`${BASE_URL}/posts/${postId}/comments/${commentId}`, null, {
    headers,
    tags: { name },
  });
}

function setupPostLike(headers, postId) {
  http.post(`${BASE_URL}/posts/${postId}/like`, null, {
    headers,
    tags: { name: "SETUP_POST_LIKE" },
  });
}

function setupCommentLike(headers, postId, commentId) {
  http.post(`${BASE_URL}/posts/${postId}/comments/${commentId}/like`, null, {
    headers,
    tags: { name: "SETUP_COMMENT_LIKE" },
  });
}

function handleUnitSummary(config, data) {
  const baseName = reportBaseName(config);
  const mdPath = `${REPORT_DIR}/${baseName}.md`;
  const jsonPath = `${SUMMARY_DIR}/${baseName}-summary.json`;
  const durationSeconds = measuredSeconds(data);
  const row = endpointResult(config, data, durationSeconds);

  const markdown = `# ${config.id} post API unit performance

## Run

| Item | Value |
|---|---:|
| Name | \`${baseName}\` |
| API | \`${config.label}\` |
| Target | \`${BASE_URL}\` |
| Duration | ${durationSeconds.toFixed(2)} s |
| VUs | ${API_VUS} |

## Result

| Requests | Success Tx | RPS | TPS | Req/min | Tx/min | avg | med | p95 | p99 | max | Success |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ${row.requests} | ${row.successfulTransactions} | ${formatNumber(row.rps)} | ${formatNumber(row.tps)} | ${formatNumber(row.requestsPerMinute)} | ${formatNumber(row.transactionsPerMinute)} | ${formatMs(row.avg)} | ${formatMs(row.med)} | ${formatMs(row.p95)} | ${formatMs(row.p99)} | ${formatMs(row.max)} | ${formatPercent(row.successRate)} |

## k6

| Metric | Value |
|---|---:|
| checks | ${(data.metrics.checks?.values?.passes ?? 0)}/${(data.metrics.checks?.values?.passes ?? 0) + (data.metrics.checks?.values?.fails ?? 0)} |
| http_req_failed | ${formatPercent(data.metrics.http_req_failed?.values?.rate ?? 0)} |

## Notes

- Only \`${config.id}\` contributes to RPS/TPS.
- Login, seed, and cleanup requests are tagged separately and excluded from target transaction counters.

## Artifact

| Type | Path |
|---|---|
| k6 summary JSON | \`${jsonPath}\` |
| k6 markdown report | \`${mdPath}\` |
`;

  return {
    stdout: `\n[post-api-unit] markdown: ${mdPath}\n[post-api-unit] summary: ${jsonPath}\n\n`,
    [mdPath]: markdown,
    [jsonPath]: JSON.stringify(redactSecrets(data), null, 2),
  };
}

function endpointResult(config, data, seconds) {
  const duration = data.metrics[`http_req_duration{name:${config.id}}`]?.values || {};
  const requests = data.metrics[`post_api_unit_target_transactions{api:${config.id}}`]?.values?.count || 0;
  const successfulTransactions = data.metrics[`post_api_unit_successful_transactions{api:${config.id}}`]?.values?.count || 0;

  return {
    requests,
    successfulTransactions,
    rps: requests / seconds,
    tps: successfulTransactions / seconds,
    requestsPerMinute: (requests / seconds) * 60,
    transactionsPerMinute: (successfulTransactions / seconds) * 60,
    avg: duration.avg,
    med: duration.med,
    p95: duration["p(95)"],
    p99: duration["p(99)"],
    max: duration.max,
    successRate: requests > 0 ? successfulTransactions / requests : 0,
  };
}

function postPayload(name) {
  const suffix = requestSuffix(name);
  return {
    title: `k6 ${suffix}`,
    content: `k6 post API unit performance content ${suffix}.`,
    fileIds: [],
  };
}

function commentPayload(name) {
  const suffix = requestSuffix(name);
  return {
    parentId: null,
    content: `k6 comment API unit performance content ${suffix}.`,
  };
}

function resourceTag(name) {
  return `SETUP_${name.toUpperCase().replace(/-/g, "_")}`;
}

function requestSuffix(name) {
  let vuId = "setup";
  let iteration = 0;

  try {
    vuId = exec.vu.idInTest || vuId;
  } catch {
    vuId = "setup";
  }

  try {
    iteration = exec.scenario.iterationInTest || iteration;
  } catch {
    iteration = 0;
  }

  return `${name}-${vuId}-${iteration}-${Date.now()}`;
}

function reportBaseName(config) {
  if (REPORT_NAME) return slug(REPORT_NAME);
  const now = new Date();
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}-${slug(config.id)}`;
}

function measuredSeconds(data) {
  const seconds = (data.state?.testRunDurationMs || 0) / 1000;
  return seconds > 0 ? seconds : TEST_SECONDS;
}

function parseJson(res) {
  try {
    return res.json();
  } catch {
    return null;
  }
}

function numericId(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null;
}

function firstCsvValue(value) {
  return String(value || "").split(",").map((part) => part.trim()).filter(Boolean)[0] || "";
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function slug(value) {
  return String(value || "unknown")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "") || "unknown";
}

function formatNumber(value) {
  return Number.isFinite(value) ? value.toFixed(2) : "-";
}

function formatMs(value) {
  return Number.isFinite(value) ? `${value.toFixed(2)} ms` : "-";
}

function formatPercent(value) {
  return Number.isFinite(value) ? `${(value * 100).toFixed(2)}%` : "-";
}

function redactSecrets(value) {
  if (Array.isArray(value)) {
    return value.map(redactSecrets);
  }

  if (value && typeof value === "object") {
    for (const [key, nestedValue] of Object.entries(value)) {
      if (/(authorization|token|cookie|password|secret)/i.test(key)) {
        value[key] = "[redacted]";
      } else {
        value[key] = redactSecrets(nestedValue);
      }
    }
  }

  return value;
}

for (const [id, config] of Object.entries(API_CONFIGS)) {
  config.id = id;
}
