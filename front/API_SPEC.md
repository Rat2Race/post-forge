# API 명세서

아래는 현재 레포의 컨트롤러/DTO 기준으로 정리한, 프론트엔드에서 사용하는 엔드포인트 명세입니다. 별도의 서버 코드 변경 없이 사용할 수 있도록 작성되었습니다.

## 공통 전제

- Base URLs
  - Auth Service: `${VITE_AUTH_BASE_URL}` (기본값: `http://localhost:8080`)
  - Board Service: `${VITE_BOARD_BASE_URL}` (기본값: `http://localhost:8081`)
- 인증 헤더: `Authorization: Bearer <accessToken>`
- 에러 응답: 상황에 따라 문자열 또는 JSON. 본 프론트는 응답의 `content-type`을 확인해 파싱합니다.

---

## Auth Service

Base: `${VITE_AUTH_BASE_URL}`

### 회원가입
- Method/Path: `POST /api/auth/signup`
- Request Body (JSON):
  ```json
  { "name": "홍길동", "id": "user01", "pw": "Abcdef12!" }
  ```
- Response: 문자열(메시지) 또는 JSON (서버 구성에 따라 상이)
- Status: `201 Created` 성공

### 로그인
- Method/Path: `POST /api/auth/login`
- Request Body (JSON):
  ```json
  { "id": "user01", "pw": "Abcdef12!" }
  ```
- Response Body (JSON):
  ```json
  { "grantType": "Bearer", "accessToken": "...", "refreshToken": "..." }
  ```
- Status: `200 OK` 성공

### 토큰 재발급
- Method/Path: `POST /api/auth/reissue`
- Auth: 필요(서버 @PreAuthorize). 프론트는 보유한 `refreshToken`을 사용합니다.
- Request Body (JSON):
  ```json
  { "refreshToken": "..." }
  ```
- Response Body (JSON): 로그인과 동일한 토큰 구조
- Status: `200 OK` 성공

### 로그아웃
- Method/Path: `POST /api/auth/logout`
- Auth: 필요 (@PreAuthorize)
- Request Body: 없음
- Response: 문자열(메시지)
- Status: `200 OK` 성공

### 내 프로필 조회
- Method/Path: `GET /api/user/profile`
- Auth: 필요 (@PreAuthorize)
- Response Body (JSON):
  ```json
  {
    "id": 1,
    "name": "홍길동",
    "userId": "user01",
    "roles": ["ROLE_USER"]
  }
  ```
- Status: `200 OK` 성공

---

## Board Service

Base: `${VITE_BOARD_BASE_URL}`

### 게시글 생성
- Method/Path: `POST /articles/create`
- Request Body (JSON):
  ```json
  { "title": "제목", "content": "내용" }
  ```
- Response Body (JSON):
  ```json
  1
  ```
  (생성된 게시글 ID, 숫자)
- Status: `201 Created` 성공

### 게시글 목록 조회
- Method/Path: `GET /articles/read`
- Response Body (JSON): `ArticleDto[]`
  ```json
  [
    { "id": 1, "title": "제목", "content": "내용" }
  ]
  ```
- Status: `200 OK` 성공

### 게시글 단건 조회
- Method/Path: `GET /articles/read/{id}`
- Path Params: `id` (number)
- Response Body (JSON): `ArticleDto`
  ```json
  { "id": 1, "title": "제목", "content": "내용" }
  ```
- Status: `200 OK` 성공

### 게시글 수정
- Method/Path: `PUT /articles/update?id={id}`
- Query Params: `id` (number)
- Request Body (JSON): `ArticleDto`
  ```json
  { "title": "수정제목", "content": "수정내용" }
  ```
- Response: 문자열(메시지)
- Status: `200 OK` 성공

### 게시글 삭제
- Method/Path: `DELETE /articles/delete?id={id}`
- Query Params: `id` (number)
- Response: 문자열(메시지)
- Status: `200 OK` 성공

### 댓글 생성 (데모)
- Method/Path: `POST /comments/create/{article_id}`
- Path Params: `article_id` (number)
- Response Body (JSON): 댓글 ID (number)
- Status: `200 OK` 성공

---

## 응답/에러 규칙 메모

- 일부 엔드포인트는 문자열 메시지(plain text)를 반환합니다(`signup`, `logout`, `update`, `delete`).
- 본 프론트는 `content-type`을 확인하여 JSON/문자열을 구분 파싱합니다.
- 인증 필요한 요청에서 401이 발생하면 `reissue`를 통해 토큰을 재발급 후 원 요청을 1회 재시도합니다. 재시도 실패 시 로그인 화면으로 유도하세요.

