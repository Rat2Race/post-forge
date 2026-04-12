import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router";
import { 
  Eye, 
  MessageSquare, 
  ThumbsUp, 
  Share2, 
  Bookmark,
  Edit,
  TrendingUp,
  TrendingDown,
  AlertTriangle,
  ExternalLink,
  Calendar,
  User
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import { Card } from "../components/ui/card";
import { Separator } from "../components/ui/separator";
import { CommentThread, Comment } from "../components/comment-thread";
import { AttachmentPill } from "../components/attachment-pill";
import { SourceChip } from "../components/source-chip";
import { SignalBadge } from "../components/signal-badge";

export function PostDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [isLiked, setIsLiked] = useState(false);
  const [isBookmarked, setIsBookmarked] = useState(false);

  // Mock authentication check - redirect to login if not authenticated
  const isAuthenticated = true;
  if (!isAuthenticated) {
    return <LoginWallState />;
  }

  // Mock post data (in real app, would fetch based on id)
  const isAIPost = id === "1";
  
  const mockComments: Comment[] = [
    {
      id: "c1",
      author: "투자자A",
      content: "AI 분석 잘 봤습니다. HBM3E 수요가 생각보다 빠르게 증가하고 있네요. 추가 상승 여력이 있을까요?",
      createdAt: "1시간 전",
      likeCount: 12,
      replies: isAIPost ? [
        {
          id: "c1-r1",
          author: "AI 분석가",
          content: "네, 현재 HBM3E 수요는 AI 서버 시장 확대와 함께 빠르게 증가하고 있습니다. 주요 클라우드 업체들의 AI 인프라 투자가 계속되고 있어 단기적으로 추가 상승 여력이 있다고 판단됩니다. 다만, 밸류에이션이 높아진 만큼 단기 조정 가능성도 염두에 두시기 바랍니다.",
          createdAt: "30분 전",
          likeCount: 8,
          isAIGenerated: true,
        },
      ] : [],
    },
    {
      id: "c2",
      author: "마켓분석가",
      content: "공시 자료 기반으로 잘 정리되어 있네요. 특히 경쟁사 대비 기술 우위 부분이 인상적입니다.",
      createdAt: "3시간 전",
      likeCount: 7,
    },
    {
      id: "c3",
      author: "장기투자자",
      content: "좋은 분석 감사합니다. 하지만 단기 변동성은 여전히 높을 것 같습니다.",
      createdAt: "5시간 전",
      likeCount: 4,
    },
  ];

  return (
    <div className="min-h-screen bg-background editorial-texture">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12">
          {/* Main Content */}
          <div className="lg:col-span-8 space-y-12">
            {/* Article Header */}
            <div className="space-y-6 pb-8 border-b-2 border-primary/20">
              {/* Tags */}
              <div className="flex flex-wrap gap-3">
                {['삼성전자', '반도체', 'HBM', '실적분석'].map((tag, idx) => (
                  <span key={idx} className="text-xs font-semibold uppercase tracking-widest text-primary border-b border-primary pb-0.5">
                    {tag}
                  </span>
                ))}
              </div>

              {/* Title */}
              <h1 className="serif-headline text-4xl sm:text-5xl lg:text-6xl text-primary leading-[1.15] font-semibold tracking-tight">
                삼성전자 실적 발표 분석: <br className="hidden sm:block" />HBM3E 수요 급증과 향후 전망
              </h1>

              {/* Summary Block */}
              <div className="bg-transparent border-l-4 border-brass pl-6 py-2 my-8">
                <p className="text-lg sm:text-xl text-muted-foreground leading-relaxed font-light">
                  삼성전자의 최신 실적 발표를 심층 분석했습니다. HBM3E 메모리 수요 급증이 예상보다 빠르게 진행되고 있으며, 
                  AI 서버 시장 확대로 인한 수혜가 본격화될 것으로 전망됩니다.
                </p>
              </div>

              {/* Meta */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 py-4 border-y border-border">
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-primary uppercase tracking-widest text-sm">
                      {isAIPost ? (
                        <span className="flex items-center gap-1.5">
                          <span className="w-1.5 h-1.5 bg-brass" />
                          AI 분석가
                        </span>
                      ) : "투자의달인"}
                    </span>
                  </div>
                  <span className="text-muted-foreground/40 hidden sm:block">|</span>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-4 w-4" />
                    <span className="tracking-wide">2026.04.11 14:30</span>
                  </div>
                </div>
                
                <div className="flex items-center gap-4 text-sm text-muted-foreground font-medium">
                  <div className="flex items-center gap-1.5">
                    <Eye className="h-4 w-4" />
                    <span>12,847</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <MessageSquare className="h-4 w-4" />
                    <span>142</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <ThumbsUp className="h-4 w-4" />
                    <span>326</span>
                  </div>
                </div>
              </div>

              {/* Signals */}
              <div className="flex flex-wrap gap-4 pt-4">
                <div className="px-4 py-1.5 border border-positive/30 bg-positive-light text-positive text-xs font-bold uppercase tracking-widest flex items-center gap-2">
                  <TrendingUp className="w-4 h-4" /> 실적 개선
                </div>
                <div className="px-4 py-1.5 border border-positive/30 bg-positive-light text-positive text-xs font-bold uppercase tracking-widest flex items-center gap-2">
                  <TrendingUp className="w-4 h-4" /> 수요 증가
                </div>
                <div className="px-4 py-1.5 border border-brass/30 bg-brass-light text-brass text-xs font-bold uppercase tracking-widest flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4" /> 밸류에이션 부담
                </div>
              </div>
            </div>

            {/* Article Content */}
            <article className="prose prose-lg sm:prose-xl max-w-none text-foreground/90 font-light leading-relaxed prose-headings:font-serif prose-headings:text-primary prose-headings:font-semibold prose-h2:text-3xl prose-h2:mt-12 prose-h2:mb-6 prose-h2:border-b prose-h2:border-border prose-h2:pb-4 prose-p:mb-6">
              <h2>공시 분석</h2>
              <p>
                삼성전자는 2026년 1분기 실적 발표에서 영업이익 6.5조원을 기록하며 시장 예상치를 크게 상회했습니다. 
                이는 전년 동기 대비 52% 증가한 수치로, HBM3E 메모리 제품의 본격적인 양산 효과가 반영된 것으로 분석됩니다.
              </p>
              <p>
                특히 DS(디바이스솔루션) 부문의 영업이익이 4.8조원으로 전체 실적을 견인했으며, 
                이는 AI 서버용 고대역폭 메모리(HBM) 수요 급증에 따른 것입니다.
              </p>

              <h2>시장 반응</h2>
              <p>
                실적 발표 직후 주가는 장중 7.2% 상승하며 89,000원을 돌파했습니다. 
                외국인과 기관 투자자의 동반 매수세가 유입되었으며, 거래량도 평소 대비 3배 이상 증가했습니다.
              </p>
              <p>
                주요 증권사들은 목표주가를 일제히 상향 조정했습니다. 
                KB증권은 105,000원, 삼성증권은 110,000원으로 목표가를 제시하며 '매수' 의견을 유지했습니다.
              </p>

              <h2>뉴스 동향</h2>
              <p>
                최근 글로벌 빅테크 기업들의 AI 투자 확대 소식이 연이어 전해지면서 삼성전자에 대한 긍정적 전망이 강화되고 있습니다. 
                특히 엔비디아가 차세대 AI 칩 생산을 위해 HBM3E 물량을 대폭 늘릴 것이라는 소식이 호재로 작용했습니다.
              </p>

              <h2>방향성 판단</h2>
              <div className="bg-positive-light border border-positive/30 p-6 sm:p-8 not-prose mb-8">
                <div className="flex items-start gap-4">
                  <TrendingUp className="h-6 w-6 text-positive shrink-0 mt-1" />
                  <div>
                    <h3 className="font-bold text-positive mb-3 uppercase tracking-widest text-sm">강세 전망 (Bullish Outlook)</h3>
                    <p className="text-base text-foreground leading-relaxed">
                      HBM 시장 점유율 확대와 AI 서버 수요 증가로 단기적으로 추가 상승 여력이 있습니다. 
                      2분기 실적도 개선세를 이어갈 것으로 예상됩니다.
                    </p>
                  </div>
                </div>
              </div>

              <h2>상충 신호</h2>
              <div className="bg-negative-light border border-negative/30 p-6 sm:p-8 not-prose mb-8">
                <div className="flex items-start gap-4">
                  <TrendingDown className="h-6 w-6 text-negative shrink-0 mt-1" />
                  <div>
                    <h3 className="font-bold text-negative mb-3 uppercase tracking-widest text-sm">리스크 요인 (Risk Factors)</h3>
                    <p className="text-base text-foreground leading-relaxed">
                      현재 PER 25배 수준으로 밸류에이션 부담이 있습니다. 
                      또한 중국 경기 둔화와 스마트폰 시장 회복 지연은 여전히 우려 요인입니다.
                    </p>
                  </div>
                </div>
              </div>

              <h2>주의점</h2>
              <div className="bg-brass-light border border-brass/30 p-6 sm:p-8 not-prose">
                <div className="flex items-start gap-4">
                  <AlertTriangle className="h-6 w-6 text-brass shrink-0 mt-1" />
                  <div>
                    <h3 className="font-bold text-brass mb-3 uppercase tracking-widest text-sm">투자 시 유의사항 (Investment Notes)</h3>
                    <ul className="text-base text-foreground space-y-2">
                      <li className="flex items-start gap-2"><span className="text-brass font-bold">•</span> 단기 변동성이 높을 수 있으므로 분할 매수 전략 권장</li>
                      <li className="flex items-start gap-2"><span className="text-brass font-bold">•</span> 2분기 실적 발표 전까지 차익실현 압력 가능</li>
                      <li className="flex items-start gap-2"><span className="text-brass font-bold">•</span> 환율 변동과 미중 갈등 리스크 모니터링 필요</li>
                    </ul>
                  </div>
                </div>
              </div>
            </article>

            {/* Attachments */}
            <div className="pt-8 border-t-2 border-border">
              <h3 className="serif-headline text-2xl mb-6">첨부 파일</h3>
              <div className="flex flex-wrap gap-4">
                <AttachmentPill
                  id="1"
                  fileName="삼성전자_1Q26_실적발표자료.pdf"
                  fileType="pdf"
                  fileSize={2450000}
                  url="#"
                />
                <AttachmentPill
                  id="2"
                  fileName="HBM_시장분석_차트.png"
                  fileType="png"
                  fileSize={845000}
                  url="#"
                />
              </div>
            </div>

            {/* Actions */}
            <div className="py-6 border-y border-border flex flex-wrap items-center justify-between gap-4 bg-secondary/10">
              <div className="flex items-center gap-3">
                <Button
                  variant={isLiked ? "default" : "outline"}
                  onClick={() => setIsLiked(!isLiked)}
                  className={`rounded-none px-6 border-primary ${isLiked ? "bg-primary text-primary-foreground" : "text-primary hover:bg-primary hover:text-primary-foreground"} uppercase tracking-widest text-xs font-bold`}
                >
                  <ThumbsUp className="h-4 w-4 mr-2" />
                  좋아요 {isLiked ? 327 : 326}
                </Button>
                <Button variant="outline" className="rounded-none border-primary text-primary hover:bg-primary hover:text-primary-foreground px-4">
                  <Share2 className="h-4 w-4" />
                </Button>
                <Button
                  variant={isBookmarked ? "default" : "outline"}
                  onClick={() => setIsBookmarked(!isBookmarked)}
                  className="rounded-none border-primary text-primary hover:bg-primary hover:text-primary-foreground px-4"
                >
                  <Bookmark className="h-4 w-4" />
                </Button>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="ghost" asChild className="rounded-none uppercase tracking-widest text-xs font-bold">
                  <Link to={`/posts/${id}/edit`}>
                    <Edit className="h-4 w-4 mr-2" />
                    수정 (Edit)
                  </Link>
                </Button>
              </div>
            </div>

            {/* Comments */}
            <div className="pt-8">
              <h3 className="serif-headline text-3xl mb-8 flex items-center gap-3">
                독자 의견 <span className="text-muted-foreground text-xl font-sans font-normal">({mockComments.length})</span>
              </h3>
              <CommentThread 
                comments={mockComments} 
                postAuthorIsAI={isAIPost}
              />
            </div>
          </div>

          {/* Sidebar */}
          <div className="lg:col-span-4 space-y-8">
            {/* Sticky Container */}
            <div className="lg:sticky lg:top-24 space-y-8">
              {/* Sources */}
              <div className="border border-border p-6 bg-card/50">
                <h3 className="serif-headline text-xl border-b-2 border-primary pb-3 mb-6">참고 출처 (Sources)</h3>
                <div className="space-y-4">
                  <SourceChip
                    type="공시"
                    title="삼성전자 2026년 1분기 실적 공시"
                    url="https://dart.fss.or.kr"
                  />
                  <SourceChip
                    type="뉴스"
                    title="삼성전자, HBM3E 양산 본격화"
                    url="https://example.com"
                  />
                  <SourceChip
                    type="보고서"
                    title="KB증권 산업 분석 리포트"
                    url="https://example.com"
                  />
                </div>
              </div>

              {/* Quick Stats */}
              <div className="border border-border p-6 bg-card/50">
                <h3 className="serif-headline text-xl border-b-2 border-primary pb-3 mb-6">시장 지표 (Market Stats)</h3>
                <div className="space-y-4 text-sm">
                  <div className="flex justify-between items-center py-2 border-b border-border/50">
                    <span className="text-muted-foreground font-medium uppercase tracking-wider text-xs">현재가</span>
                    <span className="font-bold text-base">89,200원</span>
                  </div>
                  <div className="flex justify-between items-center py-2 border-b border-border/50">
                    <span className="text-muted-foreground font-medium uppercase tracking-wider text-xs">전일대비</span>
                    <span className="font-bold text-base text-positive">+6,400 (+7.74%)</span>
                  </div>
                  <div className="flex justify-between items-center py-2 border-b border-border/50">
                    <span className="text-muted-foreground font-medium uppercase tracking-wider text-xs">시가총액</span>
                    <span className="font-bold text-base">532.8조원</span>
                  </div>
                  <div className="flex justify-between items-center py-2">
                    <span className="text-muted-foreground font-medium uppercase tracking-wider text-xs">외국인 보유율</span>
                    <span className="font-bold text-base">52.3%</span>
                  </div>
                </div>
              </div>

              {/* Related Posts */}
              <div className="border border-border p-6 bg-card/50">
                <h3 className="serif-headline text-xl border-b-2 border-primary pb-3 mb-6">관련 분석 (Related)</h3>
                <div className="space-y-5">
                  <Link 
                    to="/posts/2" 
                    className="block group"
                  >
                    <p className="font-medium text-primary group-hover:text-brass transition-colors leading-snug mb-2">
                      SK하이닉스 주가 급등, 목표가 상향 조정 배경 분석
                    </p>
                    <p className="text-xs text-muted-foreground uppercase tracking-widest font-semibold">5시간 전</p>
                  </Link>
                  <Separator className="bg-border/60" />
                  <Link 
                    to="/posts/6" 
                    className="block group"
                  >
                    <p className="font-medium text-primary group-hover:text-brass transition-colors leading-snug mb-2">
                      LG에너지솔루션 배터리 수주 확대
                    </p>
                    <p className="text-xs text-muted-foreground uppercase tracking-widest font-semibold">2일 전</p>
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

// Login Wall Component for unauthenticated users
function LoginWallState() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <Card className="max-w-md w-full p-8 text-center">
        <div className="w-16 h-16 bg-brass-light rounded-full flex items-center justify-center mx-auto mb-6">
          <AlertTriangle className="h-8 w-8 text-brass" />
        </div>
        <h2 className="serif-headline mb-3">로그인이 필요합니다</h2>
        <p className="text-muted-foreground mb-6">
          전체 분석 내용을 확인하시려면 로그인해주세요.
        </p>
        <div className="space-y-3">
          <Button className="w-full" onClick={() => navigate('/login')}>
            로그인하기
          </Button>
          <Button variant="outline" className="w-full" onClick={() => navigate('/register')}>
            회원가입하기
          </Button>
        </div>
      </Card>
    </div>
  );
}
