import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchBalance } from '../lib/billing';
import { AVATAR_EMOJI, fetchCurrentUser, getAuthToken, logout, type CurrentUser } from '../lib/auth';

const PROVIDER_LABEL: Record<string, string> = {
  KAKAO: '카카오',
  GOOGLE: 'Google',
  NAVER: '네이버',
  DEV_BYPASS: '개발용',
};

interface MenuItem {
  to?: string;
  label: string;
  icon: string;
  disabled?: boolean;
}

const MENU_ITEMS: MenuItem[] = [
  { label: '세이프티', icon: '🛡️', disabled: true },
  { to: '/my-saju', label: '내 사주', icon: '🔮' },
  { to: '/consultations', label: '내 상담', icon: '💬' },
  { to: '/shop', label: '상점', icon: '🛒' },
  { to: '/rewards', label: '보상', icon: '🎁' },
  { to: '/payments', label: '결제내역', icon: '💳' },
  { to: '/settings', label: '설정', icon: '⚙️' },
];

interface AccountMenuProps {
  /** 드롭다운으로 쓸 때, 항목을 누르면 드롭다운을 닫기 위한 콜백 */
  onNavigate?: () => void;
}

// 마이페이지(하단 탭)와 헤더 계정 버튼(드롭다운) 둘 다 같은 메뉴 구성을
// 보여준다 — 참고 사이트(foxbunny.io/saju)의 계정 드롭다운 구조.
export default function AccountMenu({ onNavigate }: AccountMenuProps) {
  const navigate = useNavigate();
  const [user, setUser] = useState<CurrentUser | null | undefined>(() =>
    getAuthToken() ? undefined : null,
  );
  const [creditBalance, setCreditBalance] = useState<number | null>(null);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchCurrentUser().then(setUser);
  }, []);

  useEffect(() => {
    if (!user) return;
    fetchBalance().then((b) => setCreditBalance(b?.creditBalance ?? null));
  }, [user]);

  if (user === undefined) {
    return <p className="p-4 text-sm text-neutral-400">확인 중...</p>;
  }

  if (user === null) {
    return (
      <div className="flex flex-col items-center gap-3 p-6 text-center">
        <p className="text-sm text-neutral-400">로그인하면 크레딧, 상담 기록을 계정에 저장할 수 있어요.</p>
        <Link
          to="/login"
          onClick={onNavigate}
          className="rounded-full bg-violet-600 px-6 py-3 text-sm font-bold text-white"
        >
          로그인하기
        </Link>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <div className="flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-full bg-violet-950 text-2xl">
          {AVATAR_EMOJI[user.avatarKey]}
        </span>
        <div className="flex flex-col">
          <span className="text-sm font-bold text-white">{user.nickname || '(닉네임 없음)'}</span>
          <span className="text-xs text-neutral-400">{PROVIDER_LABEL[user.provider] ?? user.provider} 로그인</span>
        </div>
      </div>

      <div className="flex items-center justify-between rounded-2xl border border-violet-900/50 bg-gradient-to-br from-violet-950 to-neutral-900 px-4 py-3.5">
        <div className="flex flex-col">
          <span className="text-[11px] text-violet-300">보유 크레딧</span>
          <span className="text-lg font-bold text-white">
            {creditBalance === null ? '—' : `${creditBalance.toLocaleString('ko-KR')} 크레딧`}
          </span>
        </div>
        <Link
          to="/shop"
          onClick={onNavigate}
          className="rounded-full bg-violet-500 px-4 py-2 text-xs font-bold text-white"
        >
          충전하기
        </Link>
      </div>

      <nav className="flex flex-col divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
        {MENU_ITEMS.map((item) =>
          item.disabled ? (
            <span
              key={item.label}
              className="flex items-center justify-between px-4 py-3 text-sm text-neutral-600"
            >
              <span className="flex items-center gap-2">
                <span className="text-base">{item.icon}</span>
                {item.label}
              </span>
              <span className="rounded-full bg-neutral-800 px-2 py-0.5 text-[10px] font-semibold text-neutral-500">
                준비중
              </span>
            </span>
          ) : (
            <Link
              key={item.to}
              to={item.to!}
              onClick={onNavigate}
              className="flex items-center gap-2 px-4 py-3 text-sm font-medium text-neutral-100"
            >
              <span className="text-base">{item.icon}</span>
              {item.label}
            </Link>
          ),
        )}
      </nav>

      {user.isAdmin && (
        <Link
          to="/admin"
          onClick={onNavigate}
          className="rounded-full bg-violet-600 py-3 text-center text-sm font-semibold text-white"
        >
          관리자 화면
        </Link>
      )}

      <button
        type="button"
        className="flex items-center gap-2 rounded-2xl border border-neutral-800 px-4 py-3 text-sm font-semibold text-red-400"
        onClick={() => {
          void logout();
          setUser(null);
          setCreditBalance(null);
          onNavigate?.();
          // 헤더의 계정 버튼은 로그인 상태를 나타내는 상태가 따로 없어
          // 경로가 바뀌어야(App의 useLocation 재렌더) 로그아웃이 반영된다.
          navigate('/');
        }}
      >
        <span>🚪</span>
        로그아웃
      </button>
    </div>
  );
}
