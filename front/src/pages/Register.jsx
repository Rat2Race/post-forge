import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { UserPlus, CheckCircle } from 'lucide-react';

export default function Register() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const register = useAuthStore((state) => state.register);

  const [formData, setFormData] = useState({
    name: '',
    id: '',
    pw: '',
    pwConfirm: '',
    email: '',
    nickname: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      // 토큰이 없으면 이메일 인증 페이지로 리다이렉트
      navigate('/email-verification', { replace: true });
    }
  }, [searchParams, navigate]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    if (errors[e.target.name]) {
      setErrors({
        ...errors,
        [e.target.name]: '',
      });
    }
  };

  const validate = () => {
    const newErrors = {};

    if (formData.name.length < 2 || formData.name.length > 20) {
      newErrors.name = '이름은 2-20자 사이여야 합니다.';
    }
    if (!/^[가-힣a-zA-Z]+$/.test(formData.name)) {
      newErrors.name = '이름은 한글 또는 영문만 가능합니다.';
    }

    if (formData.id.length < 4 || formData.id.length > 20) {
      newErrors.id = '아이디는 4-20자 사이여야 합니다.';
    }
    if (!/^[a-zA-Z0-9]+$/.test(formData.id)) {
      newErrors.id = '아이디는 영문과 숫자만 가능합니다.';
    }

    if (formData.pw.length < 8) {
      newErrors.pw = '비밀번호는 8자 이상이어야 합니다.';
    }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/.test(formData.pw)) {
      newErrors.pw = '비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다.';
    }

    if (formData.pw !== formData.pwConfirm) {
      newErrors.pwConfirm = '비밀번호가 일치하지 않습니다.';
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = '올바른 이메일 형식이 아닙니다.';
    }

    if (formData.nickname.length < 2 || formData.nickname.length > 20) {
      newErrors.nickname = '닉네임은 2-20자 사이여야 합니다.';
    }
    if (!/^[가-힣a-zA-Z0-9]+$/.test(formData.nickname)) {
      newErrors.nickname = '닉네임은 한글, 영문, 숫자만 가능합니다.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validate()) {
      return;
    }

    setLoading(true);

    const result = await register({
      name: formData.name,
      id: formData.id,
      pw: formData.pw,
      email: formData.email,
      nickname: formData.nickname,
    });

    if (result.success) {
      alert('회원가입이 완료되었습니다. 로그인해주세요.');
      navigate('/login');
    } else {
      setErrors({ submit: result.error });
    }

    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-carrot-light to-orange-50 flex items-start justify-center px-4 sm:px-6 lg:px-8 pt-24">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-carrot rounded-2xl mb-4 shadow-lg">
            <UserPlus className="h-8 w-8 text-white" />
          </div>
          <h2 className="text-3xl font-extrabold text-gray-900">
            회원가입
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            이메일 인증 완료! 정보를 입력해주세요.
          </p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleSubmit} className="space-y-5">
            {errors.submit && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
                {errors.submit}
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="name" className="block text-sm font-semibold text-gray-700 mb-2">
                  이름
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  required
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                  placeholder="홍길동"
                  value={formData.name}
                  onChange={handleChange}
                />
                {errors.name && <p className="mt-1 text-xs text-red-600">{errors.name}</p>}
              </div>

              <div>
                <label htmlFor="nickname" className="block text-sm font-semibold text-gray-700 mb-2">
                  닉네임
                </label>
                <input
                  id="nickname"
                  name="nickname"
                  type="text"
                  required
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                  placeholder="닉네임"
                  value={formData.nickname}
                  onChange={handleChange}
                />
                {errors.nickname && <p className="mt-1 text-xs text-red-600">{errors.nickname}</p>}
              </div>
            </div>

            <div>
              <label htmlFor="id" className="block text-sm font-semibold text-gray-700 mb-2">
                아이디
              </label>
              <input
                id="id"
                name="id"
                type="text"
                required
                className="w-full px-4 py-2.5 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                placeholder="영문, 숫자 4-20자"
                value={formData.id}
                onChange={handleChange}
              />
              {errors.id && <p className="mt-1 text-xs text-red-600">{errors.id}</p>}
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-semibold text-gray-700 mb-2">
                이메일
              </label>
              <div className="relative">
                <input
                  id="email"
                  name="email"
                  type="email"
                  required
                  className="w-full px-4 py-2.5 pr-10 border border-green-300 bg-green-50 rounded-xl focus:outline-none"
                  placeholder="example@email.com"
                  value={formData.email}
                  onChange={handleChange}
                />
                <CheckCircle className="absolute right-3 top-1/2 -translate-y-1/2 h-5 w-5 text-green-600" />
              </div>
              <p className="mt-1 text-xs text-green-600">인증 완료</p>
            </div>

            <div>
              <label htmlFor="pw" className="block text-sm font-semibold text-gray-700 mb-2">
                비밀번호
              </label>
              <input
                id="pw"
                name="pw"
                type="password"
                required
                className="w-full px-4 py-2.5 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                placeholder="대소문자, 숫자, 특수문자 포함 8자 이상"
                value={formData.pw}
                onChange={handleChange}
              />
              {errors.pw && <p className="mt-1 text-xs text-red-600">{errors.pw}</p>}
            </div>

            <div>
              <label htmlFor="pwConfirm" className="block text-sm font-semibold text-gray-700 mb-2">
                비밀번호 확인
              </label>
              <input
                id="pwConfirm"
                name="pwConfirm"
                type="password"
                required
                className="w-full px-4 py-2.5 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                placeholder="비밀번호를 다시 입력하세요"
                value={formData.pwConfirm}
                onChange={handleChange}
              />
              {errors.pwConfirm && <p className="mt-1 text-xs text-red-600">{errors.pwConfirm}</p>}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-carrot hover:bg-carrot-dark text-white font-semibold py-3 px-4 rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl mt-6"
            >
              {loading ? '가입 중...' : '회원가입 완료'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <span className="text-sm text-gray-600">이미 계정이 있으신가요?</span>
            <Link
              to="/login"
              className="ml-2 text-sm font-semibold text-carrot hover:text-carrot-dark"
            >
              로그인
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
