import { Link } from 'react-router-dom';
import { Eye, Heart, MessageCircle } from 'lucide-react';
import { formatDistanceToNow } from '../utils/dateUtils';

export default function PostCard({ post }) {
  return (
    <Link to={`/posts/${post.id}`}>
      <div className="card p-6 hover:border-carrot hover:border transition-all">
        <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
          {post.title}
        </h3>
        <p className="text-gray-600 text-sm mb-4 line-clamp-2">
          {post.content}
        </p>

        <div className="flex items-center justify-between text-sm text-gray-500">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1">
              <Eye size={16} />
              {post.views}
            </span>
            <span className="flex items-center gap-1">
              <Heart size={16} />
              {post.likeCount}
            </span>
            <span className="flex items-center gap-1">
              <MessageCircle size={16} />
              {post.commentCount}
            </span>
          </div>
          <span>{formatDistanceToNow(post.createdAt)}</span>
        </div>

        <div className="mt-3 pt-3 border-t border-gray-100">
          <span className="text-sm text-gray-600">{post.userId}</span>
        </div>
      </div>
    </Link>
  );
}
