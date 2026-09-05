import { ArrowLeft, Heart, HeartCrack, Search, SearchX } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PersonaCard from '../components/PersonaCard';
import { PERSONAS } from '../data/personas';

// 참고 사이트(foxbunny.io/saju)는 검색 결과에서 자기 카탈로그의 여러 상품을
// 보여주는데, 우리는 실제 상품이 2개뿐이라 그 자리를 가짜 데이터로 채우지
// 않는다 — 실제 두 페르소나(제목/부제/캐릭터명)만 검색 대상으로 삼는다.
const POPULAR_KEYWORDS: { label: string; Icon: typeof Heart }[] = [
  { label: '이별', Icon: HeartCrack },
  { label: '궁합', Icon: Heart },
];

function matches(query: string, persona: (typeof PERSONAS)[number]): boolean {
  const q = query.trim().toLowerCase();
  if (!q) return false;
  return [persona.title, persona.subtitle, persona.characterName].some((field) =>
    field.toLowerCase().includes(q),
  );
}

export default function SearchPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState<string | null>(null);

  function runSearch(q: string) {
    setQuery(q);
    setSubmittedQuery(q);
  }

  const results = submittedQuery === null ? null : PERSONAS.filter((p) => matches(submittedQuery, p));

  return (
    <main className="flex flex-1 flex-col">
      <div className="sticky top-0 z-10 flex items-center gap-3 border-b border-slate-800 bg-slate-900/90 px-4 py-3.5 backdrop-blur">
        <button type="button" aria-label="뒤로" onClick={() => navigate(-1)} className="text-slate-300">
          <ArrowLeft className="h-5 w-5" strokeWidth={2} />
        </button>
        <input
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && runSearch(query)}
          placeholder="사주 상품 검색..."
          className="flex-1 rounded-full border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm text-white placeholder:text-slate-500 outline-none focus:border-violet-400"
        />
        <button
          type="button"
          aria-label="검색"
          onClick={() => runSearch(query)}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-teal-500 text-slate-950"
        >
          <Search className="h-5 w-5" strokeWidth={2.5} />
        </button>
      </div>

      <div className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-6">
        {submittedQuery === null && (
          <div className="flex flex-col items-center gap-2 pt-10 text-center">
            <Search className="h-10 w-10 text-slate-600" strokeWidth={1.5} />
            <p className="text-base font-bold text-white">사주 상품을 검색해보세요</p>
            <p className="text-xs text-slate-500">이별, 궁합 등 원하는 주제로 찾을 수 있어요</p>

            <div className="mt-6 flex flex-col items-center gap-3">
              <span className="text-xs font-semibold text-slate-500">인기 검색어</span>
              <div className="flex flex-wrap justify-center gap-2">
                {POPULAR_KEYWORDS.map(({ label, Icon }) => (
                  <button
                    key={label}
                    type="button"
                    onClick={() => runSearch(label)}
                    className="flex items-center gap-1.5 rounded-full border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-200"
                  >
                    <Icon className="h-4 w-4 text-pink-400" strokeWidth={2} />
                    {label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {results !== null && (
          <>
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white">&quot;{submittedQuery}&quot; 검색 결과</h3>
              <span className="text-xs text-slate-500">{results.length}개의 사주 상품</span>
            </div>

            {results.length === 0 ? (
              <div className="flex flex-1 flex-col items-center justify-center gap-2 pt-10 text-center">
                <SearchX className="h-10 w-10 text-slate-600" strokeWidth={1.5} />
                <p className="text-sm font-bold text-white">검색 결과가 없어요</p>
                <p className="text-xs text-slate-500">다른 검색어로 다시 시도해보세요</p>
                <Link to="/" className="mt-2 text-xs font-semibold text-violet-400 underline">
                  전체 상품 보러 가기
                </Link>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-4">
                {results.map((persona) => (
                  <PersonaCard key={persona.id} persona={persona} />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}
