import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { listArticles } from '../api/board';
import type { ArticleDto } from '../api/types';
import { useSyncTokenStore } from '../api/http';
import { useAuth } from '../context/AuthContext';
import Button from '../components/Button';
import { Card } from '../components/Card';
import { useToast } from '../context/ToastContext';
import { useLoading } from '../context/LoadingContext';

export default function ArticlesPage() {
  useSyncTokenStore();
  const { user } = useAuth();
  const [items, setItems] = useState<ArticleDto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const nav = useNavigate();
  const toast = useToast();
  const { withLoading } = useLoading();

  useEffect(() => {
    (async () => {
      try {
        const list = await withLoading(() => listArticles());
        setItems(list);
      } catch (e: any) { setError(e.message); toast.error(e.message || '목록 조회 실패'); }
    })();
  }, []);

  return (
    <div className="stack-v">
      <div className="space-between">
        <h2>게시글 목록</h2>
        {user && <Button onClick={() => nav('/articles/new')}>새 글</Button>}
      </div>
      {error && <p style={{ color: 'var(--danger)' }}>{error}</p>}
      <div className="stack-v">
        {items.map(a => (
          <Card key={a.id}>
            <div className="space-between">
              <Link to={`/articles/${a.id}`}>{a.title}</Link>
              <span className="muted">#{a.id}</span>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
