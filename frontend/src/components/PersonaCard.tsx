import { Link } from 'react-router-dom';
import type { SajuPersona } from '../types/saju';

export default function PersonaCard({ persona }: { persona: SajuPersona }) {
  return (
    <Link
      to={`/persona/${persona.id}`}
      className="flex flex-col overflow-hidden rounded-2xl border border-neutral-200 bg-white shadow-sm active:scale-[0.98] transition-transform"
    >
      <div
        className="flex h-28 items-center justify-center text-3xl"
        style={{ backgroundColor: `${persona.accentColor}22` }}
      >
        {/* Thumbnail illustration goes here once character art is ready */}
        🔮
      </div>
      <div className="flex flex-col gap-1 p-3">
        <span className="text-[11px] font-medium text-neutral-400">
          {persona.characterName}
        </span>
        <span className="text-sm font-semibold text-neutral-800">
          {persona.title}
        </span>
        <span className="text-xs text-neutral-500">{persona.subtitle}</span>
        <span className="text-[11px] text-neutral-400">{persona.personality}</span>
        <span
          className="mt-1 self-start rounded-full px-2 py-0.5 text-[11px] font-medium"
          style={{
            backgroundColor: `${persona.accentColor}22`,
            color: persona.accentColor,
          }}
        >
          {persona.isPremium ? '시네마틱으로 시작하기 →' : '무료로 시작하기 →'}
        </span>
      </div>
    </Link>
  );
}
