#!/usr/bin/env bash
# 업그레이드 뒤 스모크 테스트가 보지 않는 경로를 실제 요청으로 확인한다.
B=http://localhost:8080; J=$(mktemp); C=$(mktemp)
st() { printf "%-34s %s\n" "$1" "$2"; }
code() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
MON=$(grep -E '^MONITORING_(USERNAME|PASSWORD)=' .env.local | cut -d= -f2- | paste -sd: -)

R=$(curl -s -c $C -H 'Content-Type: application/json' -d '{"username":"bench1","password":"Demo1234"}' $B/api/auth/login)
T=$(echo "$R" | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])' 2>/dev/null)
st "POST /api/auth/login" "$([ -n "$T" ] && echo 200 || echo "FAIL $R")"
A="Authorization: Bearer $T"
P=$(curl -s -H "$A" -H 'Content-Type: application/json' -d '{"title":"업그레이드 확인","content":"Boot 4.1 업그레이드 뒤 쓰기 경로 확인","tags":["check"]}' -w "\n%{http_code}" $B/api/posts)
PID=$(echo "$P" | head -1 | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])' 2>/dev/null); st "POST /api/posts" "$(echo "$P" | tail -1) id=$PID"
st "PUT /api/posts/{id}" "$(code -X PUT -H "$A" -H 'Content-Type: application/json' -d '{"title":"업그레이드 확인2","content":"수정 경로 확인입니다","tags":["check"]}' $B/api/posts/$PID)"
st "POST /api/posts/{id}/comments" "$(code -H "$A" -H 'Content-Type: application/json' -d '{"content":"댓글 확인"}' $B/api/posts/$PID/comments)"
st "POST /api/posts/{id}/like" "$(code -X POST -H "$A" $B/api/posts/$PID/like)"
st "GET /api/posts/{id} (auth)" "$(code -H "$A" $B/api/posts/$PID)"
st "GET /api/user/profile" "$(code -H "$A" $B/api/user/profile)"
st "POST /api/auth/token/reissue" "$(code -X POST -b $C -c $C $B/api/auth/token/reissue)"
st "DELETE /api/posts/{id}" "$(code -X DELETE -H "$A" $B/api/posts/$PID)"
st "POST /api/auth/logout" "$(code -X POST -b $C -H "$A" $B/api/auth/logout)"
BEFORE=$(curl -s localhost:8025/api/v1/messages | python3 -c 'import sys,json; print(json.load(sys.stdin)["total"])')
st "POST /api/auth/email/send" "$(code -H 'Content-Type: application/json' -d '{"email":"upgrade-check@test.local"}' $B/api/auth/email/send)"
AFTER=$(curl -s localhost:8025/api/v1/messages | python3 -c 'import sys,json; print(json.load(sys.stdin)["total"])')
st "Mailpit 수신" "$BEFORE -> $AFTER"
st "GET /oauth2/authorization/google" "$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' $B/oauth2/authorization/google | cut -c1-60)"
st "GET /actuator/prometheus (no auth)" "$(code $B/actuator/prometheus)"
st "GET /actuator/prometheus (basic)" "$(code -u "$MON" $B/actuator/prometheus) jvm_memory_used_bytes=$(curl -s -u "$MON" $B/actuator/prometheus | grep -c '^jvm_memory_used_bytes')"
st "GET /v3/api-docs" "$(code $B/v3/api-docs)"
st "GET /swagger-ui.html" "$(code -L $B/swagger-ui.html)"
st "GET / (index.html)" "$(code $B/)"
st "GET /api/nope (404 JSON)" "$(curl -s $B/api/posts/abc | cut -c1-80)"
rm -f $J $C
