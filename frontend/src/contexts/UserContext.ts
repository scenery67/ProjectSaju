import { createContext } from 'react';
import type { CurrentUser } from '../lib/auth';

export interface UserContextValue {
  /** undefined = 아직 서버 확인 전, null = 비로그인 */
  user: CurrentUser | null | undefined;
  creditBalance: number | null;
  setUser: (user: CurrentUser | null) => void;
  refreshUser: () => Promise<void>;
  refreshBalance: () => Promise<void>;
  logout: () => Promise<void>;
}

export const UserContext = createContext<UserContextValue | null>(null);
