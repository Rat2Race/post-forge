import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createArticle } from '../api/board';
import { useSyncTokenStore } from '../api/http';
import { Card, CardTitle } from '../components/Card';
import { Input, Textarea } from '../components/Input';
import Button from '../components/Button';
import { useToast } from '../context/ToastContext';
import { useLoading } from '../context/LoadingContext';

export default function CreateArticlePage() {
  useSyncTokenStore();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | null>(null);
  const nav = useNavigate();
  const toast = useToast();
  const { withLoading } = useLoading();

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      const id = await withLoading(() => createArticle({ title, content }));
      toast.success('작성 완료');
      nav(`/articles/${id}`);
    } catch (e: any) { setError(e.message); toast.error(e.message || '작성 실패'); }
  };

  return (
    <div style={{ maxWidth: 720 }}>
      <Card>
        <CardTitle>새 글 작성</CardTitle>
        <form className="stack-v" onSubmit={onSubmit}>
          <Input label="제목" value={title} onChange={e => setTitle(e.target.value)} required />
          <Textarea label="내용" value={content} onChange={e => setContent(e.target.value)} rows={10} required />
          <Button type="submit">등록</Button>
          {error && <span className="label" style={{ color: 'var(--danger)' }}>{error}</span>}
        </form>
      </Card>
    </div>
  );
}
