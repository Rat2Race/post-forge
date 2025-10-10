import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { createComment, deleteArticle, getArticle } from '../api/board';
import type { ArticleDto } from '../api/types';
import { useAuth } from '../context/AuthContext';
import { useSyncTokenStore } from '../api/http';
import Button from '../components/Button';
import { Card, CardTitle } from '../components/Card';
import { useToast } from '../context/ToastContext';
import { useLoading } from '../context/LoadingContext';

export default function ArticleDetailPage() {
  useSyncTokenStore();
  const { id } = useParams();
  const { user } = useAuth();
  const [article, setArticle] = useState<ArticleDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const toast = useToast();
  const { withLoading } = useLoading();
  const nav = useNavigate();

  useEffect(() => {
    (async () => {
      if (!id) return;
      try {
        const a = await withLoading(() => getArticle(Number(id)));
        setArticle(a);
      } catch (e: any) { setError(e.message); toast.error(e.message || '조회 실패'); }
    })();
  }, [id]);

  const onDelete = async () => {
    if (!id) return;
    if (!confirm('삭제하시겠습니까?')) return;
    try {
      await withLoading(() => deleteArticle(Number(id)));
      toast.success('삭제되었습니다');
      nav('/articles');
    } catch (e: any) { setError(e.message); toast.error(e.message || '삭제 실패'); }
  };

  const onComment = async () => {
    if (!id) return;
    try {
      const cid = await withLoading(() => createComment(Number(id)));
      setMsg(`댓글 생성: ${cid}`);
      toast.success('댓글이 생성되었습니다');
    } catch (e: any) { setError(e.message); toast.error(e.message || '댓글 실패'); }
  };

  if (error) return <p style={{ color: 'var(--danger)' }}>{error}</p>;
  if (!article) return <p className="muted">불러오는 중...</p>;

  return (
    <div>
      <Card>
        <CardTitle>{article.title}</CardTitle>
        <div style={{ whiteSpace: 'pre-wrap', marginBottom: 12 }}>{article.content}</div>
        <div className="stack">
          {user && (
            <>
              <Link to={`/articles/${id}/edit`}><Button variant="ghost">수정</Button></Link>
              <Button variant="danger" onClick={onDelete}>삭제</Button>
              <Button variant="ghost" onClick={onComment}>댓글 추가</Button>
            </>
          )}
          <Link to="/articles"><Button variant="ghost">목록</Button></Link>
        </div>
        {msg && <p className="label" style={{ color: 'var(--success)' }}>{msg}</p>}
      </Card>
    </div>
  );
}
