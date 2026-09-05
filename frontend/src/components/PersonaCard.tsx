import { Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { SajuPersona } from '../types/saju';

export default function PersonaCard({ persona }: { persona: SajuPersona }) {
  return (
    <Link
      to={`/persona/${persona.id}`}
      className="flex flex-col overflow-hidden rounded-3xl border border-slate-800 bg-slate-900 transition-transform active:scale-[0.97]"
    >
      <div
        className="flex h-32 items-center justify-center"
        style={{
          // 라이트 배경 기준으로 잡았던 옅은 tint(20%→5% 불투명도)가 다크
          // 배경에서는 거의 안 보였다 — 실제 색이 드러나는 진한 그라데이션으로.
          background: `linear-gradient(160deg, ${persona.accentColor}cc, ${persona.accentColor}40)`,
        }}
      >
        {/* Thumbnail illustration goes here once character art is ready */}
        <Sparkles
          className="h-10 w-10 text-white/90 drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]"
          strokeWidth={1.5}
        />
      </div>
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        <span
          className="text-[11px] font-semibold tracking-wide"
          style={{ color: persona.accentColor }}
        >
          {persona.characterName}
        </span>
        <span className="text-base font-bold text-white">
          {persona.title}
        </span>
        <span className="text-xs text-slate-400">{persona.subtitle}</span>
        <span className="line-clamp-2 text-[11px] leading-relaxed text-slate-500">
          {persona.personality}
        </span>
        <span
          className="mt-3 self-start rounded-full px-3 py-1.5 text-xs font-semibold text-white"
          style={{ backgroundColor: persona.accentColor }}
        >
          {persona.isPremium ? '시네마틱으로 시작하기 →' : '무료로 시작하기 →'}
        </span>
      </div>
    </Link>
  );
}
