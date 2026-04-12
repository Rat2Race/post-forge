import { useState } from "react";
import { MessageSquare, ThumbsUp, Sparkles } from "lucide-react";
import { Button } from "./ui/button";
import { Textarea } from "./ui/textarea";
import { Avatar, AvatarFallback } from "./ui/avatar";

export interface Comment {
  id: string;
  author: string;
  content: string;
  createdAt: string;
  likeCount: number;
  replies?: Comment[];
  isAIGenerated?: boolean;
}

interface CommentThreadProps {
  comments: Comment[];
  postAuthorIsAI?: boolean;
  onAddComment?: (content: string, parentId?: string) => void;
  onAskAIReply?: (commentId: string) => void;
}

export function CommentThread({ comments, postAuthorIsAI, onAddComment, onAskAIReply }: CommentThreadProps) {
  const [newComment, setNewComment] = useState("");
  const [replyTo, setReplyTo] = useState<string | null>(null);
  const [replyContent, setReplyContent] = useState("");

  const handleSubmitComment = () => {
    if (newComment.trim() && onAddComment) {
      onAddComment(newComment);
      setNewComment("");
    }
  };

  const handleSubmitReply = (parentId: string) => {
    if (replyContent.trim() && onAddComment) {
      onAddComment(replyContent, parentId);
      setReplyContent("");
      setReplyTo(null);
    }
  };

  const handleAskAI = (commentId: string) => {
    if (onAskAIReply) {
      onAskAIReply(commentId);
    }
  };

  return (
    <div className="space-y-6">
      {/* New Comment Form */}
      <div className="bg-card rounded-lg border border-border p-6">
        <h3 className="mb-4">댓글 작성</h3>
        <Textarea
          placeholder="의견을 작성해주세요..."
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          className="mb-3 min-h-24"
        />
        <div className="flex justify-end">
          <Button onClick={handleSubmitComment}>댓글 등록</Button>
        </div>
      </div>

      {/* Comments List */}
      <div className="space-y-6">
        {comments.map((comment) => (
          <div key={comment.id} className="space-y-4">
            {/* Top-level Comment */}
            <div className="flex gap-4">
              <Avatar className="h-10 w-10 shrink-0">
                <AvatarFallback className={comment.isAIGenerated ? "bg-brass text-brass-foreground" : ""}>
                  {comment.isAIGenerated ? "AI" : comment.author[0]}
                </AvatarFallback>
              </Avatar>
              
              <div className="flex-1 min-w-0">
                <div className="bg-secondary/50 rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="font-medium">{comment.author}</span>
                    {comment.isAIGenerated && (
                      <span className="text-xs px-2 py-0.5 rounded bg-brass/10 text-brass border border-brass/20">
                        AI 분석가
                      </span>
                    )}
                    <span className="text-xs text-muted-foreground">{comment.createdAt}</span>
                  </div>
                  <p className="text-sm whitespace-pre-wrap">{comment.content}</p>
                </div>
                
                <div className="flex items-center gap-3 mt-2 text-sm">
                  <button className="flex items-center gap-1 text-muted-foreground hover:text-foreground transition-colors">
                    <ThumbsUp className="h-4 w-4" />
                    <span>{comment.likeCount}</span>
                  </button>
                  <button 
                    onClick={() => setReplyTo(comment.id)}
                    className="flex items-center gap-1 text-muted-foreground hover:text-foreground transition-colors"
                  >
                    <MessageSquare className="h-4 w-4" />
                    <span>답글</span>
                  </button>
                  {postAuthorIsAI && !comment.isAIGenerated && (
                    <button
                      onClick={() => handleAskAI(comment.id)}
                      className="flex items-center gap-1 text-brass hover:text-brass/80 transition-colors"
                    >
                      <Sparkles className="h-4 w-4" />
                      <span>AI에게 답글 요청</span>
                    </button>
                  )}
                </div>

                {/* Reply Form */}
                {replyTo === comment.id && (
                  <div className="mt-4 pl-4 border-l-2 border-border">
                    <Textarea
                      placeholder="답글을 작성해주세요..."
                      value={replyContent}
                      onChange={(e) => setReplyContent(e.target.value)}
                      className="mb-2 min-h-20"
                    />
                    <div className="flex gap-2">
                      <Button size="sm" onClick={() => handleSubmitReply(comment.id)}>
                        답글 등록
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => setReplyTo(null)}>
                        취소
                      </Button>
                    </div>
                  </div>
                )}

                {/* Nested Replies (Only 1 level deep) */}
                {comment.replies && comment.replies.length > 0 && (
                  <div className="mt-4 pl-4 border-l-2 border-border space-y-4">
                    {comment.replies.map((reply) => (
                      <div key={reply.id} className="flex gap-3">
                        <Avatar className="h-8 w-8 shrink-0">
                          <AvatarFallback className={reply.isAIGenerated ? "bg-brass text-brass-foreground" : ""}>
                            {reply.isAIGenerated ? "AI" : reply.author[0]}
                          </AvatarFallback>
                        </Avatar>
                        
                        <div className="flex-1 min-w-0">
                          <div className="bg-secondary/30 rounded-lg p-3">
                            <div className="flex items-center gap-2 mb-2">
                              <span className="font-medium text-sm">{reply.author}</span>
                              {reply.isAIGenerated && (
                                <span className="text-xs px-2 py-0.5 rounded bg-brass/10 text-brass border border-brass/20">
                                  AI 분석가
                                </span>
                              )}
                              <span className="text-xs text-muted-foreground">{reply.createdAt}</span>
                            </div>
                            <p className="text-sm whitespace-pre-wrap">{reply.content}</p>
                          </div>
                          
                          <div className="flex items-center gap-3 mt-2 text-sm">
                            <button className="flex items-center gap-1 text-muted-foreground hover:text-foreground transition-colors">
                              <ThumbsUp className="h-4 w-4" />
                              <span>{reply.likeCount}</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
