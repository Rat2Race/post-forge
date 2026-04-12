"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import {
  ArrowLeft,
  Loader2,
  CheckCircle2,
  FileText,
  Search,
  Newspaper,
  AlertTriangle,
} from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

type GenerationState = "idle" | "generating" | "success" | "error"

interface GenerationStep {
  id: string
  label: string
  status: "pending" | "active" | "complete"
}

const initialSteps: GenerationStep[] = [
  { id: "search", label: "종목 정보 검색", status: "pending" },
  { id: "disclosure", label: "공시 자료 수집", status: "pending" },
  { id: "news", label: "뉴스 분석", status: "pending" },
  { id: "market", label: "시장 반응 분석", status: "pending" },
  { id: "writing", label: "분석 리포트 작성", status: "pending" },
]

const popularStocks = [
  { code: "005930", name: "삼성전자" },
  { code: "000660", name: "SK하이닉스" },
  { code: "035420", name: "NAVER" },
  { code: "035720", name: "카카오" },
  { code: "005380", name: "현대자동차" },
  { code: "068270", name: "셀트리온" },
]

export default function AIGeneratePage() {
  const router = useRouter()
  const [stockCode, setStockCode] = useState("")
  const [corpName, setCorpName] = useState("")
  const [state, setState] = useState<GenerationState>("idle")
  const [steps, setSteps] = useState(initialSteps)
  const [generatedPostId, setGeneratedPostId] = useState<string | null>(null)
  const [error, setError] = useState("")

  const handleSelectStock = (code: string, name: string) => {
    setStockCode(code)
    setCorpName(name)
  }

  const updateStepStatus = (
    stepId: string,
    status: "pending" | "active" | "complete"
  ) => {
    setSteps((prev) =>
      prev.map((step) => (step.id === stepId ? { ...step, status } : step))
    )
  }

  const handleGenerate = async () => {
    if (!stockCode.trim()) {
      setError("종목코드를 입력해주세요.")
      return
    }

    setError("")
    setState("generating")
    setSteps(initialSteps)

    try {
      updateStepStatus("search", "active")
      await new Promise((r) => setTimeout(r, 1500))
      updateStepStatus("search", "complete")

      updateStepStatus("disclosure", "active")
      await new Promise((r) => setTimeout(r, 2000))
      updateStepStatus("disclosure", "complete")

      updateStepStatus("news", "active")
      await new Promise((r) => setTimeout(r, 1800))
      updateStepStatus("news", "complete")

      updateStepStatus("market", "active")
      await new Promise((r) => setTimeout(r, 1500))
      updateStepStatus("market", "complete")

      updateStepStatus("writing", "active")
      await new Promise((r) => setTimeout(r, 2500))
      updateStepStatus("writing", "complete")

      setGeneratedPostId("new-ai-post-1")
      setState("success")
    } catch {
      setState("error")
      setError("분석 생성 중 오류가 발생했습니다. 다시 시도해주세요.")
    }
  }

  const handleViewPost = () => {
    if (generatedPostId) {
      router.push(`/posts/${generatedPostId}`)
    }
  }

  const handleReset = () => {
    setState("idle")
    setSteps(initialSteps)
    setStockCode("")
    setCorpName("")
    setGeneratedPostId(null)
    setError("")
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header isAuthenticated user={{ nickname: "사용자" }} />

      <main className="flex-1">
        <div className="mx-auto max-w-xl px-6 py-8">
          {/* Header */}
          <div className="mb-10">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground mb-6"
            >
              <ArrowLeft className="h-4 w-4" />
              홈으로 돌아가기
            </Link>
            <h1 className="font-serif text-2xl sm:text-3xl font-bold mb-3">
              분석 리포트 생성
            </h1>
            <p className="text-muted-foreground leading-relaxed">
              종목코드를 입력하면 공시, 뉴스, 시장 반응을 종합 분석한
              리포트를 자동으로 생성합니다.
            </p>
          </div>

          {/* Idle State - Input Form */}
          {state === "idle" && (
            <div className="space-y-8">
              {/* Error Message */}
              {error && (
                <div className="p-4 text-sm text-destructive bg-destructive/5 rounded-lg border border-destructive/10 flex items-start gap-3">
                  <AlertTriangle className="h-4 w-4 mt-0.5 shrink-0" />
                  {error}
                </div>
              )}

              {/* Input Fields */}
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="stockCode" className="text-sm text-muted-foreground">
                      종목코드
                    </Label>
                    <Input
                      id="stockCode"
                      value={stockCode}
                      onChange={(e) => setStockCode(e.target.value)}
                      placeholder="005930"
                      maxLength={6}
                      className="h-12 text-base bg-secondary/30 border-0 focus-visible:ring-1"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="corpName" className="text-sm text-muted-foreground">
                      회사명 (선택)
                    </Label>
                    <Input
                      id="corpName"
                      value={corpName}
                      onChange={(e) => setCorpName(e.target.value)}
                      placeholder="삼성전자"
                      className="h-12 text-base bg-secondary/30 border-0 focus-visible:ring-1"
                    />
                  </div>
                </div>

                {/* Popular Stocks */}
                <div className="pt-2">
                  <p className="text-sm text-muted-foreground mb-3">빠른 선택</p>
                  <div className="flex flex-wrap gap-2">
                    {popularStocks.map((stock) => (
                      <button
                        key={stock.code}
                        onClick={() => handleSelectStock(stock.code, stock.name)}
                        className={cn(
                          "px-4 py-2 text-sm rounded-full border transition-colors",
                          stockCode === stock.code
                            ? "bg-foreground text-background border-foreground"
                            : "border-border hover:border-foreground/30"
                        )}
                      >
                        {stock.name}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <Button
                onClick={handleGenerate}
                className="w-full h-12"
                disabled={!stockCode.trim()}
              >
                분석 시작
              </Button>

              {/* Info */}
              <div className="pt-6 border-t border-border">
                <p className="text-sm font-medium mb-3">분석 리포트 포함 내용</p>
                <ul className="text-sm text-muted-foreground space-y-2">
                  <li className="flex items-start gap-2">
                    <span className="text-accent">-</span>
                    최신 공시 자료 분석
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-accent">-</span>
                    관련 뉴스 동향
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-accent">-</span>
                    시장 반응 및 증권사 리포트 요약
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-accent">-</span>
                    투자 방향성 판단
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-accent">-</span>
                    리스크 요인 분석
                  </li>
                </ul>
              </div>
            </div>
          )}

          {/* Generating State */}
          {state === "generating" && (
            <div className="py-8">
              <div className="text-center mb-10">
                <h2 className="font-serif text-xl font-bold mb-2">
                  {corpName || stockCode} 분석 중
                </h2>
                <p className="text-sm text-muted-foreground">
                  분석 리포트를 생성하고 있습니다
                </p>
              </div>

              {/* Progress Steps */}
              <div className="space-y-4">
                {steps.map((step, index) => (
                  <div
                    key={step.id}
                    className={cn(
                      "flex items-center gap-4 py-3 px-4 rounded-lg transition-colors",
                      step.status === "active" && "bg-secondary/50",
                      step.status === "complete" && "bg-chart-1/5"
                    )}
                  >
                    <div className="w-6 h-6 flex items-center justify-center">
                      {step.status === "complete" ? (
                        <CheckCircle2 className="h-5 w-5 text-chart-1" />
                      ) : step.status === "active" ? (
                        <Loader2 className="h-5 w-5 text-accent animate-spin" />
                      ) : (
                        <span className="text-sm text-muted-foreground">{index + 1}</span>
                      )}
                    </div>
                    <span
                      className={cn(
                        "text-sm",
                        step.status === "pending" && "text-muted-foreground",
                        step.status === "active" && "text-foreground font-medium",
                        step.status === "complete" && "text-chart-1"
                      )}
                    >
                      {step.label}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Success State */}
          {state === "success" && (
            <div className="py-12 text-center">
              <CheckCircle2 className="h-12 w-12 text-chart-1 mx-auto mb-6" />
              <h2 className="font-serif text-xl font-bold mb-3">
                분석 리포트가 생성되었습니다
              </h2>
              <p className="text-muted-foreground mb-8">
                <span className="text-foreground font-medium">
                  {corpName || stockCode}
                </span>
                {" "}분석 리포트가 게시되었습니다.
              </p>
              <div className="flex flex-col gap-3">
                <Button onClick={handleViewPost} className="w-full">
                  리포트 보기
                </Button>
                <Button variant="outline" onClick={handleReset} className="w-full">
                  새 분석 생성
                </Button>
              </div>
            </div>
          )}

          {/* Error State */}
          {state === "error" && (
            <div className="py-12 text-center">
              <AlertTriangle className="h-12 w-12 text-destructive mx-auto mb-6" />
              <h2 className="font-serif text-xl font-bold mb-3">
                생성 중 오류가 발생했습니다
              </h2>
              <p className="text-muted-foreground mb-8">{error}</p>
              <Button onClick={handleReset}>다시 시도</Button>
            </div>
          )}
        </div>
      </main>

      <Footer />
    </div>
  )
}
