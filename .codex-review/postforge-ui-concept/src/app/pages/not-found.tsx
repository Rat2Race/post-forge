import { Link } from "react-router";
import { Home, Search, ArrowLeft } from "lucide-react";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";

export function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-secondary via-background to-accent">
      <Card className="max-w-md w-full p-8 text-center">
        <div className="mb-6">
          <h1 className="serif-headline mb-4" style={{ fontSize: '4rem', lineHeight: 1 }}>
            404
          </h1>
          <h2 className="serif-headline mb-2">페이지를 찾을 수 없습니다</h2>
          <p className="text-muted-foreground">
            요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.
          </p>
        </div>

        <div className="space-y-3">
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

        <div className="mt-8 pt-6 border-t border-border">
          <p className="text-sm text-muted-foreground mb-3">추천 페이지</p>
          <div className="flex flex-col gap-2 text-sm">
            <Link to="/ai/generate" className="text-brass hover:underline">
              AI 분석 생성
            </Link>
            <Link to="/ai/chat" className="text-brass hover:underline">
              AI 대화
            </Link>
            <Link to="/posts/1" className="text-brass hover:underline">
              최신 분석 보기
            </Link>
          </div>
        </div>
      </Card>
    </div>
  );
}
