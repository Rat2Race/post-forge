import { createPostApiUnitTest } from "./lib/post-api-unit.js";

const test = createPostApiUnitTest("07_DELETE_POSTS_DELETE");

export const options = test.options;
export const setup = test.setup;
export const teardown = test.teardown;
export const handleSummary = test.handleSummary;

export function runTarget(data) {
  test.runTarget(data);
}

