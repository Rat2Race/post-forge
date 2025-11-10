import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import Navigation from '../components/Navigation';
import { Home as HomeIcon, User, Zap } from 'lucide-react';

export default function Home() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <Navigation />

      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-r from-blue-600 to-purple-600 rounded-3xl mb-6">
            <HomeIcon className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-5xl font-bold text-gray-900 mb-4">
            PostForge에 오신 것을 환영합니다
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
            현대적이고 안전한 인증 시스템과 함께하는 커뮤니티 플랫폼
          </p>

          <div className="flex justify-center gap-4">
            {!isAuthenticated ? (
              <>
                <button
                  onClick={() => navigate('/verify-email')}
                  className="px-8 py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 focus:ring-4 focus:ring-blue-300 transition-all"
                >
                  시작하기
                </button>
                <button
                  onClick={() => navigate('/login')}
                  className="px-8 py-3 bg-white text-gray-700 border border-gray-300 rounded-lg font-semibold hover:bg-gray-50 transition-all"
                >
                  로그인
                </button>
              </>
            ) : (
              <button
                onClick={() => navigate('/posts')}
                className="px-8 py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 focus:ring-4 focus:ring-blue-300 transition-all"
              >
                게시판 바로가기
              </button>
            )}
          </div>
        </div>

        {/* Features */}
        <div className="mt-20 grid md:grid-cols-3 gap-8">
          <div className="bg-white rounded-2xl p-8 shadow-lg border border-gray-100">
            <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center mb-4">
              <User className="w-6 h-6 text-blue-600" />
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">안전한 인증</h3>
            <p className="text-gray-600">
              JWT 기반의 안전한 인증 시스템으로 사용자 정보를 보호합니다
            </p>
          </div>

          <div className="bg-white rounded-2xl p-8 shadow-lg border border-gray-100">
            <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center mb-4">
              <HomeIcon className="w-6 h-6 text-purple-600" />
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">현대적인 UI</h3>
            <p className="text-gray-600">
              Tailwind CSS로 구현된 트렌디하고 반응형 사용자 인터페이스
            </p>
          </div>

          <div className="bg-white rounded-2xl p-8 shadow-lg border border-gray-100">
            <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center mb-4">
              <Zap className="w-6 h-6 text-green-600" />
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">빠른 성능</h3>
            <p className="text-gray-600">
              React + Vite 기반의 빠르고 효율적인 개발 환경
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
