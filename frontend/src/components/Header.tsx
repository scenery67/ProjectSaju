import { Mail, Search, User } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useUser } from '../contexts/useUser';
import { AVATAR_EMOJI, getAuthToken } from '../lib/auth';
import { fetchUnreadCount } from '../lib/notifications';
import AccountMenu from './AccountMenu';
import Emoji from './Emoji';

// 참고 사이트(foxbunny.io/saju)처럼 로고(좌) + 알림/계정(우) 구조 —
// 계정 버튼을 누르면 바로 마이페이지로 가지 않고 드롭다운 메뉴가 뜬다.
export default function Header() {
  const loggedIn = Boolean(getAuthToken());
  const location = useLocation();
  const { user } = useUser();
  const [unreadCount, setUnreadCount] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // 알림함(/notifications)에서 읽음 처리를 하고 돌아와도 배지 숫자가 그대로
  // 남아있던 문제 — 로그인 시점 한 번이 아니라 경로가 바뀔 때마다 다시 조회한다.
  useEffect(() => {
    if (!loggedIn) return;
    fetchUnreadCount().then(setUnreadCount);
  }, [loggedIn, location.pathname]);

  useEffect(() => {
    if (!menuOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-20 flex items-center justify-between border-b border-slate-800 bg-slate-900/90 px-4 py-3.5 backdrop-blur">
      <Link to="/" className="text-lg font-extrabold tracking-tight text-white">
        사주 서비스
      </Link>
      <div className="flex items-center gap-2">
        <Link
          to="/search"
          aria-label="검색"
          className="rounded-full p-1.5 text-slate-300 transition-colors hover:bg-slate-800 hover:text-white active:bg-slate-700"
        >
          <Search className="h-5 w-5" strokeWidth={2} />
        </Link>
        {loggedIn && (
          <Link
            to="/notifications"
            aria-label="알림"
            className="relative rounded-full p-1.5 text-slate-300 transition-colors hover:bg-slate-800 hover:text-white active:bg-slate-700"
          >
            <Mail className="h-5 w-5" strokeWidth={2} />
            {unreadCount > 0 && (
              <span className="absolute right-0 top-0 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[9px] font-bold text-white">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Link>
        )}
        {loggedIn ? (
          <div className="relative" ref={menuRef}>
            <button
              type="button"
              aria-label="계정 메뉴"
              onClick={() => setMenuOpen((v) => !v)}
              className="flex h-8 w-8 items-center justify-center overflow-hidden rounded-full border border-violet-700 bg-violet-950 transition-colors hover:bg-violet-900 active:bg-violet-800"
            >
              {user ? (
                <Emoji name={AVATAR_EMOJI[user.avatarKey]} className="h-4.5 w-4.5" />
              ) : (
                <User className="h-4 w-4 text-slate-300" strokeWidth={2} />
              )}
            </button>
            {menuOpen && (
              <div className="animate-dropdown-in absolute right-0 top-11 w-72 max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl border border-slate-800 bg-slate-900 shadow-xl">
                <AccountMenu onNavigate={() => setMenuOpen(false)} />
              </div>
            )}
          </div>
        ) : (
          <Link
            to="/login"
            className="rounded-full bg-violet-600 px-3.5 py-1.5 text-xs font-bold text-white"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  );
}
