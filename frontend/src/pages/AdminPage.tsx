import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  adjustUserCredit,
  fetchAllPayments,
  fetchUserTransactions,
  refundPayment,
  type AdminPayment,
  type AdminTransaction,
  type PaymentStatus,
} from '../lib/admin';
import { fetchCurrentUser, getAuthToken } from '../lib/auth';

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

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR');
}

export default function AdminPage() {
  const [access, setAccess] = useState<'checking' | 'denied' | 'ok'>(
    getAuthToken() ? 'checking' : 'denied',
  );
  const [payments, setPayments] = useState<AdminPayment[] | null>(null);
  const [refundingId, setRefundingId] = useState<string | null>(null);
  const [refundReason, setRefundReason] = useState('');

  const [ledgerUserId, setLedgerUserId] = useState('');
  const [ledger, setLedger] = useState<AdminTransaction[] | null>(null);
  const [ledgerLoading, setLedgerLoading] = useState(false);

  const [adjustUserId, setAdjustUserId] = useState('');
  const [adjustAmount, setAdjustAmount] = useState('');
  const [adjustReason, setAdjustReason] = useState('');
  const [adjustMessage, setAdjustMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchCurrentUser().then((user) => {
      setAccess(user?.isAdmin ? 'ok' : 'denied');
    });
  }, []);

  useEffect(() => {
    if (access !== 'ok') return;
    fetchAllPayments().then(setPayments);
  }, [access]);

  async function loadLedger(userId: string) {
    setLedgerUserId(userId);
    setLedgerLoading(true);
    const entries = await fetchUserTransactions(userId);
    setLedger(entries);
    setLedgerLoading(false);
  }

  async function handleRefund(paymentId: string) {
    const updated = await refundPayment(paymentId, refundReason || '관리자 환불');
    if (updated) {
      setPayments((prev) => prev?.map((p) => (p.id === paymentId ? updated : p)) ?? null);
    }
    setRefundingId(null);
    setRefundReason('');
  }

  async function handleAdjust(e: React.FormEvent) {
    e.preventDefault();
    const amount = Number(adjustAmount);
    if (!adjustUserId || !amount || !adjustReason) {
      setAdjustMessage('사용자 ID·수량·사유를 모두 입력해주세요.');
      return;
    }
    const ok = await adjustUserCredit(adjustUserId, amount, adjustReason);
    setAdjustMessage(ok ? '적용됐어요.' : '실패했어요 — 사용자 ID를 확인해주세요.');
    if (ok && ledgerUserId === adjustUserId) {
      loadLedger(adjustUserId);
    }
  }

  if (access === 'checking') {
    return <main className="flex flex-1 items-center justify-center p-4 text-sm text-neutral-400">확인 중...</main>;
  }

  if (access === 'denied') {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        관리자만 볼 수 있는 화면이에요.
        <Link to="/mypage" className="font-semibold text-violet-500 underline">
          마이페이지로
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-6 px-4 pb-6 pt-5">
      <h2 className="text-2xl font-bold tracking-tight text-neutral-900">관리자</h2>

      <section className="flex flex-col gap-3">
        <h3 className="text-sm font-bold text-neutral-900">전체 결제 내역</h3>
        {payments === null && <p className="text-xs text-neutral-400">불러오는 중...</p>}
        {payments?.length === 0 && <p className="text-xs text-neutral-400">결제 내역이 없어요.</p>}
        <ul className="flex flex-col gap-2">
          {payments?.map((p) => (
            <li
              key={p.id}
              className="flex flex-col gap-2 rounded-2xl bg-white p-4 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex flex-col">
                  <span className="text-sm font-semibold text-neutral-900">
                    {p.creditAmount.toLocaleString('ko-KR')} 크레딧 · {formatKrw(p.amountKrw)}
                  </span>
                  <span className="text-[11px] text-neutral-400">{formatDate(p.createdAt)}</span>
                  <span className="font-mono text-[10px] text-neutral-400">{p.userAccountId}</span>
                  {p.refundReason && (
                    <span className="text-[11px] text-neutral-500">환불 사유: {p.refundReason}</span>
                  )}
                </div>
                <span className="shrink-0 rounded-full bg-neutral-100 px-3 py-1 text-[11px] font-semibold text-neutral-600">
                  {STATUS_LABEL[p.status]}
                </span>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  className="rounded-full border border-neutral-200 px-3 py-1.5 text-xs font-semibold text-neutral-600"
                  onClick={() => loadLedger(p.userAccountId)}
                >
                  이 사용자 원장 보기
                </button>
                {p.status === 'COMPLETED' && (
                  <button
                    type="button"
                    className="rounded-full border border-violet-200 px-3 py-1.5 text-xs font-semibold text-violet-500"
                    onClick={() => setRefundingId(refundingId === p.id ? null : p.id)}
                  >
                    환불
                  </button>
                )}
              </div>
              {refundingId === p.id && (
                <div className="flex gap-2">
                  <input
                    className="flex-1 rounded-xl border border-neutral-200 bg-neutral-50 px-3 py-2 text-xs outline-none focus:border-violet-300"
                    placeholder="환불 사유"
                    value={refundReason}
                    onChange={(e) => setRefundReason(e.target.value)}
                  />
                  <button
                    type="button"
                    className="rounded-full bg-violet-500 px-3 py-2 text-xs font-bold text-white"
                    onClick={() => handleRefund(p.id)}
                  >
                    확인
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="flex flex-col gap-3">
        <h3 className="text-sm font-bold text-neutral-900">사용자 크레딧 원장</h3>
        <div className="flex gap-2">
          <input
            className="flex-1 rounded-xl border border-neutral-200 bg-neutral-50 px-3.5 py-2.5 text-xs font-mono outline-none focus:border-violet-300 focus:bg-white"
            placeholder="user_account_id"
            value={ledgerUserId}
            onChange={(e) => setLedgerUserId(e.target.value)}
          />
          <button
            type="button"
            className="shrink-0 rounded-full border border-neutral-200 px-4 py-2 text-xs font-semibold text-neutral-600"
            onClick={() => ledgerUserId && loadLedger(ledgerUserId)}
          >
            조회
          </button>
        </div>
        {ledgerLoading && <p className="text-xs text-neutral-400">불러오는 중...</p>}
        {ledger !== null && !ledgerLoading && (
          <ul className="flex flex-col gap-2">
            {ledger.length === 0 && <p className="text-xs text-neutral-400">내역이 없어요.</p>}
            {ledger.map((t) => (
              <li
                key={t.id}
                className="flex items-center justify-between rounded-2xl bg-white p-3.5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]"
              >
                <div className="flex flex-col">
                  <span className="text-xs font-semibold text-neutral-800">
                    {t.type} {t.amount > 0 ? `+${t.amount}` : t.amount}
                  </span>
                  <span className="text-[11px] text-neutral-400">
                    잔액 {t.balanceAfter} · {formatDate(t.createdAt)}
                  </span>
                  {t.note && <span className="text-[11px] text-neutral-500">{t.note}</span>}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <h3 className="text-sm font-bold text-neutral-900">크레딧 수동 지급/회수</h3>
        <form className="flex flex-col gap-2 rounded-2xl bg-white p-4 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]" onSubmit={handleAdjust}>
          <input
            className="rounded-xl border border-neutral-200 bg-neutral-50 px-3.5 py-2.5 text-xs font-mono outline-none focus:border-violet-300 focus:bg-white"
            placeholder="user_account_id"
            value={adjustUserId}
            onChange={(e) => setAdjustUserId(e.target.value)}
          />
          <input
            className="rounded-xl border border-neutral-200 bg-neutral-50 px-3.5 py-2.5 text-xs outline-none focus:border-violet-300 focus:bg-white"
            placeholder="수량 (양수: 지급, 음수: 회수)"
            type="number"
            value={adjustAmount}
            onChange={(e) => setAdjustAmount(e.target.value)}
          />
          <input
            className="rounded-xl border border-neutral-200 bg-neutral-50 px-3.5 py-2.5 text-xs outline-none focus:border-violet-300 focus:bg-white"
            placeholder="사유"
            value={adjustReason}
            onChange={(e) => setAdjustReason(e.target.value)}
          />
          <button
            type="submit"
            className="rounded-full bg-violet-500 py-2.5 text-xs font-bold text-white"
          >
            적용
          </button>
          {adjustMessage && <p className="text-xs text-neutral-500">{adjustMessage}</p>}
        </form>
      </section>
    </main>
  );
}
