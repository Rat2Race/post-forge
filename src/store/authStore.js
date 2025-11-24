import { create } from 'zustand';
import { authAPI } from '../api/auth';

export const useAuthStore = create((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,

  // 로그인
  login: async (id, pw) => {
    try {
      const data = await authAPI.login(id, pw);
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);

      // 프로필 조회
      const profile = await authAPI.getProfile();

      set({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: profile,
        isAuthenticated: true,
      });

      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || '로그인에 실패했습니다.',
      };
    }
  },

  // 회원가입
  register: async (data) => {
    try {
      await authAPI.register(data);
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || '회원가입에 실패했습니다.',
      };
    }
  },

  // 로그아웃
  logout: async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
      });
    }
  },

  // 프로필 조회
  fetchProfile: async () => {
    try {
      const profile = await authAPI.getProfile();
      set({ user: profile });
    } catch (error) {
      console.error('Failed to fetch profile:', error);
    }
  },

  // 인증 상태 초기화
  initialize: () => {
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');

    if (accessToken && refreshToken) {
      set({
        accessToken,
        refreshToken,
        isAuthenticated: true,
      });
      get().fetchProfile();
    }
  },
}));
