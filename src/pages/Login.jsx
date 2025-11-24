import {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useAuthStore} from '../store/authStore';
import {LogIn, User, Lock} from 'lucide-react';

export default function Login() {
    const navigate = useNavigate();
    const login = useAuthStore((state) => state.login);
    const [formData, setFormData] = useState({
        id: '',
        pw: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value,
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        const result = await login(formData.id, formData.pw);

        if (result.success) {
            navigate('/');
        } else {
            setError(result.error);
        }

        setLoading(false);
    };

    return (
        <div
            className="min-h-screen bg-gradient-to-br from-carrot-light to-orange-50 flex items-start justify-center px-4 sm:px-6 lg:px-8 pt-24">
            <div className="w-full max-w-md">
                <div className="text-center mb-8">
                    <div
                        className="inline-flex items-center justify-center w-16 h-16 bg-carrot rounded-2xl mb-4 shadow-lg">
                        <span className="text-3xl font-black text-white">P</span>
                    </div>
                    <h2 className="text-3xl font-extrabold text-gray-900">
                        Post Forge
                    </h2>
                    <p className="mt-2 text-sm text-gray-600">
                        우리 동네 중고거래 커뮤니티
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
                            <label htmlFor="id" className="block text-sm font-semibold text-gray-700 mb-2">
                                아이디
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <User className="h-5 w-5 text-gray-400"/>
                                </div>
                                <input
                                    id="id"
                                    name="id"
                                    type="text"
                                    required
                                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                                    placeholder="아이디를 입력하세요"
                                    value={formData.id}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>

                        <div>
                            <label htmlFor="pw" className="block text-sm font-semibold text-gray-700 mb-2">
                                비밀번호
                            </label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="h-5 w-5 text-gray-400"/>
                                </div>
                                <input
                                    id="pw"
                                    name="pw"
                                    type="password"
                                    required
                                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-carrot focus:border-transparent transition-all"
                                    placeholder="비밀번호를 입력하세요"
                                    value={formData.pw}
                                    onChange={handleChange}
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-carrot hover:bg-carrot-dark text-white font-semibold py-3 px-4 rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl flex items-center justify-center gap-2"
                        >
                            {loading ? (
                                <span className="flex items-center gap-2">
                  <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none"
                       viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                            strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  로그인 중...
                </span>
                            ) : (
                                <>
                                    <LogIn className="h-5 w-5"/>
                                    로그인
                                </>
                            )}
                        </button>
                    </form>

                    <div className="mt-6 text-center">
                        <span className="text-sm text-gray-600">계정이 없으신가요?</span>
                        <Link
                            to="/email-verification"
                            className="ml-2 text-sm font-semibold text-carrot hover:text-carrot-dark"
                        >
                            회원가입
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
