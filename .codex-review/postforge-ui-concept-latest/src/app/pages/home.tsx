import { useState } from "react";
import { Link } from "react-router";
import { PostCard, PostCardProps } from "../components/post-card";
import { Button } from "../components/ui/button";
import { TrendingUp, TrendingDown, LayoutGrid, Sparkles, ArrowRight, BarChart3 } from "lucide-react";

const MOCK_POSTS: PostCardProps[] = [
  {
    id: "1",
    title: "삼성전자 실적 발표 분석: HBM3E 수요 급증과 전망",
    summary: "삼성전자 최신 실적을 심층 분석했습니다. HBM3E 메모리 수요가 예상보다 빠르게 증가하며 AI 서버 시장 확대로 수혜가 기대됩니다.",
    tags: ["삼성전자", "반도체", "HBM", "실적 분석"],
    author: "AI 분석가",
    views: 12847,
    commentCount: 142,
    likeCount: 326,
    createdAt: "2시간 전",
    isAIGenerated: true,
    signal: "positive",
  },
  {
    id: "2",
    title: "SK하이닉스 주가 급등, 목표가 상향 조정 배경 분석",
    summary: "주요 증권사들의 목표가 상향 조정이 이어지고 있습니다. AI 반도체 수요 확대와 HBM 시장 점유율 상승이 주요 원인으로 분석됩니다.",
    tags: ["SK하이닉스", "반도체", "목표가"],
    author: "투자의달인",
    views: 8234,
    commentCount: 89,
    likeCount: 201,
    createdAt: "5시간 전",
    signal: "positive",
  },
  {
    id: "3",
    title: "네이버 클라우드 사업 확장, AI 서비스 투자 강화",
    summary: "네이버가 클라우드 사업 확장과 함께 AI 서비스 투자를 대폭 강화한다고 발표했습니다. 하이퍼클로바X 기반 B2B 서비스 확대가 예상됩니다.",
    tags: ["NAVER", "클라우드", "AI", "하이퍼클로바"],
    author: "AI 분석가",
    views: 6521,
    commentCount: 67,
    likeCount: 154,
    createdAt: "1일 전",
    isAIGenerated: true,
    signal: "neutral",
  },
  {
    id: "4",
    title: "카카오 사업구조 개편 분석 및 투자 포인트",
    summary: "카카오의 사업구조 개편이 본격화되고 있습니다. 핵심 사업 집중과 비핵심 사업 정리를 통한 수익성 개선이 기대됩니다.",
    tags: ["카카오", "구조조정", "투자전략"],
    author: "마켓인사이트",
    views: 5432,
    commentCount: 45,
    likeCount: 112,
    createdAt: "1일 전",
    signal: "neutral",
  },
  {
    id: "5",
    title: "현대차 전기차 판매 부진, 재고 증가 우려",
    summary: "현대차의 전기차 판매가 예상을 밑돌면서 재고 증가 우려가 커지고 있습니다. 보조금 축소와 경쟁 심화가 주요 원인으로 지목됩니다.",
    tags: ["현대차", "전기차", "실적"],
    author: "AI 분석가",
    views: 9871,
    commentCount: 134,
    likeCount: 78,
    createdAt: "2일 전",
    isAIGenerated: true,
    signal: "negative",
  },
  {
    id: "6",
    title: "LG에너지솔루션 배터리 수주 확대, 북미 시장 성장세",
    summary: "LG에너지솔루션이 북미 지역에서 대규모 배터리 수주에 성공했습니다. IRA 보조금 혜택과 함께 향후 실적 개선이 기대됩니다.",
    tags: ["LG에너지솔루션", "배터리", "북미시장"],
    author: "배터리워치",
    views: 4321,
    commentCount: 56,
    likeCount: 143,
    createdAt: "2일 전",
    signal: "positive",
  },
];

type TabKey = "all" | "ai" | "positive" | "negative";

const TABS: { key: TabKey; label: string; icon: React.ReactNode }[] = [
  { key: "all", label: "전체", icon: <LayoutGrid className="h-3.5 w-3.5" /> },
  { key: "ai", label: "AI 분석", icon: <Sparkles className="h-3.5 w-3.5" /> },
  { key: "positive", label: "상승", icon: <TrendingUp className="h-3.5 w-3.5" /> },
  { key: "negative", label: "하락", icon: <TrendingDown className="h-3.5 w-3.5" /> },
];

const MARKET_SIGNALS = [
  { name: "삼성전자", code: "005930", change: "+3.2%", signal: "positive" as const },
  { name: "SK하이닉스", code: "000660", change: "+5.1%", signal: "positive" as const },
  { name: "현대차", code: "005380", change: "-1.8%", signal: "negative" as const },
  { name: "NAVER", code: "035420", change: "+0.4%", signal: "neutral" as const },
  { name: "카카오", code: "035720", change: "-0.6%", signal: "negative" as const },
];

