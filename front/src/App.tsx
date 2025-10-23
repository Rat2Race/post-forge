import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import Login from './pages/Login';
import Register from './pages/Register';
import VerifyEmail from './pages/VerifyEmail';
import Home from './pages/Home';

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
      </Routes>
    </BrowserRouter>
  );
}

export default App;
