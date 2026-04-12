"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { Header } from "@/components/layout/header"
import { Footer } from "@/components/layout/footer"
import { PostEditor } from "@/components/posts/post-editor"

export default function NewPostPage() {
  const router = useRouter()
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

    // Redirect to the new post
    router.push("/posts/1")
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Header isAuthenticated user={{ nickname: "사용자" }} />

      <main className="flex-1">
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 py-8">
          {/* Header */}
          <div className="mb-8">
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-4"
            >
              <ArrowLeft className="h-4 w-4" />
              목록으로
            </Link>
            <h1 className="font-serif text-2xl sm:text-3xl font-bold">
              새 분석 작성
            </h1>
            <p className="text-muted-foreground mt-2">
              종목 분석, 투자 아이디어, 시장 전망을 공유하세요.
            </p>
          </div>

          {/* Editor */}
          <div className="bg-card border border-border rounded-xl p-6">
            <PostEditor onSubmit={handleSubmit} isSubmitting={isSubmitting} />
          </div>
        </div>
      </main>

      <Footer />
    </div>
  )
}
