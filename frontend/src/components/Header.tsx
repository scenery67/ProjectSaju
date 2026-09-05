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
    <header className="sticky top-0 z-20 flex items-center justify-between border-b border-neutral-800 bg-neutral-900/90 px-4 py-2.5 backdrop-blur">
      <Link to="/" className="text-base font-extrabold tracking-tight text-white">
        사주 서비스
      </Link>
      <div className="flex items-center gap-2.5">
        <Link to="/" aria-label="홈" className="text-neutral-400">
          <Emoji name="search" className="h-4.5 w-4.5" />
        </Link>
        {loggedIn && (
          <Link to="/notifications" aria-label="알림" className="relative text-neutral-400">
            <Emoji name="mail" className="h-4.5 w-4.5" />
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
              className="flex h-7 w-7 items-center justify-center rounded-full border border-violet-700 bg-violet-950"
            >
              <Emoji name={user ? AVATAR_EMOJI[user.avatarKey] : 'person'} className="h-4 w-4" />
            </button>
            {menuOpen && (
              <div className="animate-dropdown-in absolute right-0 top-10 w-72 max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl border border-neutral-800 bg-neutral-900 shadow-xl">
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
