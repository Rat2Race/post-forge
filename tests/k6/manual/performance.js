import { BASE_URL, envNumber, envValue } from "../env.js";
import { createPerformanceSummary } from "../report.js";
import http from "k6/http";
import { check, fail, group, sleep } from "k6";
import exec from "k6/execution";

const PUBLIC_VUS = envNumber("PUBLIC_VUS", envNumber("VUS_COUNT", 5));
const AUTH_VUS = envNumber("AUTH_VUS", 1);
const RAMP_UP = envValue("RAMP_UP", "30s");
const DURATION = envValue("DURATION", "1m");
const RAMP_DOWN = envValue("RAMP_DOWN", "30s");
const READ_PAGE_SIZE = envNumber("READ_PAGE_SIZE", 20);
const COMMENT_PAGE_SIZE = envNumber("COMMENT_PAGE_SIZE", 50);
const PUBLIC_SLEEP_SECONDS = envNumber("PUBLIC_SLEEP_SECONDS", 1);
const AUTH_SLEEP_SECONDS = envNumber("AUTH_SLEEP_SECONDS", 1);
const READ_P95_MS = envNumber("READ_P95_MS", 500);
const WRITE_P95_MS = envNumber("WRITE_P95_MS", 1000);
const SEARCH_KEYWORD = envValue("SEARCH_KEYWORD", "Post");
const PERF_POST_ID = envValue("PERF_POST_ID", "");
const PERF_USER_ID = envValue("PERF_USER_ID", "");
const PERF_PASSWORD = envValue("PERF_PASSWORD", "");
const RUN_AUTH_FLOW = envFlag("RUN_AUTH_FLOW", false);

const JSON_HEADERS = {
  Accept: "application/json",
  "Content-Type": "application/json",
};

const scenarios = {
  public_readers: {
    executor: "ramping-vus",
    exec: "publicReader",
    stages: [
      { duration: RAMP_UP, target: PUBLIC_VUS },
      { duration: DURATION, target: PUBLIC_VUS },
      { duration: RAMP_DOWN, target: 0 },
    ],
    gracefulRampDown: "30s",
  },
};

if (RUN_AUTH_FLOW) {
  scenarios.auth_writers = {
    executor: "ramping-vus",
    exec: "authWriter",
    stages: [
      { duration: RAMP_UP, target: AUTH_VUS },
      { duration: DURATION, target: AUTH_VUS },
      { duration: RAMP_DOWN, target: 0 },
    ],
    gracefulRampDown: "30s",
  };
}

const thresholds = {
  checks: ["rate>0.99"],
  http_req_failed: ["rate<0.02"],
  "http_req_duration{name:GET /posts}": [`p(95)<${READ_P95_MS}`],
  "http_req_duration{name:GET /posts?keyword}": [`p(95)<${READ_P95_MS}`],
};

if (RUN_AUTH_FLOW) {
  Object.assign(thresholds, {
    "http_req_duration{name:POST /posts}": [`p(95)<${WRITE_P95_MS}`],
    "http_req_duration{name:PUT /posts/{id}}": [`p(95)<${WRITE_P95_MS}`],
    "http_req_duration{name:POST /posts/{id}/comments}": [`p(95)<${WRITE_P95_MS}`],
    "http_req_duration{name:PUT /posts/{id}/comments/{commentId}}": [`p(95)<${WRITE_P95_MS}`],
  });
}

export const options = {
  scenarios,
  thresholds,
};

if (!BASE_URL) {
  throw new Error("BASE_URL is empty. Set tests/k6/env.js or run with BASE_URL=http://localhost:8080 k6 run tests/k6/manual/performance.js");
}

