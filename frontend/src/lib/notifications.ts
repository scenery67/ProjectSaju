import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';

export type NotificationType = 'PAYMENT_COMPLETED' | 'ATTENDANCE_BONUS' | 'ADMIN_ANNOUNCEMENT';

export interface NotificationEntry {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  creditAmount: number | null;
  read: boolean;
  createdAt: string;
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

export function fetchNotifications(): Promise<NotificationEntry[] | null> {
  return authedGet<NotificationEntry[]>('/api/notifications');
}

export async function fetchUnreadCount(): Promise<number> {
  const result = await authedGet<{ count: number }>('/api/notifications/unread-count');
  return result?.count ?? 0;
}

/** 알림함 화면을 열었을 때 호출 — 그 시점까지 쌓인 안읽음을 전부 읽음으로 바꾼다. */
export async function markAllNotificationsRead(): Promise<void> {
  const token = getAuthToken();
  if (!token) return;
  await fetch(`${API_ROOT_URL}/api/notifications/read-all`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  }).catch(() => {
    // 실패해도 무시 — 다음에 화면을 열면 다시 시도된다
  });
}
