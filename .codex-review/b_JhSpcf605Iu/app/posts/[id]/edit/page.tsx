"use client"

import { useState } from "react"
import { useRouter, useParams } from "next/navigation"
import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { PostEditor } from "@/components/posts/post-editor"

// Sample existing post data
const existingPost = {
  title: "SK하이닉스 HBM4 개발 현황과 시장 전망",
  content: `SK하이닉스가 차세대 HBM4 개발에서 경쟁사 대비 6개월 앞선 것으로 분석됩니다.

■ 주요 포인트
- 엔비디아 B200 GPU에 HBM3E 독점 공급 계약 체결
- HBM4 샘플 출하 2026년 하반기 예정
- 중국 제외 글로벌 AI 서버 시장 점유율 50% 이상

■ 투자 의견
현재 주가 수준에서 매수 관점이 유효하며, 목표가 상향 여력이 있습니다.`,
  tags: ["SK하이닉스", "HBM4", "엔비디아", "AI반도체"],
  attachments: [
    { id: "file-1", name: "HBM_시장분석.pdf", type: "pdf" as const, size: "1.2MB" },
  ],
}

export default function EditPostPage() {
  const router = useRouter()
  const params = useParams()
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (data: {
    title: string
    content: string
    tags: string[]
    attachmentIds: string[]
  }) => {
    setIsSubmitting(true)

    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 2000))

    // Redirect back to the post
    router.push(`/posts/${params.id}`)
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header isAuthenticated user={{ nickname: "사용자" }} />

      <main className="flex-1">
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-8">
          {/* Header */}
          <div className="mb-8">
            <Link
              href={`/posts/${params.id}`}
              className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-4"
            >
              <ArrowLeft className="h-4 w-4" />
              게시글로 돌아가기
            </Link>
            <h1 className="font-serif text-2xl sm:text-3xl font-bold">
              분석 수정
            </h1>
            <p className="text-muted-foreground mt-2">
              게시글 내용을 수정하세요.
            </p>
          </div>

          {/* Editor */}
          <div className="bg-card border border-border rounded-xl p-6">
            <PostEditor
              initialData={existingPost}
              onSubmit={handleSubmit}
              isSubmitting={isSubmitting}
            />
          </div>
        </div>
      </main>

      <Footer />
    </div>
  )
}
