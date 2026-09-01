import type { PersonaType, SajuReadingResult } from '../types/saju';

// 로그인 없이 이 기기에서만 유지되는 최근 결과 기록.
// 서버에는 저장하지 않는다 — 계정(OAuth) 도입 전까지의 임시 방편.
// Local-only reading history (this device only, no server persistence)
// until OAuth-based accounts land (see docs/ROADMAP.md Task 007).

export interface SajuHistoryEntry {
  id: string;
  personaId: string;
  personaType: PersonaType;
  result: SajuReadingResult;
  createdAt: string; // ISO 8601
}

const STORAGE_KEY = 'saju.history.v1';
const MAX_ENTRIES = 20;

export function saveReadingToHistory(
  personaId: string,
  result: SajuReadingResult,
): void {
  const entry: SajuHistoryEntry = {
    id: crypto.randomUUID(),
    personaId,
    personaType: result.personaType,
    result,
    createdAt: new Date().toISOString(),
  };
  try {
    const existing = getHistory();
    const updated = [entry, ...existing].slice(0, MAX_ENTRIES);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  } catch {
    // localStorage 접근 불가(프라이빗 모드 등) — 기록 저장만 조용히 건너뛴다.
  }
}

export function getHistory(): SajuHistoryEntry[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as SajuHistoryEntry[]) : [];
  } catch {
    return [];
  }
}

export function clearHistory(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // no-op
  }
}
