import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PageTitle from '../components/PageTitle';
import { findPersonaByType, findPersonaById } from '../data/personas';
import { getAuthToken } from '../lib/auth';
import { fetchServerHistory, type ServerHistoryEntry } from '../lib/readingHistory';
import { clearHistory, getHistory } from '../lib/sajuHistory';
import type { SajuReadingResult } from '../types/saju';

interface DisplayEntry {
  key: string;
  source: 'server' | 'local';
  title: string;
  summary: string;
  createdAt: string;
  personaId: string;
  result: SajuReadingResult;
}

function toDisplayEntries(serverEntries: ServerHistoryEntry[]): DisplayEntry[] {
  return serverEntries.flatMap((entry) => {
    const persona = findPersonaByType(entry.result.personaType);
    if (!persona) return [];
    return [{
      key: `server-${entry.id}`,
      source: 'server' as const,
      title: persona.title,
      summary: entry.result.summary,
      createdAt: entry.createdAt,
      personaId: persona.id,
      result: entry.result,
    }];
  });
}

export default function MySajuPage() {
  const navigate = useNavigate();
  const [localEntries, setLocalEntries] = useState(getHistory);
  // undefined = not checked yet / not logged in, so only local entries render.
  const [serverEntries, setServerEntries] = useState<ServerHistoryEntry[] | undefined>(undefined);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchServerHistory().then((entries) => setServerEntries(entries ?? []));
  }, []);

  const displayEntries: DisplayEntry[] = [
    ...(serverEntries ? toDisplayEntries(serverEntries) : []),
    ...localEntries.map((entry) => ({
      key: `local-${entry.id}`,
      source: 'local' as const,
      title: findPersonaById(entry.personaId)?.title ?? entry.personaType,
      summary: entry.result.summary,
      createdAt: entry.createdAt,
      personaId: entry.personaId,
      result: entry.result,
    })),
  ];

  if (displayEntries.length === 0) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        아직 본 사주가 없어요.
        <Link to="/" className="font-semibold text-violet-500 underline">
          사주 보러 가기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-4 px-4 pb-6 pt-5">
      <div className="flex items-center justify-between">
        <PageTitle>내 사주</PageTitle>
        {localEntries.length > 0 && (
          <button
            type="button"
            className="text-xs font-medium text-neutral-400 underline"
            onClick={() => {
              clearHistory();
              setLocalEntries([]);
            }}
          >
            이 기기 기록 삭제
          </button>
        )}
      </div>
      <p className="text-xs text-neutral-400">
        {serverEntries !== undefined
          ? '계정에 저장된 기록과 이 기기에만 저장된 기록을 함께 보여드려요.'
          : '이 기기에만 저장된 최근 결과입니다. 앱을 지우거나 다른 기기에서 보면 보이지 않아요.'}
      </p>
      <ul className="flex flex-col gap-3">
        {displayEntries.map((entry) => (
          <li key={entry.key}>
            <button
              type="button"
              className="flex w-full flex-col gap-1 rounded-2xl border border-neutral-800 bg-neutral-900 p-4 text-left transition-all hover:bg-neutral-800 active:bg-neutral-700 active:scale-[0.98]"
              onClick={() =>
                navigate(`/persona/${entry.personaId}/result`, {
                  state: { result: entry.result },
                })
              }
            >
              <div className="flex items-center gap-1.5">
                <span className="text-sm font-bold text-white">{entry.title}</span>
                {entry.source === 'local' && (
                  <span className="rounded-full bg-neutral-800 px-2 py-0.5 text-[10px] font-medium text-neutral-400">
                    이 기기
                  </span>
                )}
              </div>
              <span className="text-xs text-neutral-500">{entry.summary}</span>
              <span className="text-[11px] text-neutral-400">
                {new Date(entry.createdAt).toLocaleString('ko-KR')}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </main>
  );
}
