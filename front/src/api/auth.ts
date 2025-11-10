import { http, baseUrls } from './http';
import type { LoginRequest, SignupRequest, TokenResponse, MemberResponse } from './types';

export interface SendEmailRequest {
  email: string;
}

export interface EmailVerificationResponse {
  message: string;
  email: string;
}

async function handleErrorResponse(res: Response): Promise<never> {
  const contentType = res.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    const errorData = await res.json();
    const message = errorData.message || errorData.error || res.statusText;
    throw new Error(message);
  }
  const text = await res.text();
  throw new Error(text || res.statusText);
}

export async function sendEmailCode(data: SendEmailRequest): Promise<void> {
  const res = await fetch(`${baseUrls.AUTH_BASE}/api/auth/email/send`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
  if (!res.ok) {
    await handleErrorResponse(res);
  }
}

export async function verifyEmail(token: string): Promise<EmailVerificationResponse> {
  const res = await fetch(`${baseUrls.AUTH_BASE}/api/auth/email/verify?token=${token}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' }
  });
  if (!res.ok) {
    await handleErrorResponse(res);
  }
  return res.json();
}

export async function login(req: LoginRequest): Promise<TokenResponse> {
  return http.post(baseUrls.AUTH_BASE, '/api/auth/login', req);
}

export async function signup(req: SignupRequest): Promise<string> {
  // Controller returns a plain message string
  const res = await fetch(`${baseUrls.AUTH_BASE}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req)
  });
  if (!res.ok) {
    await handleErrorResponse(res);
  }
  const ct = res.headers.get('content-type');
  if (ct && ct.includes('application/json')) return res.json();
  return res.text();
}

export async function reissue(refreshToken: string): Promise<TokenResponse> {
  return http.post(baseUrls.AUTH_BASE, '/api/auth/reissue', { refreshToken });
}

export async function logout(): Promise<void> {
  const res = await fetch(`${baseUrls.AUTH_BASE}/api/auth/logout`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
    }
  });
  if (!res.ok) {
    await handleErrorResponse(res);
  }
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

export async function getProfile(): Promise<MemberResponse> {
  return http.get(baseUrls.AUTH_BASE, '/api/user/profile');
}

export type { TokenResponse, EmailVerificationResponse };

