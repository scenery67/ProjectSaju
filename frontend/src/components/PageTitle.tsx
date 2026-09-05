import type { ReactNode } from 'react';

/** 화면 제목 — 굵은 큰 글자 + 짧은 보라색 밑줄(참고 사이트 공통 패턴). */
export default function PageTitle({ children }: { children: ReactNode }) {
  return (
    <div>
      <h2 className="text-2xl font-bold tracking-tight text-white">{children}</h2>
      <div className="mt-1.5 h-0.5 w-8 rounded-full bg-violet-500" />
    </div>
  );
}
