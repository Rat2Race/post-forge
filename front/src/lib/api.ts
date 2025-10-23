import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 로그인/회원가입 API는 interceptor 처리 제외
    const isAuthEndpoint =
      originalRequest.url?.includes('/api/auth/login') ||
      originalRequest.url?.includes('/api/auth/register') ||
      originalRequest.url?.includes('/api/auth/email');

    if (isAuthEndpoint) {
      return Promise.reject(error);
    }

    // 인증된 사용자의 401 에러만 처리
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) throw new Error('No refresh token');

        const response = await axios.post(`${API_BASE_URL}/api/auth/reissue`, {
          refreshToken,
        });

        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export interface RegisterRequest {
  name: string;
  id: string;
  pw: string;
}

export interface LoginRequest {
  id: string;
  pw: string;
}

export interface TokenResponse {
  grantType: string;
  accessToken: string;
  refreshToken: string;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}

export interface SendEmailCodeRequest {
  email: string;
}

export interface VerifyEmailCodeRequest {
  email: string;
  code: string;
}

export interface EmailVerificationResponse {
  verified: boolean;
  message: string;
}

export const authApi = {
  // 이메일 인증 코드 전송
  sendEmailCode: async (data: SendEmailCodeRequest) => {
    const response = await api.post<void>('/api/auth/email/send', data);
    return response.data;
  },

  // 이메일 인증 코드 확인
  verifyEmailCode: async (data: VerifyEmailCodeRequest) => {
    const response = await api.post<EmailVerificationResponse>('/api/auth/email/verify', data);
    return response.data;
  },

  register: async (data: RegisterRequest) => {
    const response = await api.post<string>('/api/auth/register', data);
    return response.data;
  },

  login: async (data: LoginRequest) => {
    const response = await api.post<TokenResponse>('/api/auth/login', data);
    return response.data;
  },

  logout: async () => {
    await api.post('/api/auth/logout');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  },
};
