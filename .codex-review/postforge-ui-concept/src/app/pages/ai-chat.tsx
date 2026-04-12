import { useState, useRef, useEffect } from "react";
import { Send, Sparkles, TrendingUp, FileText, Lightbulb } from "lucide-react";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";
import { Card } from "../components/ui/card";
import { Avatar, AvatarFallback } from "../components/ui/avatar";
import { Badge } from "../components/ui/badge";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: string;
}

const SUGGESTED_QUESTIONS = [
  "삼성전자의 최근 공시 내용을 분석해줘",
  "HBM 시장 전망은 어때?",
  "반도체 업종 투자 전략을 알려줘",
  "최근 증시 이슈가 뭐야?",
];

const MOCK_RESPONSES = [
  "삼성전자의 최근 공시를 분석한 결과, HBM3E 메모리 양산이 본격화되면서 AI 서버 시장 수요가 급증하고 있습니다. 1분기 영업이익이 6.5조원으로 시장 예상치를 상회했으며, 특히 DS 부문의 실적 개선이 두드러집니다.\n\n주요 포인트:\n• HBM3E 양산 본격화\n• AI 서버 수요 증가\n• 영업이익 전년 대비 52% 증가\n\n다만 밸류에이션 부담과 중국 경기 둔화 리스크는 주의가 필요합니다.",
  "HBM(고대역폭 메모리) 시장은 2026년에도 강한 성장세를 이어갈 전망입니다.\n\n시장 전망:\n• 2026년 시장 규모: 약 250억 달러 예상\n• 전년 대비 성장률: 70% 이상\n• 주요 성장 동력: AI 서버, 데이터센터 확장\n\n주요 업체:\n• SK하이닉스: 시장 점유율 1위 (약 50%)\n• 삼성전자: 빠른 추격 중 (약 40%)\n• 마이크론: 신규 진입 (약 10%)\n\nHBM4 개발 경쟁이 본격화되고 있으며, 기술 우위 확보가 중요한 시점입니다.",
];

export function AIChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "1",
      role: "assistant",
      content: "안녕하세요! PostForge AI 분석가입니다. 주식 시장, 공시, 투자 전략에 대해 무엇이든 물어보세요. 😊",
      timestamp: "지금",
    },
  ]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

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

    // Simulate AI response
    setTimeout(() => {
      const responseContent = MOCK_RESPONSES[Math.floor(Math.random() * MOCK_RESPONSES.length)];
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: responseContent,
        timestamp: "지금",
      };

      setMessages((prev) => [...prev, assistantMessage]);
      setIsLoading(false);
    }, 1500);
  };

  const handleSuggestedQuestion = (question: string) => {
    handleSendMessage(question);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSendMessage(input);
  };

  return (
    <div className="h-[calc(100vh-4rem)] flex justify-center bg-secondary/30 editorial-texture overflow-hidden">
      <div className="w-full max-w-4xl flex flex-col bg-background shadow-2xl border-x border-border relative">
        {/* Header */}
        <div className="border-b-2 border-primary bg-background/95 backdrop-blur-sm z-10 shrink-0">
          <div className="px-4 sm:px-6 lg:px-8 py-6">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 border-2 border-primary bg-brass-light flex items-center justify-center shrink-0">
                <Sparkles className="h-6 w-6 text-primary" />
              </div>
              <div>
                <h1 className="serif-headline text-2xl text-primary font-semibold tracking-tight">AI 대화 (Intelligence Chat)</h1>
                <p className="text-sm text-muted-foreground uppercase tracking-widest font-medium mt-1">실시간 주식 분석 및 투자 상담</p>
              </div>
            </div>
          </div>
        </div>

        {/* Scrollable Chat Area */}
        <div className="flex-1 overflow-y-auto">
          <div className="px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
            <div className="space-y-10">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex flex-col ${
                    message.role === "user" ? "items-end" : "items-start"
                  }`}
                >
                  <div className="flex items-center gap-2 mb-2 px-1">
                    {message.role === "assistant" && (
                      <span className="text-xs font-bold uppercase tracking-widest text-brass">AI Analyst</span>
                    )}
                    {message.role === "user" && (
                      <span className="text-xs font-bold uppercase tracking-widest text-muted-foreground">User</span>
                    )}
                  </div>

                  <div
                    className={`max-w-[85%] sm:max-w-[75%] p-6 ${
                      message.role === "user"
                        ? "bg-primary text-primary-foreground border-none"
                        : "bg-card border border-border border-l-4 border-l-brass shadow-sm"
                    }`}
                  >
                    <p className="text-base sm:text-lg leading-relaxed whitespace-pre-wrap font-light">
                      {message.content}
                    </p>
                  </div>
                  <p className="text-xs text-muted-foreground mt-2 px-1 font-medium tracking-wider">
                    {message.timestamp}
                  </p>
                </div>
              ))}

              {isLoading && (
                <div className="flex flex-col items-start">
                  <div className="flex items-center gap-2 mb-2 px-1">
                    <span className="text-xs font-bold uppercase tracking-widest text-brass">AI Analyst</span>
                  </div>
                  <div className="bg-card border border-border border-l-4 border-l-brass p-6 shadow-sm">
                    <div className="flex gap-2 items-center h-6">
                      <div className="w-2 h-2 bg-brass animate-bounce" style={{ animationDelay: '0ms' }} />
                      <div className="w-2 h-2 bg-brass animate-bounce" style={{ animationDelay: '150ms' }} />
                      <div className="w-2 h-2 bg-brass animate-bounce" style={{ animationDelay: '300ms' }} />
                    </div>
                  </div>
                </div>
              )}

              {messages.length === 1 && (
                <div className="mt-12 p-8 border border-border bg-card/50">
                  <h3 className="serif-headline text-xl mb-6 flex items-center gap-3 border-b border-border pb-3">
                    <Lightbulb className="h-5 w-5 text-brass" />
                    추천 질문 (Suggested Topics)
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {SUGGESTED_QUESTIONS.map((question, index) => (
                      <button
                        key={index}
                        onClick={() => handleSuggestedQuestion(question)}
                        className="text-left p-4 bg-background border border-border hover:border-brass hover:bg-secondary/30 transition-all text-sm leading-relaxed group"
                      >
                        <span className="font-semibold text-primary group-hover:text-brass transition-colors">{question}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
          </div>
        </div>

        {/* Footer Input Area */}
        <div className="border-t-2 border-border bg-card/95 backdrop-blur-md p-4 sm:p-6 z-10 pb-8 shrink-0">
          <div className="relative">
            <form onSubmit={handleSubmit} className="relative">
              <Textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="AI에게 질문하기... (Ask the AI Analyst)"
                className="min-h-[64px] max-h-32 resize-none rounded-none border border-border focus-visible:ring-1 focus-visible:ring-primary focus-visible:border-primary text-base pl-4 pr-32 py-4 bg-background shadow-sm"
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
                className="absolute right-2 bottom-2 h-12 shrink-0 rounded-none px-6 uppercase tracking-widest text-xs font-bold"
              >
                <Send className="h-4 w-4 sm:mr-2" />
                <span className="hidden sm:inline">전송 (Send)</span>
              </Button>
            </form>
            <div className="flex justify-center mt-4">
              <p className="text-[10px] text-muted-foreground uppercase tracking-widest font-semibold flex items-center gap-2">
                <span className="w-2 h-px bg-muted-foreground/30"></span>
                AI가 생성한 답변은 참고용이며, 투자 결정은 신중히 하시기 바랍니다
                <span className="w-2 h-px bg-muted-foreground/30"></span>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
