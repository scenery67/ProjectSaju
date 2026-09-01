import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { setAuthToken } from '../lib/auth';

// Landing page for the OAuth2 redirect back from the backend:
// #/auth/callback?token=... (success) or ?error=1 (failure).
export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    if (token) {
      setAuthToken(token);
    }
    navigate('/mypage', { replace: true });
  }, [searchParams, navigate]);

  return (
    <main className="flex flex-1 items-center justify-center p-4 text-sm text-neutral-400">
      로그인 처리 중...
    </main>
  );
}
