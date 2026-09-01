import { Link, useLocation, useParams } from 'react-router-dom';
import { findPersonaById } from '../data/personas';
import type { SajuReadingResult } from '../types/saju';

export default function ResultPage() {
  const { personaId } = useParams<{ personaId: string }>();
  const location = useLocation();
  const persona = personaId ? findPersonaById(personaId) : undefined;
  const result = (location.state as { result?: SajuReadingResult } | null)
    ?.result;

  if (!persona || !result) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        결과 정보가 없습니다.
        <Link to="/" className="text-rose-500 underline">
          홈으로 돌아가기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-4 p-4">
      <h2 className="text-xl font-bold text-neutral-800">{persona.title} 결과</h2>
      <section className="rounded-xl border border-neutral-200 bg-white p-4">
        <p className="mb-2 text-sm font-semibold text-neutral-800">
          {result.summary}
        </p>
        <p className="whitespace-pre-line text-sm text-neutral-600">
          {result.detail}
        </p>
      </section>
      <Link
        to="/"
        className="rounded-xl border border-neutral-300 py-3 text-center text-sm font-semibold text-neutral-700"
      >
        다른 사주 보러가기
      </Link>
    </main>
  );
}
