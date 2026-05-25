# Local Test Console PRD

## Problem Statement

현재 테스트 환경은 Bruno, k6, setup runner, generated artifacts로 구성되어 있지만 사용자가 테스트를 실행하고 결과를 해석하려면 여러 CLI 명령과 JSON/Markdown 산출물을 직접 오가야 한다.

이 방식은 학습과 초기 성능 개선 단계에서는 충분히 유연하지만, 반복 실행이 늘어날수록 다음 문제가 생긴다.

- 테스트 실행 전 필요한 매개변수를 매번 기억해야 한다.
- Bruno와 k6 결과를 한 화면에서 비교하기 어렵다.
- 실행 결과, 병목 징후, 개선 방향이 산출물에 흩어진다.
- 나중에 CI/CD harness로 승격할 때 어떤 입력과 결과가 표준인지 흐려진다.
- Agent가 테스트를 실행할 때도 사람이 현재 상태를 빠르게 확인하기 어렵다.

사용자는 CLI 기반 하네스는 유지하되, 그 위에 얇은 GUI를 올려 테스트 실행과 결과 확인을 더 쉽게 만들고 싶다.

## Solution

로컬 전용 Test Console을 만든다. Test Console은 Bruno나 k6를 대체하지 않고, 이미 존재하는 setup runner와 테스트 산출물을 안전하게 실행/조회하는 orchestration UI 역할만 맡는다.

첫 버전은 다음에 집중한다.

- 테스트 대상 BASE_URL 입력
- Bruno smoke 실행
- k6 smoke 실행
- 통합 smoke 실행
- 최근 실행 결과 보기
- 실패/병목/개선 방향 요약 보기
- raw JSON/Markdown report 보기

브라우저는 로컬 shell command를 직접 실행할 수 없으므로, 작은 local runner API를 둔다. 이 API는 임의 shell command를 받지 않고 사전에 허용된 테스트 액션만 실행한다.

## User Stories

1. As a backend developer, I want to run smoke tests from a local UI, so that I do not need to remember every CLI command.
2. As a backend developer, I want to set BASE_URL before running tests, so that I can target local, staging-like, or containerized environments.
3. As a backend developer, I want to see whether Bruno CLI and k6 are installed, so that I can detect setup problems before running tests.
4. As a backend developer, I want a single run button for the combined smoke harness, so that I can quickly validate the API surface.
5. As a backend developer, I want separate Bruno and k6 run controls, so that I can isolate functional smoke failures from performance smoke failures.
6. As a backend developer, I want to see the latest test result summary, so that I can immediately know pass/fail status.
7. As a backend developer, I want to see Bruno request status codes, so that I can identify broken endpoints.
8. As a backend developer, I want to see k6 latency and failure-rate metrics, so that I can spot early performance regressions.
9. As a backend developer, I want findings and improvement directions surfaced in the UI, so that I can move from result to next action faster.
10. As a backend developer, I want raw report access, so that I can inspect details when the summary is not enough.
11. As a backend developer, I want the UI to show which generated tests exist, so that I can understand what is currently covered.
12. As a backend developer, I want manual test areas clearly marked, so that generated and hand-written tests do not get confused.
13. As a backend developer, I want long-running test actions to show progress, so that I know whether the runner is still working.
14. As a backend developer, I want failed commands to show safe stderr/stdout tails, so that I can diagnose without opening multiple files.
15. As a backend developer, I want dangerous actions excluded from the UI, so that I cannot accidentally run arbitrary shell commands.
16. As a backend developer, I want production-like high-load testing disabled in the MVP, so that early UI testing remains low risk.
17. As a backend developer, I want secrets to stay out of the UI by default, so that tokens are not accidentally persisted or exposed.
18. As a backend developer, I want the console to read existing state/report artifacts, so that CLI and GUI remain consistent.
19. As a backend developer, I want all GUI-triggered runs to write the same artifacts as CLI-triggered runs, so that future CI integration is straightforward.
20. As an Agent operator, I want a stable API for test runs, so that an LLM agent can trigger allowed test actions without inventing commands.
21. As an Agent operator, I want structured run results, so that the agent can summarize pass/fail, bottlenecks, and next improvements.
22. As a future CI maintainer, I want the local console to use the same runner contract as CI, so that local and pipeline behavior do not drift.
23. As a future maintainer, I want the UI to be small and modular, so that new test tools can be added without rewriting the console.

