import { useContext } from 'react';
import { UserContext, type UserContextValue } from './UserContext';

export function useUser(): UserContextValue {
  const ctx = useContext(UserContext);
  if (!ctx) {
    throw new Error('useUser must be used within a UserProvider');
  }
  return ctx;
}
