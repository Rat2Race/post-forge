import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Eye, EyeOff, Sparkles, BarChart3, Shield } from "lucide-react";

export function LoginPage() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({ userId: "", password: "" });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    navigate('/');
  };

  const handleOAuthLogin = (provider: string) => {
    navigate('/oauth2/callback?provider=' + provider);
  };

  return (
    <div className="px-5 lg:px-8 py-12">
      <div className="max-w-sm mx-auto">
        <div className="text-center mb-7">
          <h1 className="serif-headline text-2xl mb-1.5">로그인</h1>
          <p className="text-sm text-muted-foreground">PostForge 계정으로 로그인하세요</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="userId">아이디</Label>
            <Input
              id="userId"
              type="text"
              placeholder="아이디를 입력하세요"
              value={formData.userId}
              onChange={(e) => setFormData({ ...formData, userId: e.target.value })}
              required
            />
          </div>

          <div>
            <Label htmlFor="password">비밀번호</Label>
            <div className="relative">
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="비밀번호를 입력하세요"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          <Button type="submit" className="w-full">로그인</Button>
        </form>

        <div className="relative my-6">
          <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-border" /></div>
          <div className="relative flex justify-center text-xs"><span className="bg-card px-2 text-muted-foreground">또는</span></div>
        </div>

        <div className="space-y-2">
          <button onClick={() => handleOAuthLogin('google')} className="social-auth-button social-auth-button--google">
            <svg className="h-4 w-4" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Google로 계속하기
          </button>
          <button onClick={() => handleOAuthLogin('naver')} className="social-auth-button social-auth-button--naver">
            <span className="text-base leading-none">N</span> 네이버로 계속하기
          </button>
          <button onClick={() => handleOAuthLogin('kakao')} className="social-auth-button social-auth-button--kakao">
            <span>K</span> 카카오로 계속하기
          </button>
        </div>

        <div className="mt-6 text-center text-sm">
          <span className="text-muted-foreground">계정이 없으신가요? </span>
          <Link to="/register" className="text-brass hover:underline">회원가입</Link>
        </div>
      </div>

      {/* Supporting content */}
      <div className="max-w-lg mx-auto mt-16 pt-8 border-t border-border/50">
        <div className="grid grid-cols-3 gap-6 text-center">
          {[
            { icon: <Sparkles className="h-4 w-4" />, label: "AI 분석 리포트", desc: "자동 생성 분석" },
            { icon: <BarChart3 className="h-4 w-4" />, label: "실시간 시장 분석", desc: "공시 기반 인사이트" },
            { icon: <Shield className="h-4 w-4" />, label: "커뮤니티 검증", desc: "전문가 토론" },
          ].map((item, idx) => (
            <div key={idx}>
              <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center mx-auto mb-2 text-muted-foreground">
                {item.icon}
              </div>
              <p className="text-xs text-foreground mb-0.5">{item.label}</p>
              <p className="text-xs text-muted-foreground">{item.desc}</p>
            </div>
          ))}
        </div>
        <p className="text-[11px] text-muted-foreground text-center mt-6 leading-relaxed">
          가입 후 AI 분석 생성, 실시간 시장 요약 확인, 투자 토론 참여 기능을 바로 사용할 수 있습니다.
        </p>
      </div>
    </div>
  );
}
