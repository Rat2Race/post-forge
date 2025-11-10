import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { signup } from '../api/auth';
import { UserPlus, Mail, Lock, User, CheckCircle2 } from 'lucide-react';
import { useState, useEffect } from 'react';
import Navigation from '../components/Navigation';

interface RegisterFormData {
  name: string;
  id: string;
  pw: string;
  email: string;
  nickname: string;
}

export default function Register() {
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [verifiedEmail, setVerifiedEmail] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
    setValue,
  } = useForm<RegisterFormData>();

  // 컴포넌트 마운트 시 인증된 이메일 가져오기
  useEffect(() => {
    const email = localStorage.getItem('verifiedEmail');
    if (email) {
      setVerifiedEmail(email);
      setValue('email', email);
    }
  }, [setValue]);

  // 페이지 벗어날 때 localStorage 정리
  useEffect(() => {
    const handleBeforeUnload = () => {
      // 새로고침이나 탭 닫기 시 정리
      localStorage.removeItem('verifiedEmail');
    };

    const handleVisibilityChange = () => {
      // 다른 페이지로 이동 시 정리
      if (document.hidden) {
        localStorage.removeItem('verifiedEmail');
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  const onSubmit = async (data: RegisterFormData) => {
    if (!verifiedEmail) {
      setError('이메일 인증이 필요합니다.');
      return;
    }

    try {
      setIsLoading(true);
      setError('');

      // 회원가입 요청 (인증된 이메일 포함)
      await signup({
        name: data.name,
        id: data.id,
        pw: data.pw,
        email: verifiedEmail,
        nickname: data.nickname,
      });

      // 회원가입 성공 후 localStorage 정리
      localStorage.removeItem('verifiedEmail');

      navigate('/login', { state: { message: '회원가입이 완료되었습니다. 로그인해주세요.' } });
    } catch (err: any) {
      setError(err.message || '회원가입에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <Navigation />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="max-w-md mx-auto">
          {/* Header */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-r from-blue-600 to-purple-600 rounded-2xl mb-4 shadow-lg">
              <UserPlus className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">회원가입</h1>
            <p className="text-gray-600">새로운 계정을 만들어보세요</p>
          </div>

          {/* Form Card */}
          <div className="bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
            {error && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                {error}
              </div>
            )}

            {verifiedEmail && (
              <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
                <p className="text-sm text-green-600 flex items-center gap-2">
                  <CheckCircle2 className="w-5 h-5" />
                  <span>이메일 인증이 완료되었습니다: <strong>{verifiedEmail}</strong></span>
                </p>
              </div>
            )}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Email Field - Read Only */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  이메일
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('email')}
                    type="email"
                    disabled
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg bg-gray-100 text-gray-600 cursor-not-allowed"
                    placeholder="example@email.com"
                  />
                </div>
              </div>

              {/* Name Field */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  이름
                </label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('name', {
                      required: '이름을 입력해주세요',
                      minLength: { value: 2, message: '이름은 최소 2자 이상이어야 합니다' },
                      maxLength: { value: 20, message: '이름은 최대 20자까지 가능합니다' },
                    })}
                    type="text"
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="홍길동"
                  />
                </div>
                {errors.name && (
                  <p className="mt-1 text-sm text-red-600">{errors.name.message}</p>
                )}
              </div>

              {/* ID Field */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  아이디
                </label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('id', {
                      required: '아이디를 입력해주세요',
                      minLength: { value: 4, message: '아이디는 최소 4자 이상이어야 합니다' },
                      maxLength: { value: 20, message: '아이디는 최대 20자까지 가능합니다' },
                      pattern: {
                        value: /^[a-zA-Z0-9]+$/,
                        message: '아이디는 영문자와 숫자만 사용 가능합니다',
                      },
                    })}
                    type="text"
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="영문/숫자 조합"
                  />
                </div>
                {errors.id && (
                  <p className="mt-1 text-sm text-red-600">{errors.id.message}</p>
                )}
              </div>

              {/* Password Field */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  비밀번호
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('pw', {
                      required: '비밀번호를 입력해주세요',
                      minLength: { value: 8, message: '비밀번호는 최소 8자 이상이어야 합니다' },
                      pattern: {
                        value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/,
                        message: '대소문자, 숫자, 특수문자를 포함해야 합니다',
                      },
                    })}
                    type="password"
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="대소문자, 숫자, 특수문자 포함"
                  />
                </div>
                {errors.pw && (
                  <p className="mt-1 text-sm text-red-600">{errors.pw.message}</p>
                )}
              </div>

              {/* Nickname Field */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  닉네임
                </label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    {...register('nickname', {
                      required: '닉네임을 입력해주세요',
                      minLength: { value: 2, message: '닉네임은 최소 2자 이상이어야 합니다' },
                      maxLength: { value: 20, message: '닉네임은 최대 20자까지 가능합니다' },
                      pattern: {
                        value: /^[가-힣a-zA-Z0-9]+$/,
                        message: '닉네임은 한글, 영문, 숫자만 사용 가능합니다',
                      },
                    })}
                    type="text"
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                    placeholder="한글/영문/숫자 조합"
                  />
                </div>
                {errors.nickname && (
                  <p className="mt-1 text-sm text-red-600">{errors.nickname.message}</p>
                )}
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 focus:ring-4 focus:ring-blue-300 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg"
              >
                {isLoading ? '가입 중...' : '회원가입'}
              </button>
            </form>

            {/* Login Link */}
            <div className="mt-6 text-center">
              <p className="text-gray-600">
                이미 계정이 있으신가요?{' '}
                <Link
                  to="/login"
                  className="text-blue-600 hover:text-blue-700 font-semibold transition-colors"
                >
                  로그인
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
