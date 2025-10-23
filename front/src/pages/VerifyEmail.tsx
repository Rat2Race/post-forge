import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../lib/api';
import { Mail, CheckCircle2, XCircle, Loader2 } from 'lucide-react';

type VerificationStep = 'input' | 'sent' | 'verifying' | 'success' | 'error';

export default function VerifyEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [step, setStep] = useState<VerificationStep>('input');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [verifiedEmail, setVerifiedEmail] = useState('');
  const hasVerified = useRef(false);

  // URL에 token이 있으면 자동으로 인증 처리
  useEffect(() => {
    const token = searchParams.get('token');
    if (token && !hasVerified.current) {
      hasVerified.current = true;
      verifyEmailWithToken(token);
    }
  }, [searchParams]);

  const verifyEmailWithToken = async (token: string) => {
    setStep('verifying');
    try {
      const response = await authApi.verifyEmail(token);
      setVerifiedEmail(response.email);
      setStep('success');

      // localStorage에 인증된 이메일 저장
      localStorage.setItem('verifiedEmail', response.email);

      // 3초 후 회원가입 페이지로 이동
      setTimeout(() => {
        navigate('/register');
      }, 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || '이메일 인증에 실패했습니다.');
      setStep('error');
    }
  };

  const handleSendEmail = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setError('올바른 이메일 주소를 입력해주세요.');
      return;
    }

    try {
      setIsLoading(true);
      setError('');
      await authApi.sendEmailCode({ email });
      setStep('sent');
    } catch (err: any) {
      setError(err.response?.data?.message || '이메일 전송에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleContinueToRegister = () => {
    navigate('/register');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="max-w-md mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-r from-blue-600 to-purple-600 rounded-2xl mb-4 shadow-lg">
              <Mail className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">이메일 인증</h1>
            <p className="text-gray-600">회원가입을 위해 이메일을 인증해주세요</p>
          </div>

          {/* Form Card */}
          <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
            {/* Email Input Step */}
            {step === 'input' && (
              <>

                {error && (
                  <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                    {error}
                  </div>
                )}

                <form onSubmit={handleSendEmail} className="space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      이메일 주소
                    </label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                        placeholder="example@email.com"
                        required
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 focus:ring-4 focus:ring-blue-300 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
                  >
                    {isLoading ? '전송 중...' : '인증 메일 보내기'}
                  </button>
                </form>
              </>
            )}

            {/* Email Sent Step */}
            {step === 'sent' && (
              <div className="text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
                  <Mail className="w-8 h-8 text-blue-600" />
                </div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">인증 메일 발송 완료</h2>
                <p className="text-gray-600 mb-2">다음 이메일로 인증 메일을 발송했습니다:</p>
                <p className="text-blue-600 font-medium mb-6">{email}</p>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6 text-left">
                  <p className="text-sm text-gray-700 mb-2">다음 단계:</p>
                  <ol className="text-sm text-gray-600 space-y-1 list-decimal list-inside">
                    <li>이메일 받은 편지함을 확인하세요</li>
                    <li>인증 메일의 링크를 클릭하세요</li>
                    <li>자동으로 회원가입 페이지로 이동합니다</li>
                  </ol>
                </div>
                <button
                  onClick={() => setStep('input')}
                  className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                >
                  다른 이메일로 재시도
                </button>
              </div>
            )}

            {/* Verifying Step */}
            {step === 'verifying' && (
              <div className="text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 bg-blue-100 rounded-full mb-4">
                  <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
                </div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">이메일 인증 중...</h2>
                <p className="text-gray-600">잠시만 기다려주세요.</p>
              </div>
            )}

            {/* Success Step */}
            {step === 'success' && (
              <div className="text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
                  <CheckCircle2 className="w-8 h-8 text-green-600" />
                </div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">인증 완료!</h2>
                <p className="text-gray-600 mb-2">이메일 인증이 완료되었습니다.</p>
                {verifiedEmail && (
                  <p className="text-sm text-blue-600 font-medium mb-4">
                    {verifiedEmail}
                  </p>
                )}
                <p className="text-sm text-gray-500 mb-6">
                  잠시 후 회원가입 페이지로 자동 이동됩니다...
                </p>
                <button
                  onClick={handleContinueToRegister}
                  className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 focus:ring-4 focus:ring-blue-300 transition-all shadow-lg"
                >
                  바로 이동하기
                </button>
              </div>
            )}

            {/* Error Step */}
            {step === 'error' && (
              <div className="text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 bg-red-100 rounded-full mb-4">
                  <XCircle className="w-8 h-8 text-red-600" />
                </div>
                <h2 className="text-2xl font-bold text-gray-900 mb-2">인증 실패</h2>
                <p className="text-gray-600 mb-6">{error}</p>
                <button
                  onClick={() => {
                    setStep('input');
                    setError('');
                  }}
                  className="w-full bg-gray-600 text-white py-3 rounded-lg font-semibold hover:bg-gray-700 focus:ring-4 focus:ring-gray-300 transition-all"
                >
                  다시 시도하기
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
