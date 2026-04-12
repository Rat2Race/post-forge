import Link from "next/link"
import { Eye, MessageCircle } from "lucide-react"
import { cn } from "@/lib/utils"

export interface PostCardProps {
  id: string
  title: string
  summary: string
  tags: string[]
  author: {
    nickname: string
    isAI?: boolean
  }
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
  signal?: "positive" | "negative" | "neutral"
}

export function PostCard({
  id,
  title,
  summary,
  tags,
  author,
  viewCount,
  commentCount,
  createdAt,
  signal,
}: PostCardProps) {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffInHours = Math.floor(
      (now.getTime() - date.getTime()) / (1000 * 60 * 60)
    )

    if (diffInHours < 1) return "방금 전"
    if (diffInHours < 24) return `${diffInHours}시간 전`
    if (diffInHours < 48) return "어제"
    return date.toLocaleDateString("ko-KR", {
      month: "long",
      day: "numeric",
    })
  }

  const formatNumber = (num: number) => {
    if (num >= 10000) return `${(num / 10000).toFixed(1)}만`
    if (num >= 1000) return `${(num / 1000).toFixed(1)}천`
    return num.toString()
  }

  const signalLabel = {
    positive: "긍정적",
    negative: "부정적",
    neutral: "중립",
  }

  return (
    <Link href={`/posts/${id}`} className="block group">
      <article className="py-6 border-b border-border last:border-0 transition-colors">
        {/* Meta row - author, date, signal */}
        <div className="flex items-center gap-3 mb-3 text-sm">
          <span className={cn(
            "font-medium",
            author.isAI ? "text-accent" : "text-muted-foreground"
          )}>
            {author.nickname}
          </span>
          <span className="text-border">·</span>
          <span className="text-muted-foreground">{formatDate(createdAt)}</span>
          {signal && (
            <>
              <span className="text-border">·</span>
              <span className={cn(
                "text-sm",
                signal === "positive" && "text-chart-1",
                signal === "negative" && "text-chart-2",
                signal === "neutral" && "text-muted-foreground"
              )}>
                {signalLabel[signal]}
              </span>
            </>
          )}
        </div>

        {/* Title */}
        <h3 className="font-serif text-xl font-semibold leading-snug mb-3 group-hover:text-accent transition-colors text-balance">
          {title}
        </h3>

        {/* Summary */}
        <p className="text-[15px] text-muted-foreground leading-relaxed line-clamp-2 mb-4">
          {summary}
        </p>

        {/* Footer - tags and stats */}
        <div className="flex items-center justify-between">
          {/* Tags */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            {tags.slice(0, 3).map((tag, index) => (
              <span key={tag}>
                {tag}
                {index < Math.min(tags.length, 3) - 1 && <span className="ml-2 text-border">/</span>}
              </span>
            ))}
            {tags.length > 3 && (
              <span className="text-muted-foreground/60">외 {tags.length - 3}개</span>
            )}
          </div>

          {/* Stats */}
          <div className="flex items-center gap-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <Eye className="h-3.5 w-3.5" />
              {formatNumber(viewCount)}
            </span>
            <span className="flex items-center gap-1.5">
              <MessageCircle className="h-3.5 w-3.5" />
              {formatNumber(commentCount)}
            </span>
          </div>
        </div>
      </article>
    </Link>
  )
}
