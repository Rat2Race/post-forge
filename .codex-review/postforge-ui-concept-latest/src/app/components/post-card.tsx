import { Link } from "react-router";
import { Eye, MessageSquare, ThumbsUp, TrendingUp, TrendingDown, Sparkles } from "lucide-react";

export interface PostCardProps {
  id: string;
  title: string;
  summary: string;
  tags: string[];
  author: string;
  views: number;
  commentCount: number;
  likeCount: number;
  createdAt: string;
  isAIGenerated?: boolean;
  signal?: "positive" | "negative" | "neutral";
}

export function PostCard({
  id,
  title,
  summary,
  tags,
  author,
  views,
  commentCount,
  likeCount,
  createdAt,
  isAIGenerated,
  signal,
}: PostCardProps) {
  return (
    <Link to={`/posts/${id}`} className="block group py-5 hover:bg-accent/50 transition-colors -mx-2 px-2 rounded">
      <div className="flex gap-3 items-start">
        {/* Signal icon */}
        {signal && signal !== "neutral" && (
          <div className="shrink-0 mt-0.5">
            {signal === "positive" ? (
              <TrendingUp className="h-5 w-5 text-positive" />
            ) : (
              <TrendingDown className="h-5 w-5 text-negative" />
            )}
          </div>
        )}

        <div className="flex-1 min-w-0">
          {/* Title */}
          <h3 className="serif-headline text-lg mb-1.5 group-hover:text-foreground/70 transition-colors line-clamp-2 leading-snug">
            {title}
          </h3>

          {/* Summary */}
          <p className="text-sm text-muted-foreground line-clamp-2 mb-3 leading-relaxed">
            {summary}
          </p>

          {/* Tags */}
          <div className="flex flex-wrap gap-1.5 mb-3">
            {tags.map((tag, index) => (
              <span
                key={index}
                className="inline-flex items-center px-2 py-0.5 rounded-md bg-secondary text-xs text-primary"
              >
                {tag}
              </span>
            ))}
          </div>

          {/* Meta row */}
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <div className="flex items-center gap-2">
              {isAIGenerated && (
                <span className="flex items-center gap-1 text-muted-foreground">
                  <Sparkles className="h-3 w-3" />
                  AI 분석가
                </span>
              )}
              {!isAIGenerated && <span>{author}</span>}
              <span>·</span>
              <span>{createdAt}</span>
            </div>
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1">
                <Eye className="h-3.5 w-3.5" />
                {views.toLocaleString()}
              </span>
              <span className="flex items-center gap-1">
                <MessageSquare className="h-3.5 w-3.5" />
                {commentCount}
              </span>
              <span className="flex items-center gap-1">
                <ThumbsUp className="h-3.5 w-3.5" />
                {likeCount}
              </span>
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
}