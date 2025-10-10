import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { getProfile, login as apiLogin, logout as apiLogout, signup as apiSignup, TokenResponse } from '../api/auth';
import { storage } from '../utils/storage';

type User = {
  id: number;
  name: string;
  userId: string;
  roles: string[];
} | null;

type AuthContextType = {
  user: User;
  accessToken: string | null;
  refreshToken: string | null;
  login: (id: string, pw: string) => Promise<void>;
  signup: (name: string, id: string, pw: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshTokens: (t: TokenResponse) => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User>(null);
  const [accessToken, setAccessToken] = useState<string | null>(storage.get('accessToken'));
  const [refreshToken, setRefreshToken] = useState<string | null>(storage.get('refreshToken'));

  useEffect(() => {
    if (accessToken) storage.set('accessToken', accessToken); else storage.remove('accessToken');
  }, [accessToken]);
  useEffect(() => {
    if (refreshToken) storage.set('refreshToken', refreshToken); else storage.remove('refreshToken');
  }, [refreshToken]);

  useEffect(() => {
    // Try load profile when tokens present
    (async () => {
      if (!accessToken) return;
      try {
        const profile = await getProfile();
        setUser(profile);
      } catch (_) {
        // ignore
      }
    })();
  }, [accessToken]);

  const login = async (id: string, pw: string) => {
    const token = await apiLogin({ id, pw });
    setAccessToken(token.accessToken);
    setRefreshToken(token.refreshToken);
    const profile = await getProfile();
    setUser(profile);
  };

  const signup = async (name: string, id: string, pw: string) => {
    await apiSignup({ name, id, pw });
  };

  const logout = async () => {
    try { await apiLogout(); } catch (_) { /* ignore */ }
    setUser(null);
    setAccessToken(null);
    setRefreshToken(null);
  };

  const refreshTokens = (t: TokenResponse) => {
    setAccessToken(t.accessToken);
    setRefreshToken(t.refreshToken);
  };

  const value = useMemo(() => ({ user, accessToken, refreshToken, login, signup, logout, refreshTokens }), [user, accessToken, refreshToken]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

