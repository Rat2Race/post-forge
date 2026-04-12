import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Eye, EyeOff, Check, X, ArrowLeft, Mail, Sparkles, Users, FileText } from "lucide-react";

type Step = "email" | "verify" | "profile";

export function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [profileData, setProfileData] = useState({
    userId: "", nickname: "", password: "", confirmPassword: "",
  });

  const handleEmailSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setStep("verify");
  };

  const handleVerifySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setStep("profile");
  };

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (profileData.password !== profileData.confirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }
    navigate("/login");
  };

  const handleOAuthRegister = (provider: string) => {
    navigate("/oauth2/callback?provider=" + provider);
  };

  const passwordRequirements = [
    { label: "8자 이상", valid: profileData.password.length >= 8 },
    { label: "영문 포함", valid: /[a-zA-Z]/.test(profileData.password) },
    { label: "숫자 포함", valid: /\d/.test(profileData.password) },
  ];

  const steps = [
    { key: "email", label: "이메일" },
    { key: "verify", label: "인증" },
    { key: "profile", label: "정보 입력" },
  ];
  const currentStepIndex = steps.findIndex((s) => s.key === step);

  return (
    <div className="px-5 lg:px-8 py-12">
      <div className="max-w-sm mx-auto">
        {/* Step indicator */}
        <div className="flex items-center justify-center gap-2 mb-7">
          {steps.map((s, idx) => (
            <div key={s.key} className="flex items-center gap-2">
              <div className={`w-6 h-6 rounded-full flex items-center justify-center text-xs transition-colors ${
                idx < currentStepIndex
                  ? "bg-positive text-white"
                  : idx === currentStepIndex
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-muted-foreground"
              }`}>
                {idx < currentStepIndex ? <Check className="h-3.5 w-3.5" /> : idx + 1}
              </div>
              <span className={`text-xs hidden sm:inline ${idx === currentStepIndex ? "text-foreground" : "text-muted-foreground"}`}>
                {s.label}
              </span>
              {idx < steps.length - 1 && (
                <div className={`w-6 h-px ${idx < currentStepIndex ? "bg-positive" : "bg-border"}`} />
              )}
            </div>
          ))}
        </div>

        {/* Step 1: Email */}
        {step === "email" && (
          <>
            <div className="text-center mb-7">
              <h1 className="serif-headline text-2xl mb-1.5">회원가입</h1>
              <p className="text-sm text-muted-foreground">이메일 주소를 입력해주세요</p>
            </div>

            <form onSubmit={handleEmailSubmit} className="space-y-4">
              <div>
                <Label htmlFor="email">이메일</Label>
                <Input
                  id="email" type="email" placeholder="example@email.com"
                  value={email} onChange={(e) => setEmail(e.target.value)} required
                />
              </div>
              <Button type="submit" className="w-full">인증 코드 발송</Button>
            </form>

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-border" /></div>
              <div className="relative flex justify-center text-xs"><span className="bg-card px-2 text-muted-foreground">또는</span></div>
            </div>

            <div className="space-y-2">
              <button onClick={() => handleOAuthRegister("google")} className="social-auth-button social-auth-button--google">
                <svg className="h-4 w-4" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                </svg>
                Google로 계속하기
              </button>
              <button onClick={() => handleOAuthRegister("naver")} className="social-auth-button social-auth-button--naver">
                <span className="text-base leading-none">N</span> 네이버로 계속하기
              </button>
              <button onClick={() => handleOAuthRegister("kakao")} className="social-auth-button social-auth-button--kakao">
                <span>K</span> 카카오로 계속하기
              </button>
            </div>

            <div className="mt-6 text-center text-sm">
              <span className="text-muted-foreground">이미 계정이 있으신가요? </span>
              <Link to="/login" className="text-brass hover:underline">로그인</Link>
            </div>
          </>
        )}

        {/* Step 2: Verify email */}
        {step === "verify" && (
          <>
            <div className="text-center mb-7">
              <div className="w-12 h-12 bg-secondary rounded-full flex items-center justify-center mx-auto mb-4">
                <Mail className="h-5 w-5 text-muted-foreground" />
              </div>
              <h1 className="serif-headline text-2xl mb-1.5">이메일 인증</h1>
              <p className="text-sm text-muted-foreground">
                <span className="text-foreground">{email}</span>로<br />인증 코드를 전송했습니다.
              </p>
            </div>

            <form onSubmit={handleVerifySubmit} className="space-y-4">
              <div>
                <Label htmlFor="code">인증 코드</Label>
                <Input
                  id="code" type="text" placeholder="6자리 인증 코드"
                  value={verificationCode} onChange={(e) => setVerificationCode(e.target.value)}
                  maxLength={6} required
                />
              </div>
              <Button type="submit" className="w-full">인증 확인</Button>
              <div className="flex items-center justify-between">
                <button type="button" onClick={() => setStep("email")}
                  className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
                  <ArrowLeft className="h-3.5 w-3.5" /> 이메일 변경
                </button>
                <button type="button" className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                  onClick={() => alert("인증 코드가 재전송되었습니다.")}>
                  코드 재전송
                </button>
              </div>
            </form>
          </>
        )}

        {/* Step 3: Profile info */}
        {step === "profile" && (
          <>
            <div className="text-center mb-7">
              <h1 className="serif-headline text-2xl mb-1.5">회원 정보 입력</h1>
              <p className="text-sm text-muted-foreground">마지막 단계입니다. 계정 정보를 입력해주세요.</p>
            </div>

            <form onSubmit={handleProfileSubmit} className="space-y-4">
              <div>
                <Label htmlFor="userId">아이디</Label>
                <Input id="userId" type="text" placeholder="영문, 숫자 조합"
                  value={profileData.userId} onChange={(e) => setProfileData({ ...profileData, userId: e.target.value })} required />
              </div>
              <div>
                <Label htmlFor="nickname">닉네임</Label>
                <Input id="nickname" type="text" placeholder="닉네임"
                  value={profileData.nickname} onChange={(e) => setProfileData({ ...profileData, nickname: e.target.value })} required />
              </div>
              <div>
                <Label htmlFor="password">비밀번호</Label>
                <div className="relative">
                  <Input id="password" type={showPassword ? "text" : "password"} placeholder="비밀번호"
                    value={profileData.password} onChange={(e) => setProfileData({ ...profileData, password: e.target.value })} required />
                  <button type="button" onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors">
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {profileData.password && (
                  <div className="mt-2 space-y-1">
                    {passwordRequirements.map((req, index) => (
                      <div key={index} className="flex items-center gap-2 text-xs">
                        {req.valid ? <Check className="h-3 w-3 text-positive" /> : <X className="h-3 w-3 text-muted-foreground" />}
                        <span className={req.valid ? "text-positive" : "text-muted-foreground"}>{req.label}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              <div>
                <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                <div className="relative">
                  <Input id="confirmPassword" type={showConfirmPassword ? "text" : "password"} placeholder="비밀번호 확인"
                    value={profileData.confirmPassword} onChange={(e) => setProfileData({ ...profileData, confirmPassword: e.target.value })} required />
                  <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors">
                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {profileData.confirmPassword && profileData.password !== profileData.confirmPassword && (
                  <p className="text-xs text-negative mt-1">비밀번호가 일치하지 않습니다</p>
                )}
              </div>
              <Button type="submit" className="w-full">가입 완료</Button>
            </form>
          </>
        )}
      </div>

      {/* Supporting content */}
      <div className="max-w-lg mx-auto mt-16 pt-8 border-t border-border/50">
        <h3 className="text-xs text-muted-foreground text-center mb-6">PostForge에서 할 수 있는 것</h3>
        <div className="grid grid-cols-3 gap-6 text-center">
          {[
            { icon: <Sparkles className="h-4 w-4" />, label: "AI 분석 생성" },
            { icon: <Users className="h-4 w-4" />, label: "투자 토론" },
            { icon: <FileText className="h-4 w-4" />, label: "리포트 작성" },
          ].map((item, idx) => (
            <div key={idx}>
              <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center mx-auto mb-2 text-muted-foreground">
                {item.icon}
              </div>
              <p className="text-xs text-foreground">{item.label}</p>
            </div>
          ))}
        </div>
        <p className="text-[11px] text-muted-foreground text-center mt-6 leading-relaxed">
          계정 생성 후 관심 종목 분석을 저장하고, AI가 만든 리포트를 읽고, 직접 인사이트를 작성해 커뮤니티와 공유할 수 있습니다.
        </p>
      </div>
    </div>
  );
}
