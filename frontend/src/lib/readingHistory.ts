import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';
import type { SajuReadingResult } from '../types/saju';

// Server-side history for logged-in users (backend: reading_record rows
// linked to user_account_id). Coexists with the device-local history in
// sajuHistory.ts rather than replacing it — see docs/ROADMAP.md Task 012.
export interface ServerHistoryEntry {
  id: string;
  createdAt: string; // ISO 8601
  result: SajuReadingResult;
}

export async function fetchServerHistory(): Promise<ServerHistoryEntry[] | null> {
  const token = getAuthToken();
  if (!token) return null;

  const res = await fetch(`${API_ROOT_URL}/api/saju/history`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    if (res.status === 401) clearAuthToken();
    return null;
  }
  return res.json() as Promise<ServerHistoryEntry[]>;
}
