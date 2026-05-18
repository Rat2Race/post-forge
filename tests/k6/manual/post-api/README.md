# Post API unit k6 scripts

Use these when investigating a concrete Post API bottleneck. Run one script at a time so the result points to the endpoint under investigation instead of mixing unrelated read, write, like, and cleanup traffic.

Each script measures one Post API as the target transaction. Login, seed, and cleanup requests are tagged separately and excluded from target RPS/TPS counters.

## Files

| Script | Target |
|---|---|
| `01-get-posts.js` | `GET /posts` |
| `02-search-posts.js` | `GET /posts?keyword` |
| `03-get-post-detail.js` | `GET /posts/{id}` |
| `04-get-post-comments.js` | `GET /posts/{id}/comments` |
| `05-create-post.js` | `POST /posts` |
| `06-update-post.js` | `PUT /posts/{id}` |
| `07-delete-post.js` | `DELETE /posts/{id}` |
| `08-like-post.js` | `POST /posts/{id}/like` |
| `09-unlike-post.js` | `DELETE /posts/{id}/like` |
| `10-create-comment.js` | `POST /posts/{id}/comments` |
| `11-update-comment.js` | `PUT /posts/{id}/comments/{commentId}` |
| `12-delete-comment.js` | `DELETE /posts/{id}/comments/{commentId}` |
| `13-like-comment.js` | `POST /posts/{id}/comments/{commentId}/like` |
| `14-unlike-comment.js` | `DELETE /posts/{id}/comments/{commentId}/like` |

## Example

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
REPORT_DIR="docs/performance/manual-runs/$RUN_ID"
mkdir -p "$REPORT_DIR"

BASE_URL=http://127.0.0.1:8080 \
PERF_USER_ID=perfpost01 \
PERF_PASSWORD="$PERF_PASSWORD" \
TEST_SECONDS=60 \
API_VUS=1 \
K6_REPORT_NAME="$RUN_ID-01-get-posts" \
K6_REPORT_DIR="$REPORT_DIR" \
K6_SUMMARY_DIR="$REPORT_DIR" \
k6 run tests/k6/manual/post-api/01-get-posts.js
```

Use `PERF_POST_ID` and `PERF_COMMENT_ID` when you want to target existing fixtures instead of letting the script create temporary seed data.

When running many unit scripts back-to-back, prefer `PERF_ACCESS_TOKEN` or `PERF_AUTH_HEADER` to avoid measuring or rate-limiting `/auth/login` itself.
