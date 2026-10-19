#!/usr/bin/env bash
# 업그레이드 전후를 같은 조건으로 잰다. 레포 루트의 .env.local과 docker-compose.local.yml 컨테이너(postforge-db, postforge-redis, postforge-mailpit)를 쓴다.
# 사용: bash bench.sh <label> <app.jar> <app-log-dir>
# 결과: <label>-summary.json(k6), <label>-k6.txt, <label>-proc.txt(기동 시간·RSS)를 이 디렉터리에 남긴다.
set -euo pipefail

LABEL=$1
JAR=$(cd "$(dirname "$2")" && pwd)/$(basename "$2")
LOG_DIR=$3
DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(git -C "$DIR" rev-parse --show-toplevel)
DB=postforge_bench

pg() { docker exec -i postforge-db sh -c "psql -X -q -v ON_ERROR_STOP=1 -U \"\$POSTGRES_USER\" -d $1"; }

echo "DROP DATABASE IF EXISTS $DB WITH (FORCE); CREATE DATABASE $DB;" | pg postgres
docker exec postforge-redis redis-cli -n 15 FLUSHDB > /dev/null

cd "$ROOT"
POSTGRES_DB=$DB SPRING_DATA_REDIS_DATABASE=15 java -Xms512m -Xmx512m -jar "$JAR" > "$LOG_DIR/$LABEL-app.log" 2>&1 &
PID=$!
trap 'kill $PID 2>/dev/null || true; wait $PID 2>/dev/null || true' EXIT

curl -sf --retry 120 --retry-connrefused --retry-delay 1 localhost:8080/actuator/health > /dev/null
pg $DB < "$DIR/seed.sql" > /dev/null

k6 run --quiet --summary-export "$DIR/$LABEL-summary.json" "$DIR/read-mix.js" > "$DIR/$LABEL-k6.txt" 2>&1

{
  grep -o "Started ApplicationServer in [0-9.]* seconds" "$LOG_DIR/$LABEL-app.log"
  ps -o rss= -p $PID | awk '{printf "RSS after load: %.0f MB\n", $1 / 1024}'
} > "$DIR/$LABEL-proc.txt"
