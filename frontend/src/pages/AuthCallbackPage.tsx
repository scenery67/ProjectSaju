import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useUser } from '../contexts/useUser';
import { setAuthToken } from '../lib/auth';

// Landing page for the OAuth2 redirect back from the backend:
// #/auth/callback?token=... (success) or ?error=1 (failure).
export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { refreshUser } = useUser();

  useEffect(() => {
    const token = searchParams.get('token');
    if (token) {
      setAuthToken(token);
      // UserProvider는 앱이 처음 뜰 때 딱 한 번만 로그인 여부를 확인한다 —
      // 그 시점엔 토큰이 없었을 수 있으니, 토큰이 막 생긴 지금 다시 불러온다.
      refreshUser().then(() => navigate('/mypage', { replace: true }));
      return;
    }
    navigate('/mypage', { replace: true });
  }, [searchParams, navigate, refreshUser]);

  return (
    <main className="flex flex-1 items-center justify-center p-4 text-sm text-slate-400">
      로그인 처리 중...
    </main>
  );
}
