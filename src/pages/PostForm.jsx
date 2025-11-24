import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { postsAPI } from '../api/posts';
import { ArrowLeft } from 'lucide-react';

export default function PostForm() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const isEditMode = !!postId;

  const [formData, setFormData] = useState({
    title: '',
    content: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isEditMode) {
      fetchPost();
    }
  }, [postId]);

  const fetchPost = async () => {
    try {
      const data = await postsAPI.getPost(postId);
      setFormData({
        title: data.title,
        content: data.content,
      });
    } catch (err) {
      alert('게시글을 불러오는데 실패했습니다.');
      navigate('/');
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (formData.title.length < 2 || formData.title.length > 100) {
      setError('제목은 2-100자 사이여야 합니다.');
      return;
    }

    if (formData.content.length < 10 || formData.content.length > 10000) {
      setError('내용은 10-10000자 사이여야 합니다.');
      return;
    }

    setLoading(true);

    try {
      if (isEditMode) {
        await postsAPI.updatePost(postId, formData);
        alert('게시글이 수정되었습니다.');
        navigate(`/posts/${postId}`);
      } else {
        const data = await postsAPI.createPost(formData);
        alert('게시글이 작성되었습니다.');
        navigate(`/posts/${data.id}`);
      }
    } catch (err) {
      setError(err.response?.data?.message || '저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft size={20} />
        뒤로가기
      </button>

      <div className="card p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">
          {isEditMode ? '게시글 수정' : '새 게시글 작성'}
        </h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-2">
              제목
            </label>
            <input
              id="title"
              name="title"
              type="text"
              required
              className="input-field"
              placeholder="제목을 입력하세요 (2-100자)"
              value={formData.title}
              onChange={handleChange}
              maxLength={100}
            />
            <p className="text-sm text-gray-500 mt-1">
              {formData.title.length}/100
            </p>
          </div>

          <div>
            <label htmlFor="content" className="block text-sm font-medium text-gray-700 mb-2">
              내용
            </label>
            <textarea
              id="content"
              name="content"
              required
              rows={15}
              className="input-field resize-none"
              placeholder="내용을 입력하세요 (10-10000자)"
              value={formData.content}
              onChange={handleChange}
              maxLength={10000}
            />
            <p className="text-sm text-gray-500 mt-1">
              {formData.content.length}/10000
            </p>
          </div>

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={loading}
              className="btn-primary disabled:opacity-50"
            >
              {loading ? '저장 중...' : isEditMode ? '수정하기' : '작성하기'}
            </button>
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="btn-secondary"
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
