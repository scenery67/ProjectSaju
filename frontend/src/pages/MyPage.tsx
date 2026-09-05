import {
  ChevronRight,
  CreditCard,
  Gift,
  LogOut,
  MessageCircle,
  Settings,
  ShoppingCart,
  Sparkles,
  type LucideIcon,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import Emoji from '../components/Emoji';
import PageTitle from '../components/PageTitle';
import { useUser } from '../contexts/useUser';
import { AVATAR_EMOJI } from '../lib/auth';

const PROVIDER_LABEL: Record<string, string> = {
  KAKAO: '카카오',
  GOOGLE: 'Google',
  NAVER: '네이버',
  DEV_BYPASS: '개발용',
};

interface RowItem {
  to: string;
  label: string;
  Icon: LucideIcon;
}

const ACTIVITY_ROWS: RowItem[] = [
  { to: '/my-saju', label: '내 사주', Icon: Sparkles },
  { to: '/consultations', label: '내 상담', Icon: MessageCircle },
];

const BENEFIT_ROWS: RowItem[] = [
  { to: '/shop', label: '상점', Icon: ShoppingCart },
  { to: '/rewards', label: '보상', Icon: Gift },
  { to: '/payments', label: '결제내역', Icon: CreditCard },
];

const OTHER_ROWS: RowItem[] = [{ to: '/settings', label: '설정', Icon: Settings }];

function RowSection({ title, rows }: { title: string; rows: RowItem[] }) {
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-xs font-semibold text-slate-500">{title}</h3>
      <nav className="flex flex-col divide-y divide-slate-800 rounded-2xl border border-slate-800 bg-slate-900">
        {rows.map((row) => (
          <Link
            key={row.to}
            to={row.to}
            className="flex items-center justify-between px-4 py-3.5 text-sm font-medium text-slate-100 transition-colors hover:bg-slate-800/60"
          >
            <span className="flex items-center gap-2.5">
              <row.Icon className="h-4.5 w-4.5" strokeWidth={2} />
              {row.label}
            </span>
            <ChevronRight className="h-4 w-4 text-slate-600" strokeWidth={2} />
          </Link>
        ))}
      </nav>
    </div>
  );
}

function PageShell({ children }: { children: ReactNode }) {
  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <PageTitle>마이페이지</PageTitle>
      {children}
    </main>
  );
}

// 하단 탭 "마이페이지"의 전체 화면 — 헤더 계정 버튼의 드롭다운(AccountMenu)과는
// 참고 사이트에서도 구성이 다르다(플랫 목록 vs 그룹별 섹션+화살표 행).
export default function MyPage() {
  const navigate = useNavigate();
  const { user, creditBalance, logout } = useUser();

  if (user === undefined) {
    return (
      <PageShell>
        <p className="text-sm text-slate-400">확인 중...</p>
      </PageShell>
    );
  }

  if (user === null) {
    return (
      <PageShell>
        <div className="flex flex-col items-center gap-3 rounded-3xl border border-slate-800 bg-slate-900 p-8 text-center">
          <p className="text-sm text-slate-400">로그인하면 크레딧, 상담 기록을 계정에 저장할 수 있어요.</p>
          <Link to="/login" className="rounded-full bg-violet-600 px-6 py-3 text-sm font-bold text-white">
            로그인하기
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <section className="flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900 p-4">
        <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-violet-950">
          <Emoji name={AVATAR_EMOJI[user.avatarKey]} className="h-7 w-7" />
        </span>
        <div className="flex flex-col">
          <span className="text-sm font-bold text-white">{user.nickname || '(닉네임 없음)'}</span>
          <span className="text-xs text-slate-500">{PROVIDER_LABEL[user.provider] ?? user.provider} 계정</span>
        </div>
      </section>

      <section className="flex flex-col gap-3 rounded-2xl border border-amber-900/40 bg-gradient-to-br from-amber-950/40 to-slate-900 p-4">
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-300">
            보유 크레딧 <span className="ml-1.5 text-lg font-bold text-amber-400">{creditBalance === null ? '—' : creditBalance.toLocaleString('ko-KR')}</span>
          </span>
        </div>
        <div className="flex gap-2">
          <Link
            to="/payments"
            className="flex-1 rounded-full border border-slate-700 py-2 text-center text-xs font-semibold text-slate-300 transition-colors hover:bg-slate-800"
          >
            내역
          </Link>
          <Link
            to="/shop"
            className="flex-1 rounded-full bg-amber-500 py-2 text-center text-xs font-bold text-slate-950 transition-colors hover:bg-amber-400"
          >
            충전하기
          </Link>
        </div>
      </section>

      <RowSection title="활동" rows={ACTIVITY_ROWS} />
      <RowSection title="혜택 · 결제" rows={BENEFIT_ROWS} />
      <RowSection title="기타" rows={OTHER_ROWS} />

      {user.isAdmin && (
        <Link
          to="/admin"
          className="rounded-full bg-violet-600 py-3 text-center text-sm font-semibold text-white transition-colors hover:bg-violet-500"
        >
          관리자 화면
        </Link>
      )}

      <button
        type="button"
        className="flex items-center justify-center gap-2 rounded-2xl border border-slate-800 py-3 text-sm font-semibold text-red-400 transition-colors hover:bg-slate-800/60"
        onClick={() => {
          void logout();
          navigate('/');
        }}
      >
        <LogOut className="h-4.5 w-4.5" strokeWidth={2} />
        로그아웃
      </button>
    </PageShell>
  );
}
