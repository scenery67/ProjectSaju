import {
  CreditCard,
  Gift,
  LogOut,
  MessageCircle,
  Settings,
  Shield,
  ShoppingCart,
  Sparkles,
  type LucideIcon,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useUser } from '../contexts/useUser';
import { AVATAR_EMOJI } from '../lib/auth';
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
  Icon: LucideIcon;
  disabled?: boolean;
}

// 드롭다운 항목이 위→아래로 순서대로 나타나는 시간차(stagger) 간격.
const STAGGER_STEP_MS = 16;
function staggerDelay(index: number) {
  return { animationDelay: `${index * STAGGER_STEP_MS}ms` };
}

const MENU_ITEMS: MenuItem[] = [
  { label: '세이프티', Icon: Shield, disabled: true },
  { to: '/my-saju', label: '내 사주', Icon: Sparkles },
  { to: '/consultations', label: '내 상담', Icon: MessageCircle },
  { to: '/shop', label: '상점', Icon: ShoppingCart },
  { to: '/rewards', label: '보상', Icon: Gift },
  { to: '/payments', label: '결제내역', Icon: CreditCard },
  { to: '/settings', label: '설정', Icon: Settings },
];

interface AccountMenuProps {
  /** 드롭다운으로 쓸 때, 항목을 누르면 드롭다운을 닫기 위한 콜백 */
  onNavigate?: () => void;
}

// 마이페이지(하단 탭)와 헤더 계정 버튼(드롭다운) 둘 다 같은 메뉴 구성을
// 보여준다 — 참고 사이트(foxbunny.io/saju)의 계정 드롭다운 구조. 사용자/잔액은
// UserContext에서 가져온다 — 앱 전체가 같은 값을 공유해서, 이미 다른 화면에서
// 불러온 뒤라면 여기서 다시 "확인 중..."을 보여주지 않고 즉시 열린다.
export default function AccountMenu({ onNavigate }: AccountMenuProps) {
  const navigate = useNavigate();
  const { user, creditBalance, logout } = useUser();

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
                <item.Icon className="h-4.5 w-4.5" strokeWidth={2} />
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
              className="animate-dropdown-item-in flex items-center gap-2 px-3.5 py-2.5 text-sm font-medium text-neutral-100 transition-colors hover:bg-neutral-800/60 active:bg-neutral-700/60"
              style={staggerDelay(2 + i)}
            >
              <item.Icon className="h-4.5 w-4.5" strokeWidth={2} />
              {item.label}
            </Link>
          ),
        )}
      </nav>

      {user.isAdmin && (
        <Link
          to="/admin"
          onClick={onNavigate}
          className="animate-dropdown-item-in rounded-full bg-violet-600 py-2.5 text-center text-sm font-semibold text-white transition-colors hover:bg-violet-500 active:bg-violet-400"
          style={staggerDelay(2 + MENU_ITEMS.length)}
        >
          관리자 화면
        </Link>
      )}

      <button
        type="button"
        className="animate-dropdown-item-in flex items-center gap-2 rounded-2xl border border-neutral-800 px-3.5 py-2.5 text-sm font-semibold text-red-400 transition-colors hover:bg-neutral-800/60 active:bg-neutral-700/60"
        style={staggerDelay(2 + MENU_ITEMS.length + (user.isAdmin ? 1 : 0))}
        onClick={() => {
          void logout();
          onNavigate?.();
          navigate('/');
        }}
      >
        <LogOut className="h-4.5 w-4.5" strokeWidth={2} />
        로그아웃
      </button>
    </div>
  );
}
