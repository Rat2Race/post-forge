import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getArticle, updateArticle } from '../api/board';
import { useSyncTokenStore } from '../api/http';
import { Card, CardTitle } from '../components/Card';
import { Input, Textarea } from '../components/Input';
import Button from '../components/Button';
import { useToast } from '../context/ToastContext';
import { useLoading } from '../context/LoadingContext';

export default function EditArticlePage() {
  useSyncTokenStore();
  const { id } = useParams();
  const nav = useNavigate();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | null>(null);
  const toast = useToast();
  const { withLoading } = useLoading();

  useEffect(() => {
    (async () => {
      if (!id) return;
      try {
        const a = await withLoading(() => getArticle(Number(id)));
        setTitle(a.title);
        setContent(a.content);
      } catch (e: any) { setError(e.message); }
    })();
  }, [id]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!id) return;
    try {
      await withLoading(() => updateArticle(Number(id), { title, content }));
      toast.success('저장되었습니다');
      nav(`/articles/${id}`);
    } catch (e: any) { setError(e.message); toast.error(e.message || '수정 실패'); }
  };

  return (
    <div style={{ maxWidth: 720 }}>
      <Card>
        <CardTitle>글 수정</CardTitle>
        <form className="stack-v" onSubmit={onSubmit}>
          <Input label="제목" value={title} onChange={e => setTitle(e.target.value)} required />
          <Textarea label="내용" value={content} onChange={e => setContent(e.target.value)} rows={10} required />
          <Button type="submit">저장</Button>
          {error && <span className="label" style={{ color: 'var(--danger)' }}>{error}</span>}
        </form>
      </Card>
    </div>
  );
}
