const DEV = import.meta.env.DEV;
const USE_PROXY = (import.meta.env.VITE_USE_PROXY ?? 'true') !== 'false';
const AUTH_BASE = DEV && USE_PROXY ? '' : (import.meta.env.VITE_AUTH_BASE_URL || 'http://localhost:8080');
const BOARD_BASE = DEV && USE_PROXY ? '' : (import.meta.env.VITE_BOARD_BASE_URL || 'http://localhost:8081');

export const baseUrls = { AUTH_BASE, BOARD_BASE };

type FetchOptions = RequestInit & { retry?: boolean };

async function request(base: string, path: string, options: FetchOptions = {}) {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const accessToken = localStorage.getItem('accessToken');
  if (accessToken) {
    (headers as any)['Authorization'] = `Bearer ${accessToken}`;
  }

  const res = await fetch(`${base}${path}`, { ...options, headers });
  if (res.status === 401 && options.retry !== false) {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      // try reissue
      const re = await fetch(`${AUTH_BASE}/api/auth/reissue`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
      if (re.ok) {
        const t = await re.json();
        // Update localStorage with new tokens
        localStorage.setItem('accessToken', t.accessToken);
        localStorage.setItem('refreshToken', t.refreshToken);
        // retry original once
        return request(base, path, { ...options, retry: false });
      } else {
        // Refresh token도 만료되었으면 모든 토큰 제거하고 로그인 페이지로
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        // Storage 이벤트를 발생시켜 authStore가 감지하도록
        window.dispatchEvent(new Event('storage'));
        // 로그인이 필요한 페이지에서 실패했다면 로그인 페이지로 리다이렉트
        if (window.location.pathname.startsWith('/posts/create')) {
          window.location.href = '/login';
        }
      }
    }
  }
  return res;
}

export const http = {
  get: async (base: string, path: string) => {
    const res = await request(base, path, { method: 'GET' });
    if (!res.ok) throw await toError(res);
    return res.json();
  },
  post: async (base: string, path: string, body?: any) => {
    const res = await request(base, path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
    if (!res.ok) throw await toError(res);
    const contentType = res.headers.get('content-type');
    if (contentType?.includes('application/json')) return res.json();
    return res.text();
  },
  put: async (base: string, path: string, body?: any) => {
    const res = await request(base, path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined });
    if (!res.ok) throw await toError(res);
    const contentType = res.headers.get('content-type');
    if (contentType?.includes('application/json')) return res.json();
    return res.text();
  },
  delete: async (base: string, path: string) => {
    const res = await request(base, path, { method: 'DELETE' });
    if (!res.ok) throw await toError(res);
    const contentType = res.headers.get('content-type');
    if (contentType?.includes('application/json')) return res.json();
    return res.text();
  }
};

async function toError(res: Response) {
  const contentType = res.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    const errorData = await res.json();
    // ErrorResponse 구조에서 message 필드 추출
    const message = errorData.message || errorData.error || res.statusText;
    return new Error(message);
  }
  const text = await res.text();
  return new Error(text || res.statusText);
}
