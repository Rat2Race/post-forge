# PostForge

신상품 출시 뉴스를 자동으로 수집·게시하고, 커뮤니티가 구매 판단 투표(`BUYABLE`/`UNSURE`/`WAIT`)를 내리는 서비스.

이 한 문장에 기여하지 않는 축은 동결이 아니라 **삭제**한다. 근거와 전체 판단 기준은 [ADR-004](docs/decisions/adr-004-scope-reduction.md).

## 모듈 경계

Gradle modular monolith. 의존성 허용 표와 새 코드 배치 기준은 [module-dependencies.md](docs/architecture/module-dependencies.md)가 단일 기준이고, `app/src/test/.../ModuleBoundaryTest.java`가 강제한다.

**주의: ADR-004 스코프 컷이 아직 적용되지 않았다.** `settings.gradle`, `module-dependencies.md`, README는 컷 이전 상태이므로 아래 표를 목표 상태로 본다.

| 모듈 | 처분 | 이유 |
|------|------|------|
| `core` | 유지 | 모듈 간 계약 (interface, record, 공통 예외) |
| `support` | 유지 | 공통 Spring 인프라 (`@Configuration`, advice, redis/persistence helper) |
| `app` | 유지 | 조립 + 실행 |
| `auth` | 유지 | 로그인, refresh token rotation |
| `board` | 유지 | 출시뉴스 게시글 + 구매 판단 투표 — 제품의 코어 |
| `ai` | 유지 | `chat`·`search`(RAG), `draft`(뉴스 초안 생성) |
| `ingest` | 축소 | `document`(pgvector 적재)·`news` 유지, `product` 삭제 |
| `source` | 축소 | `news` 유지, `product` 삭제 (Naver Shopping API 종료로 살아있는 소스 0개) |
| `catalog` | 삭제 | 상품 임베딩 매칭 — 매칭할 상품 데이터가 없음 |
| `price` | 삭제 | `check`는 커뮤니티 투표가 대체, `tracking`은 가격 소스가 없어 항상 503 |
| `messaging` | 삭제 | outbox 소비자가 존재한 적 없음 |

실행 순서와 체크리스트는 [scope-cut-plan.md](docs/scope-cut-plan.md). 컷이 끝나면 그 파일을 지우고 이 절의 표도 "현재 구조"로 바꾼다.

선행 블로커 하나만 여기 옮겨 적는다: 생존 코드인 `ingest/news`의 `LaunchNewsPublishScheduler`가 삭제 대상인 `ingest/product`의 `TrackedKeyword`를 참조하고, 그게 다시 `source/product`의 `SourceType`을 참조한다. `TrackedKeyword`를 `ingest/news`로 먼저 옮기지 않으면 삭제 즉시 컴파일이 깨진다.

복구는 컷 직전에 찍는 `pre-scope-cut` 태그가 담당한다(아직 없음). 동결 브랜치나 feature flag는 두지 않는다.

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
