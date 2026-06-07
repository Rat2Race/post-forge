export class ApiScenario {
  constructor({
    key,
    method,
    path,
    requiresAuth = false,
    seedPost = false,
    seedComment = false,
    seedProduct = false,
    body = null,
    prepare = null,
    prepareTarget = null,
  }) {
    this.key = key;
    this.method = method;
    this.path = path;
    this.requiresAuth = requiresAuth;
    this.seedPost = seedPost;
    this.seedComment = seedComment;
    this.seedProduct = seedProduct;
    this.body = body;
    this.prepare = prepare;
    this.prepareTarget = prepareTarget;
  }

  resolvePath(seed) {
    return this.path
      .replace("{postId}", String(seed.postId || ""))
      .replace("{commentId}", String(seed.commentId || ""))
      .replace("{productId}", String(seed.productId || ""));
  }

  requestBody(seed) {
    if (!this.body) {
      return null;
    }
    return JSON.stringify(this.body(seed));
  }
}
