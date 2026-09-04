import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { AVATAR_EMOJI, fetchCurrentUser, getAuthToken, type CurrentUser } from '../lib/auth';
import { fetchUnreadCount } from '../lib/notifications';
import AccountMenu from './AccountMenu';

// 참고 사이트(foxbunny.io/saju)처럼 로고(좌) + 알림/계정(우) 구조 —
// 계정 버튼을 누르면 바로 마이페이지로 가지 않고 드롭다운 메뉴가 뜬다.
export default function Header() {
  const loggedIn = Boolean(getAuthToken());
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!loggedIn) return;
    fetchCurrentUser().then(setUser);
    fetchUnreadCount().then(setUnreadCount);
  }, [loggedIn]);

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
    <header className="sticky top-0 z-20 flex items-center justify-between border-b border-neutral-800 bg-neutral-900/90 px-4 py-3.5 backdrop-blur">
      <Link to="/" className="text-lg font-extrabold tracking-tight text-white">
        사주 서비스
      </Link>
      <div className="flex items-center gap-3">
        <Link to="/" aria-label="홈" className="text-lg text-neutral-400">
          🔍
        </Link>
        {loggedIn && (
          <Link to="/notifications" aria-label="알림" className="relative text-lg text-neutral-400">
            ✉️
            {unreadCount > 0 && (
              <span className="absolute -right-1.5 -top-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[9px] font-bold text-white">
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
              className="flex h-8 w-8 items-center justify-center rounded-full border border-violet-700 bg-violet-950 text-base"
            >
              {user ? AVATAR_EMOJI[user.avatarKey] : '👤'}
            </button>
            {menuOpen && (
              <div className="absolute right-0 top-11 w-72 max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl border border-neutral-800 bg-neutral-900 shadow-xl">
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
