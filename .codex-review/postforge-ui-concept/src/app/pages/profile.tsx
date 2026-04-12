import { useState } from "react";
import { User, Mail, Shield, Edit2, Check } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Card } from "../components/ui/card";
import { Badge } from "../components/ui/badge";
import { Separator } from "../components/ui/separator";

export function ProfilePage() {
  const [isEditingNickname, setIsEditingNickname] = useState(false);
  const [nickname, setNickname] = useState("투자의달인");
  const [tempNickname, setTempNickname] = useState(nickname);
  
  const [passwordData, setPasswordData] = useState({
    current: "",
    new: "",
    confirm: "",
  });

  // Mock user data - in real app, would come from backend
  const user = {
    userId: "investor2026",
    email: "investor@example.com",
    provider: "email", // "email", "google", "naver", "kakao"
    roles: ["user"],
    createdAt: "2026.03.15",
  };

  const isOAuthUser = user.provider !== "email";

  const handleSaveNickname = () => {
    setNickname(tempNickname);
    setIsEditingNickname(false);
  };

  const handleCancelNickname = () => {
    setTempNickname(nickname);
    setIsEditingNickname(false);
  };

  const handleChangePassword = (e: React.FormEvent) => {
    e.preventDefault();
    if (passwordData.new !== passwordData.confirm) {
      alert("새 비밀번호가 일치하지 않습니다.");
      return;
    }
    // In real app, would send to backend
    alert("비밀번호가 변경되었습니다.");
    setPasswordData({ current: "", new: "", confirm: "" });
  };

  const getProviderDisplay = (provider: string) => {
    switch (provider) {
      case "google": return "Google";
      case "naver": return "네이버";
      case "kakao": return "카카오";
      default: return "이메일";
    }
  };

  const getProviderColor = (provider: string) => {
    switch (provider) {
      case "google": return "bg-blue-100 text-blue-700 border-blue-200";
      case "naver": return "bg-green-100 text-green-700 border-green-200";
      case "kakao": return "bg-yellow-100 text-yellow-800 border-yellow-200";
      default: return "bg-secondary text-foreground border-border";
    }
  };

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="serif-headline mb-2">프로필 설정</h1>
          <p className="text-muted-foreground">
            계정 정보를 관리하고 설정을 변경할 수 있습니다
          </p>
        </div>

        <div className="space-y-6">
          {/* Account Info */}
          <Card className="p-6">
            <h2 className="mb-6 flex items-center gap-2">
              <User className="h-5 w-5" />
              계정 정보
            </h2>

            <div className="space-y-6">
              {/* User ID */}
              <div>
                <Label className="text-muted-foreground">아이디</Label>
                <div className="mt-2 flex items-center gap-3">
                  <p className="font-medium">{user.userId}</p>
                  <Badge variant="outline" className={getProviderColor(user.provider)}>
                    {getProviderDisplay(user.provider)}
                  </Badge>
                </div>
              </div>

              <Separator />

              {/* Email */}
              <div>
                <Label className="text-muted-foreground">이메일</Label>
                <div className="mt-2 flex items-center gap-2">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <p className="font-medium">{user.email}</p>
                </div>
              </div>

              <Separator />

              {/* Nickname */}
              <div>
                <Label className="text-muted-foreground">닉네임</Label>
                {isEditingNickname ? (
                  <div className="mt-2 flex gap-2">
                    <Input
                      value={tempNickname}
                      onChange={(e) => setTempNickname(e.target.value)}
                      placeholder="새 닉네임"
                    />
                    <Button size="sm" onClick={handleSaveNickname}>
                      <Check className="h-4 w-4" />
                    </Button>
                    <Button size="sm" variant="outline" onClick={handleCancelNickname}>
                      취소
                    </Button>
                  </div>
                ) : (
                  <div className="mt-2 flex items-center justify-between">
                    <p className="font-medium">{nickname}</p>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setIsEditingNickname(true)}
                    >
                      <Edit2 className="h-4 w-4 mr-1" />
                      변경
                    </Button>
                  </div>
                )}
              </div>

              <Separator />

              {/* Roles */}
              <div>
                <Label className="text-muted-foreground">권한</Label>
                <div className="mt-2 flex items-center gap-2">
                  <Shield className="h-4 w-4 text-muted-foreground" />
                  <div className="flex gap-2">
                    {user.roles.map((role, index) => (
                      <Badge key={index} variant="secondary">
                        {role === "user" ? "일반 사용자" : role}
                      </Badge>
                    ))}
                  </div>
                </div>
              </div>

              <Separator />

              {/* Join Date */}
              <div>
                <Label className="text-muted-foreground">가입일</Label>
                <p className="mt-2 font-medium">{user.createdAt}</p>
              </div>
            </div>
          </Card>

          {/* Password Change */}
          <Card className="p-6">
            <h2 className="mb-6">비밀번호 변경</h2>

            {isOAuthUser ? (
              <div className="bg-secondary/50 rounded-lg p-6 text-center">
                <p className="text-sm text-muted-foreground">
                  소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.
                  <br />
                  {getProviderDisplay(user.provider)} 계정 설정에서 비밀번호를 관리해주세요.
                </p>
              </div>
            ) : (
              <form onSubmit={handleChangePassword} className="space-y-4">
                <div>
                  <Label htmlFor="currentPassword">현재 비밀번호</Label>
                  <Input
                    id="currentPassword"
                    type="password"
                    value={passwordData.current}
                    onChange={(e) =>
                      setPasswordData({ ...passwordData, current: e.target.value })
                    }
                    required
                  />
                </div>

                <div>
                  <Label htmlFor="newPassword">새 비밀번호</Label>
                  <Input
                    id="newPassword"
                    type="password"
                    value={passwordData.new}
                    onChange={(e) =>
                      setPasswordData({ ...passwordData, new: e.target.value })
                    }
                    required
                  />
                  <p className="text-xs text-muted-foreground mt-1">
                    8자 이상, 영문, 숫자 포함
                  </p>
                </div>

                <div>
                  <Label htmlFor="confirmPassword">새 비밀번호 확인</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={passwordData.confirm}
                    onChange={(e) =>
                      setPasswordData({ ...passwordData, confirm: e.target.value })
                    }
                    required
                  />
                </div>

                <div className="flex justify-end">
                  <Button type="submit">비밀번호 변경</Button>
                </div>
              </form>
            )}
          </Card>

          {/* Activity Stats */}
          <Card className="p-6">
            <h2 className="mb-6">활동 통계</h2>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
              <div className="text-center p-4 bg-secondary/30 rounded-lg">
                <p className="text-3xl font-bold text-brass mb-1">24</p>
                <p className="text-sm text-muted-foreground">작성한 글</p>
              </div>
              <div className="text-center p-4 bg-secondary/30 rounded-lg">
                <p className="text-3xl font-bold text-positive mb-1">186</p>
                <p className="text-sm text-muted-foreground">작성한 댓글</p>
              </div>
              <div className="text-center p-4 bg-secondary/30 rounded-lg">
                <p className="text-3xl font-bold text-foreground mb-1">432</p>
                <p className="text-sm text-muted-foreground">받은 좋아요</p>
              </div>
            </div>
          </Card>

          {/* Danger Zone */}
          <Card className="p-6 border-destructive/50">
            <h2 className="mb-4 text-destructive">계정 관리</h2>
            <div className="space-y-3">
              <Button variant="outline" className="w-full sm:w-auto text-destructive border-destructive/50 hover:bg-destructive/10">
                회원 탈퇴
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
