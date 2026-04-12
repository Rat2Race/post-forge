import { useState, useRef } from "react";
import { Send, Sparkles, Lightbulb, FileText } from "lucide-react";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: string;
  sources?: { title: string; type: string }[];
}

const SUGGESTED_QUESTIONS = [
  "삼성전자의 최근 공시 내용을 분석해줘",
  "HBM 시장 전망은 어때?",
  "반도체 업종 투자 전략을 알려줘",
  "최근 증시 이슈가 뭐야?",
];

const MOCK_RESPONSES: Omit<Message, "id" | "timestamp">[] = [
  {
    role: "assistant",
    content: "삼성전자의 최근 공시를 분석한 결과, HBM3E 메모리 양산이 본격화되면서 AI 서버 시장 수요가 급증하고 있습니다. 1분기 영업이익이 6.5조원으로 시장 예상치를 상회했으며, 특히 DS 부문의 실적 개선이 두드러집니다.\n\n주요 포인트:\n• HBM3E 양산 본격화\n• AI 서버 수요 증가\n• 영업이익 전년 대비 52% 증가\n\n다만 밸류에이션 부담과 중국 경기 둔화 리스크는 주의가 필요합니다.",
    sources: [
      { title: "삼성전자 1Q26 실적 공시", type: "공시" },
      { title: "반도체 시장 동향 보고서", type: "보고서" },
    ],
  },
  {
    role: "assistant",
    content: "HBM(고대역폭 메모리) 시장은 2026년에도 강한 성장세를 이어갈 전망입니다.\n\n시장 전망:\n• 2026년 시장 규모: 약 250억 달러 예상\n• 전년 대비 성장률: 70% 이상\n• 주요 성장 동력: AI 서버, 데이터센터 확장\n\n주요 업체:\n• SK하이닉스: 시장 점유율 1위 (약 50%)\n• 삼성전자: 빠른 추격 중 (약 40%)\n• 마이크론: 신규 진입 (약 10%)\n\nHBM4 개발 경쟁이 본격화되고 있으며, 기술 우위 확보가 중요한 시점입니다.",
    sources: [
      { title: "HBM 시장 분석 리포트", type: "보고서" },
    ],
  },
];

export function AIChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "1",
      role: "assistant",
      content: "안녕하세요, PostForge AI 분석가입니다. 주식 시장, 공시, 투자 전략에 대해 무엇이든 물어보세요.",
      timestamp: "지금",
    },
  ]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const handleSendMessage = async (content: string) => {
    if (!content.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: "user",
      content: content.trim(),
      timestamp: "지금",
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setIsLoading(true);

    setTimeout(() => {
      const mockResp = MOCK_RESPONSES[Math.floor(Math.random() * MOCK_RESPONSES.length)];
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        ...mockResp,
        timestamp: "지금",
      };
      setMessages((prev) => [...prev, assistantMessage]);
      setIsLoading(false);
    }, 1500);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSendMessage(input);
  };

  return (
    <div className="h-[calc(100vh-3.5rem)] flex flex-col">
      {/* Header */}
      <div className="border-b border-border/60 bg-card px-5 lg:px-8 py-3 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-brass/10 rounded-lg flex items-center justify-center">
            <Sparkles className="h-4 w-4 text-brass" />
          </div>
          <div>
            <h1 className="text-sm text-foreground">AI 주식 분석 대화</h1>
            <p className="text-xs text-muted-foreground">실시간 주식 분석 및 투자 상담</p>
          </div>
        </div>
      </div>

      {/* Chat Area */}
      <div className="flex-1 overflow-y-auto bg-secondary/30">
        <div className="px-5 lg:px-8 py-5 max-w-3xl mx-auto">
          <div className="space-y-5">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex flex-col ${message.role === "user" ? "items-end" : "items-start"}`}
              >
                <div className="flex items-center gap-1.5 mb-1 px-1">
                  <span className="text-xs text-muted-foreground">
                    {message.role === "assistant" ? "AI 분석가" : "나"}
                  </span>
                  <span className="text-xs text-muted-foreground/50">·</span>
                  <span className="text-xs text-muted-foreground/50">{message.timestamp}</span>
                </div>

                <div className={`max-w-[85%] sm:max-w-[75%] rounded-lg p-4 ${
                  message.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-card border border-border/60"
                }`}>
                  <p className="text-sm leading-relaxed whitespace-pre-wrap">
                    {message.content}
                  </p>
                </div>

                {message.sources && message.sources.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2 px-1">
                    {message.sources.map((src, idx) => (
                      <span key={idx} className="inline-flex items-center gap-1 text-xs text-muted-foreground bg-secondary rounded px-2 py-0.5">
                        <FileText className="h-3 w-3" /> {src.title}
                      </span>
                    ))}
                  </div>
                )}

                {message.role === "assistant" && message.id !== "1" && (
                  <div className="flex flex-wrap gap-1.5 mt-2 px-1">
                    <button
                      onClick={() => handleSendMessage("더 자세히 분석해줘")}
                      className="text-xs text-muted-foreground bg-secondary hover:bg-secondary/80 rounded-full px-3 py-1 transition-colors"
                    >
                      더 자세히 분석
                    </button>
                    <button
                      onClick={() => handleSendMessage("관련 종목도 알려줘")}
                      className="text-xs text-muted-foreground bg-secondary hover:bg-secondary/80 rounded-full px-3 py-1 transition-colors"
                    >
                      관련 종목 보기
                    </button>
                  </div>
                )}
              </div>
            ))}

            {isLoading && (
              <div className="flex flex-col items-start">
                <span className="text-xs text-muted-foreground mb-1 px-1">AI 분석가</span>
                <div className="bg-card border border-border/60 rounded-lg p-4">
                  <div className="flex gap-1.5 items-center h-5">
                    <div className="w-1.5 h-1.5 bg-muted-foreground/40 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <div className="w-1.5 h-1.5 bg-muted-foreground/40 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <div className="w-1.5 h-1.5 bg-muted-foreground/40 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              </div>
            )}

            {messages.length === 1 && (
              <div className="bg-card border border-border/60 rounded-lg p-5 mt-4">
                <h3 className="text-sm mb-3 flex items-center gap-2 text-muted-foreground">
                  <Lightbulb className="h-4 w-4" /> 추천 질문
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {SUGGESTED_QUESTIONS.map((question, index) => (
                    <button
                      key={index}
                      onClick={() => handleSendMessage(question)}
                      className="text-left p-3 bg-secondary/50 hover:bg-secondary rounded-lg transition-colors text-sm text-foreground/80"
                    >
                      {question}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        </div>
      </div>

      {/* Input Area */}
      <div className="border-t border-border/60 bg-card px-5 lg:px-8 py-3 shrink-0">
        <div className="max-w-3xl mx-auto">
          <form onSubmit={handleSubmit} className="relative">
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="AI에게 질문하기..."
              className="min-h-[48px] max-h-28 resize-none rounded-lg pr-20 text-sm"
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  handleSubmit(e);
                }
              }}
            />
            <Button
              type="submit"
              disabled={!input.trim() || isLoading}
              size="sm"
              className="absolute right-2 bottom-2 rounded-md"
            >
              <Send className="h-3.5 w-3.5 mr-1" /> 전송
            </Button>
          </form>
          <p className="text-[11px] text-muted-foreground/50 text-center mt-2">
            AI가 생성한 답변은 참고용이며, 투자 결정은 신중히 하시기 바랍니다.
          </p>
        </div>
      </div>
    </div>
  );
}
