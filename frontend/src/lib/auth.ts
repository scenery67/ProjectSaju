import { API_ROOT_URL } from '../api/client';

// JWT from the backend after a successful OAuth2 login. Not a session
// cookie — frontend (GitHub Pages) and backend (Fly.io) are different
// origins, so the token travels as an Authorization header instead.
const STORAGE_KEY = 'saju.auth.token.v1';

export type OAuthProvider = 'kakao' | 'google' | 'naver';

export function loginUrl(provider: OAuthProvider): string {
  return `${API_ROOT_URL}/oauth2/authorization/${provider}`;
}

// Only responds while the backend's ADMIN_BYPASS_ENABLED is on (404 otherwise) —
// a stopgap for using the app as an admin before real OAuth apps are registered.
export function devAdminLoginUrl(): string {
  return `${API_ROOT_URL}/api/auth/dev-admin-login`;
}

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

export interface CurrentUser {
  provider: string;
  nickname: string;
}

/** Returns null if not logged in or the token is no longer valid. */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  const token = getAuthToken();
  if (!token) return null;

  const res = await fetch(`${API_ROOT_URL}/api/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    if (res.status === 401) clearAuthToken();
    return null;
  }
  return res.json() as Promise<CurrentUser>;
}
