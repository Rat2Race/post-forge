import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Sparkles, TrendingUp, FileText, CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Card } from "../components/ui/card";
import { Progress } from "../components/ui/progress";

type GenerationState = "idle" | "loading" | "success";

export function AIGeneratePage() {
  const navigate = useNavigate();
  const [state, setState] = useState<GenerationState>("idle");
  const [formData, setFormData] = useState({
    stockCode: "",
    corpName: "",
  });
  const [progress, setProgress] = useState(0);
  const [generatedPostId, setGeneratedPostId] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setState("loading");
    setProgress(0);

    // Simulate AI generation process with progress
    const stages = [
      { progress: 20, delay: 500, message: "공시 데이터 수집 중..." },
      { progress: 40, delay: 1000, message: "뉴스 분석 중..." },
      { progress: 60, delay: 1500, message: "시장 데이터 분석 중..." },
      { progress: 80, delay: 2000, message: "AI 리포트 생성 중..." },
      { progress: 100, delay: 2500, message: "완료!" },
    ];

    stages.forEach((stage, index) => {
      setTimeout(() => {
        setProgress(stage.progress);
        if (stage.progress === 100) {
          setTimeout(() => {
            setGeneratedPostId("ai-generated-1");
            setState("success");
          }, 500);
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
      <div className="min-h-screen bg-positive-light editorial-texture flex items-center justify-center px-4 py-12">
        <div className="max-w-2xl w-full p-8 sm:p-12 text-center border-2 border-positive/20 bg-background/95 backdrop-blur-sm shadow-xl">
          <div className="w-20 h-20 border-2 border-positive bg-positive-light flex items-center justify-center mx-auto mb-8 animate-in zoom-in duration-500">
            <CheckCircle2 className="h-10 w-10 text-positive" />
          </div>

          <h1 className="serif-headline text-3xl mb-4 text-primary">AI 분석 생성 완료</h1>
          <p className="text-lg text-muted-foreground mb-10 font-light">
            <span className="font-semibold text-primary">{formData.corpName || formData.stockCode}</span>에 대한 AI 분석 리포트가 성공적으로 생성되었습니다.
          </p>

          <div className="border-y border-border py-6 mb-10">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 text-sm">
              <div className="border-r border-border">
                <p className="text-muted-foreground uppercase tracking-widest text-xs font-bold mb-2">생성된 콘텐츠</p>
                <p className="font-medium text-primary">종합 분석 리포트</p>
              </div>
              <div className="border-r border-border">
                <p className="text-muted-foreground uppercase tracking-widest text-xs font-bold mb-2">분석 섹션</p>
                <p className="font-medium text-primary">8개 섹션</p>
              </div>
              <div>
                <p className="text-muted-foreground uppercase tracking-widest text-xs font-bold mb-2">참조 출처</p>
                <p className="font-medium text-primary">12개</p>
              </div>
            </div>
          </div>

          <div className="space-y-4 max-w-sm mx-auto">
            <Button size="lg" className="w-full rounded-none" asChild>
              <Link to={`/posts/${generatedPostId}`}>
                <FileText className="h-4 w-4 mr-2" />
                생성된 분석 보기
              </Link>
            </Button>
            <Button size="lg" variant="outline" className="w-full rounded-none border-primary text-primary hover:bg-primary hover:text-primary-foreground" onClick={handleReset}>
              새 분석 생성하기
            </Button>
          </div>
        </div>
      </div>
    );
  }

  if (state === "loading") {
    return (
      <div className="min-h-screen bg-background editorial-texture flex items-center justify-center px-4 py-12">
        <div className="max-w-2xl w-full p-8 sm:p-12 border border-border bg-card">
          <div className="text-center mb-10">
            <div className="w-20 h-20 bg-brass-light border border-brass/20 flex items-center justify-center mx-auto mb-6">
              <Sparkles className="h-10 w-10 text-brass animate-pulse" />
            </div>
            <h2 className="serif-headline text-3xl mb-4 text-primary">AI 분석 생성 중</h2>
            <p className="text-muted-foreground font-light">
              <span className="font-medium text-primary">{formData.corpName || formData.stockCode}</span>에 대한 종합 분석을 생성하고 있습니다
            </p>
          </div>

          <div className="space-y-8">
            <div>
              <div className="flex justify-between text-sm mb-3">
                <span className="text-primary font-medium uppercase tracking-widest text-xs">
                  {progress < 20 && "공시 데이터 수집 중..."}
                  {progress >= 20 && progress < 40 && "뉴스 분석 중..."}
                  {progress >= 40 && progress < 60 && "시장 데이터 분석 중..."}
                  {progress >= 60 && progress < 100 && "AI 리포트 생성 중..."}
                  {progress === 100 && "완료!"}
                </span>
                <span className="font-bold text-primary">{progress}%</span>
              </div>
              <Progress value={progress} className="h-1 rounded-none bg-secondary" />
            </div>

            <div className="border border-border p-6 space-y-4 text-sm bg-card/50">
              <div className="flex items-center gap-4">
                <Loader2 className={`h-4 w-4 shrink-0 ${progress >= 20 ? "text-positive" : "text-muted-foreground"} ${progress < 20 ? "animate-spin" : ""}`} />
                <span className={`${progress >= 20 ? "text-primary font-medium" : "text-muted-foreground"} tracking-wide`}>
                  공시 및 실적 데이터 수집
                </span>
              </div>
              <div className="flex items-center gap-4">
                <Loader2 className={`h-4 w-4 shrink-0 ${progress >= 40 ? "text-positive" : "text-muted-foreground"} ${progress >= 20 && progress < 40 ? "animate-spin" : ""}`} />
                <span className={`${progress >= 40 ? "text-primary font-medium" : "text-muted-foreground"} tracking-wide`}>
                  최신 뉴스 및 시장 동향 분석
                </span>
              </div>
              <div className="flex items-center gap-4">
                <Loader2 className={`h-4 w-4 shrink-0 ${progress >= 60 ? "text-positive" : "text-muted-foreground"} ${progress >= 40 && progress < 60 ? "animate-spin" : ""}`} />
                <span className={`${progress >= 60 ? "text-primary font-medium" : "text-muted-foreground"} tracking-wide`}>
                  주가 데이터 및 기술적 분석
                </span>
              </div>
              <div className="flex items-center gap-4">
                <Loader2 className={`h-4 w-4 shrink-0 ${progress >= 100 ? "text-positive" : "text-muted-foreground"} ${progress >= 60 && progress < 100 ? "animate-spin" : ""}`} />
                <span className={`${progress >= 100 ? "text-primary font-medium" : "text-muted-foreground"} tracking-wide`}>
                  종합 리포트 생성 및 게시
                </span>
              </div>
            </div>

            <p className="text-xs text-center text-muted-foreground uppercase tracking-widest font-semibold">
              예상 소요 시간: 약 30초 ~ 1분
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background editorial-texture py-12">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <div className="w-16 h-16 bg-brass-light border border-brass/20 flex items-center justify-center mx-auto mb-6">
            <Sparkles className="h-8 w-8 text-brass" />
          </div>
          <h1 className="serif-headline text-4xl sm:text-5xl text-primary mb-4 tracking-tight">AI 분석 생성</h1>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto font-light leading-relaxed">
            종목 코드를 입력하면 AI가 실시간 공시, 뉴스, 시장 데이터를 분석하여
            <br className="hidden sm:block" />종합 투자 리포트를 자동으로 생성합니다.
          </p>
        </div>

        <div className="p-8 sm:p-10 max-w-2xl mx-auto border-t-4 border-primary bg-card shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-8">
            <div className="space-y-2">
              <Label htmlFor="stockCode" className="uppercase tracking-widest text-xs font-bold text-primary">종목 코드 (Stock Code) <span className="text-negative">*</span></Label>
              <Input
                id="stockCode"
                placeholder="예: 005930 (삼성전자)"
                value={formData.stockCode}
                onChange={(e) => setFormData({ ...formData, stockCode: e.target.value })}
                required
                className="rounded-none border-border h-12"
              />
              <p className="text-xs text-muted-foreground mt-1">
                6자리 종목 코드를 정확히 입력하세요
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="corpName" className="uppercase tracking-widest text-xs font-bold text-primary">기업명 (Company Name) <span className="text-muted-foreground font-normal">(선택)</span></Label>
              <Input
                id="corpName"
                placeholder="예: 삼성전자"
                value={formData.corpName}
                onChange={(e) => setFormData({ ...formData, corpName: e.target.value })}
                className="rounded-none border-border h-12"
              />
              <p className="text-xs text-muted-foreground mt-1">
                입력하지 않으면 종목 코드로 자동 조회됩니다
              </p>
            </div>

            <Button type="submit" size="lg" className="w-full rounded-none h-14 text-base uppercase tracking-widest font-bold">
              <Sparkles className="h-5 w-5 mr-2" />
              AI 분석 생성하기 (Generate)
            </Button>
          </form>

          <div className="mt-12 pt-8 border-t border-border">
            <h3 className="mb-6 flex items-center gap-2 serif-headline text-2xl">
              <TrendingUp className="h-6 w-6 text-brass" />
              생성되는 분석 콘텐츠
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-sm">
              <div className="flex gap-4">
                <div className="w-10 h-10 bg-positive-light border border-positive/20 flex items-center justify-center shrink-0">
                  <span className="text-positive text-lg">📊</span>
                </div>
                <div>
                  <p className="font-bold text-primary mb-1 uppercase tracking-wider text-xs">공시 분석</p>
                  <p className="text-muted-foreground text-xs leading-relaxed">
                    최신 실적 및 중요 공시 내용 요약
                  </p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="w-10 h-10 bg-brass-light border border-brass/20 flex items-center justify-center shrink-0">
                  <span className="text-brass text-lg">📰</span>
                </div>
                <div>
                  <p className="font-bold text-primary mb-1 uppercase tracking-wider text-xs">뉴스 동향</p>
                  <p className="text-muted-foreground text-xs leading-relaxed">
                    최근 보도 자료 및 핵심 이슈 분석
                  </p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="w-10 h-10 bg-secondary border border-border flex items-center justify-center shrink-0">
                  <span className="text-lg">📈</span>
                </div>
                <div>
                  <p className="font-bold text-primary mb-1 uppercase tracking-wider text-xs">시장 반응</p>
                  <p className="text-muted-foreground text-xs leading-relaxed">
                    주가 추이 및 거래 동향 분석
                  </p>
                </div>
              </div>
              <div className="flex gap-4">
                <div className="w-10 h-10 bg-secondary border border-border flex items-center justify-center shrink-0">
                  <span className="text-lg">🎯</span>
                </div>
                <div>
                  <p className="font-bold text-primary mb-1 uppercase tracking-wider text-xs">투자 방향성</p>
                  <p className="text-muted-foreground text-xs leading-relaxed">
                    종합 의견 및 리스크/주의사항
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="mt-8 p-4 bg-brass-light/30 border-l-4 border-brass">
            <p className="text-sm text-foreground/80 leading-relaxed font-light">
              <strong className="font-semibold text-primary">참고:</strong> AI가 생성한 분석은 참고 자료이며, 
              실제 투자 결정 시에는 추가적인 검증이 필요합니다.
            </p>
          </div>
        </div>

        {/* Recent AI Generated Posts */}
        <div className="mt-16 max-w-2xl mx-auto">
          <h2 className="mb-6 flex items-center gap-2 serif-headline text-2xl border-b-2 border-primary pb-3">
            <Sparkles className="h-6 w-6 text-brass" />
            최근 생성된 AI 분석 (Recent Reports)
          </h2>
          <div className="space-y-4">
            <Link to="/posts/1" className="block group">
              <div className="p-5 border border-border bg-card/50 hover:bg-secondary/30 transition-colors">
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-lg text-primary mb-2 line-clamp-1 group-hover:text-brass transition-colors">
                      삼성전자 실적 발표 분석: HBM3E 수요 급증과 향후 전망
                    </h3>
                    <p className="text-xs text-muted-foreground uppercase tracking-widest font-semibold">2시간 전 <span className="mx-2 font-normal text-border">|</span> 조회 12,847</p>
                  </div>
                  <div className="shrink-0 pt-1">
                    <div className="px-3 py-1 border border-positive/30 bg-positive-light text-positive text-[10px] font-bold uppercase tracking-widest flex items-center gap-1.5">
                      <TrendingUp className="h-3 w-3" /> 매수 신호
                    </div>
                  </div>
                </div>
              </div>
            </Link>
            <Link to="/posts/3" className="block group">
              <div className="p-5 border border-border bg-card/50 hover:bg-secondary/30 transition-colors">
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-lg text-primary mb-2 line-clamp-1 group-hover:text-brass transition-colors">
                      네이버 클라우드 사업 확장, AI 서비스 투자 강화
                    </h3>
                    <p className="text-xs text-muted-foreground uppercase tracking-widest font-semibold">1일 전 <span className="mx-2 font-normal text-border">|</span> 조회 6,521</p>
                  </div>
                  <div className="shrink-0 pt-1">
                    <div className="px-3 py-1 border border-border bg-secondary text-muted-foreground text-[10px] font-bold uppercase tracking-widest flex items-center gap-1.5">
                      <span className="h-3 w-3 flex items-center justify-center font-bold">−</span> 중립
                    </div>
                  </div>
                </div>
              </div>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
