# PostForge

사용자가 올린 자료로 학습하는 서비스. 자료에서 근거 문장이 그대로 있는 복습 문제를 만들고, 간격 반복·빈 페이지 정리·가르치기·꼬리질문으로 기억을 다진다. 채점은 학습자가 하고, LLM은 버튼을 누를 때만 부른다. 전환 근거·원칙·게이트 기준은 [ADR-008](docs/decisions/adr-008-switch-to-learning-platform.md), 학습 API와 규칙은 [API 명세](docs/api/README.md#study)를 따른다.

뉴스 수집·자동 게시·데일리 종합은 ADR-008에 따라 지웠다. 그 시절의 RAG 채팅·문서 적재·첨부파일은 같은 ADR의 삭제 목록에 따라 지우는 중이다. 게시판과 로그인은 유지한다.

## 모듈 경계

Gradle modular monolith. 의존성 허용 표와 새 코드 배치 기준은 [module-dependencies.md](docs/architecture/module-dependencies.md)가 단일 기준이고, `app/src/test/.../ModuleBoundaryTest.java`가 강제한다.

| 모듈 | 책임 |
|------|------|
| `core` | 모듈 간 계약 (interface, record, 공통 예외) |
| `support` | 공통 Spring 인프라 (`@Configuration`, advice, redis/persistence helper) |
| `app` | 조립 + 실행 |
| `auth` | 로그인, refresh token rotation |
| `board` | 게시글·댓글·좋아요·조회수·파일 |
| `ai` | `chat`·`search`(RAG), `study`(학습 문제 초안·AI 학생 질문·꼬리질문) |
| `ingest` | `document`(pgvector 적재) |
| `study` | 사용자 자료 학습: 근거가 검증된 복습 문제·간격 반복·빈 페이지 정리·가르치기·꼬리질문·잔디·게이트 지표 (`/study.html`) |

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
