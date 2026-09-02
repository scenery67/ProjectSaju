import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  requestBreakupReading,
  requestCoupleCompatibilityReading,
} from '../api/sajuApi';
import PersonInputForm from '../components/PersonInputForm';
import { findPersonaById } from '../data/personas';
import { getAuthToken } from '../lib/auth';
import { saveReadingToHistory } from '../lib/sajuHistory';
import type { PersonaType, PersonReadingInput } from '../types/saju';

const STATUS_MESSAGES: Record<PersonaType, string[]> = {
  BREAKUP: [
    '생년월일로 사주팔자를 세우고 있어요',
    '오행 기운의 흐름을 살펴보고 있어요',
    '성격과 성향을 풀이하고 있어요',
    '다정한 위로의 말을 고르고 있어요',
  ],
  COUPLE_COMPATIBILITY: [
    '두 분의 사주를 각각 세우고 있어요',
    '오행 궁합을 맞춰보고 있어요',
    '인연의 흐름을 살펴보고 있어요',
    '설레는 마음으로 정리하고 있어요',
  ],
};

// 실제 진행률을 알 방법이 없는 단일 API 호출이라(서버가 단계를 보고해주지
// 않음), 시간이 지날수록 증가폭을 줄여가며 95%에서 멈추는 방식으로 "진행되고
// 있다"는 느낌만 준다 — 응답이 오면 바로 페이지가 바뀌니 100%를 굳이 보여줄
// 필요는 없다. Fly 머신이 idle 상태에서 깨어나는 데 수 초~수십 초 걸릴 수 있어
// 실제로 체감 대기 시간이 꽤 길 수 있다.
function useFakeProgress(active: boolean, messages: string[]) {
  const [progress, setProgress] = useState(0);
  const [messageIndex, setMessageIndex] = useState(0);
  const startRef = useRef(0);

  useEffect(() => {
    if (!active) return;
    const tick = setInterval(() => {
      const elapsed = (Date.now() - startRef.current) / 1000;
      // 처음엔 빠르게, 갈수록 느리게 — 95%에 점근.
      setProgress(Math.min(95, 95 * (1 - Math.exp(-elapsed / 4))));
    }, 150);
    const messageTick = setInterval(() => {
      setMessageIndex((i) => Math.min(i + 1, messages.length - 1));
    }, 1800);
    return () => {
      clearInterval(tick);
      clearInterval(messageTick);
    };
  }, [active, messages.length]);

  // 리셋은 effect가 아니라 active=true를 만드는 이벤트 핸들러(handleSubmit)가
  // 직접 호출한다 — "이펙트 안에서 동기적으로 setState" 대신 원인이 된
  // 이벤트에서 상태를 갱신하는 편이 더 명확하다.
  function start() {
    startRef.current = Date.now();
    setProgress(0);
    setMessageIndex(0);
  }

  return { progress, message: messages[messageIndex], start };
}

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
  // Hooks must run unconditionally (before the !persona early return below),
  // so fall back to BREAKUP's messages for the brief render where persona is
  // still undefined — it's never actually shown since we bail out right after.
  const { progress, message, start } = useFakeProgress(
    submitting,
    STATUS_MESSAGES[persona?.type ?? 'BREAKUP'],
  );

  if (!persona) {
    return <main className="p-4 text-sm text-neutral-500">존재하지 않는 상품입니다.</main>;
  }

  const isCouple = persona.type === 'COUPLE_COMPATIBILITY';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    start();
    setSubmitting(true);
    setError(null);
    try {
      const result = isCouple
        ? await requestCoupleCompatibilityReading({ self, partner })
        : await requestBreakupReading({ self });
      // Logged in: the backend already saved this to the account's server-side
      // history (see api/sajuApi.ts's auth header) — avoid a duplicate-looking
      // local entry. Not logged in: local storage stays the only copy, as before.
      if (!getAuthToken()) {
        saveReadingToHistory(persona.id, result);
      }
      navigate(`/persona/${persona.id}/result`, { state: { result } });
    } catch {
      setError('결과를 불러오지 못했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <section className="flex flex-col gap-1.5">
        <h2 className="text-2xl font-bold tracking-tight text-white">
          {persona.title}
        </h2>
        <p className="text-sm text-neutral-500">{persona.subtitle}</p>
        <p className="text-xs text-neutral-400">
          {persona.characterName} · {persona.personality}
        </p>
      </section>

      {submitting ? (
        <section className="flex flex-1 flex-col items-center justify-center gap-4 rounded-3xl border border-neutral-800 bg-neutral-900 p-8 text-center">
          <span className="text-4xl">🔮</span>
          <div className="w-full max-w-xs">
            <div className="h-2.5 w-full overflow-hidden rounded-full bg-neutral-800">
              <div
                className="h-full rounded-full bg-violet-500 transition-[width] duration-150 ease-out"
                style={{ width: `${progress}%` }}
              />
            </div>
            <p className="mt-1 text-right text-[11px] tabular-nums text-neutral-400">
              {Math.round(progress)}%
            </p>
          </div>
          <p className="text-sm font-medium text-neutral-400">{message}</p>
          <p className="text-xs text-neutral-400">
            서버가 잠시 쉬고 있었다면 깨어나는 데 시간이 좀 더 걸릴 수 있어요.
          </p>
        </section>
      ) : (
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <PersonInputForm label="본인 정보" value={self} onChange={setSelf} />
          {isCouple && (
            <PersonInputForm
              label="상대방 정보"
              value={partner}
              onChange={setPartner}
            />
          )}

          {error && <p className="text-xs font-medium text-violet-500">{error}</p>}

          <button
            type="submit"
            className="rounded-full bg-violet-500 py-3.5 text-sm font-bold text-white shadow-[0_8px_20px_-8px_rgba(139,92,246,0.6)]"
          >
            사주 풀이 시작하기 →
          </button>
        </form>
      )}
    </main>
  );
}
