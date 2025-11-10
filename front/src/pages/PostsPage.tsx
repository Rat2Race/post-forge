import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { listPosts } from '../api/board';
import type { PostResponse, Page } from '../api/types';
import { Heart, MessageCircle, Eye, Plus } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import Navigation from '../components/Navigation';

export default function PostsPage() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [posts, setPosts] = useState<Page<PostResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadPosts();
  }, [page]);

  const loadPosts = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await listPosts(page, 20);
      setPosts(data);
    } catch (err: any) {
      setError('게시글을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return '방금 전';
    if (minutes < 60) return `${minutes}분 전`;
    if (hours < 24) return `${hours}시간 전`;
    if (days < 7) return `${days}일 전`;
    return date.toLocaleDateString('ko-KR');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">게시판</h1>
            <p className="text-gray-600 mt-1">자유롭게 소통하는 공간입니다</p>
          </div>
          {isAuthenticated && (
            <Link
              to="/posts/create"
              className="flex items-center justify-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors sm:self-start whitespace-nowrap"
            >
              <Plus className="w-5 h-5" />
              글쓰기
            </Link>
          )}
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600">
            {error}
          </div>
        )}

        {/* Posts List */}
        {loading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : posts && posts.content.length > 0 ? (
          <div className="bg-white rounded-lg shadow">
            {posts.content.map((post, index) => (
              <div
                key={post.id}
                className={`p-6 hover:bg-gray-50 cursor-pointer transition-colors ${
                  index !== posts.content.length - 1 ? 'border-b border-gray-200' : ''
                }`}
                onClick={() => navigate(`/posts/${post.id}`)}
              >
                <h2 className="text-xl font-semibold text-gray-900 mb-2 hover:text-blue-600">
                  {post.title}
                </h2>
                <p className="text-gray-600 mb-4 line-clamp-2">{post.content}</p>
                <div className="flex items-center gap-4 text-sm text-gray-500">
                  <span className="font-medium text-gray-700">{post.userId}</span>
                  <span>{formatDate(post.createdAt)}</span>
                  <div className="flex items-center gap-4 ml-auto">
                    <span className="flex items-center gap-1">
                      <Eye className="w-4 h-4" />
                      {post.views}
                    </span>
                    <span className="flex items-center gap-1">
                      <Heart className={`w-4 h-4 ${post.isLiked ? 'fill-red-500 text-red-500' : ''}`} />
                      {post.likeCount}
                    </span>
                    <span className="flex items-center gap-1">
                      <MessageCircle className="w-4 h-4" />
                      {post.commentCount}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12 bg-white rounded-lg shadow">
            <p className="text-gray-500">게시글이 없습니다.</p>
          </div>
        )}

        {/* Pagination */}
        {posts && posts.totalPages > 1 && (
          <div className="flex justify-center gap-2 mt-8">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              이전
            </button>
            <span className="px-4 py-2">
              {page + 1} / {posts.totalPages}
            </span>
            <button
              onClick={() => setPage(Math.min(posts.totalPages - 1, page + 1))}
              disabled={page >= posts.totalPages - 1}
              className="px-4 py-2 border border-gray-300 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
            >
              다음
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