export function setup() {
  const data = {
    publicPostId: numericId(PERF_POST_ID),
    authHeader: "",
  };

  if (!data.publicPostId) {
    const res = http.get(`${BASE_URL}/posts?page=0&size=1`, {
      tags: { name: "SETUP GET /posts" },
    });
    const body = parseJson(res);
    data.publicPostId = numericId(body?.content?.[0]?.id);
  }

  if (RUN_AUTH_FLOW) {
    if (!PERF_USER_ID || !PERF_PASSWORD) {
      fail("RUN_AUTH_FLOW=true requires PERF_USER_ID and PERF_PASSWORD. Do not hardcode credentials in this script.");
    }

    const loginRes = http.post(
      `${BASE_URL}/auth/login`,
      JSON.stringify({ userId: PERF_USER_ID, password: PERF_PASSWORD }),
      {
        headers: JSON_HEADERS,
        tags: { name: "SETUP POST /auth/login" },
      },
    );
    const loginBody = parseJson(loginRes);
    check(loginRes, {
      "login returns 200": (r) => r.status === 200,
      "login returns access token": () => typeof loginBody?.accessToken === "string" && loginBody.accessToken.length > 0,
    });

    if (!loginBody?.accessToken) {
      fail(`Login failed for PERF_USER_ID=${PERF_USER_ID}. Seed a local user or provide valid local credentials.`);
    }

    data.authHeader = `${loginBody.grantType || "Bearer"} ${loginBody.accessToken}`;
  }

  return data;
}

export function publicReader(data) {
  let postId = data.publicPostId;

  group("public post list", () => {
    const res = http.get(`${BASE_URL}/posts?page=0&size=${READ_PAGE_SIZE}`, {
      tags: { name: "GET /posts" },
    });
    const body = parseJson(res);
    postId = postId || numericId(body?.content?.[0]?.id);

    check(res, {
      "GET /posts returns 200": (r) => r.status === 200,
      "GET /posts returns page body": () => Array.isArray(body?.content),
    });
  });

  group("public post search", () => {
    const res = http.get(`${BASE_URL}/posts?keyword=${encodeURIComponent(SEARCH_KEYWORD)}&page=0&size=${READ_PAGE_SIZE}`, {
      tags: { name: "GET /posts?keyword" },
    });

    check(res, {
      "GET /posts?keyword returns 200": (r) => r.status === 200,
    });
  });

  if (postId) {
    group("public post detail", () => {
      const res = http.get(`${BASE_URL}/posts/${postId}`, {
        tags: { name: "GET /posts/{id}" },
      });

      check(res, {
        "GET /posts/{id} returns 200 or 404": (r) => r.status === 200 || r.status === 404,
      });
    });

    group("public comments", () => {
      const res = http.get(`${BASE_URL}/posts/${postId}/comments?page=0&size=${COMMENT_PAGE_SIZE}`, {
        tags: { name: "GET /posts/{id}/comments" },
      });

      check(res, {
        "GET /posts/{id}/comments returns 200 or 404": (r) => r.status === 200 || r.status === 404,
      });
    });
  }

  sleep(PUBLIC_SLEEP_SECONDS);
}

