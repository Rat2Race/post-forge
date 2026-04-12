import { useState } from "react";
import { Link } from "react-router";
import { PostCard, PostCardProps } from "../components/post-card";
import { Button } from "../components/ui/button";
import { TrendingUp, TrendingDown, Clock, Sparkles } from "lucide-react";

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
  { key: "all", label: "전체 보기", icon: <Clock className="h-[17px] w-[17px]" /> },
  { key: "ai", label: "AI 분석", icon: <Sparkles className="h-[17px] w-[17px]" /> },
  { key: "positive", label: "상승 신호", icon: <TrendingUp className="h-[17px] w-[17px] text-positive" /> },
  { key: "negative", label: "하락 신호", icon: <TrendingDown className="h-[17px] w-[17px] text-negative" /> },
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
      {/* Hero Section */}
      <div className="flex flex-col items-center justify-center py-16 sm:py-20 px-4">
        <div className="inline-flex items-center justify-center gap-2 px-4 py-1 mb-3 border border-primary text-[11px] font-bold uppercase tracking-[1px]">
          <span className="w-1.5 h-1.5 rounded-full bg-primary" />
          Market Insights
        </div>
        <h1 className="serif-headline text-4xl sm:text-5xl text-center text-primary mb-3 leading-tight">
          AI 기반 한국 주식 분석 커뮤니티
        </h1>
        <p className="text-center text-muted-foreground text-lg max-w-xl mb-8 font-light leading-relaxed">
          실시간 공시 분석과 AI 생성 리포트로 데이터 기반 투자 인사이트를 명확히 제공합니다.
        </p>
        <div className="flex items-center gap-4">
          <Button className="rounded-none px-6 h-9" asChild>
            <Link to="/ai/generate">
              <Sparkles className="h-4 w-4 mr-2" />
              AI 분석 생성하기
            </Link>
          </Button>
          <Button variant="outline" className="rounded-none px-6 h-9 border-primary text-primary hover:bg-primary hover:text-primary-foreground">
            최신 분석 보기
          </Button>
        </div>
      </div>

      {/* Pill Tabs + Post List */}
      <div className="px-4 sm:px-6 lg:px-8 pb-16">
        {/* Pill tab bar */}
        <div className="inline-flex bg-[#e8e6e1] rounded-full p-1 mb-8">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-sm transition-colors ${
                activeTab === tab.key
                  ? "bg-white shadow-sm text-primary"
                  : "text-primary hover:bg-white/50"
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>

        {/* Post list */}
        <div className="divide-y divide-border">
          {filteredPosts.map((post) => (
            <PostCard key={post.id} {...post} />
          ))}
        </div>

        {/* Load More */}
        <div className="mt-12 text-center">
          <Button variant="outline" className="rounded-none border-primary text-primary px-10 hover:bg-primary hover:text-primary-foreground text-xs uppercase tracking-widest font-bold">
            더 보기
          </Button>
        </div>
      </div>
    </div>
  );
}
