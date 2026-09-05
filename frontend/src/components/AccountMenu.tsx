import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import type { EmojiName } from '../assets/emoji';
import { fetchBalance } from '../lib/billing';
import { AVATAR_EMOJI, fetchCurrentUser, getAuthToken, logout, type CurrentUser } from '../lib/auth';
import Emoji from './Emoji';

const PROVIDER_LABEL: Record<string, string> = {
  KAKAO: '카카오',
  GOOGLE: 'Google',
  NAVER: '네이버',
  DEV_BYPASS: '개발용',
};

interface MenuItem {
  to?: string;
  label: string;
  icon: EmojiName;
  disabled?: boolean;
}

// 드롭다운 항목이 위→아래로 순서대로 나타나는 시간차(stagger) 간격.
const STAGGER_STEP_MS = 28;
function staggerDelay(index: number) {
  return { animationDelay: `${index * STAGGER_STEP_MS}ms` };
}

const MENU_ITEMS: MenuItem[] = [
  { label: '세이프티', icon: 'shield', disabled: true },
  { to: '/my-saju', label: '내 사주', icon: 'crystalball' },
  { to: '/consultations', label: '내 상담', icon: 'speech' },
  { to: '/shop', label: '상점', icon: 'cart' },
  { to: '/rewards', label: '보상', icon: 'gift' },
  { to: '/payments', label: '결제내역', icon: 'card' },
  { to: '/settings', label: '설정', icon: 'gear' },
];

interface AccountMenuProps {
  /** 드롭다운으로 쓸 때, 항목을 누르면 드롭다운을 닫기 위한 콜백 */
  onNavigate?: () => void;
  /**
   * 헤더가 이미 불러와 둔 사용자 정보 — 넘겨주면 드롭다운을 열 때 "확인
   * 중..."이 뜨지 않고 바로 메뉴가 보인다(FoxBunny처럼 즉시 열림). 헤더 없이
   * 단독 페이지(마이페이지)로 쓸 때는 안 넘어오니 이 컴포넌트가 직접 조회한다.
   */
  initialUser?: CurrentUser | null;
}

// 마이페이지(하단 탭)와 헤더 계정 버튼(드롭다운) 둘 다 같은 메뉴 구성을
// 보여준다 — 참고 사이트(foxbunny.io/saju)의 계정 드롭다운 구조.
export default function AccountMenu({ onNavigate, initialUser }: AccountMenuProps) {
  const navigate = useNavigate();
  const [user, setUser] = useState<CurrentUser | null | undefined>(() =>
    initialUser !== undefined ? initialUser : getAuthToken() ? undefined : null,
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
    <div className="flex flex-col gap-3 p-3.5">
      <div
        className="animate-dropdown-item-in flex items-center gap-2.5"
        style={staggerDelay(0)}
      >
        <span className="flex h-10 w-10 items-center justify-center rounded-full bg-violet-950">
          <Emoji name={AVATAR_EMOJI[user.avatarKey]} className="h-5.5 w-5.5" />
        </span>
        <div className="flex flex-col">
          <span className="text-sm font-bold text-white">{user.nickname || '(닉네임 없음)'}</span>
          <span className="text-xs text-neutral-400">{PROVIDER_LABEL[user.provider] ?? user.provider} 로그인</span>
        </div>
      </div>

      <div
        className="animate-dropdown-item-in flex items-center justify-between rounded-2xl border border-violet-900/50 bg-gradient-to-br from-violet-950 to-neutral-900 px-3.5 py-3"
        style={staggerDelay(1)}
      >
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
        {MENU_ITEMS.map((item, i) =>
          item.disabled ? (
            <span
              key={item.label}
              className="animate-dropdown-item-in flex items-center justify-between px-3.5 py-2.5 text-sm text-neutral-600"
              style={staggerDelay(2 + i)}
            >
              <span className="flex items-center gap-2">
                <Emoji name={item.icon} className="h-4.5 w-4.5" />
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
              className="animate-dropdown-item-in flex items-center gap-2 px-3.5 py-2.5 text-sm font-medium text-neutral-100"
              style={staggerDelay(2 + i)}
            >
              <Emoji name={item.icon} className="h-4.5 w-4.5" />
              {item.label}
            </Link>
          ),
        )}
      </nav>

      {user.isAdmin && (
        <Link
          to="/admin"
          onClick={onNavigate}
          className="animate-dropdown-item-in rounded-full bg-violet-600 py-2.5 text-center text-sm font-semibold text-white"
          style={staggerDelay(2 + MENU_ITEMS.length)}
        >
          관리자 화면
        </Link>
      )}

      <button
        type="button"
        className="animate-dropdown-item-in flex items-center gap-2 rounded-2xl border border-neutral-800 px-3.5 py-2.5 text-sm font-semibold text-red-400"
        style={staggerDelay(2 + MENU_ITEMS.length + (user.isAdmin ? 1 : 0))}
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
        <Emoji name="door" className="h-4.5 w-4.5" />
        로그아웃
      </button>
    </div>
  );
}
