import {useState} from 'react';
import {Link} from 'react-router-dom';
import {authAPI} from '../api/auth';
import {Mail, CheckCircle} from 'lucide-react';

export default function EmailVerification() {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        // 이메일 유효성 검증
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            setError('올바른 이메일 형식이 아닙니다.');
            return;
        }

        setLoading(true);

        try {
            await authAPI.sendEmail(email);
            setSuccess(true);
        } catch (err) {
            setError(err.response?.data?.message || '이메일 발송에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return (
            <div
                className="min-h-screen flex items-start justify-center bg-gradient-to-br from-carrot-light to-orange-50 px-4 sm:px-6 lg:px-8 pt-24">
                <div className="max-w-md w-full">
                    <div className="bg-white rounded-2xl shadow-xl p-8 text-center">
                        <div
                            className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-6">
                            <CheckCircle className="h-10 w-10 text-green-600"/>
                        </div>
                        <h2 className="text-2xl font-bold text-gray-900 mb-3">
                            이메일을 확인하세요!
                        </h2>
                        <p className="text-gray-600 mb-2">
                            <strong className="text-gray-900">{email}</strong>로
                        </p>
                        <p className="text-gray-600 mb-6">
                            인증 메일을 발송했습니다.
                        </p>
                        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                            <p className="text-sm text-blue-800">
                                이메일의 인증 링크를 클릭하면 회원가입을 완료할 수 있습니다.
                            </p>
                        </div>
                        <Link
                            to="/login"
                            className="text-carrot hover:text-carrot-dark font-medium"
                        >
                            로그인 페이지로 돌아가기
                        </Link>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div
            className="min-h-screen flex items-start justify-center bg-gradient-to-br from-carrot-light to-orange-50 px-4 sm:px-6 lg:px-8 pt-24">
            <div className="max-w-md w-full">
                <div className="text-center mb-6">
                    <div
                        className="inline-flex items-center justify-center w-16 h-16 bg-carrot rounded-2xl mb-4 shadow-lg">
                        <Mail className="h-8 w-8 text-white"/>
                    </div>
                    <h2 className="text-3xl font-extrabold text-gray-900">
                        Verify Email
                    </h2>
                    <p className="mt-2 text-sm text-gray-600">
                        PostForge와 함께 시작하세요
                    </p>
                </div>

                <div className="bg-white rounded-2xl shadow-xl p-8">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {error && (
                            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">
                                {error}
                            </div>
                        )}

                        <div>
                            <label htmlFor="email" className="block text-sm font-semibold text-gray-700 mb-2">
                                이메일 주소
                            </label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                required
                                className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                                placeholder="example@email.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                            <p className="mt-2 text-xs text-gray-500">
                                입력하신 이메일로 인증 링크를 보내드립니다.
                            </p>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-carrot hover:bg-carrot-dark text-white font-semibold py-3 px-4 rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl"
                        >
                            {loading ? (
                                <span className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg"
                       fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                            strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  발송 중...
                </span>
                            ) : (
                                '인증 메일 받기'
                            )}
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
