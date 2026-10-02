'use strict';
// study.html·index.html이 함께 쓰는 인증. 두 화면은 refresh cookie(경로 /api/auth)와 localStorage(pf.token·pf.name·pf.session)를
// 함께 쓰므로 로그인·로그아웃·재발급 규칙도 하나다. 인증 상태가 바뀌면 window에 'pf-auth' 이벤트를 보낸다.
const token  = () => localStorage.getItem('pf.token');
const meName = () => localStorage.getItem('pf.name');
// 로그인마다 새로 정하고 로그아웃 때 지우는 세션 표식. 재발급은 같은 세션 안의 일이라 바꾸지 않는다.
// 요청을 보낼 때의 표식과 지금 표식이 다르면 다른 로그인이 끼어든 것이다. 탭끼리 localStorage를 함께 쓰므로 다른 탭의 로그인도 잡는다.
const session = () => localStorage.getItem('pf.session');

const { api, post, login, logout } = (() => {
  const changed = () => window.dispatchEvent(new Event('pf-auth'));
  const bearer = t => t ? { Authorization: 'Bearer ' + t } : {};
  async function failure(res){
    let msg = res.status + ' 오류';
    try { msg = (await res.json()).message || msg; } catch(e){}
    return new Error(msg);
  }

  // 로그인·로그아웃·재발급은 탭 사이에서도 한 줄로 세운다(Web Locks).
  // refresh cookie는 응답이 도착하면 JS 처리와 상관없이 바로 바뀐다. 이전 세션의 재발급·로그아웃 응답이 다른 계정의 로그인보다
  // 늦게 오면 그 Set-Cookie가 새 쿠키를 덮으므로, 앞 요청의 응답을 다 받은 뒤에 다음 요청을 보낸다.
  // 잠금 안에서는 api()를 쓰지 않는다. api()의 재발급이 같은 잠금을 기다려 멈춘다.
  let queue = Promise.resolve();
  function withAuthLock(fn){
    if (navigator.locks) return navigator.locks.request('pf-auth', fn);
    // Web Locks가 없는 출처(보안 출처가 아닌 http)에서는 이 탭 안의 순서만 지킨다.
    const run = queue.then(fn, fn);
    queue = run.catch(() => {});
    return run;
  }

  // 같은 세션의 401은 재발급 하나를 기다린다. refresh token이 회전하므로 따로 재발급하면 늦은 쪽은 이미 바뀐 토큰을 내밀어 거절된다.
  let reissuing = null;
  function reissue(sess, sent){
    if (reissuing?.sess !== sess) {
      const p = withAuthLock(async () => {
        // 잠금을 기다리는 사이 로그아웃·다른 계정 로그인이 있었거나 다른 탭이 이미 새 토큰을 받았으면 보내지 않는다.
        if (session() !== sess || token() !== sent) return;
        const res = await fetch('/api/auth/token/reissue', { method: 'POST' });
        const b = res.ok ? await res.json() : null;
        if (b?.accessToken && session() === sess) localStorage.setItem('pf.token', b.accessToken);
      }).catch(() => {})
        .finally(() => { if (reissuing?.p === p) reissuing = null; });
      reissuing = { sess, p };
    }
    return reissuing.p;
  }

  // access token이 만료되면 refresh cookie로 한 번 재발급받아 같은 요청을 다시 보낸다.
  async function api(path, opt={}, retried=false){
    const headers = { ...(opt.headers||{}) };
    const sent = token(), sess = session();
    if (opt.body) headers['Content-Type'] = 'application/json';
    Object.assign(headers, bearer(sent));
    const res = await fetch(path, { ...opt, headers });
    if (res.status === 401 && sent) {
      // 이전 세션의 요청을 새 계정의 토큰으로 다시 보내면 남의 계정에 쓰게 된다. 다시 보내지도, 새 세션의 토큰을 지우지도 않는다.
      if (session() !== sess) throw new Error('로그인이 바뀌어 이전 요청을 보내지 않았어요');
      if (!retried) {
        if (token() === sent) await reissue(sess, sent);
        // 이 요청이나 다른 요청·다른 탭이 새 토큰을 받았으면 그 토큰으로 다시 보낸다.
        if (session() === sess && token() !== sent) return api(path, opt, true);
      }
      if (session() === sess && token() === sent) { localStorage.removeItem('pf.token'); changed(); }
    }
    if (!res.ok) throw await failure(res);
    return res.status === 204 ? null : res.json();
  }

  // 로그인 응답의 쿠키와 세션 표식·토큰을 같은 잠금 안에서 바꾼다.
  async function login(username, password){
    await withAuthLock(async () => {
      const res = await fetch('/api/auth/login', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify({ username, password }) });
      if (!res.ok) throw await failure(res);
      const r = await res.json();
      // 세션 표식은 마지막에 쓴다. 다른 탭은 표식이 바뀐 것을 보고 다시 그리므로, 그때 토큰·이름도 이미 새 값이어야 한다.
      localStorage.setItem('pf.token', r.accessToken);
      localStorage.setItem('pf.name', username);
      localStorage.setItem('pf.session', Date.now().toString(36) + Math.random().toString(36).slice(2));
    });
    changed();
  }

  // 서버의 refresh token도 지운다. 재발급을 쓰므로 남겨 두면 로그아웃 뒤에도 세션이 살아난다.
  // access가 만료됐으면 잠금 안에서 재발급받아 지운다.
  async function logout(){
    await withAuthLock(async () => {
      const res = await fetch('/api/auth/logout', { method:'POST', headers: bearer(token()) }).catch(() => null);
      if (res?.status === 401) {
        const r = await fetch('/api/auth/token/reissue', { method:'POST' }).catch(() => null);
        const b = r?.ok ? await r.json() : null;
        if (b?.accessToken) await fetch('/api/auth/logout', { method:'POST', headers: bearer(b.accessToken) }).catch(() => {});
      }
      ['pf.token', 'pf.name', 'pf.session'].forEach(k => localStorage.removeItem(k));
    }).catch(() => {});
    changed();
  }

  // 다른 탭에서 로그인·로그아웃해 세션 표식이 바뀌면 이 탭도 새 상태로 다시 그린다. 그대로 두면 이전 계정의 화면에서
  // 보낸 요청이 지금 저장된 다른 계정의 토큰으로 나간다. 같은 세션의 토큰 재발급(pf.token만 바뀜)은 다시 그리지 않아
  // 쓰던 답이 남는다. 로그아웃은 세션 표식을 마지막에 지우고, clear()는 key가 null이다.
  window.addEventListener('storage', e => {
    if (e.key === 'pf.session' || e.key === null) changed();
  });

  return { api, post: (path, body) => api(path, { method:'POST', body: JSON.stringify(body) }), login, logout };
})();
