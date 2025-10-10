import { useAuth } from '../context/AuthContext';

const DEV = import.meta.env.DEV;
const USE_PROXY = (import.meta.env.VITE_USE_PROXY ?? 'true') !== 'false';
const AUTH_BASE = DEV && USE_PROXY ? '' : (import.meta.env.VITE_AUTH_BASE_URL || 'http://localhost:8080');
const BOARD_BASE = DEV && USE_PROXY ? '' : (import.meta.env.VITE_BOARD_BASE_URL || 'http://localhost:8080');

export const baseUrls = { AUTH_BASE, BOARD_BASE };

// We cannot use hooks outside components; create a lightweight token store synced by AuthContext
const tokenStore = {
  accessToken: null as string | null,
  refreshToken: null as string | null,
  setTokens(access: string | null, refresh: string | null) {
    this.accessToken = access; this.refreshToken = refresh;
  }
};

// Helper to allow AuthContext to sync tokens
export function useSyncTokenStore() {
  const { accessToken, refreshToken } = useAuth();
  tokenStore.setTokens(accessToken, refreshToken);
}

type FetchOptions = RequestInit & { retry?: boolean };

async function request(base: string, path: string, options: FetchOptions = {}) {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (tokenStore.accessToken) {
    (headers as any)['Authorization'] = `Bearer ${tokenStore.accessToken}`;
  }

  const res = await fetch(`${base}${path}`, { ...options, headers });
  if (res.status === 401 && tokenStore.refreshToken && options.retry !== false) {
    // try reissue
    const re = await fetch(`${AUTH_BASE}/api/auth/reissue`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: tokenStore.refreshToken })
    });
    if (re.ok) {
      const t = await re.json();
      // Update in-memory store; AuthContext will update via callback on callers when needed
      tokenStore.setTokens(t.accessToken, t.refreshToken);
      // retry original once
      return request(base, path, { ...options, retry: false });
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
    return res.json();
  },
  put: async (base: string, path: string, body?: any) => {
    const res = await request(base, path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined });
    if (!res.ok) throw await toError(res);
    return res.text();
  },
  delete: async (base: string, path: string) => {
    const res = await request(base, path, { method: 'DELETE' });
    if (!res.ok) throw await toError(res);
    return res.text();
  }
};

async function toError(res: Response) {
  const contentType = res.headers.get('content-type');
  const text = contentType?.includes('application/json') ? JSON.stringify(await res.json()) : await res.text();
  return new Error(`${res.status} ${res.statusText}: ${text}`);
}
