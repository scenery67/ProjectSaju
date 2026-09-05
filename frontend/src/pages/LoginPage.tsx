import { Link } from 'react-router-dom';
import { loginUrl, type OAuthProvider } from '../lib/auth';

// 각 공급자의 실제 브랜드 색을 써야 사용자가 로고 없이도 버튼을 바로 알아본다
// (카카오=노랑+검정 글자, 구글=흰 배경, 네이버=초록 — 참고 사이트 로그인
// 화면과 동일한 관례).
const PROVIDERS: { id: OAuthProvider; label: string; className: string }[] = [
  {
    id: 'kakao',
    label: '카카오로 시작하기',
    className: 'bg-[#FEE500] text-slate-900',
  },
  {
    id: 'google',
    label: 'Google로 시작하기',
    className: 'bg-white text-slate-900',
  },
  {
    id: 'naver',
    label: '네이버로 시작하기',
    className: 'bg-[#03C75A] text-white',
  },
];

// 로그인 UI를 마이페이지 안에 끼워넣지 않고 독립된 진입 화면으로 뺐다 —
// 참고 사이트(foxbunny.io/saju)도 로그인을 별도 화면으로 분리해 둔다.
export default function LoginPage() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 px-4 py-10">
      <div className="flex w-full max-w-sm flex-col gap-6 rounded-3xl border border-slate-800 bg-slate-900 p-8">
        <div className="flex flex-col items-center gap-2 text-center">
          <h1 className="text-2xl font-extrabold tracking-tight text-white">
            사주 서비스
          </h1>
          <p className="text-sm text-slate-400">
            이별과 인연, 사주로 다정하게 짚어드려요
          </p>
        </div>

        <div className="flex flex-col gap-2.5">
          {PROVIDERS.map((p) => (
            <a
              key={p.id}
              href={loginUrl(p.id)}
              className={`rounded-full py-3 text-center text-sm font-bold ${p.className}`}
            >
              {p.label}
            </a>
          ))}
        </div>

        <Link
          to="/"
          className="text-center text-xs font-medium text-slate-500 underline"
        >
          로그인 없이 둘러보기
        </Link>
      </div>
    </main>
  );
}
