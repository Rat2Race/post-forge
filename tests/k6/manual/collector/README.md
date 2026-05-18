# Collector k6 scripts

These scripts are manual, opt-in checks for collector endpoints. They can trigger external API calls and write collected data, so keep the default load low and run them only against local or explicitly approved staging-like targets.

## Files

| Script | Target |
|---|---|
| `01-trigger-collector.js` | `POST /collector/{source}` |

## Example

```bash
BASE_URL=http://127.0.0.1:8080 \
INTERNAL_API_KEY="$INTERNAL_API_KEY" \
COLLECTOR_SOURCE=naver-news \
COLLECTOR_VUS=1 \
COLLECTOR_ITERATIONS=1 \
k6 run tests/k6/manual/collector/01-trigger-collector.js
```

Use `COLLECTOR_INTERNAL_API_KEY` when you want this script to use a collector-only secret instead of the shared `INTERNAL_API_KEY` shell variable.
