import { Link } from "react-router";
import { Home, ArrowLeft, Sparkles, FileText, MessageSquare } from "lucide-react";
import { Button } from "../components/ui/button";

export function NotFoundPage() {
  return (
    <div className="px-5 lg:px-8 py-16">
      <div className="max-w-sm mx-auto text-center">
        <h1 className="serif-headline mb-4" style={{ fontSize: '3.5rem', lineHeight: 1, color: 'var(--muted-foreground)', opacity: 0.3 }}>
          404
        </h1>
        <h2 className="serif-headline text-xl mb-2">페이지를 찾을 수 없습니다</h2>
        <p className="text-sm text-muted-foreground mb-8">
          요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.
        </p>

        <div className="space-y-2.5 max-w-xs mx-auto">
          <Button className="w-full" asChild>
            <Link to="/">
              <Home className="h-4 w-4 mr-2" />
              홈으로 돌아가기
            </Link>
          </Button>
          <Button variant="outline" className="w-full" onClick={() => window.history.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            이전 페이지로
          </Button>
        </div>
      </div>

      {/* Supporting content */}
      <div className="max-w-md mx-auto mt-16 pt-8 border-t border-border/40">
        <p className="text-xs text-muted-foreground text-center mb-5">이 페이지들은 어떠세요?</p>
        <div className="grid grid-cols-3 gap-4 text-center">
          <Link to="/ai/generate" className="group">
            <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center mx-auto mb-2 text-muted-foreground group-hover:text-brass transition-colors">
              <Sparkles className="h-4 w-4" />
            </div>
            <p className="text-xs text-foreground group-hover:text-brass transition-colors">AI 분석 생성</p>
          </Link>
          <Link to="/ai/chat" className="group">
            <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center mx-auto mb-2 text-muted-foreground group-hover:text-brass transition-colors">
              <MessageSquare className="h-4 w-4" />
            </div>
            <p className="text-xs text-foreground group-hover:text-brass transition-colors">AI 대화</p>
          </Link>
          <Link to="/posts/1" className="group">
            <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center mx-auto mb-2 text-muted-foreground group-hover:text-brass transition-colors">
              <FileText className="h-4 w-4" />
            </div>
            <p className="text-xs text-foreground group-hover:text-brass transition-colors">최신 분석</p>
          </Link>
        </div>
      </div>
    </div>
  );
}
