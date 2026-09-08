# PostForge

외부 뉴스를 수집·분류하고 LLM으로 초안을 작성해 스케줄에 맞춰 자동 게시하는 서비스. 매일 06:00(Asia/Seoul)에 전날 게시된 뉴스를 분야별로 종합한 데일리 포스트도 자동 게시한다. 메일 구독은 향후 계획이며 아직 구현되지 않았다.

현재 자동 게시 대상은 신제품 출시뉴스다. 분야는 수집 키워드 또는 수동 요청에서 정하고, LLM은 본문·요약·태그 초안을 만든다. 스케줄과 활성화 조건은 [API 명세](docs/api/README.md#자동-게시-스케줄), 제품 범위와 메일 구독 계획은 [ADR-005](docs/decisions/adr-005-subscription-information-service.md)를 따른다.

## 모듈 경계

Gradle modular monolith. 의존성 허용 표와 새 코드 배치 기준은 [module-dependencies.md](docs/architecture/module-dependencies.md)가 단일 기준이고, `app/src/test/.../ModuleBoundaryTest.java`가 강제한다.

| 모듈 | 책임 |
|------|------|
| `core` | 모듈 간 계약 (interface, record, 공통 예외) |
| `support` | 공통 Spring 인프라 (`@Configuration`, advice, redis/persistence helper) |
| `app` | 조립 + 실행 |
| `auth` | 로그인, refresh token rotation |
| `board` | 게시글·댓글·좋아요·조회수·파일 |
| `ai` | `chat`·`search`(RAG), `draft`(뉴스·데일리 포스트 초안 생성) |
| `ingest` | `document`(pgvector 적재), `news`(뉴스 수집·선별·스케줄 게시·데일리 종합) |
| `source` | 외부 뉴스 source adapter (Naver API HUB News) |

이전 제품 기능을 제거한 배경은 [ADR-004](docs/decisions/adr-004-scope-reduction.md)에 이력으로 보존한다. 새 작업의 범위는 현재 제품 정의와 실제 코드에서 판단한다.

## 작업 규칙

- 커밋 메시지는 한국어, `type(scope): 요약`.
- 문서가 코드보다 오래 살아남지 않게 한다 — 동작을 바꾸면 해당 문서를 같은 커밋에서 고친다.
- 삭제 결정의 근거만 ADR로 남기고, 코드 보존을 위한 주석·비활성 코드는 남기지 않는다.

## 개발 환경 재현

다른 머신에서 이 프로젝트를 같은 조건으로 열기 위한 목록. 개인 설정(`~/.claude`)이라 레포가 아니라 각 머신에 설치한다.

### 플러그인

```bash
claude plugin marketplace add anthropics/claude-plugins-official && claude plugin marketplace add upstash/context7 && claude plugin marketplace add obra/superpowers-marketplace && claude plugin marketplace add Egonex-AI/Understand-Anything && claude plugin marketplace add DietrichGebert/ponytail && claude plugin marketplace add Yeachan-Heo/oh-my-claudecode
```

```bash
claude plugin install learning-output-style@claude-plugins-official && claude plugin install context7@context7-marketplace && claude plugin install superpowers@superpowers-marketplace && claude plugin install understand-anything@understand-anything && claude plugin install ponytail@ponytail && claude plugin install oh-my-claudecode@omc
```

| 플러그인 | 마켓플레이스 repo | 역할 |
|---|---|---|
| `superpowers` | `obra/superpowers-marketplace` | 프로세스 규율 (brainstorming, TDD, systematic-debugging) |
| `oh-my-claudecode` | `Yeachan-Heo/oh-my-claudecode` | 오케스트레이션·모델 라우팅 (autopilot, ralph, team, ask) |
| `ponytail` | `DietrichGebert/ponytail` | 과잉 설계 억제 페르소나. 기본 `full` |
| `understand-anything` | `Egonex-AI/Understand-Anything` | 코드베이스 지식 그래프 |
| `context7` | `upstash/context7` | 라이브러리 최신 문서 조회 |
| `learning-output-style` | `anthropics/claude-plugins-official` | 학습형 출력 스타일 |

### 개인 스킬 (`~/.claude/skills/`)

- **mattpocock/skills** 22개 — `git clone https://github.com/mattpocock/skills` 후 engineering·productivity 스킬을 `~/.claude/skills/`로 복사. `code-review`·TDD는 내장/superpowers와 중복이라 제외한다.
  `ask-matt` `codebase-design` `diagnosing-bugs` `domain-modeling` `grill-me` `grill-with-docs` `grilling` `handoff` `implement` `improve-codebase-architecture` `prototype` `research` `resolving-merge-conflicts` `teach` `to-questionnaire` `to-spec` `to-tickets` `triage` `wait-what` `wayfinder` `wizard` `writing-for-agents`
- **karpathy-guidelines**, **skill-creator** — 외부 출처
- **skill-router** — 직접 만든 라우터. 개발/피드백 요청을 위 스킬로 분배한다. **복사본이 이 목록에 없으므로 `~/.claude` 백업에서만 복구된다.**

mattpocock 스킬 다수는 `disable-model-invocation`(slash 전용)이라 자동 로드되지 않는다.

### 그 외

- `~/.claude/settings.json`, `mcp.json`, `~/.claude/projects/*/memory/`는 private 레포로 동기화. `.credentials.json`은 머신별 로그인이므로 제외.
- 프로젝트 메모리 경로가 워크스페이스 절대경로 기반이라, 다른 머신에서도 `~/spring-workspace/post-forge`에 클론해야 붙는다.
