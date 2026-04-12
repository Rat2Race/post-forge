"use client"

import { useState } from "react"
import { MessageCircle, MoreHorizontal, Send, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { cn } from "@/lib/utils"

export interface Comment {
  id: string
  content: string
  author: {
    nickname: string
    isAI?: boolean
  }
  createdAt: string
  likeCount: number
  isLiked?: boolean
  replies?: Comment[]
}

interface CommentThreadProps {
  comments: Comment[]
  isAIPost?: boolean
  onAddComment?: (content: string, parentId?: string) => void
  onLikeComment?: (commentId: string) => void
  onAskAI?: (commentId: string) => void
}

function CommentItem({
  comment,
  isAIPost,
  isReply,
  onReply,
  onAskAI,
}: {
  comment: Comment
  isAIPost?: boolean
  isReply?: boolean
  onReply?: (commentId: string, content: string) => void
  onAskAI?: (commentId: string) => void
}) {
  const [showReplyForm, setShowReplyForm] = useState(false)
  const [replyContent, setReplyContent] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isAskingAI, setIsAskingAI] = useState(false)

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffInHours = Math.floor(
      (now.getTime() - date.getTime()) / (1000 * 60 * 60)
    )

    if (diffInHours < 1) return "방금 전"
    if (diffInHours < 24) return `${diffInHours}시간 전`
    return date.toLocaleDateString("ko-KR", {
      month: "long",
      day: "numeric",
    })
  }

  const handleSubmitReply = async () => {
    if (!replyContent.trim()) return
    setIsSubmitting(true)
    await new Promise((r) => setTimeout(r, 1000))
    onReply?.(comment.id, replyContent)
    setReplyContent("")
    setShowReplyForm(false)
    setIsSubmitting(false)
  }

  const handleAskAI = async () => {
    setIsAskingAI(true)
    await new Promise((r) => setTimeout(r, 2000))
    onAskAI?.(comment.id)
    setIsAskingAI(false)
  }

  return (
    <div className={cn(isReply && "ml-8 mt-6")}>
      <div className="group">
        {/* Author and date */}
        <div className="flex items-center gap-2 mb-2">
          <span className={cn(
            "text-sm font-medium",
            comment.author.isAI ? "text-accent" : "text-foreground"
          )}>
            {comment.author.nickname}
          </span>
          <span className="text-sm text-muted-foreground">
            {formatDate(comment.createdAt)}
          </span>
        </div>

        {/* Content */}
        <p className="text-[15px] leading-relaxed text-foreground/90 whitespace-pre-wrap">
          {comment.content}
        </p>

        {/* Actions */}
        <div className="flex items-center gap-4 mt-3">
          {!isReply && (
            <button
              onClick={() => setShowReplyForm(!showReplyForm)}
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              답글
            </button>
          )}

          {/* Ask AI - only on AI posts, for top-level non-AI comments */}
          {isAIPost && !isReply && !comment.author.isAI && (
            <button
              onClick={handleAskAI}
              disabled={isAskingAI}
              className="text-sm text-accent hover:text-accent/80 transition-colors flex items-center gap-1"
            >
              {isAskingAI ? (
                <>
                  <Loader2 className="h-3 w-3 animate-spin" />
                  분석 중...
                </>
              ) : (
                "AI 분석 요청"
              )}
            </button>
          )}

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="text-muted-foreground hover:text-foreground opacity-0 group-hover:opacity-100 transition-opacity">
                <MoreHorizontal className="h-4 w-4" />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start">
              <DropdownMenuItem>신고</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Reply Form */}
        {showReplyForm && (
          <div className="mt-4 pl-0">
            <Textarea
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              placeholder="답글을 작성하세요..."
              className="min-h-20 text-[15px] resize-none bg-secondary/30 border-0 focus-visible:ring-1"
            />
            <div className="flex items-center gap-2 mt-2">
              <Button
                size="sm"
                onClick={handleSubmitReply}
                disabled={!replyContent.trim() || isSubmitting}
              >
                {isSubmitting ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  "등록"
                )}
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setShowReplyForm(false)}
              >
                취소
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Replies */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="mt-6 pl-6 border-l border-border">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              isReply
            />
          ))}
        </div>
      )}
    </div>
  )
}

export function CommentThread({
  comments,
  isAIPost,
  onAddComment,
  onAskAI,
}: CommentThreadProps) {
  const [newComment, setNewComment] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async () => {
    if (!newComment.trim()) return
    setIsSubmitting(true)
    await new Promise((r) => setTimeout(r, 1000))
    onAddComment?.(newComment)
    setNewComment("")
    setIsSubmitting(false)
  }

  return (
    <div>
      {/* Comment Form */}
      <div className="mb-10">
        <Textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="의견을 나눠주세요..."
          className="min-h-24 resize-none text-[15px] bg-secondary/30 border-0 focus-visible:ring-1"
        />
        <div className="flex justify-end mt-3">
          <Button
            onClick={handleSubmit}
            disabled={!newComment.trim() || isSubmitting}
            size="sm"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                등록 중
              </>
            ) : (
              "댓글 등록"
            )}
          </Button>
        </div>
      </div>

      {/* Comments List */}
      <div className="space-y-8">
        {comments.map((comment) => (
          <CommentItem
            key={comment.id}
            comment={comment}
            isAIPost={isAIPost}
            onReply={(commentId, content) =>
              onAddComment?.(content, commentId)
            }
            onAskAI={onAskAI}
          />
        ))}
      </div>

      {comments.length === 0 && (
        <div className="text-center py-12">
          <p className="text-muted-foreground">
            아직 댓글이 없습니다.
            <br />
            첫 번째 의견을 남겨주세요.
          </p>
        </div>
      )}
    </div>
  )
}
