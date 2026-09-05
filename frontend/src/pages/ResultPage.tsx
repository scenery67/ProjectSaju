import { Lock, Unlock } from 'lucide-react';
import { useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import PageTitle from '../components/PageTitle';
import { findPersonaById } from '../data/personas';
import { getAuthToken } from '../lib/auth';
import { ConsultationApiError, createConsultationSession } from '../lib/consultation';
import type { PersonalityProfile, SajuChart, SajuReadingResult } from '../types/saju';

const PROFILE_ROWS: { key: keyof PersonalityProfile; label: string }[] = [
  { key: 'personality', label: '기본 성격' },
  { key: 'love', label: '연애' },
  { key: 'career', label: '직업' },
  { key: 'wealth', label: '재물' },
  { key: 'relationships', label: '대인관계' },
];

function PersonalityProfileCard({
  label,
  profile,
  accentColor,
}: {
  label: string;
  profile: PersonalityProfile;
  accentColor: string;
}) {
  return (
    <section className="rounded-3xl border border-slate-800 bg-slate-900 p-5">
      <p
        className="mb-3 text-[11px] font-bold tracking-wide"
        style={{ color: accentColor }}
      >
        {label}
      </p>
      <dl className="flex flex-col gap-3">
        {PROFILE_ROWS.map(({ key, label: rowLabel }) => (
          <div key={key}>
            <dt className="mb-0.5 text-xs font-bold text-slate-100">
              {rowLabel}
            </dt>
            <dd className="text-sm leading-relaxed text-slate-400">
              {profile[key]}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

// 전통적인 오행 색(목=초록/화=빨강/토=황토/금=회색/수=파랑)을 그대로 쓴다 — 막대마다
// 이름+퍼센트 라벨이 항상 붙어 있어 색만으로 구분하지 않는다 (dataviz 보조 인코딩 원칙).
const FIVE_ELEMENT_COLORS: Record<string, string> = {
  목: '#008300',
  화: '#e34948',
  토: '#eda100',
  금: '#898781',
  수: '#2a78d6',
};
const FIVE_ELEMENT_ORDER = ['목', '화', '토', '금', '수'];

function FiveElementBars({ counts }: { counts: Record<string, number> }) {
  const total = Object.values(counts).reduce((sum, n) => sum + n, 0) || 1;
  return (
    <div className="flex flex-col gap-2">
      {FIVE_ELEMENT_ORDER.map((element) => {
        const count = counts[element] ?? 0;
        const percent = Math.round((count / total) * 100);
        return (
          <div key={element} className="flex items-center gap-2">
            <span className="w-4 text-xs font-bold text-slate-300">
              {element}
            </span>
            <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-slate-800">
              <div
                className="h-full rounded-full"
                style={{
                  width: `${percent}%`,
                  backgroundColor: FIVE_ELEMENT_COLORS[element],
                }}
              />
            </div>
            <span className="w-9 text-right text-[11px] tabular-nums text-slate-500">
              {percent}%
            </span>
          </div>
        );
      })}
    </div>
  );
}

function PillarColumn({
  palace,
  pillar,
  tenGod,
  hideGan,
  twelveStage,
}: {
  palace: string;
  pillar: string;
  tenGod: string | null;
  hideGan: string[] | null;
  twelveStage: string | null;
}) {
  const gan = pillar.charAt(0);
  const zhi = pillar.charAt(1);
  return (
    <div className="flex flex-col items-center gap-1 text-center">
      <span className="text-[10px] text-slate-400">{palace}</span>
      <span className="text-[11px] font-medium text-violet-400">
        {tenGod ?? '—'}
      </span>
      <span className="text-xl font-bold text-white">{gan}</span>
      <span className="text-xl font-bold text-slate-300">{zhi}</span>
      <span className="text-[11px] text-slate-400">
        {twelveStage ?? '—'}
      </span>
      <span className="text-[10px] leading-tight text-slate-400">
        {hideGan && hideGan.length > 0 ? hideGan.join('·') : '—'}
      </span>
    </div>
  );
}

function ChartCard({ label, chart }: { label: string; chart: SajuChart }) {
  return (
    <div className="rounded-2xl bg-slate-800 p-4">
      <p className="mb-3 text-[11px] font-bold tracking-wide text-slate-400">
        {label}
      </p>
      <div className="grid grid-cols-4 gap-1 border-b border-slate-800 pb-4">
        <PillarColumn
          palace="말년(시)"
          pillar={chart.hourPillar ?? '--'}
          tenGod={chart.timeTenGod}
          hideGan={chart.timeHideGan}
          twelveStage={chart.timeTwelveStage}
        />
        <PillarColumn
          palace="중년(일)"
          pillar={chart.dayPillar}
          tenGod={null}
          hideGan={chart.dayHideGan}
          twelveStage={chart.dayTwelveStage}
        />
        <PillarColumn
          palace="청년(월)"
          pillar={chart.monthPillar}
          tenGod={chart.monthTenGod}
          hideGan={chart.monthHideGan}
          twelveStage={chart.monthTwelveStage}
        />
        <PillarColumn
          palace="초년(년)"
          pillar={chart.yearPillar}
          tenGod={chart.yearTenGod}
          hideGan={chart.yearHideGan}
          twelveStage={chart.yearTwelveStage}
        />
      </div>
      {!chart.hourPillar && (
        <p className="mt-2 text-[11px] text-slate-400">
          시주는 출생시간 미상이라 제외했어요.
        </p>
      )}

      <p className="mb-2 mt-4 text-[11px] font-bold tracking-wide text-slate-400">
        오행 분포
      </p>
      <FiveElementBars counts={chart.fiveElementCounts} />

      {chart.daYunPeriods.length > 0 && (
        <div className="mt-4 overflow-x-auto">
          <p className="mb-1.5 text-[11px] font-bold tracking-wide text-slate-400">
            대운
          </p>
          <div className="flex gap-2">
            {chart.daYunPeriods.map((p) => (
              <div
                key={p.startAge}
                className="flex shrink-0 flex-col items-center rounded-lg border border-slate-800 bg-slate-900 px-2 py-1.5 text-center"
              >
                <span className="text-[10px] text-slate-400">
                  {p.startAge}~{p.endAge}세
                </span>
                <span className="text-sm font-semibold text-slate-100">
                  {p.pillar}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {chart.currentLiuNian.length > 0 && (
        <div className="mt-3 overflow-x-auto">
          <p className="mb-1.5 text-[11px] font-bold tracking-wide text-slate-400">
            요즘 흐름 (세운)
          </p>
          <div className="flex gap-2">
            {chart.currentLiuNian.map((p) => (
              <div
                key={p.year}
                className="flex shrink-0 flex-col items-center rounded-lg border border-slate-800 bg-slate-900 px-2 py-1.5 text-center"
              >
                <span className="text-[10px] text-slate-400">
                  {p.year} ({p.age}세)
                </span>
                <span className="text-sm font-semibold text-slate-100">
                  {p.pillar}
                </span>
                <span className="text-[10px] text-violet-400">{p.tenGod}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default function ResultPage() {
  const { personaId } = useParams<{ personaId: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const persona = personaId ? findPersonaById(personaId) : undefined;
  const result = (location.state as { result?: SajuReadingResult } | null)
    ?.result;
  const [startingConsultation, setStartingConsultation] = useState(false);
  const [consultationError, setConsultationError] = useState<string | null>(null);

  if (!persona || !result) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-slate-500">
        결과 정보가 없습니다.
        <Link to="/" className="font-semibold text-violet-500 underline">
          홈으로 돌아가기
        </Link>
      </main>
    );
  }

  async function handleStartConsultation() {
    if (!result?.id) return;
    setStartingConsultation(true);
    setConsultationError(null);
    try {
      const session = await createConsultationSession(result.id);
      navigate(`/consultation/${session.id}`, { state: { session } });
    } catch (e) {
      setConsultationError(
        e instanceof ConsultationApiError && e.status === 404
          ? '이 결과로는 상담을 시작할 수 없어요.'
          : '상담을 시작하지 못했어요. 잠시 후 다시 시도해주세요.',
      );
    } finally {
      setStartingConsultation(false);
    }
  }

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <PageTitle>{persona.title} 결과</PageTitle>
      <section
        className="flex flex-col gap-3 rounded-3xl border p-5"
        style={{
          borderColor: `${persona.accentColor}40`,
          background: `linear-gradient(160deg, ${persona.accentColor}20, transparent 60%)`,
        }}
      >
        <span
          className="w-fit rounded-full px-2.5 py-1 text-[10px] font-bold text-white"
          style={{ backgroundColor: persona.accentColor }}
        >
          무료 공개
        </span>
        <p className="text-sm font-bold text-white">{result.summary}</p>
        <p className="whitespace-pre-line text-sm leading-relaxed text-slate-400">
          {result.detail}
        </p>
      </section>

      {getAuthToken() && result.id && (
        <section
          className="flex flex-col gap-3 rounded-3xl border p-5"
          style={{
            borderColor: `${persona.accentColor}55`,
            background: `linear-gradient(160deg, ${persona.accentColor}26, transparent)`,
          }}
        >
          <div className="flex items-center gap-2">
            <Unlock className="h-4.5 w-4.5" strokeWidth={2} />
            <p className="text-sm font-bold text-white">
              {persona.characterName}에게 더 물어볼 수 있어요
            </p>
          </div>
          <ul className="flex flex-col gap-1.5 text-xs text-slate-300">
            <li>· 이 결과에서 궁금한 부분을 자유롭게 물어보기</li>
            <li>· 지금 이 시기에 뭘 준비하면 좋을지 구체적으로 상담받기</li>
          </ul>
          <button
            type="button"
            disabled={startingConsultation}
            onClick={handleStartConsultation}
            className="rounded-full py-3.5 text-center text-sm font-bold text-white disabled:opacity-50"
            style={{ backgroundColor: persona.accentColor }}
          >
            {startingConsultation
              ? '연결하는 중...'
              : `${persona.characterName}에게 상담받기 (크레딧 1개)`}
          </button>
          {consultationError && (
            <p className="text-xs font-medium text-violet-500">{consultationError}</p>
          )}
        </section>
      )}
      {!getAuthToken() && (
        <section className="flex flex-col items-center gap-2 rounded-3xl border border-dashed border-slate-700 bg-slate-900/60 p-5 text-center">
          <Lock className="h-4.5 w-4.5" strokeWidth={2} />
          <p className="text-sm font-bold text-white">
            로그인하면 {persona.characterName}에게 더 물어볼 수 있어요
          </p>
          <Link to="/login" className="text-xs font-semibold text-violet-400 underline">
            로그인하러 가기
          </Link>
        </section>
      )}

      <PersonalityProfileCard
        label={result.partnerChart ? '나의 성향' : '기본 프로필'}
        profile={result.selfChart.personalityProfile}
        accentColor={persona.accentColor}
      />
      {result.partnerChart && (
        <PersonalityProfileCard
          label="상대방의 성향"
          profile={result.partnerChart.personalityProfile}
          accentColor={persona.accentColor}
        />
      )}
      <section className="flex flex-col gap-3">
        <p className="text-[11px] text-slate-400">
          아래는 해석에 쓰인 사주 원국표예요 (참고용)
        </p>
        <ChartCard label="나의 사주" chart={result.selfChart} />
        {result.partnerChart && (
          <ChartCard label="상대방의 사주" chart={result.partnerChart} />
        )}
      </section>
      <Link
        to="/"
        className="rounded-full bg-violet-500 py-3.5 text-center text-sm font-bold text-white shadow-[0_8px_20px_-8px_rgba(139,92,246,0.6)]"
      >
        다른 사주 보러가기
      </Link>
    </main>
  );
}
