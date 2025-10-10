import { Link } from 'react-router-dom';
import Button from './Button';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  return (
    <header className="navbar">
      <div className="nav-left">
        <span className="brand">Post Forge</span>
        <Link to="/articles">게시글</Link>
        {user && <Link to="/profile">프로필</Link>}
      </div>
      <div className="nav-right">
        <Button variant="ghost" onClick={toggle} aria-label="테마 전환">
          {theme === 'light' ? '🌙' : '☀️'}
        </Button>
        {user ? (
          <Button variant="ghost" onClick={logout}>로그아웃</Button>
        ) : (
          <>
            <Link to="/login"><Button variant="ghost">로그인</Button></Link>
            <Link to="/signup"><Button>회원가입</Button></Link>
          </>
        )}
      </div>
    </header>
  );
}

