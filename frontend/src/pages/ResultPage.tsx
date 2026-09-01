import { Link, useLocation, useParams } from 'react-router-dom';
import { findPersonaById } from '../data/personas';
import type { SajuChart, SajuReadingResult } from '../types/saju';

function ChartCard({ label, chart }: { label: string; chart: SajuChart }) {
  return (
    <div className="rounded-2xl bg-neutral-50 p-4 text-xs leading-relaxed text-neutral-600">
      <p className="mb-1.5 text-[11px] font-bold tracking-wide text-neutral-400">
        {label}
      </p>
      <p>
        년주 {chart.yearPillar} · 월주 {chart.monthPillar} · 일주 {chart.dayPillar}
        {chart.hourPillar ? ` · 시주 ${chart.hourPillar}` : ' (시주 미상)'}
      </p>
      <p className="mt-1">
        일간 {chart.dayMaster} · 대표 오행 {chart.dominantFiveElement}
      </p>
      <p className="mt-1">
        십성 — 년 {chart.yearTenGod} · 월 {chart.monthTenGod}
        {chart.timeTenGod ? ` · 시 ${chart.timeTenGod}` : ''}
      </p>
      {chart.daYunPeriods.length > 0 && (
        <p className="mt-1 text-neutral-500">
          대운:{' '}
          {chart.daYunPeriods
            .map((p) => `${p.startAge}~${p.endAge}세 ${p.pillar}`)
            .join(' · ')}
        </p>
      )}
    </div>
  );
}

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
        <Link to="/" className="font-semibold text-rose-500 underline">
          홈으로 돌아가기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <h2 className="text-2xl font-bold tracking-tight text-neutral-900">
        {persona.title} 결과
      </h2>
      <section className="rounded-3xl bg-white p-5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.12)]">
        <p className="mb-3 text-sm font-bold text-neutral-900">
          {result.summary}
        </p>
        <p className="whitespace-pre-line text-sm leading-relaxed text-neutral-600">
          {result.detail}
        </p>
      </section>
      <section className="flex flex-col gap-3">
        <p className="text-[11px] text-neutral-400">
          아래는 해석에 쓰인 원본 사주 정보예요 (참고용)
        </p>
        <ChartCard label="나의 사주" chart={result.selfChart} />
        {result.partnerChart && (
          <ChartCard label="상대방의 사주" chart={result.partnerChart} />
        )}
      </section>
      <Link
        to="/"
        className="rounded-full bg-rose-500 py-3.5 text-center text-sm font-bold text-white shadow-[0_8px_20px_-8px_rgba(244,63,94,0.6)]"
      >
        다른 사주 보러가기
      </Link>
    </main>
  );
}
