"use client"

import { useState } from "react"
import Link from "next/link"
import { ArrowLeft, User, Shield, Key, Loader2, CheckCircle2, AlertCircle } from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"

// Sample user data
const sampleUser = {
  userId: "investor123",
  nickname: "투자연구원",
  email: "investor@example.com",
  provider: "local", // 'local' | 'google' | 'naver' | 'kakao'
  roles: ["USER"],
  createdAt: "2024-01-15",
}

type Provider = "local" | "google" | "naver" | "kakao"

const providerLabels: Record<Provider, string> = {
  local: "이메일",
  google: "Google",
  naver: "Naver",
  kakao: "Kakao",
}

export default function ProfilePage() {
  const [user, setUser] = useState(sampleUser)
  const [nickname, setNickname] = useState(user.nickname)
  const [isEditingNickname, setIsEditingNickname] = useState(false)
  const [isSavingNickname, setIsSavingNickname] = useState(false)
  const [nicknameSuccess, setNicknameSuccess] = useState(false)

  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [passwordError, setPasswordError] = useState("")
  const [passwordSuccess, setPasswordSuccess] = useState(false)

  const isOAuthUser = user.provider !== "local"

  const handleSaveNickname = async () => {
    if (!nickname.trim() || nickname === user.nickname) {
      setIsEditingNickname(false)
      return
    }

    setIsSavingNickname(true)
    await new Promise((r) => setTimeout(r, 1000))
    setUser({ ...user, nickname })
    setIsSavingNickname(false)
    setIsEditingNickname(false)
    setNicknameSuccess(true)
    setTimeout(() => setNicknameSuccess(false), 3000)
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    setPasswordError("")

    if (newPassword !== confirmPassword) {
      setPasswordError("새 비밀번호가 일치하지 않습니다.")
      return
    }

    if (newPassword.length < 8) {
      setPasswordError("비밀번호는 8자 이상이어야 합니다.")
      return
    }

    setIsChangingPassword(true)
    await new Promise((r) => setTimeout(r, 1500))
    setIsChangingPassword(false)
    setCurrentPassword("")
    setNewPassword("")
    setConfirmPassword("")
    setPasswordSuccess(true)
    setTimeout(() => setPasswordSuccess(false), 3000)
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header isAuthenticated user={{ nickname: user.nickname }} />

      <main className="flex-1">
        <div className="mx-auto max-w-2xl px-4 sm:px-6 lg:px-8 py-8">
          {/* Header */}
          <div className="mb-8">
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-4"
            >
              <ArrowLeft className="h-4 w-4" />
              홈으로
            </Link>
            <h1 className="font-serif text-2xl sm:text-3xl font-bold">
              프로필 설정
            </h1>
            <p className="text-muted-foreground mt-2">
              계정 정보를 관리하세요.
            </p>
          </div>

          <div className="space-y-6">
            {/* Account Info */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <User className="h-5 w-5" />
                  계정 정보
                </CardTitle>
                <CardDescription>
                  기본 계정 정보를 확인하고 수정하세요.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* User ID */}
                <div className="space-y-2">
                  <Label className="text-muted-foreground">아이디</Label>
                  <div className="flex items-center gap-2">
                    <Input value={user.userId} disabled className="bg-muted" />
                    <Badge variant="secondary">변경 불가</Badge>
                  </div>
                </div>

                {/* Email */}
                <div className="space-y-2">
                  <Label className="text-muted-foreground">이메일</Label>
                  <Input value={user.email} disabled className="bg-muted" />
                </div>

                {/* Provider */}
                <div className="space-y-2">
                  <Label className="text-muted-foreground">로그인 방식</Label>
                  <div className="flex items-center gap-2">
                    <Badge variant="outline">
                      {providerLabels[user.provider as Provider]}
                    </Badge>
                    {user.roles.includes("ADMIN") && (
                      <Badge className="bg-accent text-accent-foreground">
                        관리자
                      </Badge>
                    )}
                  </div>
                </div>

                {/* Nickname */}
                <div className="space-y-2">
                  <Label htmlFor="nickname">닉네임</Label>
                  <div className="flex items-center gap-2">
                    {isEditingNickname ? (
                      <>
                        <Input
                          id="nickname"
                          value={nickname}
                          onChange={(e) => setNickname(e.target.value)}
                          placeholder="닉네임을 입력하세요"
                        />
                        <Button
                          onClick={handleSaveNickname}
                          disabled={isSavingNickname}
                          size="sm"
                        >
                          {isSavingNickname ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            "저장"
                          )}
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => {
                            setNickname(user.nickname)
                            setIsEditingNickname(false)
                          }}
                          size="sm"
                        >
                          취소
                        </Button>
                      </>
                    ) : (
                      <>
                        <Input
                          value={user.nickname}
                          disabled
                          className="bg-muted"
                        />
                        <Button
                          variant="outline"
                          onClick={() => setIsEditingNickname(true)}
                          size="sm"
                        >
                          변경
                        </Button>
                      </>
                    )}
                  </div>
                  {nicknameSuccess && (
                    <p className="text-sm text-chart-1 flex items-center gap-1">
                      <CheckCircle2 className="h-4 w-4" />
                      닉네임이 변경되었습니다.
                    </p>
                  )}
                </div>

                {/* Join Date */}
                <div className="space-y-2">
                  <Label className="text-muted-foreground">가입일</Label>
                  <p className="text-sm">
                    {new Date(user.createdAt).toLocaleDateString("ko-KR", {
                      year: "numeric",
                      month: "long",
                      day: "numeric",
                    })}
                  </p>
                </div>
              </CardContent>
            </Card>

            {/* Password Change */}
            <Card className={cn(isOAuthUser && "opacity-60")}>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Key className="h-5 w-5" />
                  비밀번호 변경
                </CardTitle>
                <CardDescription>
                  {isOAuthUser
                    ? `${providerLabels[user.provider as Provider]} 계정은 비밀번호를 변경할 수 없습니다.`
                    : "계정 보안을 위해 정기적으로 비밀번호를 변경하세요."}
                </CardDescription>
              </CardHeader>
              <CardContent>
                {isOAuthUser ? (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground bg-muted p-4 rounded-lg">
                    <Shield className="h-5 w-5" />
                    <span>
                      소셜 로그인 계정은 해당 서비스에서 비밀번호를 관리합니다.
                    </span>
                  </div>
                ) : (
                  <form onSubmit={handleChangePassword} className="space-y-4">
                    {passwordError && (
                      <div className="p-3 text-sm text-destructive bg-destructive/10 rounded-lg border border-destructive/20 flex items-center gap-2">
                        <AlertCircle className="h-4 w-4" />
                        {passwordError}
                      </div>
                    )}

                    {passwordSuccess && (
                      <div className="p-3 text-sm text-chart-1 bg-chart-1/10 rounded-lg border border-chart-1/20 flex items-center gap-2">
                        <CheckCircle2 className="h-4 w-4" />
                        비밀번호가 성공적으로 변경되었습니다.
                      </div>
                    )}

                    <div className="space-y-2">
                      <Label htmlFor="currentPassword">현재 비밀번호</Label>
                      <Input
                        id="currentPassword"
                        type="password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        placeholder="현재 비밀번호를 입력하세요"
                        required
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="newPassword">새 비밀번호</Label>
                      <Input
                        id="newPassword"
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="새 비밀번호를 입력하세요"
                        required
                      />
                      <p className="text-xs text-muted-foreground">
                        8자 이상, 영문과 숫자를 포함해주세요.
                      </p>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="confirmPassword">새 비밀번호 확인</Label>
                      <Input
                        id="confirmPassword"
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="새 비밀번호를 다시 입력하세요"
                        required
                      />
                    </div>

                    <Button
                      type="submit"
                      disabled={isChangingPassword}
                      className="w-full"
                    >
                      {isChangingPassword ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          변경 중...
                        </>
                      ) : (
                        "비밀번호 변경"
                      )}
                    </Button>
                  </form>
                )}
              </CardContent>
            </Card>

            {/* Danger Zone */}
            <Card className="border-destructive/30">
              <CardHeader>
                <CardTitle className="text-destructive flex items-center gap-2">
                  <AlertCircle className="h-5 w-5" />
                  계정 삭제
                </CardTitle>
                <CardDescription>
                  계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Button variant="outline" className="text-destructive border-destructive/30 hover:bg-destructive/10">
                  계정 삭제 요청
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  )
}
