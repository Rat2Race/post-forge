import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';

export default function VerifyEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');
  const [verifiedEmail, setVerifiedEmail] = useState('');
  const hasVerified = useRef(false); // 중복 실행 방지

  useEffect(() => {
    const verifyEmail = async () => {
      // 이미 실행했으면 스킵 (StrictMode 중복 실행 방지)
      if (hasVerified.current) return;
      hasVerified.current = true;

      const token = searchParams.get('token');

      if (!token) {
        setStatus('error');
        setMessage('유효하지 않은 인증 링크입니다.');
        return;
      }

      try {
        const response = await api.get(`/api/auth/email/verify?token=${token}`);
        setStatus('success');
        setMessage(response.data.message || '이메일 인증이 완료되었습니다!');
        setVerifiedEmail(response.data.email);

        // localStorage에 인증된 이메일 저장 (회원가입 폼에서 사용)
        localStorage.setItem('verifiedEmail', response.data.email);

        // 3초 후 자동으로 회원가입 페이지로 이동
        setTimeout(() => {
          navigate('/register');
        }, 3000);
      } catch (err: any) {
        setStatus('error');
        setMessage(err.response?.data?.message || '이메일 인증에 실패했습니다.');
      }
    };

    verifyEmail();
  }, [searchParams]);

  const handleContinueRegistration = () => {
    navigate('/register');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 flex items-center justify-center">
      <div className="max-w-md w-full mx-4">
        <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
          {/* Loading State */}
          {status === 'loading' && (
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
                <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
              </div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">이메일 인증 중...</h1>
              <p className="text-gray-600">잠시만 기다려주세요.</p>
            </div>
          )}

          {/* Success State */}
          {status === 'success' && (
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
                <CheckCircle2 className="w-8 h-8 text-green-600" />
              </div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">인증 완료!</h1>
              <p className="text-gray-600 mb-2">{message}</p>
              {verifiedEmail && (
                <p className="text-sm text-blue-600 font-medium mb-4">
                  {verifiedEmail}
                </p>
              )}
              <p className="text-sm text-gray-500 mb-6">
                잠시 후 회원가입 페이지로 자동 이동됩니다...
              </p>
              <button
                onClick={handleContinueRegistration}
                className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 focus:ring-4 focus:ring-blue-300 transition-all shadow-lg"
              >
                바로 이동하기
              </button>
            </div>
          )}

          {/* Error State */}
          {status === 'error' && (
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-red-100 rounded-full mb-4">
                <XCircle className="w-8 h-8 text-red-600" />
              </div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">인증 실패</h1>
              <p className="text-gray-600 mb-6">{message}</p>
              <button
                onClick={() => navigate('/register')}
                className="w-full bg-gray-600 text-white py-3 rounded-lg font-semibold hover:bg-gray-700 focus:ring-4 focus:ring-gray-300 transition-all"
              >
                회원가입 페이지로 돌아가기
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
