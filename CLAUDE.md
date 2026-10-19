# PostForge

게시판 서비스 백엔드. 회원가입·로그인과 게시글·댓글·좋아요·조회수를 다루고, 구현·측정·트러블슈팅 과정을 기술 블로그 글로 남긴다. API는 [API 명세](docs/api/README.md)를 따른다.

학습 플랫폼은 `release/postforge-study` 브랜치에서 따로 진행한다. 이 브랜치에서 study 모듈을 뗀 근거는 [ADR-009](docs/decisions/adr-009-split-study-from-board.md)에 있다. 뉴스 수집·자동 게시·데일리 종합과 그 시절의 초안 생성기·RAG 채팅·문서 적재·첨부파일은 ADR-008에 따라 지웠다.

## 모듈 경계

Gradle modular monolith. 모듈 사이 의존은 각 모듈 `build.gradle`의 `implementation project(...)`가 정한다.

| 모듈 | 책임 |
|------|------|
| `core` | 모듈 간 계약 (interface, record, 공통 예외) |
| `support` | 공통 Spring 인프라 (`@Configuration`, advice, redis/persistence helper) |
| `app` | 조립 + 실행 |
| `auth` | 로그인, refresh token rotation |
| `board` | 게시글·댓글·좋아요·조회수 |
| `ai` | LLM 호출 경로(`TextGenerationClient`)와 설정. 아직 이를 쓰는 기능은 없다 |

이전 제품 기능을 제거한 배경은 [ADR-004](docs/decisions/adr-004-scope-reduction.md)에 이력으로 보존한다. 새 작업의 범위는 현재 제품 정의와 실제 코드에서 판단한다.

## 작업 규칙

- 커밋 메시지는 한국어, `type(scope): 요약`.
- 문서가 코드보다 오래 살아남지 않게 한다 — 동작을 바꾸면 해당 문서를 같은 커밋에서 고친다.
- 삭제 결정의 근거만 ADR로 남기고, 코드 보존을 위한 주석·비활성 코드는 남기지 않는다.

## 개발 환경 재현

다른 머신에서 이 프로젝트를 같은 조건으로 열기 위한 목록. 개인 설정(`~/.claude`)이라 레포가 아니라 각 머신에 설치한다.

### 플러그인

```bash
claude plugin marketplace add anthropics/claude-plugins-official && claude plugin marketplace add upstash/context7 && claude plugin marketplace add obra/superpowers-marketplace && claude plugin marketplace add Egonex-AI/Understand-Anything && claude plugin marketplace add DietrichGebert/ponytail && claude plugin marketplace add Yeachan-Heo/oh-my-claudecode && claude plugin marketplace add anthropics/claude-plugins-community
```

```bash
claude plugin install learning-output-style@claude-plugins-official && claude plugin install context7@context7-marketplace && claude plugin install superpowers@superpowers-marketplace && claude plugin install understand-anything@understand-anything && claude plugin install ponytail@ponytail && claude plugin install oh-my-claudecode@omc && claude plugin install eli5@claude-community
```

| 플러그인 | 마켓플레이스 repo | 역할 |
|---|---|---|
| `superpowers` | `obra/superpowers-marketplace` | 프로세스 규율 (brainstorming, TDD, systematic-debugging) |
| `oh-my-claudecode` | `Yeachan-Heo/oh-my-claudecode` | 오케스트레이션·모델 라우팅 (autopilot, ralph, team, ask) |
| `ponytail` | `DietrichGebert/ponytail` | 과잉 설계 억제 페르소나. 기본 `full` |
| `understand-anything` | `Egonex-AI/Understand-Anything` | 코드베이스 지식 그래프 |
| `context7` | `upstash/context7` | 라이브러리 최신 문서 조회 |
| `learning-output-style` | `anthropics/claude-plugins-official` | 학습형 출력 스타일 |
| `eli5` | `anthropics/claude-plugins-community` | `/eli5 <주제>` — 그림 위주 HTML 설명서 생성 |

### 개인 스킬 (`~/.claude/skills/`)

- **mattpocock/skills** 22개 — `git clone https://github.com/mattpocock/skills` 후 engineering·productivity 스킬을 `~/.claude/skills/`로 복사. `code-review`·TDD는 내장/superpowers와 중복이라 제외한다.
  `ask-matt` `codebase-design` `diagnosing-bugs` `domain-modeling` `grill-me` `grill-with-docs` `grilling` `handoff` `implement` `improve-codebase-architecture` `prototype` `research` `resolving-merge-conflicts` `teach` `to-questionnaire` `to-spec` `to-tickets` `triage` `wait-what` `wayfinder` `wizard` `writing-for-agents`
- **karpathy-guidelines**, **skill-creator** — 외부 출처
- **skill-router** — 직접 만든 라우터. 개발/피드백 요청을 위 스킬로 분배한다. **복사본이 이 목록에 없으므로 `~/.claude` 백업에서만 복구된다.**

mattpocock 스킬 다수는 `disable-model-invocation`(slash 전용)이라 자동 로드되지 않는다.

### 그 외

- `~/.claude/settings.json`, `mcp.json`, `~/.claude/projects/*/memory/`는 private 레포로 동기화. `.credentials.json`은 머신별 로그인이므로 제외.
- 프로젝트 메모리 경로가 워크스페이스 절대경로 기반이라, 다른 머신에서도 `~/spring-workspace/post-forge`에 클론해야 붙는다.
