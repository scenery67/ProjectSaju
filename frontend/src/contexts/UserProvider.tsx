import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { UserContext } from './UserContext';
import { fetchCurrentUser, getAuthToken, logout as logoutRequest, type CurrentUser } from '../lib/auth';
import { fetchBalance } from '../lib/billing';

/**
 * 앱 루트에 한 번만 두고 로그인 사용자/잔액을 여기서만 불러온다. 예전엔
 * 헤더·드롭다운·마이페이지·상점 등 화면마다 각자 fetchCurrentUser를 불러서,
 * 화면을 옮길 때마다 이미 아는 값인데도 다시 "확인 중..."이 떴다(참고
 * 사이트는 한 번만 불러오고 재사용해서 즉시 열림) — 이 컨텍스트로 통일한다.
 */
export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null | undefined>(() =>
    getAuthToken() ? undefined : null,
  );
  const [creditBalance, setCreditBalance] = useState<number | null>(null);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchCurrentUser().then(setUser);
  }, []);

  useEffect(() => {
    if (!user) return;
    fetchBalance().then((b) => setCreditBalance(b?.creditBalance ?? null));
  }, [user]);

  // useCallback으로 참조를 고정한다 — AuthCallbackPage 등에서 이 함수를
  // useEffect 의존성 배열에 안전하게 넣으려면(exhaustive-deps) 매 렌더마다
  // 새 함수가 되면 안 된다(그러면 그 effect가 렌더할 때마다 다시 실행된다).
  const refreshUser = useCallback(async () => {
    if (!getAuthToken()) {
      setUser(null);
      return;
    }
    setUser(await fetchCurrentUser());
  }, []);

  const refreshBalance = useCallback(async () => {
    if (!getAuthToken()) {
      setCreditBalance(null);
      return;
    }
    const balance = await fetchBalance();
    setCreditBalance(balance?.creditBalance ?? null);
  }, []);

  const logout = useCallback(async () => {
    await logoutRequest();
    setUser(null);
    setCreditBalance(null);
  }, []);

  return (
    <UserContext.Provider value={{ user, creditBalance, setUser, refreshUser, refreshBalance, logout }}>
      {children}
    </UserContext.Provider>
  );
}
