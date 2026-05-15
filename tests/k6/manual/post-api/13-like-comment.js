import { createPostApiUnitTest } from "./lib/post-api-unit.js";

const test = createPostApiUnitTest("13_POST_COMMENT_LIKE");

export const options = test.options;
export const setup = test.setup;
export const teardown = test.teardown;
export const handleSummary = test.handleSummary;

export function runTarget(data) {
  test.runTarget(data);
}