export function authWriter(data) {
  if (!data.authHeader) {
    fail("authWriter requires an access token from setup()");
  }

  const headers = {
    ...JSON_HEADERS,
    Authorization: data.authHeader,
  };
  const suffix = `${exec.vu.idInTest}-${exec.scenario.iterationInTest}-${Date.now()}`;
  let postId = null;
  let commentId = null;

  group("authenticated write flow", () => {
    const createPostRes = http.post(
      `${BASE_URL}/posts`,
      JSON.stringify({
        title: `k6 perf ${suffix}`,
        content: `k6 performance test content ${suffix}. This post is created and deleted by k6.`,
        fileIds: [],
      }),
      {
        headers,
        tags: { name: "POST /posts" },
      },
    );
    const createdPost = parseJson(createPostRes);
    postId = numericId(createdPost?.id);

    check(createPostRes, {
      "POST /posts returns 201": (r) => r.status === 201,
      "POST /posts returns id": () => Boolean(postId),
    });

    if (!postId) {
      return;
    }

    const updatePostRes = http.put(
      `${BASE_URL}/posts/${postId}`,
      JSON.stringify({
        title: `k6 perf updated ${suffix}`,
        content: `k6 performance test updated content ${suffix}. This post is created and deleted by k6.`,
        fileIds: [],
      }),
      {
        headers,
        tags: { name: "PUT /posts/{id}" },
      },
    );
    check(updatePostRes, {
      "PUT /posts/{id} returns 200": (r) => r.status === 200,
    });

    const likePostRes = http.post(`${BASE_URL}/posts/${postId}/like`, null, {
      headers,
      tags: { name: "POST /posts/{id}/like" },
    });
    check(likePostRes, {
      "POST /posts/{id}/like returns 200": (r) => r.status === 200,
    });

    const createCommentRes = http.post(
      `${BASE_URL}/posts/${postId}/comments`,
      JSON.stringify({
        parentId: null,
        content: `k6 comment ${suffix}`,
      }),
      {
        headers,
        tags: { name: "POST /posts/{id}/comments" },
      },
    );
    const createdComment = parseJson(createCommentRes);
    commentId = numericId(createdComment?.id);

    check(createCommentRes, {
      "POST /posts/{id}/comments returns 201": (r) => r.status === 201,
      "POST /posts/{id}/comments returns id": () => Boolean(commentId),
    });

    if (commentId) {
      const updateCommentRes = http.put(
        `${BASE_URL}/posts/${postId}/comments/${commentId}`,
        JSON.stringify({
          parentId: null,
          content: `k6 comment updated ${suffix}`,
        }),
        {
          headers,
          tags: { name: "PUT /posts/{id}/comments/{commentId}" },
        },
      );
      check(updateCommentRes, {
        "PUT /posts/{id}/comments/{commentId} returns 200": (r) => r.status === 200,
      });

      const likeCommentRes = http.post(`${BASE_URL}/posts/${postId}/comments/${commentId}/like`, null, {
        headers,
        tags: { name: "POST /posts/{id}/comments/{commentId}/like" },
      });
      check(likeCommentRes, {
        "POST /posts/{id}/comments/{commentId}/like returns 200": (r) => r.status === 200,
      });
    }
  });

  cleanupCreatedComment(headers, postId, commentId);
  cleanupCreatedPost(headers, postId);
  sleep(AUTH_SLEEP_SECONDS);
}

export function handleSummary(data) {
  return createPerformanceSummary(redactSummaryData(data), {
    purpose: "manual performance",
    script: "tests/k6/manual/performance.js",
  });
}

function cleanupCreatedComment(headers, postId, commentId) {
  if (!postId || !commentId) return;

  const res = http.del(`${BASE_URL}/posts/${postId}/comments/${commentId}`, null, {
    headers,
    tags: { name: "DELETE /posts/{id}/comments/{commentId}" },
  });
  check(res, {
    "DELETE /posts/{id}/comments/{commentId} returns 200 or 404": (r) => r.status === 200 || r.status === 404,
  });
}

function cleanupCreatedPost(headers, postId) {
  if (!postId) return;

  const res = http.del(`${BASE_URL}/posts/${postId}`, null, {
    headers,
    tags: { name: "DELETE /posts/{id}" },
  });
  check(res, {
    "DELETE /posts/{id} returns 200 or 404": (r) => r.status === 200 || r.status === 404,
  });
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

function envFlag(name, fallback) {
  const value = String(envValue(name, fallback ? "true" : "false")).toLowerCase();
  return ["1", "true", "yes", "y", "on"].includes(value);
}

function redactSummaryData(data) {
  return redactSecrets(JSON.parse(JSON.stringify(data)));
}

function redactSecrets(value) {
  if (Array.isArray(value)) {
    return value.map(redactSecrets);
  }

  if (value && typeof value === "object") {
    for (const [key, nestedValue] of Object.entries(value)) {
      if (isSecretKey(key)) {
        value[key] = "[redacted]";
      } else {
        value[key] = redactSecrets(nestedValue);
      }
    }
  }

  return value;
}

function isSecretKey(key) {
  return /(authorization|authheader|token|cookie|password|secret)/i.test(key);
}
