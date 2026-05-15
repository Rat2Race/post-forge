import { createPostApiUnitTest } from "./lib/post-api-unit.js";

const test = createPostApiUnitTest("05_POST_POSTS_CREATE");

export const options = test.options;
export const setup = test.setup;
export const teardown = test.teardown;
export const handleSummary = test.handleSummary;

export function runTarget(data) {
  test.runTarget(data);
}

