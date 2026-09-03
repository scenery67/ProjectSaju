import { API_ROOT_URL } from '../api/client';

// JWT from the backend after a successful OAuth2 login. Not a session
// cookie — frontend (GitHub Pages) and backend (Fly.io) are different
// origins, so the token travels as an Authorization header instead.
const STORAGE_KEY = 'saju.auth.token.v1';

export type OAuthProvider = 'kakao' | 'google' | 'naver';

export function loginUrl(provider: OAuthProvider): string {
  return `${API_ROOT_URL}/oauth2/authorization/${provider}`;
}

// Deliberately not linked from any page: /api/auth/dev-admin-login requires a
// ?key= secret (ADMIN_BYPASS_SECRET) that must never ship in the frontend
// bundle — the team hits it directly (e.g. a private bookmark), not via the UI.

export function getAuthToken(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setAuthToken(token: string): void {
  try {
    localStorage.setItem(STORAGE_KEY, token);
  } catch {
    // ignore — private browsing / storage disabled
  }
}

export function clearAuthToken(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // ignore
  }
}

/**
 * 서버에 이 토큰을 무효화해달라고 알린 다음 로컬 저장소를 지운다. 서버
 * 호출이 실패해도(오프라인 등) 로컬은 항상 지운다 — 그렇지 않으면 사용자가
 * "로그아웃"을 눌렀는데 화면상 로그인 상태가 그대로 남는 이상한 경험이 된다.
 * (탈취된 토큰 자체는 이걸로 막을 수 없고, 이건 정상적인 로그아웃 흐름에서
 * 서버 쪽도 그 토큰을 더는 신뢰하지 않게 만드는 것뿐이다.)
 */
export async function logout(): Promise<void> {
  const token = getAuthToken();
  if (token) {
    try {
      await fetch(`${API_ROOT_URL}/api/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch {
      // ignore — 로컬 로그아웃은 아래에서 항상 진행된다
    }
  }
  clearAuthToken();
}

export interface CurrentUser {
  provider: string;
  nickname: string;
  isAdmin: boolean;
}

/**
 * Returns null if not logged in, the token is no longer valid, or the
 * request itself fails (offline, backend unreachable/asleep) — never
 * rejects. A rejected promise here left callers' "확인 중..." loading state
 * stuck forever with no way to see the login/logout buttons, since nothing
 * caught the error.
 */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  const token = getAuthToken();
  if (!token) return null;

  try {
    const res = await fetch(`${API_ROOT_URL}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) {
      if (res.status === 401) clearAuthToken();
      return null;
    }
    return (await res.json()) as CurrentUser;
  } catch {
    return null;
  }
}
