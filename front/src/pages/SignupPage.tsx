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

export default function SignupPage() {
  useSyncTokenStore();
  const { signup } = useAuth();
  const nav = useNavigate();
  const [name, setName] = useState('');
  const [id, setId] = useState('');
  const [pw, setPw] = useState('');
  const [msg, setMsg] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();
  const { withLoading } = useLoading();

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true); setError(null); setMsg(null);
    try {
      await withLoading(() => signup(name, id, pw));
      toast.success('회원가입 완료! 로그인해주세요.');
      setTimeout(() => nav('/login'), 600);
    } catch (err: any) {
      setError(err.message || '회원가입 실패');
      toast.error(err.message || '회원가입 실패');
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
              <h1 className="form-title">회원가입</h1>
              <p className="form-subtitle">정보를 입력하고 시작해 보세요</p>
            </div>
            <form className="stack-v" onSubmit={onSubmit}>
              <Input label="이름" placeholder="홍길동" value={name} onChange={e => setName(e.target.value)} required />
              <Input label="아이디" placeholder="your_id" value={id} onChange={e => setId(e.target.value)} required minLength={4} />
              <PasswordInput label="비밀번호" placeholder="영문 대/소문자, 숫자, 특수문자 포함" value={pw} onChange={e => setPw(e.target.value)} required minLength={8} />
              <div className="spacer-sm" />
              <Button block disabled={loading} type="submit">{loading ? '처리중...' : '가입하기'}</Button>
              {error && <span className="error-text">{error}</span>}
            </form>
          </div>
        </Card>
      </div>
    </div>
  );
}
