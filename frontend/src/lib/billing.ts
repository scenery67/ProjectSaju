import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';

export interface CreditPackage {
  id: string;
  name: string;
  creditAmount: number;
  priceKrw: number;
}

export interface Balance {
  creditBalance: number;
}

export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'REFUNDED';

export interface PaymentHistoryEntry {
  id: string;
  creditAmount: number;
  amountKrw: number;
  status: PaymentStatus;
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

export function fetchPackages(): Promise<CreditPackage[] | null> {
  return authedGet<CreditPackage[]>('/api/billing/packages');
}

export function fetchBalance(): Promise<Balance | null> {
  return authedGet<Balance>('/api/billing/me');
}

export function fetchPaymentHistory(): Promise<PaymentHistoryEntry[] | null> {
  return authedGet<PaymentHistoryEntry[]>('/api/billing/payments');
}

// PG 연동 전이라 실제 결제는 안 되고, PENDING 상태 결제 레코드만 남긴다 —
// 결제내역에서 "결제 대기중"으로 보이는 게 정상이다.
export async function purchasePackage(creditPackageId: string): Promise<PaymentHistoryEntry | null> {
  const token = getAuthToken();
  if (!token) return null;

  const res = await fetch(`${API_ROOT_URL}/api/billing/purchases`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ creditPackageId }),
  });
  if (!res.ok) return null;
  return res.json() as Promise<PaymentHistoryEntry>;
}
