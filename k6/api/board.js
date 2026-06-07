import { ApiScenario } from "../lib/api-scenario.js";
import {
  prepareCommentLikeTarget,
  prepareCommentTarget,
  preparePostLikeTarget,
  preparePostTarget,
  setupCommentLike,
  setupPostLike,
} from "../lib/fixtures.js";

function postBody() {
  return {
    title: `k6 target ${Date.now()}`,
    content: "k6 benchmark content for board post write path.",
    summary: "k6 target",
    tags: ["k6", "benchmark"],
    category: "GENERAL",
    fileIds: [],
  };
}

function commentBody() {
  return {
    parentId: null,
    content: `k6 benchmark comment ${Date.now()}`,
  };
}

export const boardScenarios = [
  new ApiScenario({
    key: "board.post.list",
    method: "GET",
    path: "/posts?page=0&size=20",
  }),
  new ApiScenario({
    key: "board.post.search",
    method: "GET",
    path: "/posts?keyword=k6&page=0&size=20",
  }),
  new ApiScenario({
    key: "board.post.auto-price-drops",
    method: "GET",
    path: "/posts/auto/price-drops",
  }),
  new ApiScenario({
    key: "board.post.detail",
    method: "GET",
    path: "/posts/{postId}",
    seedPost: true,
  }),
  new ApiScenario({
    key: "board.post.view-count",
    method: "GET",
    path: "/posts/{postId}",
    seedPost: true,
  }),
  new ApiScenario({
    key: "board.comment.list",
    method: "GET",
    path: "/posts/{postId}/comments?page=0&size=50",
    seedPost: true,
  }),
  new ApiScenario({
    key: "board.post.create",
    method: "POST",
    path: "/posts",
    requiresAuth: true,
    body: postBody,
  }),
  new ApiScenario({
    key: "board.post.update",
    method: "PUT",
    path: "/posts/{postId}",
    requiresAuth: true,
    seedPost: true,
    body: postBody,
    prepareTarget: preparePostTarget,
  }),
  new ApiScenario({
    key: "board.post.delete",
    method: "DELETE",
    path: "/posts/{postId}",
    requiresAuth: true,
    seedPost: true,
    prepareTarget: preparePostTarget,
  }),
  new ApiScenario({
    key: "board.post.like",
    method: "POST",
    path: "/posts/{postId}/like",
    requiresAuth: true,
    seedPost: true,
    prepareTarget: preparePostTarget,
  }),
  new ApiScenario({
    key: "board.post.unlike",
    method: "DELETE",
    path: "/posts/{postId}/like",
    requiresAuth: true,
    seedPost: true,
    prepare: setupPostLike,
    prepareTarget: preparePostLikeTarget,
  }),
  new ApiScenario({
    key: "board.comment.create",
    method: "POST",
    path: "/posts/{postId}/comments",
    requiresAuth: true,
    seedPost: true,
    body: commentBody,
  }),
  new ApiScenario({
    key: "board.comment.update",
    method: "PUT",
    path: "/posts/{postId}/comments/{commentId}",
    requiresAuth: true,
    seedPost: true,
    seedComment: true,
    body: commentBody,
    prepareTarget: prepareCommentTarget,
  }),
  new ApiScenario({
    key: "board.comment.delete",
    method: "DELETE",
    path: "/posts/{postId}/comments/{commentId}",
    requiresAuth: true,
    seedPost: true,
    seedComment: true,
    prepareTarget: prepareCommentTarget,
  }),
  new ApiScenario({
    key: "board.comment.like",
    method: "POST",
    path: "/posts/{postId}/comments/{commentId}/like",
    requiresAuth: true,
    seedPost: true,
    seedComment: true,
    prepareTarget: prepareCommentTarget,
  }),
  new ApiScenario({
    key: "board.comment.unlike",
    method: "DELETE",
    path: "/posts/{postId}/comments/{commentId}/like",
    requiresAuth: true,
    seedPost: true,
    seedComment: true,
    prepare: setupCommentLike,
    prepareTarget: prepareCommentLikeTarget,
  }),
];
