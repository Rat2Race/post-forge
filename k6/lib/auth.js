import http from "k6/http";
import { env, envBool, parseJson } from "./env.js";

const PERF_USER_ID = env("PERF_USER_ID", "");
const PERF_PASSWORD = env("PERF_PASSWORD", "");
const PERF_ACCESS_TOKEN = env("PERF_ACCESS_TOKEN", "");
const PERF_AUTH_HEADER = env("PERF_AUTH_HEADER", "");
const PERF_REGISTER_IF_MISSING = envBool("PERF_REGISTER_IF_MISSING", true);
const PERF_AUTH_MODE = env("PERF_AUTH_MODE", "AUTO").toUpperCase();

export function authMode() {
  if (PERF_AUTH_MODE === "GUEST" || PERF_AUTH_MODE === "USER") {
    return PERF_AUTH_MODE;
  }
  return "AUTO";
}

export function resolveAuthHeader(baseUrl, jsonHeaders, options = {}) {
  const mode = authMode();
  if (mode === "GUEST") {
    return "";
  }
  if (mode === "AUTO" && !options.targetRequiresAuth && !options.setupRequiresAuth) {
    return "";
  }
  if (PERF_AUTH_HEADER) {
    return PERF_AUTH_HEADER;
  }
  if (PERF_ACCESS_TOKEN) {
    return `Bearer ${PERF_ACCESS_TOKEN}`;
  }
  if (!PERF_USER_ID || !PERF_PASSWORD) {
    return "";
  }

  const loginResult = login(baseUrl, jsonHeaders);
  if (loginResult) {
    return loginResult;
  }
  if (PERF_REGISTER_IF_MISSING) {
    registerUser(baseUrl, jsonHeaders);
    return login(baseUrl, jsonHeaders);
  }
  return "";
}

function login(baseUrl, jsonHeaders) {
  const res = http.post(
    `${baseUrl}/auth/login`,
    JSON.stringify({ username: PERF_USER_ID, password: PERF_PASSWORD }),
    { headers: jsonHeaders, tags: { name: "SETUP_AUTH_LOGIN" } },
  );
  const body = parseJson(res);
  if (res.status >= 200 && res.status < 300 && body?.accessToken) {
    return `${body.grantType || "Bearer"} ${body.accessToken}`;
  }
  return "";
}

function registerUser(baseUrl, jsonHeaders) {
  http.post(
    `${baseUrl}/auth/register`,
    JSON.stringify({
      username: PERF_USER_ID,
      password: PERF_PASSWORD,
      email: `${PERF_USER_ID}@postforge.perf`,
      nickname: `perf_${PERF_USER_ID}`.slice(0, 20),
    }),
    { headers: jsonHeaders, tags: { name: "SETUP_AUTH_REGISTER" } },
  );
}
