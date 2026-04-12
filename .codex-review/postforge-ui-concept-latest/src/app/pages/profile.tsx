import { useState } from "react";
import { Link } from "react-router";
import { User, Mail, Shield, Edit2, Check, Sparkles, FileText, MessageSquare } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Separator } from "../components/ui/separator";

export function ProfilePage() {
  const [isEditingNickname, setIsEditingNickname] = useState(false);
  const [nickname, setNickname] = useState("투자의달인");
  const [tempNickname, setTempNickname] = useState(nickname);
  const [passwordData, setPasswordData] = useState({ current: "", new: "", confirm: "" });

  const user = {
    userId: "investor2026",
    email: "investor@example.com",
    provider: "email",
    roles: ["user"],
    createdAt: "2026.03.15",
  };

  const isOAuthUser = user.provider !== "email";

  const getProviderDisplay = (provider: string) => {
    switch (provider) { case "google": return "Google"; case "naver": return "네이버"; case "kakao": return "카카오"; default: return "이메일"; }
  };

  const handleSaveNickname = () => { setNickname(tempNickname); setIsEditingNickname(false); };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (passwordData.new !== passwordData.confirm) { alert("새 비밀번호가 일치하지 않습니다."); return; }
    alert("비밀번호가 변경되었습니다.");
    setPasswordData({ current: "", new: "", confirm: "" });
  };

  return (
    <div className="px-5 lg:px-8 py-8 pb-20">
      <div className="max-w-2xl mx-auto">
        <div className="mb-6">
          <h1 className="serif-headline text-2xl mb-1">프로필 설정</h1>
          <p className="text-sm text-muted-foreground">계정 정보를 관리하고 설정을 변경할 수 있습니다</p>
        </div>

        <div className="space-y-5">
          {/* Account Info */}
          <div className="border border-border/60 rounded-lg p-5 bg-card">
            <h2 className="text-sm mb-5 flex items-center gap-2"><User className="h-4 w-4 text-muted-foreground" /> 계정 정보</h2>
            <div className="space-y-4">
              <div>
                <Label className="text-muted-foreground text-xs">아이디</Label>
                <div className="mt-1 flex items-center gap-2">
                  <p className="text-sm">{user.userId}</p>
                  <span className="text-xs bg-secondary px-2 py-0.5 rounded text-muted-foreground">{getProviderDisplay(user.provider)}</span>
                </div>
              </div>
              <Separator className="bg-border/40" />
              <div>
                <Label className="text-muted-foreground text-xs">이메일</Label>
                <div className="mt-1 flex items-center gap-2 text-sm"><Mail className="h-3.5 w-3.5 text-muted-foreground" /><p>{user.email}</p></div>
              </div>
              <Separator className="bg-border/40" />
              <div>
                <Label className="text-muted-foreground text-xs">닉네임</Label>
                {isEditingNickname ? (
                  <div className="mt-1 flex gap-2">
                    <Input value={tempNickname} onChange={(e) => setTempNickname(e.target.value)} placeholder="새 닉네임" className="h-8" />
                    <Button size="sm" onClick={handleSaveNickname}><Check className="h-4 w-4" /></Button>
                    <Button size="sm" variant="outline" onClick={() => { setTempNickname(nickname); setIsEditingNickname(false); }}>취소</Button>
                  </div>
                ) : (
                  <div className="mt-1 flex items-center justify-between">
                    <p className="text-sm">{nickname}</p>
                    <button onClick={() => setIsEditingNickname(true)} className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors">
                      <Edit2 className="h-3 w-3" /> 변경
                    </button>
                  </div>
                )}
              </div>
              <Separator className="bg-border/40" />
              <div>
                <Label className="text-muted-foreground text-xs">권한</Label>
                <div className="mt-1 flex items-center gap-2">
                  <Shield className="h-3.5 w-3.5 text-muted-foreground" />
                  <span className="text-xs bg-secondary px-2 py-0.5 rounded text-muted-foreground">일반 사용자</span>
                </div>
              </div>
              <Separator className="bg-border/40" />
              <div>
                <Label className="text-muted-foreground text-xs">가입일</Label>
                <p className="mt-1 text-sm">{user.createdAt}</p>
              </div>
            </div>
          </div>

          {/* Password */}
          <div className="border border-border/60 rounded-lg p-5 bg-card">
            <h2 className="text-sm mb-5">비밀번호 변경</h2>
            {isOAuthUser ? (
              <div className="bg-secondary/50 rounded-lg p-4 text-center">
                <p className="text-sm text-muted-foreground">소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.</p>
              </div>
            ) : (
              <form onSubmit={handleChangePassword} className="space-y-4">
                <div>
                  <Label htmlFor="currentPassword" className="text-xs">현재 비밀번호</Label>
                  <Input id="currentPassword" type="password" value={passwordData.current} onChange={(e) => setPasswordData({ ...passwordData, current: e.target.value })} required />
                </div>
                <div>
                  <Label htmlFor="newPassword" className="text-xs">새 비밀번호</Label>
                  <Input id="newPassword" type="password" value={passwordData.new} onChange={(e) => setPasswordData({ ...passwordData, new: e.target.value })} required />
                  <p className="text-xs text-muted-foreground mt-1">8자 이상, 영문, 숫자 포함</p>
                </div>
                <div>
                  <Label htmlFor="confirmPassword" className="text-xs">새 비밀번호 확인</Label>
                  <Input id="confirmPassword" type="password" value={passwordData.confirm} onChange={(e) => setPasswordData({ ...passwordData, confirm: e.target.value })} required />
                </div>
                <div className="flex justify-end"><Button type="submit" size="sm">비밀번호 변경</Button></div>
              </form>
            )}
          </div>

          {/* Stats */}
          <div className="border border-border/60 rounded-lg p-5 bg-card">
            <h2 className="text-sm mb-5">활동 통계</h2>
            <div className="grid grid-cols-3 gap-3">
              <div className="text-center p-3 bg-secondary/30 rounded-lg">
                <p className="text-xl text-brass mb-0.5">24</p>
                <p className="text-xs text-muted-foreground">작성한 글</p>
              </div>
              <div className="text-center p-3 bg-secondary/30 rounded-lg">
                <p className="text-xl text-positive mb-0.5">186</p>
                <p className="text-xs text-muted-foreground">작성한 댓글</p>
              </div>
              <div className="text-center p-3 bg-secondary/30 rounded-lg">
                <p className="text-xl text-foreground mb-0.5">432</p>
                <p className="text-xs text-muted-foreground">받은 좋아요</p>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="border border-border/60 rounded-lg p-5 bg-card">
            <h2 className="text-sm mb-4">빠른 이동</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              <Link to="/posts/new" className="flex items-center gap-2 p-3 bg-secondary/30 hover:bg-secondary/50 rounded-lg transition-colors text-sm">
                <FileText className="h-4 w-4 text-muted-foreground" /> 새 글 작성
              </Link>
              <Link to="/ai/generate" className="flex items-center gap-2 p-3 bg-secondary/30 hover:bg-secondary/50 rounded-lg transition-colors text-sm">
                <Sparkles className="h-4 w-4 text-brass" /> AI 분석 생성
              </Link>
              <Link to="/ai/chat" className="flex items-center gap-2 p-3 bg-secondary/30 hover:bg-secondary/50 rounded-lg transition-colors text-sm">
                <MessageSquare className="h-4 w-4 text-muted-foreground" /> AI 대화
              </Link>
            </div>
          </div>

          {/* Danger Zone */}
          <div className="border border-destructive/20 rounded-lg p-5 bg-card">
            <h2 className="text-sm text-destructive mb-3">계정 관리</h2>
            <button className="text-xs text-destructive border border-destructive/30 hover:bg-destructive/5 rounded-md px-3 py-1.5 transition-colors">
              회원 탈퇴
            </button>
          </div>
        </div>

        <div className="mt-10 pt-8 border-t border-border/50">
          <h2 className="text-sm text-muted-foreground mb-4">추천 작업</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {[
              { to: "/ai/generate", title: "새 AI 분석 생성", desc: "종목 코드를 넣고 자동 리포트를 만들어보세요." },
              { to: "/ai/chat", title: "시장 질문 이어가기", desc: "최근 이슈나 관련 종목을 AI에게 바로 물어볼 수 있습니다." },
              { to: "/posts/new", title: "직접 분석 작성", desc: "본인만의 투자 인사이트를 글로 정리해 공유하세요." },
            ].map((item) => (
              <Link key={item.title} to={item.to} className="rounded-lg border border-border/60 bg-card p-4 hover:bg-secondary/30 transition-colors">
                <p className="text-sm text-foreground mb-1.5">{item.title}</p>
                <p className="text-xs text-muted-foreground leading-relaxed">{item.desc}</p>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
