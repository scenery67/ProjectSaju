import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageTitle from '../components/PageTitle';
import { findPersonaByType } from '../data/personas';
import { getAuthToken } from '../lib/auth';
import { fetchConsultationSessions, type ConsultationSession } from '../lib/consultation';
import { withBatchimPostposition } from '../lib/korean';
import type { PersonaType } from '../types/saju';

export default function ConsultationListPage() {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<ConsultationSession[] | null>(
    getAuthToken() ? null : [],
  );

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchConsultationSessions()
      .then(setSessions)
      .catch(() => setSessions([]));
  }, []);

  if (!getAuthToken()) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        로그인하면 이전 상담을 다시 볼 수 있어요.
        <Link to="/mypage" className="font-semibold text-violet-500 underline">
          마이페이지에서 로그인
        </Link>
      </main>
    );
  }

  if (sessions === null) {
    return <main className="flex flex-1 items-center justify-center p-4 text-sm text-neutral-400">불러오는 중...</main>;
  }

  if (sessions.length === 0) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        아직 상담한 내용이 없어요.
        <Link to="/" className="font-semibold text-violet-500 underline">
          사주 보러 가기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-4 px-4 pb-6 pt-5">
      <PageTitle>내 상담</PageTitle>
      <ul className="flex flex-col gap-3">
        {sessions.map((session) => {
          const persona = findPersonaByType(session.personaType as PersonaType);
          return (
            <li key={session.id}>
              <button
                type="button"
                className="flex w-full flex-col gap-1 rounded-2xl border border-neutral-800 bg-neutral-900 p-4 text-left transition-all hover:bg-neutral-800 active:bg-neutral-700 active:scale-[0.98]"
                onClick={() =>
                  navigate(`/consultation/${session.id}`, { state: { session } })
                }
              >
                <span className="text-sm font-bold text-white">
                  {persona
                    ? `${withBatchimPostposition(persona.characterName, '와', '과')}의 상담`
                    : '상담'}
                </span>
                <span className="text-xs text-neutral-500">{persona?.title ?? session.personaType}</span>
                <span className="text-[11px] text-neutral-400">
                  {new Date(session.createdAt).toLocaleString('ko-KR')}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </main>
  );
}
