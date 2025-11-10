import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import { getProfile } from './api/auth';
import Login from './pages/Login';
import Register from './pages/Register';
import VerifyEmail from './pages/VerifyEmail';
import Home from './pages/Home';
import PostsPage from './pages/PostsPage';
import PostDetailPage from './pages/PostDetailPage';
import CreatePostPage from './pages/CreatePostPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

// 이메일 인증이 완료된 경우에만 회원가입 페이지 접근 가능
function EmailVerifiedRoute({ children }: { children: React.ReactNode }) {
  const verifiedEmail = localStorage.getItem('verifiedEmail');
  return verifiedEmail ? <>{children}</> : <Navigate to="/verify-email" replace />;
}

function App() {
  const logout = useAuthStore((state) => state.logout);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  // 앱 시작 시 토큰 유효성 검증
  useEffect(() => {
    const validateToken = async () => {
      // 로그인 상태인데 토큰이 있으면 검증
      if (isAuthenticated && localStorage.getItem('accessToken')) {
        try {
          const profile = await getProfile();
          useAuthStore.getState().setUserId(profile.userId);
        } catch (error) {
          // 토큰이 무효하면 로그아웃
          console.log('Token validation failed, logging out...');
          logout();
        }
      }
    };

    validateToken();
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route
          path="/register"
          element={
            <EmailVerifiedRoute>
              <Register />
            </EmailVerifiedRoute>
          }
        />
        <Route path="/posts" element={<PostsPage />} />
        <Route path="/posts/:id" element={<PostDetailPage />} />
        <Route
          path="/posts/create"
          element={
            <ProtectedRoute>
              <CreatePostPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
