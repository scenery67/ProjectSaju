import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';

export interface AttendanceStatus {
  checkedInToday: boolean;
  // checkedInToday=true면 오늘 실제로 달성한 연속 일수, false면 "지금
  // 체크하면" 달성할 연속 일수(미리보기).
  streak: number;
  // checkedInToday=true면 0(이미 받음), false면 지금 체크 시 받을 크레딧 수.
  reward: number;
}

export interface AttendanceCheckInResult {
  streak: number;
  creditsGranted: number;
}

async function authedGet<T>(path: string): Promise<T | null> {
  const token = getAuthToken();
  if (!token) return null;
  const res = await fetch(`${API_ROOT_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    if (res.status === 401) clearAuthToken();
    return null;
  }
  return res.json() as Promise<T>;
}

export function fetchAttendanceStatus(): Promise<AttendanceStatus | null> {
  return authedGet<AttendanceStatus>('/api/attendance/status');
}

/** null이면 이미 오늘 체크했거나(409) 네트워크 실패 — 호출 쪽에서 status를 다시 불러오면 된다. */
export async function checkInAttendance(): Promise<AttendanceCheckInResult | null> {
  const token = getAuthToken();
  if (!token) return null;
  const res = await fetch(`${API_ROOT_URL}/api/attendance/check-in`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    if (res.status === 401) clearAuthToken();
    return null;
  }
  return res.json() as Promise<AttendanceCheckInResult>;
}
