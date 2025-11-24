import apiClient from './axios';

export const authAPI = {
  // 회원가입
  register: async (data) => {
    const response = await apiClient.post('/auth/register', {
      name: data.name,
      id: data.id,
      pw: data.pw,
      email: data.email,
      nickname: data.nickname,
    });
    return response.data;
  },

  // 로그인
  login: async (id, pw) => {
    const response = await apiClient.post('/auth/login', { id, pw });
    return response.data;
  },

  // 로그아웃
  logout: async () => {
    const response = await apiClient.post('/auth/logout');
    return response.data;
  },

  // 토큰 재발급
  reissue: async (refreshToken) => {
    const response = await apiClient.post('/auth/reissue', { refreshToken });
    return response.data;
  },

  // 이메일 인증 코드 발송
  sendEmail: async (email) => {
    const response = await apiClient.post('/auth/email/send', { email });
    return response.data;
  },

  // 이메일 인증 완료
  verifyEmail: async (token) => {
    const response = await apiClient.get('/auth/email/verify', {
      params: { token },
    });
    return response.data;
  },

  // 현재 사용자 프로필 조회
  getProfile: async () => {
    const response = await apiClient.get('/user/profile');
    return response.data;
  },
};
