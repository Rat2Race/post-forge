import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authAPI } from '../api/auth';
import { CheckCircle, XCircle, Loader } from 'lucide-react';

export default function VerifyEmailCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading'); // loading, success, error
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setStatus('error');
      setMessage('유효하지 않은 인증 링크입니다.');
      return;
    }

    verifyEmail(token);
  }, [searchParams]);

  const verifyEmail = async (token) => {
    try {
      await authAPI.verifyEmail(token);
      setStatus('success');
      setMessage('이메일 인증이 완료되었습니다!');

      // 2초 후 회원가입 페이지로 이동 (token을 쿼리 파라미터로 전달)
      setTimeout(() => {
        navigate(`/register?token=${token}`);
      }, 2000);
    } catch (error) {
      setStatus('error');
      setMessage(
        error.response?.data?.message ||
        '이메일 인증에 실패했습니다. 링크가 만료되었거나 이미 사용된 링크일 수 있습니다.'
      );
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-carrot-light to-orange-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        <div className="bg-white rounded-2xl shadow-xl p-8 text-center">
          {status === 'loading' && (
            <>
              <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-blue-100 mb-6">
                <Loader className="h-10 w-10 text-blue-600 animate-spin" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-3">
                이메일 인증 중...
              </h2>
              <p className="text-gray-600">
                잠시만 기다려주세요.
              </p>
            </>
          )}

          {status === 'success' && (
            <>
              <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-6">
                <CheckCircle className="h-10 w-10 text-green-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-3">
                인증 완료!
              </h2>
              <p className="text-gray-600 mb-4">
                {message}
              </p>
              <p className="text-sm text-gray-500">
                회원가입 페이지로 이동합니다...
              </p>
            </>
          )}

          {status === 'error' && (
            <>
              <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-100 mb-6">
                <XCircle className="h-10 w-10 text-red-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-3">
                인증 실패
              </h2>
              <p className="text-gray-600 mb-6">
                {message}
              </p>
              <button
                onClick={() => navigate('/email-verification')}
                className="btn-primary"
              >
                다시 시도하기
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
