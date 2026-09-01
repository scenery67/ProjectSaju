import { useEffect, useState } from 'react';
import {
  clearAuthToken,
  fetchCurrentUser,
  getAuthToken,
  loginUrl,
  type CurrentUser,
  type OAuthProvider,
} from '../lib/auth';

const PROVIDERS: { id: OAuthProvider; label: string }[] = [
  { id: 'kakao', label: '카카오로 로그인' },
  { id: 'google', label: '구글로 로그인' },
  { id: 'naver', label: '네이버로 로그인' },
];

export default function MyPage() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(() =>
    getAuthToken() ? undefined : null,
  );

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchCurrentUser().then(setUser);
  }, []);

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-2xl font-bold tracking-tight text-neutral-900">
          마이페이지
        </h2>
        <p className="text-xs text-neutral-400">
          로그인 기능 테스트 중이에요 — 아직 실제 기능에는 연결돼 있지 않아요.
        </p>
      </div>

      {user === undefined && (
        <p className="text-sm text-neutral-400">확인 중...</p>
      )}

      {user === null && (
        <section className="flex flex-col gap-2 rounded-3xl bg-white p-5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]">
          {PROVIDERS.map((p) => (
            <a
              key={p.id}
              href={loginUrl(p.id)}
              className="rounded-full border border-neutral-200 py-3 text-center text-sm font-bold text-neutral-700"
            >
              {p.label}
            </a>
          ))}
        </section>
      )}

      {user && (
        <section className="flex flex-col gap-3 rounded-3xl bg-white p-5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]">
          <p className="text-sm text-neutral-800">
            <span className="font-bold">{user.nickname || '(닉네임 없음)'}</span>
            님, {user.provider} 계정으로 로그인됐어요.
          </p>
          <button
            type="button"
            className="rounded-full border border-neutral-200 py-3 text-sm font-semibold text-neutral-500"
            onClick={() => {
              clearAuthToken();
              setUser(null);
            }}
          >
            로그아웃
          </button>
        </section>
      )}
    </main>
  );
}
