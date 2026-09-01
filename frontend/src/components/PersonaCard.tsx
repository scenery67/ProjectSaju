import { Link } from 'react-router-dom';
import type { SajuPersona } from '../types/saju';

export default function PersonaCard({ persona }: { persona: SajuPersona }) {
  return (
    <Link
      to={`/persona/${persona.id}`}
      className="flex flex-col overflow-hidden rounded-3xl bg-white shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.12)] transition-transform active:scale-[0.97]"
    >
      <div
        className="flex h-32 items-center justify-center text-4xl"
        style={{
          background: `linear-gradient(160deg, ${persona.accentColor}33, ${persona.accentColor}0d)`,
        }}
      >
        {/* Thumbnail illustration goes here once character art is ready */}
        🔮
      </div>
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        <span
          className="text-[11px] font-semibold tracking-wide"
          style={{ color: persona.accentColor }}
        >
          {persona.characterName}
        </span>
        <span className="text-base font-bold text-neutral-900">
          {persona.title}
        </span>
        <span className="text-xs text-neutral-500">{persona.subtitle}</span>
        <span className="line-clamp-2 text-[11px] leading-relaxed text-neutral-400">
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
