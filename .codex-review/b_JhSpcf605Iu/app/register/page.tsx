"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Eye, EyeOff, Loader2, CheckCircle2 } from "lucide-react"
import { AuthCard } from "@/components/auth/auth-card"
import { SocialLoginButtons } from "@/components/auth/social-login-buttons"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

export default function RegisterPage() {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [step, setStep] = useState<"form" | "verify">("form")
  const [formData, setFormData] = useState({
    userId: "",
    email: "",
    nickname: "",
    password: "",
    confirmPassword: "",
  })
  const [error, setError] = useState("")

  const passwordRequirements = [
    { label: "8자 이상", met: formData.password.length >= 8 },
    { label: "영문 포함", met: /[a-zA-Z]/.test(formData.password) },
    { label: "숫자 포함", met: /[0-9]/.test(formData.password) },
  ]

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    if (formData.password !== formData.confirmPassword) {
      setError("비밀번호가 일치하지 않습니다.")
      return
    }

    if (!passwordRequirements.every((req) => req.met)) {
      setError("비밀번호 요구사항을 충족해주세요.")
      return
    }

    setIsLoading(true)

    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 1500))

    setIsLoading(false)
    setStep("verify")
  }

  const handleSocialLogin = (provider: string) => {
    window.location.href = `/api/auth/${provider}`
  }

  if (step === "verify") {
    return (
      <AuthCard
        title="이메일 인증"
        description="회원가입을 완료하려면 이메일을 확인하세요"
        footer={
          <p>
            이메일이 오지 않았나요?{" "}
            <button className="text-accent hover:underline font-medium">
              재전송
            </button>
          </p>
        }
      >
        <div className="text-center py-6">
          <div className="mx-auto w-16 h-16 rounded-full bg-chart-1/20 flex items-center justify-center mb-4">
            <CheckCircle2 className="h-8 w-8 text-chart-1" />
          </div>
          <h3 className="font-semibold text-lg mb-2">인증 메일을 전송했습니다</h3>
          <p className="text-sm text-muted-foreground mb-4">
            <span className="font-medium text-foreground">{formData.email}</span>
            <br />
            으로 인증 메일을 보냈습니다.
          </p>
          <p className="text-xs text-muted-foreground">
            메일 내 인증 링크를 클릭하면 회원가입이 완료됩니다.
          </p>
        </div>
        <Button
          variant="outline"
          className="w-full"
          onClick={() => router.push("/login")}
        >
          로그인 페이지로 이동
        </Button>
      </AuthCard>
    )
  }

  return (
    <AuthCard
      title="회원가입"
      description="PostForge 계정을 만들어보세요"
      footer={
        <p>
          이미 계정이 있으신가요?{" "}
          <Link href="/login" className="text-accent hover:underline font-medium">
            로그인
          </Link>
        </p>
      }
    >
      <div className="space-y-6">
        {/* Social Login */}
        <SocialLoginButtons
          onGoogleLogin={() => handleSocialLogin("google")}
          onNaverLogin={() => handleSocialLogin("naver")}
          onKakaoLogin={() => handleSocialLogin("kakao")}
        />

        {/* Divider */}
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-border" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-card px-2 text-muted-foreground">또는</span>
          </div>
        </div>

        {/* Register Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-lg border border-destructive/20">
              {error}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="userId">아이디</Label>
            <Input
              id="userId"
              type="text"
              placeholder="영문, 숫자 4-20자"
              value={formData.userId}
              onChange={(e) =>
                setFormData({ ...formData, userId: e.target.value })
              }
              required
              autoComplete="username"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">이메일</Label>
            <Input
              id="email"
              type="email"
              placeholder="example@email.com"
              value={formData.email}
              onChange={(e) =>
                setFormData({ ...formData, email: e.target.value })
              }
              required
              autoComplete="email"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="nickname">닉네임</Label>
            <Input
              id="nickname"
              type="text"
              placeholder="커뮤니티에서 사용할 이름"
              value={formData.nickname}
              onChange={(e) =>
                setFormData({ ...formData, nickname: e.target.value })
              }
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">비밀번호</Label>
            <div className="relative">
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="비밀번호를 입력하세요"
                value={formData.password}
                onChange={(e) =>
                  setFormData({ ...formData, password: e.target.value })
                }
                required
                autoComplete="new-password"
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            {/* Password Requirements */}
            <div className="flex flex-wrap gap-2 mt-2">
              {passwordRequirements.map((req) => (
                <span
                  key={req.label}
                  className={cn(
                    "text-xs px-2 py-0.5 rounded-full",
                    req.met
                      ? "bg-chart-1/20 text-chart-1"
                      : "bg-muted text-muted-foreground"
                  )}
                >
                  {req.label}
                </span>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirmPassword">비밀번호 확인</Label>
            <Input
              id="confirmPassword"
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
              value={formData.confirmPassword}
              onChange={(e) =>
                setFormData({ ...formData, confirmPassword: e.target.value })
              }
              required
              autoComplete="new-password"
            />
          </div>

          <Button type="submit" className="w-full h-11" disabled={isLoading}>
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                가입 중...
              </>
            ) : (
              "회원가입"
            )}
          </Button>

          <p className="text-xs text-center text-muted-foreground">
            가입 시{" "}
            <Link href="#" className="text-foreground hover:underline">
              이용약관
            </Link>
            {" 및 "}
            <Link href="#" className="text-foreground hover:underline">
              개인정보처리방침
            </Link>
            에 동의하게 됩니다.
          </p>
        </form>
      </div>
    </AuthCard>
  )
}
