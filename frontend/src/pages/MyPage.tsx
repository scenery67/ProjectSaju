import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import type { EmojiName } from '../assets/emoji';
import Emoji from '../components/Emoji';
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
  icon: EmojiName;
}

const ACTIVITY_ROWS: RowItem[] = [
  { to: '/my-saju', label: '내 사주', icon: 'crystalball' },
  { to: '/consultations', label: '내 상담', icon: 'speech' },
];

const BENEFIT_ROWS: RowItem[] = [
  { to: '/shop', label: '상점', icon: 'cart' },
  { to: '/rewards', label: '보상', icon: 'gift' },
  { to: '/payments', label: '결제내역', icon: 'card' },
];

const OTHER_ROWS: RowItem[] = [{ to: '/settings', label: '설정', icon: 'gear' }];

function RowSection({ title, rows }: { title: string; rows: RowItem[] }) {
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-xs font-semibold text-neutral-500">{title}</h3>
      <nav className="flex flex-col divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
        {rows.map((row) => (
          <Link
            key={row.to}
            to={row.to}
            className="flex items-center justify-between px-4 py-3.5 text-sm font-medium text-neutral-100"
          >
            <span className="flex items-center gap-2.5">
              <Emoji name={row.icon} className="h-4.5 w-4.5" />
              {row.label}
            </span>
            <span className="text-neutral-600">›</span>
          </Link>
        ))}
      </nav>
    </div>
  );
}

function PageShell({ children }: { children: ReactNode }) {
  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <div>
        <h2 className="text-2xl font-bold tracking-tight text-white">마이페이지</h2>
        <div className="mt-1.5 h-0.5 w-8 rounded-full bg-violet-500" />
      </div>
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
        <p className="text-sm text-neutral-400">확인 중...</p>
      </PageShell>
    );
  }

  if (user === null) {
    return (
      <PageShell>
        <div className="flex flex-col items-center gap-3 rounded-3xl border border-neutral-800 bg-neutral-900 p-8 text-center">
          <p className="text-sm text-neutral-400">로그인하면 크레딧, 상담 기록을 계정에 저장할 수 있어요.</p>
          <Link to="/login" className="rounded-full bg-violet-600 px-6 py-3 text-sm font-bold text-white">
            로그인하기
          </Link>
        </div>
      </PageShell>
    );
  }

  return (
    <PageShell>
      <section className="flex items-center gap-3 rounded-2xl border border-neutral-800 bg-neutral-900 p-4">
        <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-violet-950">
          <Emoji name={AVATAR_EMOJI[user.avatarKey]} className="h-7 w-7" />
        </span>
        <div className="flex flex-col">
          <span className="text-sm font-bold text-white">{user.nickname || '(닉네임 없음)'}</span>
          <span className="text-xs text-neutral-500">{PROVIDER_LABEL[user.provider] ?? user.provider} 계정</span>
        </div>
      </section>

      <section className="flex flex-col gap-3 rounded-2xl border border-amber-900/40 bg-gradient-to-br from-amber-950/40 to-neutral-900 p-4">
        <div className="flex items-center justify-between">
          <span className="text-sm text-neutral-300">
            보유 크레딧 <span className="ml-1.5 text-lg font-bold text-amber-400">{creditBalance === null ? '—' : creditBalance.toLocaleString('ko-KR')}</span>
          </span>
        </div>
        <div className="flex gap-2">
          <Link
            to="/payments"
            className="flex-1 rounded-full border border-neutral-700 py-2 text-center text-xs font-semibold text-neutral-300"
          >
            내역
          </Link>
          <Link
            to="/shop"
            className="flex-1 rounded-full bg-amber-500 py-2 text-center text-xs font-bold text-neutral-950"
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
          className="rounded-full bg-violet-600 py-3 text-center text-sm font-semibold text-white"
        >
          관리자 화면
        </Link>
      )}

      <button
        type="button"
        className="flex items-center justify-center gap-2 rounded-2xl border border-neutral-800 py-3 text-sm font-semibold text-red-400"
        onClick={() => {
          void logout();
          navigate('/');
        }}
      >
        <Emoji name="door" className="h-4.5 w-4.5" />
        로그아웃
      </button>
    </PageShell>
  );
}
