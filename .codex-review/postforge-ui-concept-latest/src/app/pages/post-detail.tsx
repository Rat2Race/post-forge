import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router";
import { 
  Eye, MessageSquare, ThumbsUp, Share2, Bookmark, Edit,
  TrendingUp, TrendingDown, AlertTriangle, Calendar, ArrowLeft,
  Sparkles
} from "lucide-react";
import { Button } from "../components/ui/button";
import { CommentThread, Comment } from "../components/comment-thread";
import { AttachmentPill } from "../components/attachment-pill";
import { SourceChip } from "../components/source-chip";

export function PostDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [isLiked, setIsLiked] = useState(false);
  const [isBookmarked, setIsBookmarked] = useState(false);

  const isAuthenticated = true;
  if (!isAuthenticated) {
    return <LoginWallState />;
  }

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
    <div>
      <div className="px-5 lg:px-8 py-6">
        {/* Back nav */}
        <button onClick={() => navigate(-1)} className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary transition-colors mb-6">
          <ArrowLeft className="h-4 w-4" /> 목록으로
        </button>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
          {/* Main Content */}
          <div className="lg:col-span-8 space-y-8">
            {/* Article Header */}
            <header className="space-y-4 pb-6 border-b border-border/60">
              <div className="flex flex-wrap gap-1.5">
                {['삼성전자', '반도체', 'HBM', '실적분석'].map((tag, idx) => (
                  <span key={idx} className="text-xs px-2 py-0.5 rounded bg-secondary text-muted-foreground">
                    {tag}
                  </span>
                ))}
              </div>

              <h1 className="serif-headline text-2xl sm:text-3xl text-primary leading-snug">
                삼성전자 실적 발표 분석: HBM3E 수요 급증과 향후 전망
              </h1>

              <div className="border-l-2 border-border pl-4 py-1">
                <p className="text-sm text-muted-foreground leading-relaxed">
                  삼성전자의 최신 실적 발표를 심층 분석했습니다. HBM3E 메모리 수요 급증이 예상보다 빠르게 진행되고 있으며, AI 서버 시장 확대로 인한 수혜가 본격화될 것으로 전망됩니다.
                </p>
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2 text-sm">
                <div className="flex items-center gap-3 text-muted-foreground">
                  {isAIPost ? (
                    <span className="flex items-center gap-1.5">
                      <Sparkles className="h-3.5 w-3.5" /> AI 분석가
                    </span>
                  ) : (
                    <span>투자의달인</span>
                  )}
                  <span className="flex items-center gap-1.5">
                    <Calendar className="h-3.5 w-3.5" /> 2026.04.11
                  </span>
                </div>
                <div className="flex items-center gap-4 text-muted-foreground text-xs">
                  <span className="flex items-center gap-1"><Eye className="h-3.5 w-3.5" /> 12,847</span>
                  <span className="flex items-center gap-1"><MessageSquare className="h-3.5 w-3.5" /> 142</span>
                  <span className="flex items-center gap-1"><ThumbsUp className="h-3.5 w-3.5" /> 326</span>
                </div>
              </div>

              <div className="flex flex-wrap gap-2 pt-1">
                <span className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-positive-light text-positive"><TrendingUp className="h-3 w-3" /> 실적 개선</span>
                <span className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-positive-light text-positive"><TrendingUp className="h-3 w-3" /> 수요 증가</span>
                <span className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-secondary text-muted-foreground"><AlertTriangle className="h-3 w-3" /> 밸류에이션 부담</span>
              </div>
            </header>

            {/* Article Body */}
            <article className="space-y-8 text-foreground/90 leading-[1.8]">
              <section>
                <h2 className="serif-headline text-xl mb-3 pb-2 border-b border-border/40">공시 분석</h2>
                <p className="mb-4">
                  삼성전자는 2026년 1분기 실적 발표에서 영업이익 6.5조원을 기록하며 시장 예상치를 크게 상회했습니다. 이는 전년 동기 대비 52% 증가한 수치로, HBM3E 메모리 제품의 본격적인 양산 효과가 반영된 것으로 분석됩니다.
                </p>
                <p>
                  특히 DS(디바이스솔루션) 부문의 영업이익이 4.8조원으로 전체 실적을 견인했으며, 이는 AI 서버용 고대역폭 메모리(HBM) 수요 급증에 따른 것입니다.
                </p>
              </section>

              <section>
                <h2 className="serif-headline text-xl mb-3 pb-2 border-b border-border/40">시장 반응</h2>
                <p className="mb-4">
                  실적 발표 직후 주가는 장중 7.2% 상승하며 89,000원을 돌파했습니다. 외국인과 기관 투자자의 동반 매수세가 유입되었으며, 거래량도 평소 대비 3배 이상 증가했습니다.
                </p>
                <p>
                  주요 증권사들은 목표주가를 일제히 상향 조정했습니다. KB증권은 105,000원, 삼성증권은 110,000원으로 목표가를 제시하며 '매수' 의견을 유지했습니다.
                </p>
              </section>

              {/* Signal Panels */}
              <div className="bg-positive-light/60 border border-positive/15 rounded-lg p-5">
                <div className="flex items-start gap-3">
                  <TrendingUp className="h-5 w-5 text-positive shrink-0 mt-0.5" />
                  <div>
                    <h3 className="text-positive mb-1.5 text-sm">강세 전망</h3>
                    <p className="text-sm text-foreground/80 leading-relaxed">
                      HBM 시장 점유율 확대와 AI 서버 수요 증가로 단기적으로 추가 상승 여력이 있습니다. 2분기 실적도 개선세를 이어갈 것으로 예상됩니다.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-negative-light/60 border border-negative/15 rounded-lg p-5">
                <div className="flex items-start gap-3">
                  <TrendingDown className="h-5 w-5 text-negative shrink-0 mt-0.5" />
                  <div>
                    <h3 className="text-negative mb-1.5 text-sm">리스크 요인</h3>
                    <p className="text-sm text-foreground/80 leading-relaxed">
                      현재 PER 25배 수준으로 밸류에이션 부담이 있습니다. 또한 중국 경기 둔화와 스마트폰 시장 회복 지연은 여전히 우려 요인입니다.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-secondary/50 border border-border/60 rounded-lg p-5">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="h-5 w-5 text-muted-foreground shrink-0 mt-0.5" />
                  <div>
                    <h3 className="text-foreground mb-1.5 text-sm">투자 시 유의사항</h3>
                    <ul className="text-sm text-foreground/80 space-y-1">
                      <li className="flex items-start gap-2"><span className="text-muted-foreground mt-1 shrink-0">·</span> 단기 변동성이 높을 수 있으므로 분할 매수 전략 권장</li>
                      <li className="flex items-start gap-2"><span className="text-muted-foreground mt-1 shrink-0">·</span> 2분기 실적 발표 전까지 차익실현 압력 가능</li>
                      <li className="flex items-start gap-2"><span className="text-muted-foreground mt-1 shrink-0">·</span> 환율 변동과 미중 갈등 리스크 모니터링 필요</li>
                    </ul>
                  </div>
                </div>
              </div>
            </article>

            {/* Attachments */}
            <div className="pt-6 border-t border-border/60">
              <h3 className="text-sm mb-3">첨부 파일</h3>
              <div className="flex flex-wrap gap-2">
                <AttachmentPill id="1" fileName="삼성전자_1Q26_실적발표자료.pdf" fileType="pdf" fileSize={2450000} url="#" />
                <AttachmentPill id="2" fileName="HBM_시장분석_차트.png" fileType="png" fileSize={845000} url="#" />
              </div>
            </div>

            {/* Actions */}
            <div className="py-4 border-y border-border/60 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setIsLiked(!isLiked)}
                  className={`inline-flex items-center gap-1.5 h-8 px-3 rounded-md text-sm border transition-colors ${
                    isLiked ? "bg-primary text-primary-foreground border-primary" : "bg-card text-foreground border-border hover:bg-accent"
                  }`}
                >
                  <ThumbsUp className="h-3.5 w-3.5" />
                  {isLiked ? 327 : 326}
                </button>
                <button className="inline-flex items-center justify-center h-8 w-8 rounded-md text-sm border border-border bg-card text-foreground hover:bg-accent transition-colors">
                  <Share2 className="h-3.5 w-3.5" />
                </button>
                <button
                  onClick={() => setIsBookmarked(!isBookmarked)}
                  className={`inline-flex items-center justify-center h-8 w-8 rounded-md text-sm border transition-colors ${
                    isBookmarked ? "bg-primary text-primary-foreground border-primary" : "bg-card text-foreground border-border hover:bg-accent"
                  }`}
                >
                  <Bookmark className="h-3.5 w-3.5" />
                </button>
              </div>
              <Link to={`/posts/${id}/edit`} className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors">
                <Edit className="h-3.5 w-3.5" /> 수정
              </Link>
            </div>

            {/* Comments */}
            <div className="pt-4 pb-8">
              <h3 className="text-sm mb-6">댓글 <span className="text-muted-foreground">({mockComments.length})</span></h3>
              <CommentThread comments={mockComments} postAuthorIsAI={isAIPost} />
            </div>
          </div>

          {/* Sidebar */}
          <aside className="lg:col-span-4 space-y-5">
            <div className="lg:sticky lg:top-20 space-y-5">
              {/* Sources */}
              <div className="border border-border/60 rounded-lg p-5 bg-card">
                <h3 className="text-sm mb-4 pb-2 border-b border-border/40">참고 출처</h3>
                <div className="space-y-2.5">
                  <SourceChip type="공시" title="삼성전자 2026년 1분기 실적 공시" url="https://dart.fss.or.kr" />
                  <SourceChip type="뉴스" title="삼성전자, HBM3E 양산 본격화" url="https://example.com" />
                  <SourceChip type="보고서" title="KB증권 산업 분석 리포트" url="https://example.com" />
                </div>
              </div>

              {/* Market Stats */}
              <div className="border border-border/60 rounded-lg p-5 bg-card">
                <h3 className="text-sm mb-4 pb-2 border-b border-border/40">시장 지표</h3>
                <div className="space-y-2.5 text-sm">
                  {[
                    { label: "현재가", value: "89,200원" },
                    { label: "전일대비", value: "+6,400 (+7.74%)", color: "text-positive" },
                    { label: "시가총액", value: "532.8조원" },
                    { label: "외국인 보유율", value: "52.3%" },
                  ].map((item, idx) => (
                    <div key={idx} className="flex justify-between py-1.5 border-b border-border/30 last:border-0">
                      <span className="text-muted-foreground">{item.label}</span>
                      <span className={item.color || ""}>{item.value}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Related */}
              <div className="border border-border/60 rounded-lg p-5 bg-card">
                <h3 className="text-sm mb-4 pb-2 border-b border-border/40">관련 분석</h3>
                <div className="space-y-3">
                  <Link to="/posts/2" className="block group">
                    <p className="text-sm group-hover:text-brass transition-colors leading-snug mb-1">
                      SK하이닉스 주가 급등, 목표가 상향 조정 배경 분석
                    </p>
                    <p className="text-xs text-muted-foreground">5시간 전</p>
                  </Link>
                  <div className="border-t border-border/30" />
                  <Link to="/posts/6" className="block group">
                    <p className="text-sm group-hover:text-brass transition-colors leading-snug mb-1">
                      LG에너지솔루션 배터리 수주 확대
                    </p>
                    <p className="text-xs text-muted-foreground">2일 전</p>
                  </Link>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}

function LoginWallState() {
  const navigate = useNavigate();
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-5">
      <div className="max-w-sm w-full text-center py-12">
        <div className="w-12 h-12 bg-secondary rounded-full flex items-center justify-center mx-auto mb-5">
          <AlertTriangle className="h-6 w-6 text-muted-foreground" />
        </div>
        <h2 className="serif-headline text-xl mb-2">로그인이 필요합니다</h2>
        <p className="text-sm text-muted-foreground mb-6">
          전체 분석 내용을 확인하시려면 로그인해주세요.
        </p>
        <div className="space-y-2.5">
          <Button className="w-full" onClick={() => navigate('/login')}>로그인하기</Button>
          <Button variant="outline" className="w-full" onClick={() => navigate('/register')}>회원가입하기</Button>
        </div>
      </div>
    </div>
  );
}
