import { Link } from 'react-router-dom';
import { ArrowRight, MessageSquare, Heart, Eye, Zap } from 'lucide-react';

export default function Home() {
  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <div className="relative overflow-hidden bg-gradient-to-b from-orange-50 to-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="pt-20 pb-24 md:pt-28 md:pb-32 text-center">
            <h1 className="text-5xl md:text-7xl font-black text-gray-900 tracking-tight mb-6">
              우리 동네를 위한
              <br />
              <span className="bg-gradient-to-r from-carrot to-orange-600 bg-clip-text text-transparent">
                소통 공간
              </span>
            </h1>
            <p className="text-xl md:text-2xl text-gray-600 mb-10 max-w-3xl mx-auto leading-relaxed">
              PostForge에서 이웃들과 자유롭게 소통하고,
              <br className="hidden md:block" />
              정보를 공유하며 함께 성장하세요
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <Link
                to="/board"
                className="group inline-flex items-center gap-2 bg-carrot hover:bg-carrot-dark text-white font-bold py-4 px-8 rounded-full transition-all duration-200 shadow-lg hover:shadow-xl hover:scale-105"
              >
                게시판 둘러보기
                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
              </Link>
              <Link
                to="/email-verification"
                className="inline-flex items-center gap-2 bg-white hover:bg-gray-50 text-gray-900 font-bold py-4 px-8 rounded-full border-2 border-gray-200 transition-all duration-200 hover:border-gray-300"
              >
                무료로 시작하기
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Stats Section */}
      <div className="bg-white py-16 border-y border-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            <div>
              <div className="text-4xl font-black text-carrot mb-2">100+</div>
              <div className="text-gray-600">활발한 사용자</div>
            </div>
            <div>
              <div className="text-4xl font-black text-carrot mb-2">500+</div>
              <div className="text-gray-600">공유된 게시글</div>
            </div>
            <div>
              <div className="text-4xl font-black text-carrot mb-2">1K+</div>
              <div className="text-gray-600">따뜻한 댓글</div>
            </div>
            <div>
              <div className="text-4xl font-black text-carrot mb-2">24/7</div>
              <div className="text-gray-600">언제나 열려있어요</div>
            </div>
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="py-24 bg-gradient-to-b from-white to-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-4xl md:text-5xl font-black text-gray-900 mb-4">
              PostForge만의 특별함
            </h2>
            <p className="text-xl text-gray-600">
              우리가 제공하는 가치를 경험해보세요
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <div className="group relative bg-white p-8 rounded-3xl border border-gray-100 hover:border-carrot transition-all duration-300 hover:shadow-xl">
              <div className="w-14 h-14 bg-gradient-to-br from-carrot to-orange-600 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <MessageSquare className="h-7 w-7 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-gray-900 mb-3">자유로운 소통</h3>
              <p className="text-gray-600 leading-relaxed">
                실시간으로 이웃들과 대화하고 정보를 공유하세요. 댓글과 대댓글로 깊이 있는 토론이 가능해요.
              </p>
            </div>

            <div className="group relative bg-white p-8 rounded-3xl border border-gray-100 hover:border-carrot transition-all duration-300 hover:shadow-xl">
              <div className="w-14 h-14 bg-gradient-to-br from-carrot to-orange-600 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Heart className="h-7 w-7 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-gray-900 mb-3">따뜻한 공감</h3>
              <p className="text-gray-600 leading-relaxed">
                좋아요와 댓글로 마음을 전하세요. 작은 공감 하나가 큰 힘이 됩니다.
              </p>
            </div>

            <div className="group relative bg-white p-8 rounded-3xl border border-gray-100 hover:border-carrot transition-all duration-300 hover:shadow-xl">
              <div className="w-14 h-14 bg-gradient-to-br from-carrot to-orange-600 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
                <Zap className="h-7 w-7 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-gray-900 mb-3">간편한 사용</h3>
              <p className="text-gray-600 leading-relaxed">
                직관적인 인터페이스로 누구나 쉽게 글을 작성하고 소통할 수 있어요.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* CTA Section */}
      <div className="py-24 bg-gradient-to-br from-carrot to-orange-600">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-4xl md:text-5xl font-black text-white mb-6">
            지금 바로 시작하세요
          </h2>
          <p className="text-xl text-orange-100 mb-10 leading-relaxed">
            무료 회원가입하고 우리 동네 이웃들과<br className="hidden md:block" />
            따뜻한 소통을 시작해보세요
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to="/email-verification"
              className="inline-flex items-center gap-2 bg-white hover:bg-gray-50 text-carrot font-bold py-4 px-8 rounded-full transition-all duration-200 shadow-lg hover:shadow-xl hover:scale-105"
            >
              무료로 가입하기
              <ArrowRight className="w-5 h-5" />
            </Link>
            <Link
              to="/board"
              className="inline-flex items-center gap-2 bg-transparent hover:bg-white/10 text-white font-bold py-4 px-8 rounded-full border-2 border-white transition-all duration-200"
            >
              먼저 둘러보기
            </Link>
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="bg-gray-50 py-12 border-t border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center text-gray-600">
            <p className="font-semibold text-carrot text-lg mb-2">PostForge</p>
            <p className="text-sm">우리 동네를 위한 따뜻한 커뮤니티</p>
            <p className="text-xs mt-4 text-gray-500">© 2024 PostForge. All rights reserved.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
