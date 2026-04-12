import { Outlet, Link, useLocation, useNavigate } from "react-router";
import { useState } from "react";
import { Search, Menu, X, User, LogOut, MessageSquare, Sparkles, PenSquare, Sun, Moon } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { useTheme } from "../components/theme-provider";

export function RootLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { theme, toggle } = useTheme();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const isAuthenticated = !location.pathname.includes('/login') && !location.pathname.includes('/register');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    navigate(`/?search=${encodeURIComponent(searchQuery)}`);
    setSearchQuery("");
  };

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex flex-col bg-muted">
      {/* Header */}
      <header className="sticky top-0 z-50">
        <div className="max-w-[1216px] mx-auto">
          <div className="bg-surface-dark text-surface-dark-text">
            <div className="px-5 lg:px-8">
              <div className="flex items-center justify-between h-14">
                {/* Logo */}
                <Link to="/" className="flex items-center gap-2.5 shrink-0">
                  <div className="w-7 h-7 bg-white/90 rounded flex items-center justify-center">
                    <span className="serif-headline text-[#1a1613] text-sm leading-none">P</span>
                  </div>
                  <span className="serif-headline text-[15px] text-white hidden sm:block">PostForge</span>
                </Link>

                {/* Search bar */}
                <div className="hidden md:flex items-center flex-1 max-w-xl mx-8">
                  <form onSubmit={handleSearch} className="flex-1">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-surface-dark-text-muted" />
                      <Input
                        type="search"
                        placeholder="종목명, 키워드 검색..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9 h-8 bg-surface-dark-secondary border-surface-dark-border text-white placeholder:text-surface-dark-text-muted text-sm focus:border-brass"
                      />
                    </div>
                  </form>
                </div>

                {/* Right actions */}
                <div className="hidden md:flex items-center gap-0.5 shrink-0">
                  {isAuthenticated ? (
                    <>
                      <button
                        onClick={toggle}
                        className="h-8 w-8 flex items-center justify-center rounded text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                      >
                        {theme === "light" ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
                      </button>
                      <Link
                        to="/ai/chat"
                        className="h-8 px-3 flex items-center gap-1.5 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                      >
                        <MessageSquare className="h-3.5 w-3.5" />
                        AI 대화
                      </Link>
                      <Link
                        to="/ai/generate"
                        className="h-8 px-3 flex items-center gap-1.5 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                      >
                        <Sparkles className="h-3.5 w-3.5" />
                        AI 생성
                      </Link>
                      <Link
                        to="/posts/new"
                        className="h-8 px-3 flex items-center gap-1.5 rounded text-sm bg-brass text-white hover:bg-brass/90 transition-colors ml-1"
                      >
                        <PenSquare className="h-3.5 w-3.5" />
                        글쓰기
                      </Link>
                      <div className="h-4 w-px bg-surface-dark-border mx-2" />
                      <Link
                        to="/profile"
                        className="h-8 w-8 flex items-center justify-center rounded text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                      >
                        <User className="h-4 w-4" />
                      </Link>
                      <button
                        onClick={handleLogout}
                        className="h-8 w-8 flex items-center justify-center rounded text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                      >
                        <LogOut className="h-4 w-4" />
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={toggle}
                        className="h-8 w-8 flex items-center justify-center rounded text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors mr-1"
                      >
                        {theme === "light" ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
                      </button>
                      <Link
                        to="/login"
                        className="h-8 px-3 flex items-center rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                      >
                        로그인
                      </Link>
                      <Link
                        to="/register"
                        className="h-8 px-3 flex items-center rounded text-sm bg-brass text-white hover:bg-brass/90 transition-colors"
                      >
                        회원가입
                      </Link>
                    </>
                  )}
                </div>

                {/* Mobile menu button */}
                <div className="flex items-center gap-1 md:hidden">
                  <button
                    onClick={toggle}
                    className="h-8 w-8 flex items-center justify-center rounded text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                  >
                    {theme === "light" ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
                  </button>
                  <button
                    className="h-8 w-8 flex items-center justify-center rounded text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors"
                    onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  >
                    {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
                  </button>
                </div>
              </div>

              {/* Mobile search */}
              <div className="md:hidden pb-3">
                <form onSubmit={handleSearch}>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-surface-dark-text-muted" />
                    <Input
                      type="search"
                      placeholder="종목명, 키워드 검색..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-9 h-8 bg-surface-dark-secondary border-surface-dark-border text-white placeholder:text-surface-dark-text-muted text-sm"
                    />
                  </div>
                </form>
              </div>
            </div>

            {/* Mobile menu */}
            {mobileMenuOpen && (
              <div className="md:hidden border-t border-surface-dark-border">
                <div className="px-4 py-3 space-y-1">
                  {isAuthenticated ? (
                    <>
                      <Link to="/ai/chat" onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center gap-2 px-3 py-2 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors">
                        <MessageSquare className="h-4 w-4" /> AI 대화
                      </Link>
                      <Link to="/ai/generate" onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center gap-2 px-3 py-2 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors">
                        <Sparkles className="h-4 w-4" /> AI 생성
                      </Link>
                      <Link to="/posts/new" onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center gap-2 px-3 py-2 rounded text-sm bg-brass text-white hover:bg-brass/90 transition-colors">
                        <PenSquare className="h-4 w-4" /> 글쓰기
                      </Link>
                      <div className="h-px bg-surface-dark-border my-1" />
                      <Link to="/profile" onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center gap-2 px-3 py-2 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors">
                        <User className="h-4 w-4" /> 프로필
                      </Link>
                      <button onClick={() => { handleLogout(); setMobileMenuOpen(false); }}
                        className="flex items-center gap-2 px-3 py-2 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors w-full text-left">
                        <LogOut className="h-4 w-4" /> 로그아웃
                      </button>
                    </>
                  ) : (
                    <>
                      <Link to="/login" onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center justify-center px-3 py-2 rounded text-sm text-surface-dark-text-subtle hover:text-white hover:bg-surface-dark-secondary transition-colors">
                        로그인
                      </Link>
                      <Link to="/register" onClick={() => setMobileMenuOpen(false)}
                        className="flex items-center justify-center px-3 py-2 rounded text-sm bg-brass text-white hover:bg-brass/90 transition-colors">
                        회원가입
                      </Link>
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="flex-1">
        <div className="max-w-[1216px] mx-auto bg-card min-h-[calc(100vh-3.5rem-12rem)]">
          <Outlet />
        </div>
      </main>

      {/* Footer */}
      <footer>
        <div className="max-w-[1216px] mx-auto">
          <div className="bg-surface-dark text-surface-dark-text px-5 lg:px-8 py-10">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
              <div className="md:col-span-2">
                <h3 className="serif-headline text-white text-sm mb-2">PostForge</h3>
                <p className="text-xs text-surface-dark-text-muted leading-relaxed max-w-xs">
                  AI 기반 한국 주식 분석 커뮤니티. 공시, 뉴스, 시장 데이터를 종합 분석하여 데이터 기반 투자 인사이트를 제공합니다.
                </p>
              </div>
              <div>
                <h4 className="text-xs text-surface-dark-text-subtle mb-3">서비스</h4>
                <ul className="space-y-2 text-xs text-surface-dark-text-muted">
                  <li><Link to="/ai/chat" className="hover:text-white transition-colors">AI 대화</Link></li>
                  <li><Link to="/ai/generate" className="hover:text-white transition-colors">AI 분석 생성</Link></li>
                  <li><Link to="/" className="hover:text-white transition-colors">분석 게시판</Link></li>
                </ul>
              </div>
              <div>
                <h4 className="text-xs text-surface-dark-text-subtle mb-3">정보</h4>
                <ul className="space-y-2 text-xs text-surface-dark-text-muted">
                  <li><a href="#" className="hover:text-white transition-colors">이용약관</a></li>
                  <li><a href="#" className="hover:text-white transition-colors">개인정보처리방침</a></li>
                  <li><a href="#" className="hover:text-white transition-colors">문의하기</a></li>
                </ul>
              </div>
            </div>
            <div className="mt-8 pt-6 border-t border-surface-dark-border text-center text-xs text-surface-dark-text-muted">
              <p>© 2026 PostForge. All rights reserved.</p>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
