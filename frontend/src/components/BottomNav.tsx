import { NavLink } from 'react-router-dom';
import type { EmojiName } from '../assets/emoji';
import Emoji from './Emoji';

const TABS: { to: string; label: string; icon: EmojiName }[] = [
  { to: '/', label: '홈', icon: 'house' },
  { to: '/my-saju', label: '내 사주', icon: 'crystalball' },
  { to: '/consultations', label: '상담', icon: 'speech' },
  { to: '/rewards', label: '보상', icon: 'gift' },
  { to: '/mypage', label: '마이페이지', icon: 'person' },
];

// Sticky bottom tab bar — the primary nav pattern on small screens.
// 모바일 화면에서 주 내비게이션으로 쓰는 하단 고정 탭바.
export default function BottomNav() {
  return (
    <nav className="sticky bottom-0 z-10 flex border-t border-neutral-800 bg-neutral-900/95 pb-1 pt-1.5 backdrop-blur">
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/'}
          className={({ isActive }) =>
            `flex flex-1 flex-col items-center gap-1 py-2 text-[11px] font-medium transition-colors ${
              isActive ? 'text-violet-500' : 'text-neutral-400'
            }`
          }
        >
          <Emoji name={tab.icon} className="h-5 w-5" />
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}
