"use client"

import { useState, useRef, useEffect } from "react"
import Link from "next/link"
import { ArrowLeft, Send, Loader2, RefreshCw } from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

interface Message {
  id: string
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

const suggestedQuestions = [
  "삼성전자 최근 공시 요약",
  "SK하이닉스와 삼성전자 HBM 비교",
  "현대차 전기차 전략 리스크",
  "반도체 업황 전망",
]

export default function AIChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      role: "assistant",
      content:
        "안녕하세요. 한국 주식, 공시, 시장 동향에 대해 질문해 주세요. 기업 분석, 투자 아이디어, 시장 전망 등 다양한 주제에 대해 리서치를 도와드립니다.",
      timestamp: new Date(),
    },
  ])
  const [input, setInput] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSubmit = async (question?: string) => {
    const messageText = question || input.trim()
    if (!messageText || isLoading) return

    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: "user",
      content: messageText,
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput("")
    setIsLoading(true)

    await new Promise((r) => setTimeout(r, 2000))

    const aiResponses: Record<string, string> = {
      "삼성전자 최근 공시 요약": `삼성전자의 최근 주요 공시를 요약해 드리겠습니다.

2026년 1분기 잠정실적 (2026.04.05)
매출액 79조 1,000억원으로 전년 동기 대비 18% 증가했습니다. 영업이익은 8조 2,000억원으로 전년 대비 156% 증가했으며, 영업이익률은 10.4%를 기록했습니다.

주요 사업부 실적을 보면, 반도체 부문에서 HBM3E 양산 본격화로 수익성이 대폭 개선되었습니다. 디스플레이 부문은 폴더블 패널 점유율이 확대되었고, 모바일 부문은 갤럭시 S26이 호조를 보이고 있습니다.

전체적으로 AI 반도체 수요 증가의 수혜를 받고 있으며, 하반기 실적 전망도 긍정적입니다.`,

      "SK하이닉스와 삼성전자 HBM 비교": `SK하이닉스와 삼성전자의 HBM 경쟁력을 비교 분석해 드리겠습니다.

SK하이닉스는 현재 HBM 시장 점유율 약 53%로 선두를 달리고 있습니다. 엔비디아 H100 및 H200 GPU에 독점 공급하고 있으며, HBM3E 양산을 2024년부터 본격화했습니다. 12단 적층 기술에서 앞서 있습니다.

삼성전자는 시장 점유율 약 38%로 2위입니다. 대규모 생산 능력이 강점이며, 수율을 개선 중입니다. HBM3E 양산은 2025년 하반기부터 본격화되었고, 엔비디아 품질 인증 통과가 과제로 남아있습니다.

결론적으로, 현재는 SK하이닉스가 기술력과 고객 관계에서 앞서 있지만, 삼성전자도 빠르게 격차를 좁히고 있습니다. HBM4 세대에서는 경쟁이 더욱 치열해질 전망입니다.`,

      default: `해당 내용에 대해 분석해 드리겠습니다.

현재 한국 주식 시장은 AI 반도체 수요 증가와 글로벌 경기 회복 기대감으로 긍정적인 흐름을 보이고 있습니다.

더 구체적인 종목이나 주제에 대해 질문해 주시면 상세한 분석을 제공해 드릴 수 있습니다. 특정 종목의 최근 공시 분석, 업종별 투자 전망, 기업 간 비교 분석 등 다양한 리서치를 지원합니다.`,
    }

    const responseContent =
      aiResponses[messageText] || aiResponses["default"]

    const aiMessage: Message = {
      id: `ai-${Date.now()}`,
      role: "assistant",
      content: responseContent,
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, aiMessage])
    setIsLoading(false)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  const handleReset = () => {
    setMessages([
      {
        id: "welcome",
        role: "assistant",
        content:
          "안녕하세요. 한국 주식, 공시, 시장 동향에 대해 질문해 주세요.",
        timestamp: new Date(),
      },
    ])
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header isAuthenticated user={{ nickname: "사용자" }} />

      <main className="flex-1 flex flex-col">
        <div className="mx-auto max-w-2xl w-full px-6 py-8 flex flex-col flex-1">
          {/* Header */}
          <div className="mb-8">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground mb-6"
            >
              <ArrowLeft className="h-4 w-4" />
              홈으로 돌아가기
            </Link>
            <div className="flex items-center justify-between">
              <div>
                <h1 className="font-serif text-2xl font-bold mb-2">
                  리서치 AI
                </h1>
                <p className="text-muted-foreground">
                  주식, 공시, 시장 동향에 대해 질문해 주세요
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleReset}
                className="text-muted-foreground"
              >
                <RefreshCw className="h-4 w-4 mr-1.5" />
                새 대화
              </Button>
            </div>
          </div>

          {/* Chat Area */}
          <div className="flex-1 flex flex-col">
            {/* Messages */}
            <div className="flex-1 space-y-8 mb-8">
              {messages.map((message) => (
                <div key={message.id}>
                  <div className="flex items-center gap-2 mb-2">
                    <span className={cn(
                      "text-sm font-medium",
                      message.role === "assistant" ? "text-accent" : "text-foreground"
                    )}>
                      {message.role === "assistant" ? "리서치 AI" : "나"}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {message.timestamp.toLocaleTimeString("ko-KR", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  </div>
                  <div className="text-[15px] leading-relaxed whitespace-pre-wrap text-foreground/90">
                    {message.content}
                  </div>
                </div>
              ))}

              {isLoading && (
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-sm font-medium text-accent">
                      리서치 AI
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span className="text-sm">분석 중...</span>
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>

            {/* Suggested Questions */}
            {messages.length <= 1 && (
              <div className="mb-6">
                <p className="text-sm text-muted-foreground mb-3">
                  자주 묻는 질문
                </p>
                <div className="flex flex-wrap gap-2">
                  {suggestedQuestions.map((question) => (
                    <button
                      key={question}
                      onClick={() => handleSubmit(question)}
                      className="text-sm px-4 py-2 rounded-full border border-border hover:border-accent/50 transition-colors"
                    >
                      {question}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Input Area */}
            <div className="border-t border-border pt-6">
              <div className="flex gap-3">
                <Textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="질문을 입력하세요..."
                  className="min-h-12 max-h-32 resize-none text-[15px] bg-secondary/30 border-0 focus-visible:ring-1"
                  disabled={isLoading}
                />
                <Button
                  onClick={() => handleSubmit()}
                  disabled={!input.trim() || isLoading}
                  size="icon"
                  className="shrink-0 h-12 w-12"
                >
                  {isLoading ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Send className="h-4 w-4" />
                  )}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground mt-3">
                응답은 참고용이며, 투자 결정은 본인의 판단으로 하시기 바랍니다.
              </p>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  )
}