## Implementation Decisions

- Build a local-only Test Console, not a hosted QA platform.
- Keep the CLI runner as the source of truth. The GUI calls allowed runner actions and reads generated artifacts.
- Use a small local runner API between the browser and shell execution.
- The runner API must expose action-based endpoints rather than accepting arbitrary shell commands.
- Initial allowed actions are setup verification, generated test refresh, smoke run, Bruno smoke run, and k6 smoke run.
- Define a parameter schema for each action. MVP parameters include BASE_URL and optional smoke path/scenario selection.
- Store run metadata as structured artifacts so the UI, Agent, and future CI can read the same result shape.
- Treat report rendering as a read-only artifact viewer.
- Keep secrets out of MVP. Token-bearing scenario execution can be added later with an explicit secret handling design.
- Separate UI state from test result artifacts. UI preferences should not mutate test policy or generated test files.
- Do not rebuild Bruno or Grafana. Bruno remains the API collection tool, k6 remains the performance tool, and external dashboards can be integrated later.
- The UI should be dense and operational, not a marketing page.
- The first screen should be the usable console: environment controls, run actions, status cards, and latest result summary.
- Test Console should be easy to remove or ignore. Existing CLI setup must continue to work without the GUI.

Recommended modules:

- Implementation placement: PostForge keeps this surface in the `ops` module under `dev.iamrat.ops.testconsole.*`, and the executable `app` only composes it.
- Console UI: renders controls, status cards, result summaries, and raw reports.
- Runner API: exposes local-only allowed actions.
- Command Runner: maps allowed actions to runner commands and enforces timeouts.
- Artifact Reader: reads structured state and report artifacts.
- Result Normalizer: converts Bruno/k6/setup outputs into a stable UI result model.
- Parameter Schema: defines allowed inputs per action.
- Safety Policy: blocks arbitrary commands, unsafe targets, and unsupported high-load actions.

## Testing Decisions

- Tests should validate external behavior: allowed actions, blocked actions, rendered result states, and artifact parsing.
- Do not test implementation details such as specific component internals or shell command construction beyond the public action contract.
- Runner API tests should verify that unknown actions are rejected.
- Runner API tests should verify that allowed actions receive only declared parameters.
- Command Runner tests should cover success, failure, timeout, and missing tool cases.
- Artifact Reader tests should cover missing artifacts, malformed JSON, and valid latest result artifacts.
- Result Normalizer tests should cover Bruno pass/fail output and k6 latency/failure-rate metrics.
- UI tests should cover parameter entry, run button disabled states, loading state, pass/fail status cards, findings display, and raw report rendering.
- Safety tests should ensure arbitrary command strings cannot be submitted from the UI or API.
- Smoke-level manual verification should run the console against a local API server and confirm that Bruno/k6 results match CLI artifacts.
- The MVP does not need full browser automation for every visual state, but at least one end-to-end local run should prove the browser can trigger a safe test action and render the result.

## Out of Scope

- Replacing Bruno Desktop or Bruno CLI.
- Replacing Grafana, k6 Cloud, or dedicated performance dashboards.
- Arbitrary shell command execution from the browser.
- Production load testing controls.
- Secret/token storage.
- Multi-user authentication.
- Hosted deployment.
- Long-term historical analytics.
- CI/CD pipeline implementation.
- Scenario creation/editing UI.
- Generated test authoring UI.
- Editing testing policy from the UI.

## Further Notes

The correct MVP is small: a local console that makes the existing testing harness visible and clickable.

The most important architectural boundary is safety. The UI should never become a general terminal. It should call a small set of declared actions and read a small set of declared artifacts.

The second most important boundary is ownership. Bruno and k6 keep their native responsibilities. The Test Console only coordinates and summarizes.

The third boundary is future portability. If the local runner and CI runner share the same action/result model, this console can become a stepping stone toward CI/CD harness testing rather than a dead-end side tool.
