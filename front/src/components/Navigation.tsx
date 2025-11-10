import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { logout as apiLogout } from '../api/auth';
import { Home as HomeIcon, LogOut, FileText, LogIn, UserPlus } from 'lucide-react';

export default function Navigation() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, logout } = useAuthStore();

  const handleLogout = async () => {
    try {
      await apiLogout();
      logout();
      navigate('/');
    } catch (err) {
      console.error('Logout failed:', err);
      logout();
      navigate('/');
    }
  };

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-3 hover:opacity-80 transition-opacity"
          >
            <div className="w-10 h-10 bg-gradient-to-r from-blue-600 to-purple-600 rounded-lg flex items-center justify-center">
              <HomeIcon className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">PostForge</span>
          </button>

          {/* Navigation Links */}
          <div className="flex items-center gap-2">
            {/* 게시판 링크 - 항상 표시 */}
            <button
              onClick={() => navigate('/posts')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                isActive('/posts')
                  ? 'bg-blue-50 text-blue-600 font-medium'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              <FileText className="w-5 h-5" />
              게시판
            </button>

            {/* 인증 상태에 따른 버튼 */}
            {isAuthenticated ? (
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <LogOut className="w-5 h-5" />
                로그아웃
              </button>
            ) : (
              <>
                <button
                  onClick={() => navigate('/login')}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                    isActive('/login')
                      ? 'bg-blue-50 text-blue-600 font-medium'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <LogIn className="w-5 h-5" />
                  로그인
                </button>
                <button
                  onClick={() => navigate('/verify-email')}
                  className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg hover:from-blue-700 hover:to-purple-700 transition-all"
                >
                  <UserPlus className="w-5 h-5" />
                  회원가입
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
