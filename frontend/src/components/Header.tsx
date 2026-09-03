import { Link } from 'react-router-dom';
import { getAuthToken } from '../lib/auth';

// 참고 사이트(foxbunny.io/saju)처럼 로고(좌) + 검색/로그인(우) 구조로 —
// 예전엔 제목만 가운데 있었다. 검색은 아직 실제 검색 기능은 없고(사주
// 상품이 2종뿐이라 당장은 의미가 적음) 홈으로 보내는 자리표시자다.
export default function Header() {
  const loggedIn = Boolean(getAuthToken());

  return (
    <header className="sticky top-0 z-10 flex items-center justify-between border-b border-neutral-800 bg-neutral-900/90 px-4 py-3.5 backdrop-blur">
      <Link to="/" className="text-lg font-extrabold tracking-tight text-white">
        사주 서비스
      </Link>
      <div className="flex items-center gap-3">
        <Link to="/" aria-label="홈" className="text-lg text-neutral-400">
          🔍
        </Link>
        {loggedIn ? (
          <Link
            to="/mypage"
            aria-label="마이페이지"
            className="flex h-8 w-8 items-center justify-center rounded-full border border-violet-700 bg-violet-950 text-base"
          >
            👤
          </Link>
        ) : (
          <Link
            to="/login"
            className="rounded-full bg-violet-600 px-3.5 py-1.5 text-xs font-bold text-white"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  );
}
