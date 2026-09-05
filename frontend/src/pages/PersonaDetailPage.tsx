import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import {
  requestBreakupReading,
  requestCoupleCompatibilityReading,
} from '../api/sajuApi';
import type { EmojiName } from '../assets/emoji';
import Emoji from '../components/Emoji';
import PersonInputForm from '../components/PersonInputForm';
import { findPersonaById } from '../data/personas';
import { getAuthToken } from '../lib/auth';
import { canStartFreeReading, recordFreeReading } from '../lib/dailyQuota';
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

// 결과 자체와 무관한 일반 사주 상식 — 대기 화면이 밋밋하지 않도록 로딩 중
// 번갈아 보여준다. 페르소나별 콘텐츠 작성은 별도 작업이라 여기선 범용 문구만.
const TRIVIA_TIPS = [
  '오행(五行)은 목·화·토·금·수 다섯 기운의 균형으로 성향을 살펴봐요.',
  '자시(23:00~01:00)는 하루가 바뀌는 경계라 사주 계산이 특히 까다로운 시간대예요.',
  '같은 날 태어났어도 태어난 시간에 따라 시주가 달라져요.',
  '대운은 10년 단위로 바뀌는 인생의 큰 흐름을 보여줘요.',
];

const PROGRESS_ICONS: EmojiName[] = ['crystalball', 'sparkles', 'moon', 'candle'];

