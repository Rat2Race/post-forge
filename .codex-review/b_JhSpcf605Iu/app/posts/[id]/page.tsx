"use client"

import { useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import {
  ArrowLeft,
  Eye,
  Heart,
  MessageCircle,
  Share2,
  Bookmark,
  ExternalLink,
} from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { SourceChip } from "@/components/ui/source-chip"
import { AttachmentPill } from "@/components/ui/attachment-pill"
import { CommentThread, Comment } from "@/components/posts/comment-thread"
import { LoginWall } from "@/components/posts/login-wall"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

// Sample post data
const samplePost = {
  id: "1",
  title: "삼성전자 2026년 1분기 실적 발표 심층 분석: HBM 수요 급증의 수혜",
  summary:
    "삼성전자의 2026년 1분기 실적이 시장 예상치를 상회했습니다. 특히 메모리 반도체 부문에서 HBM3E 양산 본격화로 인한 수익성 개선이 두드러졌으며, 파운드리 사업부의 GAA 공정 수율 안정화도 긍정적입니다.",
  tags: ["삼성전자", "반도체", "HBM", "실적발표", "AI반도체"],
  author: { nickname: "AI 분석가", isAI: true },
  viewCount: 15420,
  likeCount: 342,
  commentCount: 89,
  createdAt: "2026-04-11T10:30:00Z",
  signal: "positive" as const,
  content: {
    keySummary: `삼성전자는 2026년 1분기 매출 79조 1,000억원, 영업이익 8조 2,000억원을 기록하며 컨센서스를 상회하는 실적을 발표했습니다.

특히 주목할 점은 HBM3E 양산 본격화로 메모리 사업부 영업이익률이 20%대로 회복되었고, 파운드리 GAA 3나노 공정 수율이 85%에 도달했으며, MX 사업부에서 갤럭시 S26 시리즈가 선전하고 있다는 것입니다.

반도체 슈퍼사이클의 수혜를 본격적으로 받기 시작했으며, 하반기 실적은 더욱 개선될 전망입니다.`,

    disclosureAnalysis: `삼성전자 2026년 1분기 잠정실적 발표 (2026.04.05)

매출액 79조 1,000억원으로 전년 동기 대비 18% 증가, 전분기 대비 12% 증가했습니다. 영업이익은 8조 2,000억원으로 전년 동기 대비 156% 증가, 전분기 대비 25% 증가했으며, 영업이익률은 10.4%를 기록했습니다.

반도체(DS) 사업부는 매출 32조 5,000억원으로 전년 대비 35% 증가했고, 영업이익 4조 8,000억원으로 전년 동기 대비 흑자전환에 성공했습니다. HBM3E 양산 물량 증가로 제품 믹스가 개선되었고, 범용 DRAM 및 NAND 가격 상승세가 지속되고 있습니다.

디스플레이(SDC) 사업부는 매출 8조 2,000억원으로 전년 대비 8% 증가했으며, 폴더블 패널 점유율이 확대되고 아이폰 17 시리즈 OLED 공급 계약이 체결되었습니다.

MX 사업부의 경우 갤럭시 S26 시리즈가 출시 2개월 만에 1,200만대 판매를 기록했고, 갤럭시 AI 기능에 대한 호평으로 프리미엄 시장 점유율이 확대되고 있습니다.`,

    marketReaction: `실적 발표 당일인 4월 5일 삼성전자 주가는 3.2% 상승 마감했습니다. 외국인 순매수 규모는 2,340억원으로 3개월 내 최대 수준을 기록했습니다.

국내 증권사들은 목표가를 일제히 상향 조정했습니다. 삼성증권은 95,000원에서 110,000원으로, KB증권은 90,000원에서 105,000원으로 상향했으며, 미래에셋은 100,000원 목표가를 유지하고 있습니다. 세 증권사 모두 매수 의견을 제시하고 있습니다.

외국계 증권사들의 반응도 긍정적입니다. Morgan Stanley는 "HBM leadership secured"라며 비중확대 의견을 유지했고, Goldman Sachs는 목표가를 65달러에서 72달러로 상향했습니다. JP Morgan은 "Memory upcycle confirmed"라며 비중확대 의견을 제시했습니다.`,

    newsTrends: `최근 관련 뉴스 동향을 분석한 결과 긍정적인 뉴스가 우세합니다.

4월 3일 로이터 통신은 삼성전자가 엔비디아 H200에 HBM3E 공급을 확정했다고 보도했습니다. 4월 2일 디지타임스는 삼성 GAA 3나노가 TSMC N3 대비 전력효율에서 10% 앞선다고 전했습니다. 4월 1일 한국경제는 갤럭시 AI가 국내 AI 스마트폰 시장에서 68% 점유율을 기록했다고 보도했습니다.

한편 3월 30일 전자신문에 따르면 삼성과 TSMC의 2나노 경쟁이 본격화되고 있으며, 3월 28일 블룸버그는 중국 메모리 업체 CXMT의 HBM 개발이 가속화되고 있다고 전했습니다. 3월 25일 CNBC는 미국이 대중국 반도체 장비 수출규제를 추가 검토하고 있다고 보도하여 주의가 필요합니다.`,

    directionJudgment: `종합적으로 긍정적(Bullish) 방향성을 판단합니다.

단기 전망으로 향후 1~3개월간 HBM 수요 강세가 지속되어 메모리 실적의 우상향이 예상됩니다. 2분기 영업이익 10조원 돌파 가능성이 높으며, 목표주가 상향 릴레이로 주가 모멘텀이 유지될 것으로 보입니다.

중기 전망으로 6~12개월간 AI 서버 투자 확대로 HBM 수요가 2배 이상 성장할 전망입니다. 파운드리 GAA 기반 고객사 확보 시 점유율 반등이 기대되며, 반도체 업황 회복 사이클에 본격 진입하고 있습니다.

투자 포인트로는 PBR 1.2배 수준으로 역사적 저평가 구간에 위치해 있고, HBM 기술 경쟁력 확보로 마진 개선이 지속될 전망입니다. 배당수익률 2.5%에 더해 자사주 매입으로 주주환원이 강화되고 있습니다.`,

    conflictingSignals: `투자 판단 시 주의해야 할 상충 시그널이 있습니다.

첫째, 중국 리스크입니다. CXMT 등 중국 업체의 HBM 개발이 진행 중이며, 미중 반도체 갈등 심화 시 불확실성이 증가할 수 있습니다. 다만 HBM 기술 격차는 최소 2년 이상으로 판단됩니다.

둘째, 파운드리 경쟁 심화입니다. TSMC 대비 여전히 기술 격차가 존재하며, 2나노 양산 지연 리스크가 있습니다. 또한 인텔 파운드리 서비스 확대도 경쟁 요인입니다.

셋째, 환율 변동성입니다. 원/달러 환율이 1,300원대를 유지하면 긍정적이나, 환율 급등락 시 실적 변동성이 확대될 수 있습니다.`,

    cautions: `본 분석은 공개된 정보를 바탕으로 AI가 작성한 것으로, 투자 조언이 아닙니다. 실제 투자 결정 시에는 본인의 투자 성향, 재무 상황, 리스크 허용 범위를 고려하시기 바랍니다.

과거 실적이 미래 성과를 보장하지 않으며, 주식 투자는 원금 손실의 위험이 있습니다. 본 분석에 사용된 데이터의 정확성을 보장하지 않습니다.`,
  },
  sources: [
    {
      type: "disclosure" as const,
      title: "삼성전자 2026년 1분기 잠정실적",
      url: "https://dart.fss.or.kr",
      date: "2026.04.05",
    },
    {
      type: "news" as const,
      title: "삼성전자, 엔비디아 H200에 HBM3E 공급 확정",
      url: "https://reuters.com",
      date: "2026.04.03",
    },
    {
      type: "report" as const,
      title: "삼성전자 실적 리뷰 - 삼성증권",
      url: "#",
      date: "2026.04.06",
    },
    {
      type: "news" as const,
      title: "삼성 GAA 3나노, TSMC 대비 전력효율 우수",
      url: "https://digitimes.com",
      date: "2026.04.02",
    },
  ],
  attachments: [
    { fileName: "삼성전자_1Q26_실적발표.pdf", fileType: "pdf" as const, fileSize: "2.4MB" },
    { fileName: "HBM_시장분석_차트.png", fileType: "image" as const, fileSize: "156KB" },
  ],
}

const sampleComments: Comment[] = [
  {
    id: "c1",
    content:
      "HBM 수요가 정말 폭발적이네요. SK하이닉스와의 경쟁도 치열해질 것 같습니다. 두 회사 모두 좋은 실적 기대됩니다.",
    author: { nickname: "투자초보" },
    createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
    likeCount: 12,
    replies: [
      {
        id: "c1-r1",
        content:
          "맞습니다. HBM 시장은 삼성전자와 SK하이닉스가 양분하고 있으며, 현재 SK하이닉스가 약간 앞서 있지만 삼성전자도 빠르게 따라잡고 있습니다. 두 회사 모두 AI 반도체 수요 증가의 수혜를 볼 것으로 예상됩니다.",
        author: { nickname: "AI 분석가", isAI: true },
        createdAt: new Date(Date.now() - 2.5 * 60 * 60 * 1000).toISOString(),
        likeCount: 8,
      },
    ],
  },
  {
    id: "c2",
    content:
      "파운드리 부문은 여전히 걱정이 됩니다. TSMC와의 격차가 줄어들고 있다고는 하지만 아직 갈 길이 멀어 보입니다.",
    author: { nickname: "반도체전문가" },
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
    likeCount: 7,
  },
  {
    id: "c3",
    content: "좋은 분석 감사합니다. 배당 관련 내용도 추가해주시면 좋겠어요.",
    author: { nickname: "배당투자자" },
    createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(),
    likeCount: 3,
  },
]

const isAuthenticated = true

export default function PostDetailPage() {
  const params = useParams()
  const [isLiked, setIsLiked] = useState(false)
  const [isBookmarked, setIsBookmarked] = useState(false)
  const [likeCount, setLikeCount] = useState(samplePost.likeCount)
  const [comments, setComments] = useState(sampleComments)

  if (!isAuthenticated) {
    return (
      <>
        <Header />
        <LoginWall title={samplePost.title} summary={samplePost.summary} />
      </>
    )
  }

  const handleLike = () => {
    setIsLiked(!isLiked)
    setLikeCount(isLiked ? likeCount - 1 : likeCount + 1)
  }

  const handleAddComment = (content: string, parentId?: string) => {
    const newComment: Comment = {
      id: `c${Date.now()}`,
      content,
      author: { nickname: "나" },
      createdAt: new Date().toISOString(),
      likeCount: 0,
    }

    if (parentId) {
      setComments(
        comments.map((c) =>
          c.id === parentId
            ? { ...c, replies: [...(c.replies || []), newComment] }
            : c
        )
      )
    } else {
      setComments([newComment, ...comments])
    }
  }

  const handleAskAI = (commentId: string) => {
    const aiReply: Comment = {
      id: `ai-${Date.now()}`,
      content:
        "해당 질문에 대해 추가 분석해 드리겠습니다. 현재 시장 상황과 기업의 펀더멘털을 고려했을 때, 투자 결정 시 리스크 관리가 중요합니다. 특히 반도체 산업의 사이클 특성상 단기 변동성에 유의하시기 바랍니다.",
      author: { nickname: "AI 분석가", isAI: true },
      createdAt: new Date().toISOString(),
      likeCount: 0,
    }

    setComments(
      comments.map((c) =>
        c.id === commentId
          ? { ...c, replies: [...(c.replies || []), aiReply] }
          : c
      )
    )
  }

  const signalLabel = {
    positive: "긍정적 전망",
    negative: "부정적 전망",
    neutral: "중립",
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header isAuthenticated={isAuthenticated} user={{ nickname: "사용자" }} />

      <main className="flex-1">
        {/* Article Header */}
        <header className="border-b border-border">
          <div className="mx-auto max-w-2xl px-6 py-10 md:py-14">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground mb-8"
            >
              <ArrowLeft className="h-4 w-4" />
              목록으로 돌아가기
            </Link>

            {/* Meta */}
            <div className="flex items-center gap-3 mb-6 text-sm">
              <span className={cn(
                "font-medium",
                samplePost.author.isAI ? "text-accent" : "text-muted-foreground"
              )}>
                {samplePost.author.nickname}
              </span>
              <span className="text-border">·</span>
              <time className="text-muted-foreground">
                {new Date(samplePost.createdAt).toLocaleDateString("ko-KR", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                })}
              </time>
              <span className="text-border">·</span>
              <span className={cn(
                samplePost.signal === "positive" && "text-chart-1",
                samplePost.signal === "negative" && "text-chart-2",
                samplePost.signal === "neutral" && "text-muted-foreground"
              )}>
                {signalLabel[samplePost.signal]}
              </span>
            </div>

            {/* Title */}
            <h1 className="font-serif text-2xl sm:text-3xl md:text-[2.25rem] font-bold tracking-tight leading-[1.25] mb-6 text-balance">
              {samplePost.title}
            </h1>

            {/* Tags */}
            <div className="flex items-center gap-3 text-sm text-muted-foreground">
              {samplePost.tags.map((tag, index) => (
                <span key={tag}>
                  {tag}
                  {index < samplePost.tags.length - 1 && <span className="ml-3 text-border">/</span>}
                </span>
              ))}
            </div>
          </div>
        </header>

        {/* Article Content */}
        <article className="mx-auto max-w-2xl px-6 py-12">
          {/* Summary */}
          <section className="mb-12">
            <p className="text-lg leading-[1.85] text-foreground/90">
              {samplePost.summary}
            </p>
          </section>

          {/* Key Summary */}
          <section className="article-section">
            <h2 className="article-section-title">
              핵심 요약
            </h2>
            <div className="article-section-content whitespace-pre-wrap">
              {samplePost.content.keySummary}
            </div>
          </section>

          {/* Attachments */}
          {samplePost.attachments.length > 0 && (
            <div className="flex flex-wrap gap-2 py-8 border-t border-border">
              {samplePost.attachments.map((attachment) => (
                <AttachmentPill
                  key={attachment.fileName}
                  {...attachment}
                  downloadUrl="#"
                />
              ))}
            </div>
          )}

          {/* Disclosure Analysis */}
          <section className="article-section">
            <h2 className="article-section-title">
              공시 분석
            </h2>
            <div className="article-section-content whitespace-pre-wrap">
              {samplePost.content.disclosureAnalysis}
            </div>
          </section>

          {/* Market Reaction */}
          <section className="article-section">
            <h2 className="article-section-title">
              시장 반응
            </h2>
            <div className="article-section-content whitespace-pre-wrap">
              {samplePost.content.marketReaction}
            </div>
          </section>

          {/* News Trends */}
          <section className="article-section">
            <h2 className="article-section-title">
              뉴스 동향
            </h2>
            <div className="article-section-content whitespace-pre-wrap">
              {samplePost.content.newsTrends}
            </div>
          </section>

          {/* Direction Judgment */}
          <section className="article-section">
            <h2 className="article-section-title">
              방향성 판단
            </h2>
            <div className="article-section-content whitespace-pre-wrap">
              {samplePost.content.directionJudgment}
            </div>
          </section>

          {/* Conflicting Signals */}
          <section className="article-section">
            <h2 className="article-section-title">
              상충 시그널
            </h2>
            <div className="article-section-content whitespace-pre-wrap">
              {samplePost.content.conflictingSignals}
            </div>
          </section>

          {/* Cautions */}
          <section className="article-section">
            <div className="bg-secondary/50 rounded-lg p-6">
              <h2 className="font-serif text-base font-semibold mb-4 text-muted-foreground">
                주의사항
              </h2>
              <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {samplePost.content.cautions}
              </p>
            </div>
          </section>

          {/* Sources */}
          <section className="article-section">
            <h2 className="article-section-title flex items-center gap-2">
              <ExternalLink className="h-4 w-4" />
              출처
            </h2>
            <div className="space-y-2">
              {samplePost.sources.map((source, index) => (
                <SourceChip
                  key={index}
                  type={source.type}
                  title={source.title}
                  url={source.url}
                  date={source.date}
                />
              ))}
            </div>
          </section>

          {/* Action Bar */}
          <div className="flex items-center gap-3 py-8 border-t border-b border-border">
            <Button
              variant={isLiked ? "default" : "outline"}
              size="sm"
              onClick={handleLike}
            >
              <Heart
                className={cn("h-4 w-4 mr-1.5", isLiked && "fill-current")}
              />
              {likeCount}
            </Button>
            <Button
              variant={isBookmarked ? "default" : "outline"}
              size="sm"
              onClick={() => setIsBookmarked(!isBookmarked)}
            >
              <Bookmark
                className={cn("h-4 w-4", isBookmarked && "fill-current")}
              />
            </Button>
            <Button variant="outline" size="sm">
              <Share2 className="h-4 w-4" />
            </Button>
            <div className="ml-auto flex items-center gap-4 text-sm text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <Eye className="h-4 w-4" />
                {samplePost.viewCount.toLocaleString()}
              </span>
              <span className="flex items-center gap-1.5">
                <MessageCircle className="h-4 w-4" />
                {comments.length}
              </span>
            </div>
          </div>

          {/* Comments Section */}
          <section className="py-12">
            <h2 className="font-serif text-xl font-semibold mb-8">
              댓글 {comments.length}개
            </h2>
            <CommentThread
              comments={comments}
              isAIPost={samplePost.author.isAI}
              onAddComment={handleAddComment}
              onAskAI={handleAskAI}
            />
          </section>
        </article>
      </main>

      <Footer />
    </div>
  )
}
