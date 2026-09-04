import { API_ROOT_URL } from '../api/client';
import { clearAuthToken, getAuthToken } from './auth';

// 결제창형 SDK(v1/payment)가 전역에 심어두는 생성자. 위젯형(v2/standard)의
// widgets() API와는 다르니 섞어 쓰지 않는다.
declare global {
  interface Window {
    TossPayments?: (clientKey: string) => {
      requestPayment: (
        method: string,
        params: {
          amount: number;
          orderId: string;
          orderName: string;
          customerName?: string;
          successUrl: string;
          failUrl: string;
        },
      ) => Promise<void>;
    };
  }
}

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

// 결제창을 띄우기 전, 서버에 PENDING 결제 레코드부터 만든다 — 이 id를
// 토스의 orderId로 그대로 쓴다(BillingController 참고).
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

/**
 * 결제창형(v1) SDK로 토스 결제창을 띄운다 — 성공/실패 시 브라우저가 각각
 * successUrl/failUrl로 이동한다(현재 페이지를 떠남). 카드 결제만 우선
 * 지원 — 카카오페이 등 다른 수단은 나중에 방식 선택 UI와 함께 추가한다.
 */
export function startTossCheckout(
  payment: { id: string; amountKrw: number },
  orderName: string,
  customerName: string,
): Promise<void> {
  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
  if (!clientKey || !window.TossPayments) {
    return Promise.reject(new Error('토스 결제 SDK 또는 클라이언트 키가 설정되지 않았어요.'));
  }
  const tossPayments = window.TossPayments(clientKey);
  const baseUrl = `${window.location.origin}${window.location.pathname}`;
  return tossPayments.requestPayment('카드', {
    amount: payment.amountKrw,
    orderId: payment.id,
    orderName,
    customerName,
    successUrl: `${baseUrl}#/payment/success`,
    failUrl: `${baseUrl}#/payment/fail`,
  });
}

/** 토스 결제창에서 돌아온 뒤 호출 — 서버가 토스에 직접 재확인한 뒤에만 크레딧을 지급한다. */
export async function confirmPurchase(
  paymentId: string,
  paymentKey: string,
  amount: number,
): Promise<PaymentHistoryEntry | null> {
  const token = getAuthToken();
  if (!token) return null;

  const res = await fetch(`${API_ROOT_URL}/api/billing/purchases/${encodeURIComponent(paymentId)}/confirm`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ paymentKey, amount }),
  });
  if (res.status === 401) clearAuthToken();
  if (!res.ok) return null;
  return res.json() as Promise<PaymentHistoryEntry>;
}
