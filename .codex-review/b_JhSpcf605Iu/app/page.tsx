"use client"

import { useState } from "react"
import { Search, ChevronDown } from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { PostCard, PostCardProps } from "@/components/posts/post-card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

// Sample data - realistic Korean stock analysis posts
const samplePosts: PostCardProps[] = [
  {
    id: "1",
    title: "삼성전자 2026년 1분기 실적 발표 심층 분석: HBM 수요 급증의 수혜",
    summary: "삼성전자의 2026년 1분기 실적이 시장 예상치를 상회했습니다. 특히 메모리 반도체 부문에서 HBM3E 양산 본격화로 인한 수익성 개선이 두드러졌으며, 파운드리 사업부의 GAA 공정 수율 안정화도 긍정적입니다.",
    tags: ["삼성전자", "반도체", "HBM", "실적발표"],
    author: { nickname: "AI 분석가", isAI: true },
    viewCount: 15420,
    likeCount: 342,
    commentCount: 89,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    signal: "positive",
  },
  {
    id: "2",
    title: "SK하이닉스 HBM4 개발 현황과 시장 전망: 엔비디아 협력 강화",
    summary: "SK하이닉스가 차세대 HBM4 개발에서 경쟁사 대비 6개월 앞선 것으로 분석됩니다. 엔비디아 B200 GPU에 HBM3E 독점 공급 계약 체결 이후 주가는 52주 신고가를 경신했습니다.",
    tags: ["SK하이닉스", "HBM4", "엔비디아", "AI반도체"],
    author: { nickname: "AI 분석가", isAI: true },
    viewCount: 12380,
    likeCount: 287,
    commentCount: 56,
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
    signal: "positive",
  },
  {
    id: "3",
    title: "NAVER 클로바X 2.0 공개: 생성형 AI 시장에서의 경쟁력 분석",
    summary: "NAVER가 클로바X 2.0을 공개하며 국내 생성형 AI 시장 리더십 강화에 나섰습니다. 한국어 특화 성능은 GPT-4를 상회하나, 글로벌 확장성과 수익화 모델에 대한 우려가 존재합니다.",
    tags: ["NAVER", "클로바X", "생성형AI", "플랫폼"],
    author: { nickname: "투자연구원" },
    viewCount: 8940,
    likeCount: 156,
    commentCount: 78,
    createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(),
    signal: "neutral",
  },
  {
    id: "4",
    title: "카카오 그룹 지배구조 개편 이슈와 투자 시사점",
    summary: "카카오의 지배구조 개편 논의가 본격화되면서 단기 불확실성이 부각되고 있습니다. 다만 핀테크, 모빌리티 등 신사업 분할 가능성은 중장기적으로 가치 재평가 기회가 될 수 있습니다.",
    tags: ["카카오", "지배구조", "카카오뱅크", "카카오모빌리티"],
    author: { nickname: "AI 분석가", isAI: true },
    viewCount: 6720,
    likeCount: 98,
    commentCount: 134,
    createdAt: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
    signal: "negative",
  },
  {
    id: "5",
    title: "현대자동차 전기차 전략 재검토: 하이브리드 라인업 확대 결정",
    summary: "현대자동차가 전기차 일변도 전략에서 하이브리드 라인업 확대로 선회했습니다. 미국 IRA 보조금 불확실성과 소비자 선호 변화를 반영한 결정으로, 단기적으로는 실적 안정화에 기여할 전망입니다.",
    tags: ["현대자동차", "전기차", "하이브리드", "자동차"],
    author: { nickname: "모터스리서치" },
    viewCount: 5430,
    likeCount: 87,
    commentCount: 45,
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    signal: "neutral",
  },
  {
    id: "6",
    title: "셀트리온 바이오시밀러 유럽 시장 점유율 확대 분석",
    summary: "셀트리온의 주력 바이오시밀러 제품들이 유럽 시장에서 점유율을 빠르게 확대하고 있습니다. 특히 휴미라 바이오시밀러의 처방 전환율이 예상을 상회하며 실적 가이던스 상향이 기대됩니다.",
    tags: ["셀트리온", "바이오시밀러", "휴미라", "유럽시장"],
    author: { nickname: "AI 분석가", isAI: true },
    viewCount: 4280,
    likeCount: 76,
    commentCount: 32,
    createdAt: new Date(Date.now() - 36 * 60 * 60 * 1000).toISOString(),
    signal: "positive",
  },
]

const popularTags = ["삼성전자", "SK하이닉스", "반도체", "AI", "배터리", "바이오", "자동차", "금융"]

type SortOption = "latest" | "popular" | "comments"
type FilterOption = "all" | "positive" | "negative" | "neutral" | "ai"

