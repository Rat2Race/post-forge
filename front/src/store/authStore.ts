import { create } from 'zustand';

interface AuthState {
  isAuthenticated: boolean;
  userId: string | null;
  setUserId: (userId: string) => void;
  login: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
  checkAuth: () => void;
}

// 주기적으로 토큰 상태 확인
const checkTokens = () => {
  const token = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');
  return !!(token || refreshToken);
};

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: checkTokens(),
  userId: null,

  setUserId: (userId: string) => {
    set({ userId });
  },

  login: (accessToken: string, refreshToken: string) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    set({ isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({ isAuthenticated: false, userId: null });
  },

  checkAuth: () => {
    set({ isAuthenticated: checkTokens() });
  },
}));

// Storage 이벤트 리스닝 (다른 탭이나 http.ts에서 토큰 변경 시 감지)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', () => {
    const state = useAuthStore.getState();
    const hasTokens = checkTokens();
    if (state.isAuthenticated !== hasTokens) {
      state.checkAuth();
    }
  });

  // 주기적으로도 확인 (failsafe)
  setInterval(() => {
    const state = useAuthStore.getState();
    const hasTokens = checkTokens();
    if (state.isAuthenticated !== hasTokens) {
      state.checkAuth();
    }
  }, 1000);
}
