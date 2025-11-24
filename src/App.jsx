import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuthStore } from './store/authStore';

import Layout from './components/Layout';
import Home from './pages/Home';
import Login from './pages/Login';
import EmailVerification from './pages/EmailVerification';
import VerifyEmailCallback from './pages/VerifyEmailCallback';
import Register from './pages/Register';
import PostList from './pages/PostList';
import PostDetail from './pages/PostDetail';
import PostForm from './pages/PostForm';

function App() {
  const initialize = useAuthStore((state) => state.initialize);

  useEffect(() => {
    initialize();
  }, []);

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/email-verification" element={<EmailVerification />} />
        <Route path="/verify-email" element={<VerifyEmailCallback />} />
        <Route path="/register" element={<Register />} />

        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />
          <Route path="/board" element={<PostList />} />
          <Route path="/posts/:postId" element={<PostDetail />} />
          <Route
            path="/posts/new"
            element={
              <ProtectedRoute>
                <PostForm />
              </ProtectedRoute>
            }
          />
          <Route
            path="/posts/:postId/edit"
            element={
              <ProtectedRoute>
                <PostForm />
              </ProtectedRoute>
            }
          />
        </Route>
      </Routes>
    </Router>
  );
}

function ProtectedRoute({ children }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  if (!isAuthenticated) {
    alert('로그인이 필요합니다.');
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default App;
