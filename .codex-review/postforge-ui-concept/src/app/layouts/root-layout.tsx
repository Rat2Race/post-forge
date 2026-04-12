import { Outlet, Link, useLocation, useNavigate } from "react-router";
import { useState } from "react";
import { Search, Menu, X, User, LogOut, MessageSquare, Sparkles, PenSquare } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";

export function RootLayout() {
  const location = useLocation();
  const navigate = useNavigate();
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
    <div className="min-h-screen flex flex-col bg-[#e8e6e1]">
      {/* Header — sits inside the content column, not full bleed */}
      <header className="sticky top-0 z-50">
        <div className="max-w-[1216px] mx-auto">
          <div className="bg-[#1a1613] text-[#faf9f6]">
            <div className="px-4 sm:px-6 lg:px-8">
              <div className="flex items-center justify-between h-14">
                {/* Logo */}
                <Link to="/" className="flex items-center gap-2 shrink-0">
                  <div className="w-7 h-7 bg-white rounded flex items-center justify-center">
                    <span className="text-[#1a1613] font-bold text-sm">P</span>
                  </div>
                  <span className="serif-headline text-lg font-semibold text-white hidden sm:block">PostForge</span>
                </Link>

                {/* Search bar */}
                <div className="hidden md:flex items-center flex-1 max-w-2xl mx-8">
                  <form onSubmit={handleSearch} className="flex-1">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#8a8680]" />
                      <Input
                        type="search"
                        placeholder="종목명, 키워드 검색..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-10 bg-[#2a2723] border-[#3a3733] text-white placeholder:text-[#8a8680] focus:border-[#b8956a]"
                      />
                    </div>
                  </form>
                </div>

                {/* Right actions */}
                <div className="hidden md:flex items-center gap-1 shrink-0">
                  {isAuthenticated ? (
                    <>
                      <Button variant="ghost" size="sm" className="text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/ai/chat">
                          <MessageSquare className="h-4 w-4 mr-1.5" />
                          AI 대화
                        </Link>
                      </Button>
                      <Button variant="ghost" size="sm" className="text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/ai/generate">
                          <Sparkles className="h-4 w-4 mr-1.5" />
                          AI 생성
                        </Link>
                      </Button>
                      <Button size="sm" className="bg-[#b8956a] hover:bg-[#a6845c] text-white ml-1" asChild>
                        <Link to="/posts/new">
                          <PenSquare className="h-4 w-4 mr-1.5" />
                          글쓰기
                        </Link>
                      </Button>
                      <div className="h-5 w-px bg-[#3a3733] mx-2" />
                      <Button variant="ghost" size="sm" className="text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/profile">
                          <User className="h-4 w-4" />
                        </Link>
                      </Button>
                      <Button variant="ghost" size="sm" className="text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" onClick={handleLogout}>
                        <LogOut className="h-4 w-4" />
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button variant="ghost" size="sm" className="text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/login">로그인</Link>
                      </Button>
                      <Button size="sm" className="bg-[#b8956a] hover:bg-[#a6845c] text-white" asChild>
                        <Link to="/register">회원가입</Link>
                      </Button>
                    </>
                  )}
                </div>

                {/* Mobile menu button */}
                <Button
                  variant="ghost"
                  size="sm"
                  className="md:hidden text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]"
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                >
                  {mobileMenuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
                </Button>
              </div>

              {/* Mobile search */}
              <div className="md:hidden pb-3">
                <form onSubmit={handleSearch}>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[#8a8680]" />
                    <Input
                      type="search"
                      placeholder="종목명, 키워드 검색..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-10 bg-[#2a2723] border-[#3a3733] text-white placeholder:text-[#8a8680]"
                    />
                  </div>
                </form>
              </div>
            </div>

            {/* Mobile menu */}
            {mobileMenuOpen && (
              <div className="md:hidden border-t border-[#3a3733]">
                <div className="px-4 py-4 space-y-1">
                  {isAuthenticated ? (
                    <>
                      <Button variant="ghost" className="w-full justify-start text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/ai/chat" onClick={() => setMobileMenuOpen(false)}>
                          <MessageSquare className="h-4 w-4 mr-2" /> AI 대화
                        </Link>
                      </Button>
                      <Button variant="ghost" className="w-full justify-start text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/ai/generate" onClick={() => setMobileMenuOpen(false)}>
                          <Sparkles className="h-4 w-4 mr-2" /> AI 생성
                        </Link>
                      </Button>
                      <Button className="w-full justify-start bg-[#b8956a] hover:bg-[#a6845c] text-white" asChild>
                        <Link to="/posts/new" onClick={() => setMobileMenuOpen(false)}>
                          <PenSquare className="h-4 w-4 mr-2" /> 글쓰기
                        </Link>
                      </Button>
                      <div className="h-px bg-[#3a3733] my-2" />
                      <Button variant="ghost" className="w-full justify-start text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/profile" onClick={() => setMobileMenuOpen(false)}>
                          <User className="h-4 w-4 mr-2" /> 프로필
                        </Link>
                      </Button>
                      <Button variant="ghost" className="w-full justify-start text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" onClick={() => { handleLogout(); setMobileMenuOpen(false); }}>
                        <LogOut className="h-4 w-4 mr-2" /> 로그아웃
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button variant="ghost" className="w-full text-[#c5c0b8] hover:text-white hover:bg-[#2a2723]" asChild>
                        <Link to="/login" onClick={() => setMobileMenuOpen(false)}>로그인</Link>
                      </Button>
                      <Button className="w-full bg-[#b8956a] hover:bg-[#a6845c] text-white" asChild>
                        <Link to="/register" onClick={() => setMobileMenuOpen(false)}>회원가입</Link>
                      </Button>
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main content — white column centered on beige bg */}
      <main className="flex-1">
        <div className="max-w-[1216px] mx-auto bg-white">
          <Outlet />
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-border">
        <div className="max-w-[1216px] mx-auto px-6 lg:px-8 py-10">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div>
              <h3 className="serif-headline mb-2">PostForge</h3>
              <p className="text-sm text-muted-foreground">AI 기반 한국 주식 분석 커뮤니티</p>
            </div>
            <div>
              <h4 className="mb-3">서비스</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><Link to="/ai/chat" className="hover:text-foreground transition-colors">AI 대화</Link></li>
                <li><Link to="/ai/generate" className="hover:text-foreground transition-colors">AI 분석 생성</Link></li>
                <li><Link to="/" className="hover:text-foreground transition-colors">분석 게시판</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="mb-3">정보</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="#" className="hover:text-foreground transition-colors">이용약관</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">개인정보처리방침</a></li>
                <li><a href="#" className="hover:text-foreground transition-colors">문의하기</a></li>
              </ul>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-border text-center text-sm text-muted-foreground">
            <p>© 2026 PostForge. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
