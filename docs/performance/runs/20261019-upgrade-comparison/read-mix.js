// 업그레이드 전후 비교용 읽기 혼합 부하. 20 VU로 30초 예열한 뒤 50 VU로 60초 잰다(측정 구간만 phase:measure).
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const POSTS = 20000;
const NAMES = ['list', 'detail-anon', 'detail-auth', 'comments'];

const thresholds = {
  'http_req_duration{phase:measure}': ['max>=0'],
  'http_req_failed{phase:measure}': ['rate>=0'],
  'http_reqs{phase:measure}': ['count>=0'],
};
// 엔드포인트별 지연을 summary에 남기려고 측정 구간 하위 지표를 선언한다.
for (const name of NAMES) {
  thresholds[`http_req_duration{phase:measure,name:${name}}`] = ['max>=0'];
}

export const options = {
  scenarios: {
    warmup: { executor: 'constant-vus', vus: 20, duration: '30s', exec: 'mix', tags: { phase: 'warmup' } },
    measure: { executor: 'constant-vus', vus: 50, duration: '60s', startTime: '35s', exec: 'mix', tags: { phase: 'measure' } },
  },
  thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({ username: 'bench1', password: 'Demo1234' }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'login 200': (r) => r.status === 200 });
  return { token: res.json('accessToken') };
}

export function mix(data) {
  const r = Math.random();
  const id = 1 + Math.floor(Math.random() * POSTS);
  let res;
  if (r < 0.4) {
    res = http.get(`${BASE}/api/posts?page=${Math.floor(Math.random() * 50)}&size=20`, { tags: { name: 'list' } });
  } else if (r < 0.6) {
    res = http.get(`${BASE}/api/posts/${id}`, { tags: { name: 'detail-anon' } });
  } else if (r < 0.75) {
    res = http.get(`${BASE}/api/posts/${id}`, {
      headers: { Authorization: `Bearer ${data.token}` },
      tags: { name: 'detail-auth' },
    });
  } else {
    res = http.get(`${BASE}/api/posts/${id}/comments`, { tags: { name: 'comments' } });
  }
  check(res, { 'status 200': (x) => x.status === 200 });
}