export function HomePage() {
  const [activeTab, setActiveTab] = useState<TabKey>("all");

  const filteredPosts = MOCK_POSTS.filter((post) => {
    if (activeTab === "ai") return post.isAIGenerated;
    if (activeTab === "positive") return post.signal === "positive";
    if (activeTab === "negative") return post.signal === "negative";
    return true;
  });

  return (
    <div>
      {/* Hero */}
      <div className="px-5 lg:px-8 pt-10 pb-8">
        <div className="max-w-2xl">
          <h1 className="serif-headline text-2xl sm:text-3xl text-primary mb-3 leading-snug">
            AI 기반 한국 주식 분석
          </h1>
          <p className="text-sm text-muted-foreground mb-5 leading-relaxed max-w-lg">
            실시간 공시 분석과 AI 생성 리포트로 데이터 기반 투자 인사이트를 제공합니다.
          </p>
          <div className="flex items-center gap-2.5">
            <Link
              to="/ai/generate"
              className="inline-flex items-center gap-1.5 h-9 px-4 rounded-md text-sm bg-brass text-white hover:bg-brass/90 transition-colors"
            >
              <Sparkles className="h-3.5 w-3.5" />
              AI 분석 생성
            </Link>
            <Link
              to="/ai/chat"
              className="inline-flex items-center gap-1.5 h-9 px-4 rounded-md text-sm border border-border bg-card text-foreground hover:bg-accent transition-colors"
            >
              AI 대화
            </Link>
          </div>
        </div>
      </div>

      {/* Market Pulse */}
      <div className="px-5 lg:px-8 py-2.5 bg-secondary/60 border-y border-border/50">
        <div className="flex items-center gap-5 overflow-x-auto text-sm">
          <span className="text-muted-foreground shrink-0 text-xs flex items-center gap-1">
            <BarChart3 className="h-3 w-3" /> 시장
          </span>
          {MARKET_SIGNALS.map((item) => (
            <div key={item.code} className="flex items-center gap-1.5 shrink-0">
              <span className="text-foreground/80 text-sm">{item.name}</span>
              <span className={`text-sm ${
                item.signal === "positive" ? "text-positive" : item.signal === "negative" ? "text-negative" : "text-muted-foreground"
              }`}>
                {item.change}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Tabs + Feed */}
      <div className="px-5 lg:px-8 py-6">
        {/* Tabs — fixed width to prevent layout shift */}
        <div className="flex items-center gap-1 bg-secondary rounded-lg p-1 w-fit mb-6">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm transition-colors ${
                activeTab === tab.key
                  ? "bg-card text-primary shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>

        {/* Post list */}
        <div className="divide-y divide-border/50">
          {filteredPosts.map((post) => (
            <PostCard key={post.id} {...post} />
          ))}
        </div>

        {/* Load More */}
        <div className="mt-8 text-center">
          <Button variant="outline" size="sm" className="px-8">
            더 보기
          </Button>
        </div>
      </div>

      {/* Bottom content — Featured & AI CTA */}
      <div className="px-5 lg:px-8 pb-12 pt-4">
        <div className="border-t border-border/50 pt-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Featured Analysis */}
            <div>
              <h3 className="text-sm text-muted-foreground mb-4">주간 인기 분석</h3>
              <div className="space-y-3">
                {MOCK_POSTS.slice(0, 3).map((post, idx) => (
                  <Link key={post.id} to={`/posts/${post.id}`} className="flex items-start gap-3 group">
                    <span className="text-sm text-muted-foreground/50 tabular-nums shrink-0 mt-0.5">
                      {String(idx + 1).padStart(2, "0")}
                    </span>
                    <div className="min-w-0">
                      <p className="text-sm text-foreground group-hover:text-muted-foreground transition-colors line-clamp-1">{post.title}</p>
                      <p className="text-xs text-muted-foreground mt-0.5">조회 {post.views.toLocaleString()}</p>
                    </div>
                  </Link>
                ))}
              </div>
            </div>

            {/* AI CTA */}
            <div className="bg-secondary/50 rounded-lg p-5">
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-lg bg-brass/10 flex items-center justify-center shrink-0">
                  <Sparkles className="h-4 w-4 text-brass" />
                </div>
                <div>
                  <h3 className="text-sm text-foreground mb-1">AI로 분석 리포트 생성</h3>
                  <p className="text-xs text-muted-foreground leading-relaxed mb-3">
                    종목 코드만 입력하면 공시, 뉴스, 시장 데이터를 종합 분석한 리포트를 자동 생성합니다.
                  </p>
                  <Link
                    to="/ai/generate"
                    className="inline-flex items-center gap-1 text-xs text-brass hover:text-brass/80 transition-colors"
                  >
                    시작하기 <ArrowRight className="h-3 w-3" />
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
