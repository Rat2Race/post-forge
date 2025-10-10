import { http, baseUrls } from './http';
import type { LoginRequest, SignupRequest, TokenResponse, MemberResponse } from './types';

export async function login(req: LoginRequest): Promise<TokenResponse> {
  return http.post(baseUrls.AUTH_BASE, '/api/auth/login', req);
}

export async function signup(req: SignupRequest): Promise<{ message: string } | string> {
  // Controller returns a plain message string
  const res = await fetch(`${baseUrls.AUTH_BASE}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req)
  });
  if (!res.ok) throw new Error(await res.text());
  const ct = res.headers.get('content-type');
  if (ct && ct.includes('application/json')) return res.json();
  return res.text();
}

export async function reissue(refreshToken: string): Promise<TokenResponse> {
  return http.post(baseUrls.AUTH_BASE, '/api/auth/reissue', { refreshToken });
}

export async function logout(): Promise<string> {
  // returns plain message
  const res = await fetch(`${baseUrls.AUTH_BASE}/api/auth/logout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!res.ok) throw new Error(await res.text());
  return res.text();
}

export async function getProfile(): Promise<MemberResponse> {
  return http.get(baseUrls.AUTH_BASE, '/api/user/profile');
}

export type { TokenResponse };

