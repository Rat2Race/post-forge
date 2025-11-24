import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

export default function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user, logout } = useAuthStore();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex justify-between items-center h-[70px]">
          <div className="flex items-center gap-20">
            <Link to="/" className="flex items-center">
              <h1 className="text-2xl font-bold text-carrot">PostForge</h1>
            </Link>

            <nav className="hidden md:flex items-center gap-8">
              <Link
                to="/board"
                className={`text-[15px] font-medium transition-colors relative py-2 ${
                  isActive('/board') || isActive('/posts')
                    ? 'text-gray-900'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                게시판
                {(isActive('/board') || isActive('/posts')) && (
                  <div className="absolute bottom-0 left-0 right-0 h-[2px] bg-carrot"></div>
                )}
              </Link>
            </nav>
          </div>

          <div className="flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <Link
                  to="/posts/new"
                  className="text-[15px] font-medium text-gray-700 hover:text-gray-900 transition-colors px-3 py-2"
                >
                  글쓰기
                </Link>
                <div className="flex items-center gap-3 pl-3 border-l border-gray-200">
                  <span className="text-[15px] text-gray-700">
                    {user?.userId || '사용자'}
                  </span>
                  <button
                    onClick={handleLogout}
                    className="text-[15px] font-medium text-gray-600 hover:text-gray-900 transition-colors"
                  >
                    로그아웃
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="text-[15px] font-medium text-gray-700 hover:text-gray-900 transition-colors px-4 py-2"
                >
                  로그인
                </Link>
                <Link
                  to="/email-verification"
                  className="text-[15px] font-medium bg-carrot hover:bg-carrot-dark text-white px-4 py-2 rounded-lg transition-colors"
                >
                  회원가입
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
