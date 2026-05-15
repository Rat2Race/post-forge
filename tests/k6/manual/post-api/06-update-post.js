import { createPostApiUnitTest } from "./lib/post-api-unit.js";

const test = createPostApiUnitTest("06_PUT_POSTS_UPDATE");

export const options = test.options;
export const setup = test.setup;
export const teardown = test.teardown;
export const handleSummary = test.handleSummary;

export function runTarget(data) {
  test.runTarget(data);
}

