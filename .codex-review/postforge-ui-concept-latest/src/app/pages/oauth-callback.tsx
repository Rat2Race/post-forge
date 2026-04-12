import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { Loader2 } from "lucide-react";
import { Card } from "../components/ui/card";

export function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const provider = searchParams.get('provider');

  useEffect(() => {
    // Simulate OAuth processing
    const timer = setTimeout(() => {
      // In real app, this would process OAuth tokens from backend
      navigate('/');
    }, 2000);

    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center px-4">
      <Card className="max-w-md w-full p-8 text-center">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-12 w-12 animate-spin text-brass" />
          <div>
            <h2 className="serif-headline mb-2">
              {provider === 'google' && 'Google'}
              {provider === 'naver' && '네이버'}
              {provider === 'kakao' && '카카오'}
              {' '}로그인 중...
            </h2>
            <p className="text-sm text-muted-foreground">
              잠시만 기다려주세요
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