// 실제 진행률을 알 방법이 없는 단일 API 호출이라(서버가 단계를 보고해주지
// 않음), 시간이 지날수록 증가폭을 줄여가며 95%에서 멈추는 방식으로 "진행되고
// 있다"는 느낌만 준다 — 응답이 오면 바로 페이지가 바뀌니 100%를 굳이 보여줄
// 필요는 없다. Fly 머신이 idle 상태에서 깨어나는 데 수 초~수십 초 걸릴 수 있어
// 실제로 체감 대기 시간이 꽤 길 수 있다.
function useFakeProgress(active: boolean, messages: string[]) {
  const [progress, setProgress] = useState(0);
  const [messageIndex, setMessageIndex] = useState(0);
  const [tipIndex, setTipIndex] = useState(0);
  const startRef = useRef(0);

  useEffect(() => {
    if (!active) return;
    const tick = setInterval(() => {
      const elapsed = (Date.now() - startRef.current) / 1000;
      // 지수함수 점근(95*(1-e^-t/4))은 처음엔 빠르지만 20~30초 뒤부터는
      // 반올림 표시값이 사실상 고정돼 "멈춘 것처럼" 보였다(사용자 리포트).
      // 대신 완만하게 계속 기어가는 쌍곡선 감쇠(95*t/(t+3))를 쓴다 — 초반
      // 체감 속도는 비슷하지만, Fly 콜드스타트처럼 오래 걸려도 아주 조금씩은
      // 계속 올라가서 멈춘 느낌을 덜 준다.
      setProgress((95 * elapsed) / (elapsed + 3));
    }, 150);
    const messageTick = setInterval(() => {
      setMessageIndex((i) => Math.min(i + 1, messages.length - 1));
    }, 1800);
    const tipTick = setInterval(() => {
      setTipIndex((i) => (i + 1) % TRIVIA_TIPS.length);
    }, 3200);
    return () => {
      clearInterval(tick);
      clearInterval(messageTick);
      clearInterval(tipTick);
    };
  }, [active, messages.length]);

  // 리셋은 effect가 아니라 active=true를 만드는 이벤트 핸들러(handleSubmit)가
  // 직접 호출한다 — "이펙트 안에서 동기적으로 setState" 대신 원인이 된
  // 이벤트에서 상태를 갱신하는 편이 더 명확하다.
  function start() {
    startRef.current = Date.now();
    setProgress(0);
    setMessageIndex(0);
    setTipIndex(0);
  }

  return {
    progress,
    message: messages[messageIndex],
    icon: PROGRESS_ICONS[messageIndex % PROGRESS_ICONS.length],
    tip: TRIVIA_TIPS[tipIndex],
    start,
  };
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
  // 'anonymous': 비로그인 + localStorage 한도 소진 → 로그인 유도.
  // 'loggedIn': 로그인 계정인데 서버가 오늘 한도 초과(429)를 알려옴 → 크레딧 제안.
  const [limitModal, setLimitModal] = useState<'none' | 'anonymous' | 'loggedIn'>('none');
  // Hooks must run unconditionally (before the !persona early return below),
  // so fall back to BREAKUP's messages for the brief render where persona is
  // still undefined — it's never actually shown since we bail out right after.
  const { progress, message, icon, tip, start } = useFakeProgress(
    submitting,
    STATUS_MESSAGES[persona?.type ?? 'BREAKUP'],
  );

  if (!persona) {
    return <main className="p-4 text-sm text-neutral-500">존재하지 않는 상품입니다.</main>;
  }

  const isCouple = persona.type === 'COUPLE_COMPATIBILITY';

  const submit = async (useCredit: boolean) => {
    start();
    setSubmitting(true);
    setError(null);
    try {
      const result = isCouple
        ? await requestCoupleCompatibilityReading({ self, partner }, useCredit)
        : await requestBreakupReading({ self }, useCredit);
      // Logged in: the backend already saved this to the account's server-side
      // history (see api/sajuApi.ts's auth header) — avoid a duplicate-looking
      // local entry. Not logged in: local storage stays the only copy, as before.
      if (!getAuthToken()) {
        saveReadingToHistory(persona.id, result);
        recordFreeReading();
      }
      navigate(`/persona/${persona.id}/result`, { state: { result } });
    } catch (e) {
      if (e instanceof ApiError && e.status === 429) {
        setLimitModal('loggedIn');
      } else {
        setError('결과를 불러오지 못했어요. 잠시 후 다시 시도해주세요.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // 비로그인 사용자의 무료 횟수는 서버가 모르니(식별자를 안 남겨서) 여기서
    // 미리 막는다 — 로그인 사용자는 서버가 실제로 세고 있어 그냥 보내본다.
    if (!getAuthToken() && !canStartFreeReading()) {
      setLimitModal('anonymous');
      return;
    }
    submit(false);
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
          <Emoji name={icon} className="h-10 w-10" />
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

          <div className="w-full max-w-xs rounded-2xl border border-violet-900/50 bg-violet-950/40 p-3.5 text-left">
            <p className="text-xs font-bold text-violet-300">
              이 화면을 유지해주세요
            </p>
            <p className="mt-1 text-[11px] leading-relaxed text-neutral-400">
              다른 화면으로 이동하면 결과로 자동 연결되지 않아요. 서버가 잠시
              쉬고 있었다면 깨어나는 데 시간이 좀 더 걸릴 수 있어요.
            </p>
          </div>

          <div className="w-full max-w-xs rounded-2xl bg-neutral-800/60 p-3.5 text-left">
            <p className="flex items-center gap-1.5 text-[11px] font-bold text-neutral-300">
              <Emoji name="bulb" className="h-3.5 w-3.5" />
              알고 계셨나요?
            </p>
            <p className="mt-1 text-[11px] leading-relaxed text-neutral-400">
              {tip}
            </p>
          </div>
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

      {limitModal !== 'none' && (
        <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/70 px-4">
          <div className="flex w-full max-w-xs flex-col items-center gap-3 rounded-3xl border border-neutral-800 bg-neutral-900 p-6 text-center">
            <span className="text-3xl">⏳</span>
            <p className="text-sm font-bold text-white">
              오늘 무료 사주를 모두 사용했어요
            </p>
            <p className="text-xs leading-relaxed text-neutral-400">
              {limitModal === 'anonymous'
                ? '로그인하면 계정으로 이어서 볼 수 있어요. 매일 자정(KST)에 무료 횟수가 초기화돼요.'
                : '크레딧 1개로 계속 보거나, 내일 자정(KST) 이후 다시 무료로 볼 수 있어요.'}
            </p>
            {limitModal === 'anonymous' ? (
              <Link
                to="/login"
                className="w-full rounded-full bg-violet-500 py-3 text-center text-sm font-bold text-white"
              >
                로그인하러 가기
              </Link>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setLimitModal('none');
                  submit(true);
                }}
                className="w-full rounded-full bg-violet-500 py-3 text-sm font-bold text-white"
              >
                크레딧 1개로 계속 보기
              </button>
            )}
            <button
              type="button"
              onClick={() => setLimitModal('none')}
              className="text-xs font-medium text-neutral-500 underline"
            >
              닫기
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
