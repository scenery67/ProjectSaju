import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { findPersonaById } from '../data/personas';
import { clearHistory, getHistory } from '../lib/sajuHistory';

export default function MySajuPage() {
  const navigate = useNavigate();
  const [entries, setEntries] = useState(getHistory);

  if (entries.length === 0) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        아직 본 사주가 없어요.
        <Link to="/" className="font-semibold text-rose-500 underline">
          사주 보러 가기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-4 px-4 pb-6 pt-5">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold tracking-tight text-neutral-900">
          내 사주
        </h2>
        <button
          type="button"
          className="text-xs font-medium text-neutral-400 underline"
          onClick={() => {
            clearHistory();
            setEntries([]);
          }}
        >
          기록 삭제
        </button>
      </div>
      <p className="text-xs text-neutral-400">
        이 기기에만 저장된 최근 결과입니다. 앱을 지우거나 다른 기기에서 보면
        보이지 않아요.
      </p>
      <ul className="flex flex-col gap-3">
        {entries.map((entry) => {
          const persona = findPersonaById(entry.personaId);
          return (
            <li key={entry.id}>
              <button
                type="button"
                className="flex w-full flex-col gap-1 rounded-2xl bg-white p-4 text-left shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)] transition-transform active:scale-[0.98]"
                onClick={() =>
                  navigate(`/persona/${entry.personaId}/result`, {
                    state: { result: entry.result },
                  })
                }
              >
                <span className="text-sm font-bold text-neutral-900">
                  {persona?.title ?? entry.personaType}
                </span>
                <span className="text-xs text-neutral-500">
                  {entry.result.summary}
                </span>
                <span className="text-[11px] text-neutral-400">
                  {new Date(entry.createdAt).toLocaleString('ko-KR')}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </main>
  );
}
