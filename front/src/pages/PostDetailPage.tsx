import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  getPost,
  deletePost,
  togglePostLike,
  listComments,
  createComment,
  deleteComment,
  toggleCommentLike,
} from '../api/board';
import type { PostResponse, CommentResponse, CommentRequest, Page } from '../api/types';
import { Heart, MessageCircle, Eye, Trash2, ArrowLeft } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import Navigation from '../components/Navigation';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const currentUserId = useAuthStore((state) => state.userId);

  const [post, setPost] = useState<PostResponse | null>(null);
  const [comments, setComments] = useState<Page<CommentResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [commentPage, setCommentPage] = useState(0);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CommentRequest>();

  useEffect(() => {
    if (id) {
      loadPost();
      loadComments();
    }
  }, [id, commentPage]);

  const loadPost = async () => {
    try {
      setLoading(true);
      const data = await getPost(Number(id));
      setPost(data);
    } catch (err: any) {
      setError('게시글을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadComments = async () => {
    try {
      const data = await listComments(Number(id), commentPage, 50);
      setComments(data);
    } catch (err: any) {
      console.error('댓글 로드 실패:', err);
    }
  };

  const handleDeletePost = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await deletePost(Number(id));
      navigate('/posts');
    } catch (err: any) {
      alert('삭제에 실패했습니다: ' + err.message);
    }
  };

  const handleTogglePostLike = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      return;
    }
    try {
      await togglePostLike(Number(id));
      loadPost(); // Reload to get updated like status
    } catch (err: any) {
      console.error('좋아요 실패:', err);
    }
  };

  const onSubmitComment = async (data: CommentRequest) => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      return;
    }
    try {
      await createComment(Number(id), data);
      reset();
      loadComments();
      loadPost(); // Reload to update comment count
    } catch (err: any) {
      alert('댓글 작성에 실패했습니다.');
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!confirm('댓글을 삭제하시겠습니까?')) return;
    try {
      await deleteComment(Number(id), commentId);
      loadComments();
      loadPost(); // Reload to update comment count
    } catch (err: any) {
      alert('삭제에 실패했습니다: ' + err.message);
    }
  };

  const handleToggleCommentLike = async (commentId: number) => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      return;
    }
    try {
      await toggleCommentLike(Number(id), commentId);
      loadComments(); // Reload to get updated like status
    } catch (err: any) {
      console.error('좋아요 실패:', err);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('ko-KR');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error || '게시글을 찾을 수 없습니다.'}</p>
          <button
            onClick={() => navigate('/posts')}
            className="text-blue-600 hover:underline"
          >
            목록으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navigation />
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Back Button */}
        <button
          onClick={() => navigate('/posts')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft className="w-5 h-5" />
          목록으로
        </button>

        {/* Post */}
        <div className="bg-white rounded-lg shadow p-8 mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{post.title}</h1>
          <div className="flex items-center justify-between text-sm text-gray-500 mb-6 pb-6 border-b">
            <div className="flex items-center gap-4">
              <span className="font-medium text-gray-700">{post.userId}</span>
              <span>{formatDate(post.createdAt)}</span>
            </div>
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1">
                <Eye className="w-4 h-4" />
                {post.views}
              </span>
              <button
                onClick={handleTogglePostLike}
                className="flex items-center gap-1 hover:text-red-500"
              >
                <Heart
                  className={`w-4 h-4 ${post.isLiked ? 'fill-red-500 text-red-500' : ''}`}
                />
                {post.likeCount}
              </button>
              <span className="flex items-center gap-1">
                <MessageCircle className="w-4 h-4" />
                {post.commentCount}
              </span>
              {isAuthenticated && currentUserId === post.userId && (
                <button
                  onClick={handleDeletePost}
                  className="flex items-center gap-1 text-red-600 hover:text-red-700"
                >
                  <Trash2 className="w-4 h-4" />
                  삭제
                </button>
              )}
            </div>
          </div>
          <div className="prose max-w-none">
            <p className="whitespace-pre-wrap text-gray-700">{post.content}</p>
          </div>
        </div>

        {/* Comments Section */}
        <div className="bg-white rounded-lg shadow p-8">
          <h2 className="text-xl font-bold text-gray-900 mb-6">
            댓글 {post.commentCount}개
          </h2>

          {/* Comment Form */}
          {isAuthenticated && (
            <form onSubmit={handleSubmit(onSubmitComment)} className="mb-8">
              <textarea
                {...register('content', {
                  required: '댓글 내용을 입력해주세요',
                  minLength: { value: 1, message: '댓글은 최소 1자 이상이어야 합니다' },
                })}
                rows={3}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                placeholder="댓글을 입력하세요"
              />
              {errors.content && (
                <p className="mt-1 text-sm text-red-600">{errors.content.message}</p>
              )}
              <div className="flex justify-end mt-2">
                <button
                  type="submit"
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  댓글 작성
                </button>
              </div>
            </form>
          )}

          {/* Comments List */}
          {comments && comments.content.length > 0 ? (
            <div className="space-y-4">
              {comments.content.map((comment) => (
                <div key={comment.id} className="border-b border-gray-200 pb-4 last:border-0">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <span className="font-medium text-gray-900">{comment.userId}</span>
                      <span className="text-sm text-gray-500 ml-3">
                        {formatDate(comment.createdAt)}
                      </span>
                    </div>
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleToggleCommentLike(comment.id)}
                        className="flex items-center gap-1 text-sm text-gray-500 hover:text-red-500"
                      >
                        <Heart
                          className={`w-4 h-4 ${
                            comment.isLiked ? 'fill-red-500 text-red-500' : ''
                          }`}
                        />
                        {comment.likeCount}
                      </button>
                      {isAuthenticated && currentUserId === comment.userId && (
                        <button
                          onClick={() => handleDeleteComment(comment.id)}
                          className="text-sm text-red-600 hover:text-red-700"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                  <p className="text-gray-700">{comment.content}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-center text-gray-500 py-8">첫 댓글을 작성해보세요!</p>
          )}

          {/* Comment Pagination */}
          {comments && comments.totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-6">
              <button
                onClick={() => setCommentPage(Math.max(0, commentPage - 1))}
                disabled={commentPage === 0}
                className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50"
              >
                이전
              </button>
              <span className="px-4 py-2">
                {commentPage + 1} / {comments.totalPages}
              </span>
              <button
                onClick={() =>
                  setCommentPage(Math.min(comments.totalPages - 1, commentPage + 1))
                }
                disabled={commentPage >= comments.totalPages - 1}
                className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50"
              >
                다음
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
