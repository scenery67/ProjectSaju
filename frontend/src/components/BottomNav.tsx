import { Gift, Home, MessageCircle, Sparkles, User } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';

const TABS: { to: string; label: string; Icon: LucideIcon }[] = [
  { to: '/', label: '홈', Icon: Home },
  { to: '/my-saju', label: '내 사주', Icon: Sparkles },
  { to: '/consultations', label: '상담', Icon: MessageCircle },
  { to: '/rewards', label: '보상', Icon: Gift },
  { to: '/mypage', label: '마이페이지', Icon: User },
];

// Sticky bottom tab bar — the primary nav pattern on small screens.
// 모바일 화면에서 주 내비게이션으로 쓰는 하단 고정 탭바.
export default function BottomNav() {
  return (
    <nav className="sticky bottom-0 z-10 flex border-t border-slate-800 bg-slate-900/95 pb-0.5 pt-1 backdrop-blur">
      {TABS.map(({ to, label, Icon }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          className={({ isActive }) =>
            `mx-1 flex flex-1 flex-col items-center gap-1 rounded-2xl py-1.5 text-[11px] font-medium transition-colors hover:bg-slate-800/70 active:bg-slate-800 ${
              isActive ? 'text-violet-500' : 'text-slate-400'
            }`
          }
        >
          <Icon className="h-5 w-5" strokeWidth={2} />
          {label}
        </NavLink>
      ))}
    </nav>
  );
}
