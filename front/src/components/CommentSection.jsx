import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { commentsAPI } from '../api/comments';
import { useAuthStore } from '../store/authStore';
import { Heart, Edit, Trash2, MessageCircle } from 'lucide-react';
import { formatDistanceToNow } from '../utils/dateUtils';

export default function CommentSection({ postId }) {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState('');
  const [editingComment, setEditingComment] = useState(null);
  const [editContent, setEditContent] = useState('');

  useEffect(() => {
    fetchComments();
  }, [postId]);

  const fetchComments = async () => {
    try {
      setLoading(true);
      const data = await commentsAPI.getComments(postId);

      // 댓글과 대댓글을 구조화
      const commentsMap = new Map();
      const rootComments = [];

      data.content.forEach(comment => {
        commentsMap.set(comment.id, { ...comment, replies: [] });
      });

      data.content.forEach(comment => {
        if (comment.parentId === null) {
          rootComments.push(commentsMap.get(comment.id));
        } else {
          const parent = commentsMap.get(comment.parentId);
          if (parent) {
            parent.replies.push(commentsMap.get(comment.id));
          }
        }
      });

      setComments(rootComments);
    } catch (err) {
      console.error('댓글 로딩 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateComment = async (e) => {
    e.preventDefault();

    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    if (newComment.trim().length < 2) {
      alert('댓글은 최소 2자 이상이어야 합니다.');
      return;
    }

    try {
      await commentsAPI.createComment(postId, { content: newComment });
      setNewComment('');
      await fetchComments();
    } catch (err) {
      alert('댓글 작성에 실패했습니다.');
      console.error(err);
    }
  };

  const handleCreateReply = async (parentId) => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    if (replyContent.trim().length < 2) {
      alert('답글은 최소 2자 이상이어야 합니다.');
      return;
    }

    try {
      await commentsAPI.createComment(postId, {
        parentId,
        content: replyContent,
      });
      setReplyingTo(null);
      setReplyContent('');
      await fetchComments();
    } catch (err) {
      alert('답글 작성에 실패했습니다.');
      console.error(err);
    }
  };

  const handleUpdateComment = async (commentId) => {
    if (editContent.trim().length < 2) {
      alert('댓글은 최소 2자 이상이어야 합니다.');
      return;
    }

    try {
      await commentsAPI.updateComment(postId, commentId, editContent);
      setEditingComment(null);
      setEditContent('');
      await fetchComments();
    } catch (err) {
      alert('댓글 수정에 실패했습니다.');
      console.error(err);
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!confirm('정말 삭제하시겠습니까?')) {
      return;
    }

    try {
      await commentsAPI.deleteComment(postId, commentId);
      await fetchComments();
    } catch (err) {
      alert('댓글 삭제에 실패했습니다.');
      console.error(err);
    }
  };

  const handleLikeComment = async (commentId) => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      await commentsAPI.toggleLike(postId, commentId);
      await fetchComments();
    } catch (err) {
      console.error('좋아요 처리 실패:', err);
    }
  };

  const CommentItem = ({ comment, isReply = false }) => {
    const isAuthor = user?.userId === comment.userId;
    const isEditing = editingComment === comment.id;

    return (
      <div className={`${isReply ? 'ml-12 mt-4' : 'mt-4'}`}>
        <div className="bg-gray-50 rounded-lg p-4">
          <div className="flex items-start justify-between mb-2">
            <div>
              <span className="font-medium text-gray-900">{comment.userId}</span>
              <span className="text-sm text-gray-500 ml-2">
                {formatDistanceToNow(comment.createdAt)}
              </span>
            </div>

            {isAuthor && !isEditing && (
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    setEditingComment(comment.id);
                    setEditContent(comment.content);
                  }}
                  className="text-gray-500 hover:text-gray-700"
                >
                  <Edit size={16} />
                </button>
                <button
                  onClick={() => handleDeleteComment(comment.id)}
                  className="text-red-500 hover:text-red-700"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            )}
          </div>

          {isEditing ? (
            <div className="space-y-2">
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="input-field resize-none"
                rows={3}
                maxLength={500}
              />
              <div className="flex gap-2">
                <button
                  onClick={() => handleUpdateComment(comment.id)}
                  className="btn-primary text-sm py-1 px-3"
                >
                  수정
                </button>
                <button
                  onClick={() => {
                    setEditingComment(null);
                    setEditContent('');
                  }}
                  className="btn-secondary text-sm py-1 px-3"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <>
              <p className="text-gray-800 mb-3 whitespace-pre-wrap">{comment.content}</p>

              <div className="flex items-center gap-4">
                <button
                  onClick={() => handleLikeComment(comment.id)}
                  className={`flex items-center gap-1 text-sm ${
                    comment.isLiked ? 'text-red-600' : 'text-gray-600'
                  } hover:text-red-600`}
                >
                  <Heart
                    size={16}
                    fill={comment.isLiked ? 'currentColor' : 'none'}
                  />
                  {comment.likeCount}
                </button>

                {!isReply && (
                  <button
                    onClick={() => setReplyingTo(comment.id)}
                    className="flex items-center gap-1 text-sm text-gray-600 hover:text-carrot"
                  >
                    <MessageCircle size={16} />
                    답글 {comment.replyCount}
                  </button>
                )}
              </div>
            </>
          )}
        </div>

        {replyingTo === comment.id && (
          <div className="ml-12 mt-3">
            <textarea
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              placeholder="답글을 입력하세요..."
              className="input-field resize-none"
              rows={3}
              maxLength={500}
            />
            <div className="flex gap-2 mt-2">
              <button
                onClick={() => handleCreateReply(comment.id)}
                className="btn-primary text-sm py-1 px-3"
              >
                답글 작성
              </button>
              <button
                onClick={() => {
                  setReplyingTo(null);
                  setReplyContent('');
                }}
                className="btn-secondary text-sm py-1 px-3"
              >
                취소
              </button>
            </div>
          </div>
        )}

        {comment.replies && comment.replies.length > 0 && (
          <div>
            {comment.replies.map((reply) => (
              <CommentItem key={reply.id} comment={reply} isReply />
            ))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="card p-6">
      <h3 className="text-xl font-bold text-gray-900 mb-4">
        댓글 {comments.reduce((acc, c) => acc + 1 + c.replies.length, 0)}개
      </h3>

      <form onSubmit={handleCreateComment} className="mb-6">
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder={
            isAuthenticated
              ? '댓글을 입력하세요...'
              : '로그인 후 댓글을 작성할 수 있습니다.'
          }
          className="input-field resize-none"
          rows={4}
          disabled={!isAuthenticated}
          maxLength={500}
        />
        <div className="flex justify-between items-center mt-2">
          <span className="text-sm text-gray-500">{newComment.length}/500</span>
          <button
            type="submit"
            disabled={!isAuthenticated || newComment.trim().length < 2}
            className="btn-primary disabled:opacity-50"
          >
            댓글 작성
          </button>
        </div>
      </form>

      {loading ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-carrot mx-auto"></div>
        </div>
      ) : comments.length === 0 ? (
        <p className="text-center text-gray-500 py-8">
          첫 댓글을 작성해보세요!
        </p>
      ) : (
        <div className="space-y-4">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} />
          ))}
        </div>
      )}
    </div>
  );
}
