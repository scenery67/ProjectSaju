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

export function fetchAllPayments(): Promise<AdminPayment[] | null> {
  return authedFetch<AdminPayment[]>('/api/admin/payments?page=0&size=100');
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
