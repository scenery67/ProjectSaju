import { NavLink } from 'react-router-dom';

const TABS = [
  { to: '/', label: '홈', icon: '🏠' },
  { to: '/my-saju', label: '내 사주', icon: '🔮' },
  { to: '/mypage', label: '마이페이지', icon: '👤' },
];

// Sticky bottom tab bar — the primary nav pattern on small screens.
// 모바일 화면에서 주 내비게이션으로 쓰는 하단 고정 탭바.
export default function BottomNav() {
  return (
    <nav className="sticky bottom-0 z-10 flex border-t border-neutral-200 bg-white/95 backdrop-blur">
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/'}
          className={({ isActive }) =>
            `flex flex-1 flex-col items-center gap-0.5 py-2 text-xs ${
              isActive ? 'text-rose-500' : 'text-neutral-400'
            }`
          }
        >
          <span className="text-lg leading-none">{tab.icon}</span>
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}
