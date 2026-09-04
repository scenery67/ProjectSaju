import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REFUNDED';

export interface AdminPayment {
  id: string;
  userAccountId: string;
  creditAmount: number;
  amountKrw: number;
  status: PaymentStatus;
  pgProvider: string | null;
  pgTransactionId: string | null;
  createdAt: string;
  completedAt: string | null;
  refundedAt: string | null;
  refundedBy: string | null;
  refundReason: string | null;
}

export interface AdminTransaction {
  id: string;
  type: 'FREE_GRANT' | 'PURCHASE' | 'CONSUME' | 'REFUND' | 'ADMIN_ADJUST';
  amount: number;
  balanceAfter: number;
  note: string | null;
  createdAt: string;
}

export interface AdminUser {
  id: string;
  provider: string;
  nickname: string | null;
  creditBalance: number;
  isAdmin: boolean;
  createdAt: string;
  lastLoginAt: string;
}

async function authedFetch<T>(path: string, init?: RequestInit): Promise<T | null> {
  const token = getAuthToken();
  if (!token) return null;

  const res = await fetch(`${API_ROOT_URL}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      Authorization: `Bearer ${token}`,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    if (res.status === 401) clearAuthToken();
    return null;
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return null;
  }
  return res.json() as Promise<T>;
}

export interface AdminActionResult {
  ok: boolean;
  message?: string;
}

/**
 * adjustUserCredit처럼 boolean만 돌려주지 않고 실패 사유(message)까지 담는다 —
 * 탈퇴/권한변경의 자기 자신 보호 가드는 "왜 실패했는지"를 관리자에게
 * 보여줘야 의미가 있는 안내문이라(GlobalExceptionHandler가 그대로 내려줌).
 */
async function authedAction(path: string, init: RequestInit): Promise<AdminActionResult> {
  const token = getAuthToken();
  if (!token) return { ok: false };
  const res = await fetch(`${API_ROOT_URL}${path}`, {
    ...init,
    headers: {
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      Authorization: `Bearer ${token}`,
      ...init.headers,
    },
  });
  if (res.status === 401) clearAuthToken();
  if (res.ok) return { ok: true };
  try {
    const body = (await res.json()) as { message?: string };
    return { ok: false, message: body.message };
  } catch {
    return { ok: false };
  }
}

export function fetchAllPayments(): Promise<AdminPayment[] | null> {
  return authedFetch<AdminPayment[]>('/api/admin/payments?page=0&size=100');
}

/** query 없으면 최근 가입 50명, 있으면 닉네임 부분일치 또는 정확한 user_account_id로 검색. */
export function fetchUsers(query?: string): Promise<AdminUser[] | null> {
  const q = query?.trim() ? `?query=${encodeURIComponent(query.trim())}` : '';
  return authedFetch<AdminUser[]>(`/api/admin/users${q}`);
}

export function setUserAdmin(userAccountId: string, admin: boolean): Promise<AdminActionResult> {
  return authedAction(`/api/admin/users/${encodeURIComponent(userAccountId)}/admin`, {
    method: 'POST',
    body: JSON.stringify({ admin }),
  });
}

export function deleteUser(userAccountId: string): Promise<AdminActionResult> {
  return authedAction(`/api/admin/users/${encodeURIComponent(userAccountId)}`, {
    method: 'DELETE',
  });
}

export function fetchUserTransactions(userAccountId: string): Promise<AdminTransaction[] | null> {
  return authedFetch<AdminTransaction[]>(
    `/api/admin/users/${encodeURIComponent(userAccountId)}/transactions`,
  );
}

export async function refundPayment(paymentId: string, reason: string): Promise<AdminPayment | null> {
  return authedFetch<AdminPayment>(`/api/admin/payments/${encodeURIComponent(paymentId)}/refund`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
}

/** amount: 양수면 지급, 음수면 회수. 성공 시 응답 본문이 없다(204 취급). */
export async function adjustUserCredit(
  userAccountId: string,
  amount: number,
  reason: string,
): Promise<boolean> {
  const token = getAuthToken();
  if (!token) return false;
  const res = await fetch(
    `${API_ROOT_URL}/api/admin/users/${encodeURIComponent(userAccountId)}/credit-adjust`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ amount, reason }),
    },
  );
  if (res.status === 401) clearAuthToken();
  return res.ok;
}
