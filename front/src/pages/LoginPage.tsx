import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useSyncTokenStore } from '../api/http';
import { Card, CardTitle } from '../components/Card';
import { Input } from '../components/Input';
import Button from '../components/Button';
import { useToast } from '../context/ToastContext';
import { useLoading } from '../context/LoadingContext';
import PasswordInput from '../components/PasswordInput';

export default function LoginPage() {
  useSyncTokenStore();
  const { login } = useAuth();
  const nav = useNavigate();
  const [id, setId] = useState('');
  const [pw, setPw] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();
  const { withLoading } = useLoading();

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true); setError(null);
    try {
      await withLoading(() => login(id, pw));
      toast.success('로그인 완료');
      nav('/articles');
    } catch (err: any) {
      setError(err.message || '로그인 실패');
      toast.error(err.message || '로그인 실패');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-center">
      <div className="form-card">
        <Card>
          <div className="stack-v">
            <div className="text-center">
              <h1 className="form-title">로그인</h1>
              <p className="form-subtitle">계정으로 계속 진행하세요</p>
            </div>
            <form className="stack-v" onSubmit={onSubmit}>
              <Input label="아이디" placeholder="your_id" value={id} onChange={e => setId(e.target.value)} required />
              <PasswordInput label="비밀번호" placeholder="비밀번호" value={pw} onChange={e => setPw(e.target.value)} required />
              <div className="spacer-sm" />
              <Button block disabled={loading} type="submit">{loading ? '처리중...' : '로그인'}</Button>
            </form>
          </div>
        </Card>
      </div>
    </div>
  );
}
