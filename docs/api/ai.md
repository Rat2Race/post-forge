# AI API

## Endpoint 요약

| Method | Path | Auth | Success | Request | Response |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/ai/chat` | USER/ADMIN | `200` | `ChatRequest` | `ChatResponse` |
| `POST` | `/api/ai/generate` | USER/ADMIN | `200` | `PostDraftGenerateRequest` | `PostDraftResponse` |

## Request DTO

| DTO | Fields |
| --- | --- |
| `ChatRequest` | `message` required |
| `PostDraftGenerateRequest` | `prompt?` max 1000, `topic?` max 200, `title?` max 100, `summary?` max 500, `tags?` max 20 items and each max 50, `category?`; at least one of `prompt` or `topic` must contain text |

## Response DTO

| DTO | Fields |
| --- | --- |
| `ChatResponse` | `answer` |
| `PostDraftResponse` | `title`, `content`, `summary`, `tags`, `category` |

## Enums

| Enum | Values |
| --- | --- |
| `PostCategory` | `GENERAL`, `AI_ANALYSIS`, `PRODUCT_LAUNCH_NEWS` |
