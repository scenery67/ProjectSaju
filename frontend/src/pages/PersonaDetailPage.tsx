import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  requestBreakupReading,
  requestCoupleCompatibilityReading,
} from '../api/sajuApi';
import PersonInputForm from '../components/PersonInputForm';
import { findPersonaById } from '../data/personas';
import { saveReadingToHistory } from '../lib/sajuHistory';
import type { PersonReadingInput } from '../types/saju';

const emptyPerson = (): PersonReadingInput => ({
  name: '',
  birthDate: '',
  birthTime: null,
  calendarType: 'SOLAR',
  isLunarLeapMonth: false,
  gender: 'FEMALE',
});

export default function PersonaDetailPage() {
  const { personaId } = useParams<{ personaId: string }>();
  const navigate = useNavigate();
  const persona = personaId ? findPersonaById(personaId) : undefined;

  const [self, setSelf] = useState(emptyPerson);
  const [partner, setPartner] = useState(emptyPerson);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!persona) {
    return <main className="p-4 text-sm text-neutral-500">존재하지 않는 상품입니다.</main>;
  }

  const isCouple = persona.type === 'COUPLE_COMPATIBILITY';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const result = isCouple
        ? await requestCoupleCompatibilityReading({ self, partner })
        : await requestBreakupReading({ self });
      saveReadingToHistory(persona.id, result);
      navigate(`/persona/${persona.id}/result`, { state: { result } });
    } catch {
      setError('결과를 불러오지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex flex-1 flex-col gap-4 p-4">
      <section className="flex flex-col gap-1">
        <h2 className="text-xl font-bold text-neutral-800">{persona.title}</h2>
        <p className="text-sm text-neutral-500">{persona.subtitle}</p>
        <p className="text-xs text-neutral-400">
          {persona.characterName} · {persona.personality}
        </p>
      </section>

      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <PersonInputForm label="본인 정보" value={self} onChange={setSelf} />
        {isCouple && (
          <PersonInputForm
            label="상대방 정보"
            value={partner}
            onChange={setPartner}
          />
        )}

        {error && <p className="text-xs text-rose-500">{error}</p>}

        <button
          type="submit"
          disabled={submitting}
          className="rounded-xl bg-rose-500 py-3 text-sm font-semibold text-white disabled:opacity-50"
        >
          {submitting ? '풀이 중...' : '사주 풀이 시작하기 →'}
        </button>
      </form>
    </main>
  );
}
