import { Link } from "react-router";
import { Eye, MessageSquare, ThumbsUp, TrendingUp, TrendingDown } from "lucide-react";

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
    <Link to={`/posts/${id}`} className="block group py-6 hover:bg-secondary/30 transition-colors">
      <div className="flex gap-4 items-start">
        {/* Signal icon */}
        {signal && signal !== "neutral" && (
          <div className="shrink-0 mt-1">
            {signal === "positive" ? (
              <TrendingUp className="h-[22px] w-[22px] text-positive" />
            ) : (
              <TrendingDown className="h-[22px] w-[22px] text-negative" />
            )}
          </div>
        )}

        <div className="flex-1 min-w-0">
          {/* Title */}
          <h3 className="serif-headline text-xl mb-2 group-hover:text-brass transition-colors line-clamp-2">
            {title}
          </h3>

          {/* Summary */}
          <p className="text-muted-foreground line-clamp-2 mb-4 leading-relaxed">
            {summary}
          </p>

          {/* Tags */}
          <div className="flex flex-wrap gap-2 mb-4">
            {tags.map((tag, index) => (
              <span
                key={index}
                className="inline-flex items-center justify-center px-2.5 py-0.5 rounded-md bg-[#f5f3ef] text-sm text-primary"
              >
                {tag}
              </span>
            ))}
          </div>

          {/* Meta row */}
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <div className="flex items-center gap-3">
              <span className="flex items-center gap-1.5">
                {isAIGenerated && (
                  <span className="w-1.5 h-1.5 rounded-full bg-brass" />
                )}
                <span className={isAIGenerated ? "text-brass" : ""}>
                  {isAIGenerated ? "AI 분석가" : author}
                </span>
              </span>
              <span>{createdAt}</span>
            </div>
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1.5">
                <Eye className="h-4 w-4" />
                {views.toLocaleString()}
              </span>
              <span className="flex items-center gap-1.5">
                <MessageSquare className="h-4 w-4" />
                {commentCount}
              </span>
              <span className="flex items-center gap-1.5">
                <ThumbsUp className="h-4 w-4" />
                {likeCount}
              </span>
            </div>
          </div>
        </div>
      </div>
    </Link>
  );
}
