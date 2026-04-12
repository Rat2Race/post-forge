"use client"

import { useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Loader2, CheckCircle2, XCircle } from "lucide-react"
import { Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

type CallbackState = "loading" | "success" | "error"

export default function OAuthCallbackPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [state, setState] = useState<CallbackState>("loading")
  const [error, setError] = useState("")

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code")
      const errorParam = searchParams.get("error")

      if (errorParam) {
        setState("error")
        setError("소셜 로그인이 취소되었거나 오류가 발생했습니다.")
        return
      }

      if (!code) {
        setState("error")
        setError("인증 코드가 없습니다. 다시 시도해주세요.")
        return
      }

      // Simulate processing OAuth callback
      await new Promise((resolve) => setTimeout(resolve, 2000))

      // Success - redirect to home
      setState("success")
      setTimeout(() => {
        router.push("/")
      }, 1500)
    }

    processCallback()
  }, [searchParams, router])

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-md text-center">
        {/* Logo */}
        <Link href="/" className="inline-flex items-center gap-2 mb-8">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary">
            <Sparkles className="h-5 w-5 text-primary-foreground" />
          </div>
          <span className="font-serif text-2xl font-bold tracking-tight">
            PostForge
          </span>
        </Link>

        {/* Loading State */}
        {state === "loading" && (
          <div className="space-y-4">
            <div className="mx-auto w-16 h-16 rounded-full bg-secondary flex items-center justify-center">
              <Loader2 className="h-8 w-8 text-accent animate-spin" />
            </div>
            <div>
              <h2 className="font-serif text-xl font-semibold mb-2">
                로그인 처리 중...
              </h2>
              <p className="text-sm text-muted-foreground">
                잠시만 기다려주세요.
              </p>
            </div>
          </div>
        )}

        {/* Success State */}
        {state === "success" && (
          <div className="space-y-4">
            <div className="mx-auto w-16 h-16 rounded-full bg-chart-1/20 flex items-center justify-center">
              <CheckCircle2 className="h-8 w-8 text-chart-1" />
            </div>
            <div>
              <h2 className="font-serif text-xl font-semibold mb-2">
                로그인 성공
              </h2>
              <p className="text-sm text-muted-foreground">
                잠시 후 메인 페이지로 이동합니다.
              </p>
            </div>
          </div>
        )}

        {/* Error State */}
        {state === "error" && (
          <div className="space-y-4">
            <div className="mx-auto w-16 h-16 rounded-full bg-destructive/20 flex items-center justify-center">
              <XCircle className="h-8 w-8 text-destructive" />
            </div>
            <div>
              <h2 className="font-serif text-xl font-semibold mb-2">
                로그인 실패
              </h2>
              <p className="text-sm text-muted-foreground mb-4">{error}</p>
              <div className="flex flex-col gap-2">
                <Button onClick={() => router.push("/login")}>
                  다시 시도하기
                </Button>
                <Button variant="outline" onClick={() => router.push("/")}>
                  홈으로 돌아가기
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
