import http from "k6/http";
import { fail } from "k6";
import { env, parseJson, positiveNumber } from "./env.js";

export function createSeedContext({ baseUrl, authHeader, jsonHeaders, scenario, setupIterations }) {
  const seed = {
    authHeader,
    postId: positiveNumber(env("PERF_POST_ID", "")),
    commentId: positiveNumber(env("PERF_COMMENT_ID", "")),
    productId: positiveNumber(env("PERF_PRODUCT_ID", "")),
  };

  if (setupIterations > 0) {
    if (!authHeader) {
      fail("Seed rows require auth material to create posts.");
    }
    for (let index = 0; index < setupIterations; index += 1) {
      const created = createPost({ baseUrl, authHeader, jsonHeaders, suffix: index });
      if (!seed.postId && created) {
        seed.postId = created;
      }
    }
  }

  if (scenario.seedPost && !seed.postId) {
    if (!authHeader) {
      fail(`${scenario.key} requires PERF_POST_ID or auth material to create a seed post.`);
    }
    seed.postId = createPost({ baseUrl, authHeader, jsonHeaders, suffix: "target" });
  }

  if (scenario.seedComment && !seed.commentId) {
    if (!authHeader || !seed.postId) {
      fail(`${scenario.key} requires PERF_COMMENT_ID or auth material plus seed post.`);
    }
    seed.commentId = createComment({ baseUrl, authHeader, jsonHeaders, postId: seed.postId, suffix: "target" });
  }

  if (scenario.seedProduct && !seed.productId) {
    fail(`${scenario.key} requires PERF_PRODUCT_ID.`);
  }

  if (scenario.prepare) {
    scenario.prepare({ baseUrl, authHeader, jsonHeaders, seed });
  }

  return seed;
}

export function createPost({ baseUrl, authHeader, jsonHeaders, suffix }) {
  const res = http.post(
    `${baseUrl}/posts`,
    JSON.stringify({
      title: `k6 seed ${suffix} ${Date.now()}`,
      content: `k6 setup seed content ${suffix}. This row is intentionally kept for performance testing.`,
      summary: "k6 setup seed",
      tags: ["k6", "seed"],
      category: "GENERAL",
      fileIds: [],
    }),
    { headers: { ...jsonHeaders, Authorization: authHeader }, tags: { name: "SETUP_CREATE_POST" } },
  );
  return positiveNumber(parseJson(res)?.id);
}

export function createComment({ baseUrl, authHeader, jsonHeaders, postId, suffix }) {
  const res = http.post(
    `${baseUrl}/posts/${postId}/comments`,
    JSON.stringify({
      parentId: null,
      content: `k6 setup comment ${suffix}`,
    }),
    { headers: { ...jsonHeaders, Authorization: authHeader }, tags: { name: "SETUP_CREATE_COMMENT" } },
  );
  return positiveNumber(parseJson(res)?.id);
}

export function setupPostLike({ baseUrl, authHeader, jsonHeaders, seed }) {
  if (!seed.postId || !authHeader) return;
  http.post(`${baseUrl}/posts/${seed.postId}/like`, null, {
    headers: { ...jsonHeaders, Authorization: authHeader },
    tags: { name: "SETUP_POST_LIKE" },
  });
}

export function setupCommentLike({ baseUrl, authHeader, jsonHeaders, seed }) {
  if (!seed.postId || !seed.commentId || !authHeader) return;
  http.post(`${baseUrl}/posts/${seed.postId}/comments/${seed.commentId}/like`, null, {
    headers: { ...jsonHeaders, Authorization: authHeader },
    tags: { name: "SETUP_COMMENT_LIKE" },
  });
}

export function preparePostTarget({ baseUrl, authHeader, jsonHeaders, seed }) {
  requireAuth(authHeader, "Post target setup requires auth material.");
  const postId = createPost({ baseUrl, authHeader, jsonHeaders, suffix: `target-${__VU}-${__ITER}` });
  return { ...seed, postId };
}

export function prepareCommentTarget({ baseUrl, authHeader, jsonHeaders, seed }) {
  const nextSeed = preparePostTarget({ baseUrl, authHeader, jsonHeaders, seed });
  const commentId = createComment({
    baseUrl,
    authHeader,
    jsonHeaders,
    postId: nextSeed.postId,
    suffix: `target-${__VU}-${__ITER}`,
  });
  return { ...nextSeed, commentId };
}

export function preparePostLikeTarget(context) {
  const seed = preparePostTarget(context);
  setupPostLike({ ...context, seed });
  return seed;
}

export function prepareCommentLikeTarget(context) {
  const seed = prepareCommentTarget(context);
  setupCommentLike({ ...context, seed });
  return seed;
}

function requireAuth(authHeader, message) {
  if (!authHeader) {
    fail(message);
  }
}