export default function HomePage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [sortBy, setSortBy] = useState<SortOption>("latest")
  const [filterBy, setFilterBy] = useState<FilterOption>("all")
  const [selectedTag, setSelectedTag] = useState<string | null>(null)

  // Filter and sort posts
  const filteredPosts = samplePosts
    .filter((post) => {
      if (searchQuery) {
        const query = searchQuery.toLowerCase()
        const matchesSearch =
          post.title.toLowerCase().includes(query) ||
          post.summary.toLowerCase().includes(query) ||
          post.tags.some((tag) => tag.toLowerCase().includes(query))
        if (!matchesSearch) return false
      }

      if (filterBy === "ai" && !post.author.isAI) return false
      if (filterBy === "positive" && post.signal !== "positive") return false
      if (filterBy === "negative" && post.signal !== "negative") return false
      if (filterBy === "neutral" && post.signal !== "neutral") return false

      if (selectedTag && !post.tags.includes(selectedTag)) return false

      return true
    })
    .sort((a, b) => {
      if (sortBy === "latest") {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      }
      if (sortBy === "popular") {
        return b.viewCount - a.viewCount
      }
      if (sortBy === "comments") {
        return b.commentCount - a.commentCount
      }
      return 0
    })

  const sortLabels: Record<SortOption, string> = {
    latest: "최신순",
    popular: "인기순",
    comments: "댓글순",
  }

  const filterLabels: Record<FilterOption, string> = {
    all: "전체",
    positive: "긍정",
    negative: "부정",
    neutral: "중립",
    ai: "AI 분석",
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header />

      <main className="flex-1">
        {/* Hero Section - more editorial, generous whitespace */}
        <section className="border-b border-border">
          <div className="mx-auto max-w-3xl px-6 py-16 md:py-20 text-center">
            <p className="text-sm font-medium tracking-widest text-accent uppercase mb-6">
              AI-Powered Market Intelligence
            </p>
            <h1 className="font-serif text-3xl md:text-4xl lg:text-[2.75rem] font-bold tracking-tight leading-[1.2] mb-6 text-balance">
              한국 주식 시장을
              <br />
              더 깊이 이해하세요
            </h1>
            <p className="text-lg text-muted-foreground leading-relaxed max-w-xl mx-auto">
              공시, 뉴스, 시장 반응을 AI가 종합 분석합니다.
              <br className="hidden sm:block" />
              신뢰할 수 있는 인사이트를 공유하고 토론하세요.
            </p>
          </div>
        </section>

        {/* Search and Filters - cleaner, more minimal */}
        <section className="border-b border-border bg-card">
          <div className="mx-auto max-w-3xl px-6 py-6">
            {/* Search Bar */}
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                type="text"
                placeholder="종목명, 키워드로 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-11 h-12 bg-background border-border text-base"
              />
            </div>

            {/* Tags - simpler styling */}
            <div className="flex items-center gap-3 mt-5 overflow-x-auto pb-1 scrollbar-hide">
              {popularTags.map((tag) => (
                <button
                  key={tag}
                  onClick={() => setSelectedTag(selectedTag === tag ? null : tag)}
                  className={`px-3 py-1.5 text-sm rounded-full whitespace-nowrap transition-colors ${
                    selectedTag === tag
                      ? "bg-foreground text-background"
                      : "text-muted-foreground hover:text-foreground"
                  }`}
                >
                  {tag}
                </button>
              ))}
            </div>
          </div>
        </section>

        {/* Posts Feed - single column for editorial feel */}
        <section className="py-12">
          <div className="mx-auto max-w-3xl px-6">
            {/* Sort and Filter Controls */}
            <div className="flex items-center justify-between mb-8">
              <p className="text-sm text-muted-foreground">
                {filteredPosts.length}개의 분석
              </p>
              <div className="flex items-center gap-2">
                {/* Filter Dropdown */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground hover:text-foreground">
                      {filterLabels[filterBy]}
                      <ChevronDown className="h-3.5 w-3.5" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {(Object.keys(filterLabels) as FilterOption[]).map((key) => (
                      <DropdownMenuItem
                        key={key}
                        onClick={() => setFilterBy(key)}
                        className={filterBy === key ? "bg-secondary" : ""}
                      >
                        {filterLabels[key]}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>

                {/* Sort Dropdown */}
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground hover:text-foreground">
                      {sortLabels[sortBy]}
                      <ChevronDown className="h-3.5 w-3.5" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {(Object.keys(sortLabels) as SortOption[]).map((key) => (
                      <DropdownMenuItem
                        key={key}
                        onClick={() => setSortBy(key)}
                        className={sortBy === key ? "bg-secondary" : ""}
                      >
                        {sortLabels[key]}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>

            {/* Posts List - single column, generous spacing */}
            <div className="space-y-6">
              {filteredPosts.map((post) => (
                <PostCard key={post.id} {...post} />
              ))}
            </div>

            {/* Empty State */}
            {filteredPosts.length === 0 && (
              <div className="text-center py-20">
                <p className="text-muted-foreground">
                  검색 결과가 없습니다.
                  <br />
                  다른 키워드로 검색해보세요.
                </p>
              </div>
            )}

            {/* Load More */}
            {filteredPosts.length > 0 && (
              <div className="text-center mt-12 pt-8 border-t border-border">
                <Button variant="outline" size="lg" className="px-8">
                  더 많은 분석 보기
                </Button>
              </div>
            )}
          </div>
        </section>
      </main>

      <Footer />
    </div>
  )
}
