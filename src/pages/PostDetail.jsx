import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { postsAPI } from '../api/posts';
import { commentsAPI } from '../api/comments';
import { useAuthStore } from '../store/authStore';
import { Eye, Heart, MessageCircle, Edit, Trash2, ArrowLeft } from 'lucide-react';
import { formatDate } from '../utils/dateUtils';
import CommentSection from '../components/CommentSection';

export default function PostDetail() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPost();
  }, [postId]);

  const fetchPost = async () => {
    try {
      setLoading(true);
      const data = await postsAPI.getPost(postId);
      setPost(data);
      setError(null);
    } catch (err) {
      setError('게시글을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLike = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      navigate('/login');
      return;
    }

    try {
      const data = await postsAPI.toggleLike(postId);
      setPost({
        ...post,
        isLiked: data.isLiked,
        likeCount: data.likeCount,
      });
    } catch (err) {
      console.error('좋아요 처리 실패:', err);
    }
  };

  const handleDelete = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) {
      return;
    }

    try {
      await postsAPI.deletePost(postId);
      alert('게시글이 삭제되었습니다.');
      navigate('/');
    } catch (err) {
      alert('삭제에 실패했습니다.');
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-carrot"></div>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">{error || '게시글을 찾을 수 없습니다.'}</p>
        <Link to="/" className="mt-4 inline-block text-carrot hover:text-carrot-dark">
          목록으로 돌아가기
        </Link>
      </div>
    );
  }

  const isAuthor = user?.userId === post.userId;

  return (
    <div className="max-w-4xl mx-auto">
      <Link
        to="/"
        className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft size={20} />
        목록으로
      </Link>

      <div className="card p-8">
        <div className="border-b border-gray-200 pb-6 mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{post.title}</h1>

          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-600 font-medium">{post.userId}</p>
              <p className="text-sm text-gray-500 mt-1">{formatDate(post.createdAt)}</p>
            </div>

            <div className="flex items-center gap-6 text-gray-600">
              <span className="flex items-center gap-1">
                <Eye size={18} />
                {post.views}
              </span>
              <span className="flex items-center gap-1">
                <Heart size={18} />
                {post.likeCount}
              </span>
              <span className="flex items-center gap-1">
                <MessageCircle size={18} />
                {post.commentCount}
              </span>
            </div>
          </div>
        </div>

        <div className="prose max-w-none mb-8">
          <p className="text-gray-800 whitespace-pre-wrap">{post.content}</p>
        </div>

        <div className="flex items-center gap-3 pt-6 border-t border-gray-200">
          <button
            onClick={handleLike}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
              post.isLiked
                ? 'bg-red-50 text-red-600 hover:bg-red-100'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <Heart size={18} fill={post.isLiked ? 'currentColor' : 'none'} />
            좋아요 {post.likeCount}
          </button>

          {isAuthor && (
            <>
              <Link
                to={`/posts/${postId}/edit`}
                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200"
              >
                <Edit size={18} />
                수정
              </Link>
              <button
                onClick={handleDelete}
                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-red-50 text-red-600 hover:bg-red-100"
              >
                <Trash2 size={18} />
                삭제
              </button>
            </>
          )}
        </div>
      </div>

      <div className="mt-8">
        <CommentSection postId={postId} />
      </div>
    </div>
  );
}
