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
        <Link to="/" className="text-rose-500 underline">
          사주 보러 가기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-3 p-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-neutral-800">내 사주</h2>
        <button
          type="button"
          className="text-xs text-neutral-400 underline"
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
      <ul className="flex flex-col gap-2">
        {entries.map((entry) => {
          const persona = findPersonaById(entry.personaId);
          return (
            <li key={entry.id}>
              <button
                type="button"
                className="flex w-full flex-col gap-1 rounded-xl border border-neutral-200 bg-white p-3 text-left"
                onClick={() =>
                  navigate(`/persona/${entry.personaId}/result`, {
                    state: { result: entry.result },
                  })
                }
              >
                <span className="text-sm font-semibold text-neutral-800">
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
