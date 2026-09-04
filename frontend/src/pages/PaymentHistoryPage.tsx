import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAuthToken } from '../lib/auth';
import { fetchPaymentHistory, type PaymentHistoryEntry, type PaymentStatus } from '../lib/billing';

const STATUS_LABEL: Record<PaymentStatus, string> = {
  PENDING: '결제 대기중',
  COMPLETED: '완료',
  FAILED: '실패',
  CANCELLED: '취소됨',
  REFUNDED: '환불됨',
};

function formatKrw(amount: number): string {
  return `${amount.toLocaleString('ko-KR')}원`;
}

export default function PaymentHistoryPage() {
  const [payments, setPayments] = useState<PaymentHistoryEntry[] | null>(null);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchPaymentHistory().then(setPayments);
  }, []);

  if (!getAuthToken()) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-center text-sm text-neutral-500">
        로그인하면 결제내역을 볼 수 있어요.
        <Link to="/login" className="font-semibold text-violet-500 underline">
          로그인하기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-4 px-4 pb-6 pt-5">
      <h2 className="text-2xl font-bold tracking-tight text-white">결제내역</h2>

      {payments === null && <p className="text-xs text-neutral-400">불러오는 중...</p>}

      {payments?.length === 0 && (
        <div className="flex flex-1 flex-col items-center justify-center gap-3 rounded-3xl border border-neutral-800 bg-neutral-900 p-10 text-center">
          <span className="text-4xl text-neutral-600">🛒</span>
          <p className="text-sm font-bold text-white">결제내역이 없습니다</p>
          <p className="text-xs text-neutral-500">크레딧을 충전하고 캐릭터와 대화를 나눠보세요!</p>
          <Link
            to="/shop"
            className="mt-1 rounded-full bg-gradient-to-r from-cyan-500 to-violet-500 px-6 py-2.5 text-sm font-bold text-white"
          >
            상점 가기
          </Link>
        </div>
      )}

      {payments && payments.length > 0 && (
        <ul className="flex flex-col gap-2">
          {payments.map((payment) => (
            <li
              key={payment.id}
              className="flex items-center justify-between rounded-2xl border border-neutral-800 bg-neutral-900 p-4"
            >
              <div className="flex flex-col">
                <span className="text-sm font-semibold text-white">
                  {payment.creditAmount.toLocaleString('ko-KR')} 크레딧 · {formatKrw(payment.amountKrw)}
                </span>
                <span className="text-[11px] text-neutral-400">
                  {new Date(payment.createdAt).toLocaleString('ko-KR')}
                </span>
              </div>
              <span className="rounded-full bg-neutral-800 px-3 py-1 text-[11px] font-semibold text-neutral-400">
                {STATUS_LABEL[payment.status]}
              </span>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
