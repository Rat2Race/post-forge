import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Sparkles, TrendingUp, FileText, CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Progress } from "../components/ui/progress";

type GenerationState = "idle" | "loading" | "success";

export function AIGeneratePage() {
  const navigate = useNavigate();
  const [state, setState] = useState<GenerationState>("idle");
  const [formData, setFormData] = useState({ stockCode: "", corpName: "" });
  const [progress, setProgress] = useState(0);
  const [generatedPostId, setGeneratedPostId] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setState("loading");
    setProgress(0);

    const stages = [
      { progress: 20, delay: 500 },
      { progress: 40, delay: 1000 },
      { progress: 60, delay: 1500 },
      { progress: 80, delay: 2000 },
      { progress: 100, delay: 2500 },
    ];

    stages.forEach((stage) => {
      setTimeout(() => {
        setProgress(stage.progress);
        if (stage.progress === 100) {
          setTimeout(() => { setGeneratedPostId("ai-generated-1"); setState("success"); }, 500);
        }
      }, stage.delay);
    });
  };

  const handleReset = () => {
    setState("idle");
    setProgress(0);
    setFormData({ stockCode: "", corpName: "" });
    setGeneratedPostId(null);
  };

  if (state === "success" && generatedPostId) {
    return (
      <div className="px-5 lg:px-8 py-12 pb-16">
        <div className="max-w-md mx-auto text-center">
          <div className="w-12 h-12 bg-positive-light rounded-full flex items-center justify-center mx-auto mb-5">
            <CheckCircle2 className="h-6 w-6 text-positive" />
          </div>
          <h1 className="serif-headline text-2xl mb-3">AI 분석 생성 완료</h1>
          <p className="text-sm text-muted-foreground mb-8">
            <span className="text-foreground">{formData.corpName || formData.stockCode}</span>에 대한 종합 분석 리포트가 생성되었습니다.
          </p>
          <div className="flex items-center justify-center gap-6 text-sm text-muted-foreground mb-8 py-4 border-y border-border/50">
            <div><span className="block text-foreground text-sm">종합 리포트</span><span className="text-xs">8개 섹션</span></div>
            <div className="h-8 w-px bg-border" />
            <div><span className="block text-foreground text-sm">참조 출처</span><span className="text-xs">12개</span></div>
          </div>
          <div className="space-y-2.5 max-w-xs mx-auto">
            <Button className="w-full" asChild><Link to={`/posts/${generatedPostId}`}><FileText className="h-4 w-4 mr-2" /> 생성된 분석 보기</Link></Button>
            <Button variant="outline" className="w-full" onClick={handleReset}>새 분석 생성하기</Button>
          </div>
        </div>

        <div className="max-w-2xl mx-auto mt-16 pt-8 border-t border-border/50">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { title: "생성 리포트 검토", desc: "핵심 요약, 리스크, 출처를 확인하고 필요한 경우 직접 수정하세요." },
              { title: "관련 종목 확장", desc: "AI 대화에서 유사 업종과 경쟁사까지 연결해 비교 분석을 이어갈 수 있습니다." },
              { title: "커뮤니티 공유", desc: "생성된 분석을 게시하고 댓글을 통해 추가 근거나 반론을 받을 수 있습니다." },
            ].map((item) => (
              <div key={item.title} className="rounded-lg border border-border/60 bg-card p-4">
                <p className="text-sm text-foreground mb-1.5">{item.title}</p>
                <p className="text-xs text-muted-foreground leading-relaxed">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (state === "loading") {
    return (
      <div className="px-5 lg:px-8 py-12 pb-16">
        <div className="max-w-md mx-auto">
          <div className="border border-border/60 rounded-lg p-8 bg-card">
            <div className="text-center mb-8">
              <div className="w-12 h-12 bg-brass/10 rounded-full flex items-center justify-center mx-auto mb-4">
                <Sparkles className="h-5 w-5 text-brass animate-pulse" />
              </div>
              <h2 className="serif-headline text-xl mb-2">AI 분석 생성 중</h2>
              <p className="text-sm text-muted-foreground">
                <span className="text-foreground">{formData.corpName || formData.stockCode}</span> 종합 분석을 생성하고 있습니다
              </p>
            </div>

            <div className="space-y-6">
              <div>
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-muted-foreground text-xs">
                    {progress < 20 && "공시 데이터 수집 중..."}
                    {progress >= 20 && progress < 40 && "뉴스 분석 중..."}
                    {progress >= 40 && progress < 60 && "시장 데이터 분석 중..."}
                    {progress >= 60 && progress < 100 && "리포트 생성 중..."}
                    {progress === 100 && "완료!"}
                  </span>
                  <span className="text-foreground text-xs">{progress}%</span>
                </div>
                <Progress value={progress} className="h-1" />
              </div>

              <div className="space-y-2.5 text-sm">
                {[
                  { label: "공시 및 실적 데이터 수집", threshold: 20 },
                  { label: "최신 뉴스 및 시장 동향 분석", threshold: 40 },
                  { label: "주가 데이터 및 기술적 분석", threshold: 60 },
                  { label: "종합 리포트 생성 및 게시", threshold: 100 },
                ].map((step, idx) => (
                  <div key={idx} className="flex items-center gap-3">
                    <Loader2 className={`h-3.5 w-3.5 shrink-0 ${
                      progress >= step.threshold ? "text-positive" : "text-muted-foreground"
                    } ${progress < step.threshold && progress >= (idx > 0 ? [0, 20, 40, 60][idx] : 0) ? "animate-spin" : ""}`} />
                    <span className={`text-sm ${progress >= step.threshold ? "text-foreground" : "text-muted-foreground"}`}>
                      {step.label}
                    </span>
                  </div>
                ))}
              </div>

              <p className="text-xs text-center text-muted-foreground/60">예상 소요 시간: 약 30초 ~ 1분</p>
            </div>
          </div>
        </div>

        <div className="max-w-2xl mx-auto mt-16 pt-8 border-t border-border/50">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {[
              { title: "분석 범위", desc: "공시, 뉴스, 시장 데이터가 순서대로 수집되어 종합 리포트에 반영됩니다." },
              { title: "결과 활용", desc: "완료 후 상세 리포트 확인, 추가 질문, 게시글 공유 흐름으로 이어집니다." },
              { title: "주의 사항", desc: "자동 생성 분석은 초안이므로 투자 결정 전 추가 검토와 사실 확인이 필요합니다." },
            ].map((item) => (
              <div key={item.title} className="rounded-lg border border-border/60 bg-card p-4">
                <p className="text-sm text-foreground mb-1.5">{item.title}</p>
                <p className="text-xs text-muted-foreground leading-relaxed">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="px-5 lg:px-8 py-8 pb-16">
      <div className="max-w-xl mx-auto">
        <div className="text-center mb-8">
          <div className="w-10 h-10 bg-brass/10 rounded-full flex items-center justify-center mx-auto mb-4">
            <Sparkles className="h-4 w-4 text-brass" />
          </div>
          <h1 className="serif-headline text-2xl text-primary mb-2">AI 분석 생성</h1>
          <p className="text-sm text-muted-foreground max-w-sm mx-auto leading-relaxed">
            종목 코드를 입력하면 AI가 공시, 뉴스, 시장 데이터를 분석하여 투자 리포트를 자동 생성합니다.
          </p>
        </div>

        <div className="border border-border/60 rounded-lg p-6 bg-card">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="stockCode">종목 코드 <span className="text-negative">*</span></Label>
              <Input id="stockCode" placeholder="예: 005930 (삼성전자)" value={formData.stockCode} onChange={(e) => setFormData({ ...formData, stockCode: e.target.value })} required />
              <p className="text-xs text-muted-foreground mt-1">6자리 종목 코드를 정확히 입력하세요</p>
            </div>
            <div>
              <Label htmlFor="corpName">기업명 <span className="text-xs text-muted-foreground">(선택)</span></Label>
              <Input id="corpName" placeholder="예: 삼성전자" value={formData.corpName} onChange={(e) => setFormData({ ...formData, corpName: e.target.value })} />
            </div>
            <Button type="submit" className="w-full h-10 bg-brass text-white hover:bg-brass/90">
              <Sparkles className="h-4 w-4 mr-2" /> AI 분석 생성하기
            </Button>
          </form>

          <div className="mt-6 pt-5 border-t border-border/40">
            <h3 className="text-xs text-muted-foreground mb-3 flex items-center gap-1.5">
              <TrendingUp className="h-3.5 w-3.5" /> 생성되는 분석 콘텐츠
            </h3>
            <div className="grid grid-cols-2 gap-3 text-sm">
              {[
                { emoji: "📊", title: "공시 분석", desc: "최신 실적 및 중요 공시 요약" },
                { emoji: "📰", title: "뉴스 동향", desc: "최근 보도 및 핵심 이슈" },
                { emoji: "📈", title: "시장 반응", desc: "주가 추이 및 거래 동향" },
                { emoji: "🎯", title: "투자 방향성", desc: "종합 의견 및 리스크" },
              ].map((item, idx) => (
                <div key={idx} className="flex gap-2.5">
                  <span className="text-base shrink-0">{item.emoji}</span>
                  <div>
                    <p className="text-sm text-foreground mb-0.5">{item.title}</p>
                    <p className="text-xs text-muted-foreground">{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="mt-5 p-3 bg-secondary/50 rounded">
            <p className="text-xs text-muted-foreground leading-relaxed">
              AI가 생성한 분석은 참고 자료이며, 실제 투자 결정 시에는 추가적인 검증이 필요합니다.
            </p>
          </div>
        </div>

        {/* Recent AI Reports */}
        <div className="mt-10">
          <h2 className="text-sm text-muted-foreground mb-4 pb-2 border-b border-border/40 flex items-center gap-2">
            <Sparkles className="h-3.5 w-3.5" /> 최근 생성된 AI 분석
          </h2>
          <div className="space-y-2">
            {[
              { id: "1", title: "삼성전자 실적 발표 분석: HBM3E 수요 급증과 향후 전망", time: "2시간 전", views: "12,847", signal: "positive" },
              { id: "3", title: "네이버 클라우드 사업 확장, AI 서비스 투자 강화", time: "1일 전", views: "6,521", signal: "neutral" },
            ].map((post) => (
              <Link key={post.id} to={`/posts/${post.id}`} className="block group p-3 bg-secondary/30 hover:bg-secondary/50 rounded-lg transition-colors">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm group-hover:text-foreground/70 transition-colors line-clamp-1 mb-0.5">{post.title}</p>
                    <p className="text-xs text-muted-foreground">{post.time} · 조회 {post.views}</p>
                  </div>
                  {post.signal === "positive" && (
                    <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-positive-light text-positive shrink-0"><TrendingUp className="h-3 w-3" /> 매수</span>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
