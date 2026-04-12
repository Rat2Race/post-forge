import Link from "next/link"
import { Lock, Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"

interface LoginWallProps {
  title: string
  summary: string
}

export function LoginWall({ title, summary }: LoginWallProps) {
  return (
    <div className="min-h-screen bg-background">
      {/* Blurred Preview */}
      <div className="relative">
        <div className="mx-auto max-w-4xl px-4 pt-8">
          {/* Article Preview (blurred) */}
          <div className="relative">
            <div className="blur-sm select-none pointer-events-none">
              <h1 className="font-serif text-3xl md:text-4xl font-bold tracking-tight leading-tight mb-4">
                {title}
              </h1>
              <p className="text-lg text-muted-foreground leading-relaxed mb-6">
                {summary}
              </p>
              <div className="space-y-4">
                <div className="h-4 bg-muted rounded w-full" />
                <div className="h-4 bg-muted rounded w-11/12" />
                <div className="h-4 bg-muted rounded w-10/12" />
                <div className="h-4 bg-muted rounded w-full" />
                <div className="h-4 bg-muted rounded w-9/12" />
              </div>
            </div>

            {/* Gradient Overlay */}
            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-background/80 to-background" />
          </div>
        </div>

        {/* Login Card */}
        <div className="absolute inset-x-0 bottom-0 top-1/3 flex items-center justify-center px-4">
          <div className="w-full max-w-md bg-card border border-border rounded-xl p-8 shadow-lg text-center">
            <div className="mx-auto w-14 h-14 rounded-full bg-accent/20 flex items-center justify-center mb-4">
              <Lock className="h-6 w-6 text-accent" />
            </div>
            <h2 className="font-serif text-2xl font-bold mb-2">
              로그인이 필요합니다
            </h2>
            <p className="text-muted-foreground mb-6">
              전체 분석 리포트를 읽으려면 로그인해주세요.
              <br />
              무료로 가입하고 모든 콘텐츠를 이용하세요.
            </p>
            <div className="space-y-3">
              <Link href="/login" className="block">
                <Button className="w-full h-11">로그인</Button>
              </Link>
              <Link href="/register" className="block">
                <Button variant="outline" className="w-full h-11">
                  무료 회원가입
                </Button>
              </Link>
            </div>
            <div className="mt-6 pt-6 border-t border-border">
              <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
                <Sparkles className="h-4 w-4 text-accent" />
                <span>AI가 분석한 전문 리포트</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
