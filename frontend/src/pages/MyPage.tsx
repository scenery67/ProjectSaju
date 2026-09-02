import { useEffect, useState } from 'react';
import {
  clearAuthToken,
  devAdminLoginUrl,
  fetchCurrentUser,
  getAuthToken,
  loginUrl,
  type CurrentUser,
  type OAuthProvider,
} from '../lib/auth';
import {
  fetchBalance,
  fetchPackages,
  fetchPaymentHistory,
  purchasePackage,
  type CreditPackage,
  type PaymentHistoryEntry,
  type PaymentStatus,
} from '../lib/billing';

const PROVIDERS: { id: OAuthProvider; label: string }[] = [
  { id: 'kakao', label: '카카오로 로그인' },
  { id: 'google', label: '구글로 로그인' },
  { id: 'naver', label: '네이버로 로그인' },
];

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

export default function MyPage() {
  const [user, setUser] = useState<CurrentUser | null | undefined>(() =>
    getAuthToken() ? undefined : null,
  );
  const [creditBalance, setCreditBalance] = useState<number | null>(null);
  const [packages, setPackages] = useState<CreditPackage[] | null>(null);
  const [payments, setPayments] = useState<PaymentHistoryEntry[] | null>(null);
  const [purchasingId, setPurchasingId] = useState<string | null>(null);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchCurrentUser().then(setUser);
  }, []);

  useEffect(() => {
    if (!user) return;
    fetchBalance().then((b) => setCreditBalance(b?.creditBalance ?? null));
    fetchPackages().then(setPackages);
    fetchPaymentHistory().then(setPayments);
  }, [user]);

  async function handlePurchase(pkg: CreditPackage) {
    setPurchasingId(pkg.id);
    const payment = await purchasePackage(pkg.id);
    if (payment) {
      setPayments((prev) => [payment, ...(prev ?? [])]);
    }
    setPurchasingId(null);
  }

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-2xl font-bold tracking-tight text-neutral-900">
          마이페이지
        </h2>
        <p className="text-xs text-neutral-400">
          로그인 기능 테스트 중이에요 — 결제는 아직 실제 PG 연동 전이라
          충전하면 대기 상태로만 등록돼요.
        </p>
      </div>

      {user === undefined && (
        <p className="text-sm text-neutral-400">확인 중...</p>
      )}

      {user === null && (
        <section className="flex flex-col gap-2 rounded-3xl bg-white p-5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]">
          {PROVIDERS.map((p) => (
            <a
              key={p.id}
              href={loginUrl(p.id)}
              className="rounded-full border border-neutral-200 py-3 text-center text-sm font-bold text-neutral-700"
            >
              {p.label}
            </a>
          ))}
          <a
            href={devAdminLoginUrl()}
            className="mt-2 rounded-full border border-dashed border-amber-300 bg-amber-50 py-3 text-center text-xs font-bold text-amber-700"
          >
            [개발용] 관리자로 로그인
          </a>
        </section>
      )}

      {user && (
        <>
          <section className="flex flex-col gap-3 rounded-3xl bg-white p-5 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]">
            <p className="text-sm text-neutral-800">
              <span className="font-bold">{user.nickname || '(닉네임 없음)'}</span>
              님, {user.provider} 계정으로 로그인됐어요.
            </p>
            <button
              type="button"
              className="rounded-full border border-neutral-200 py-3 text-sm font-semibold text-neutral-500"
              onClick={() => {
                clearAuthToken();
                setUser(null);
                setCreditBalance(null);
                setPackages(null);
                setPayments(null);
              }}
            >
              로그아웃
            </button>
          </section>

          <section className="flex flex-col gap-1 rounded-3xl bg-neutral-900 p-5 text-white shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]">
            <span className="text-xs text-neutral-400">보유 크레딧</span>
            <span className="text-3xl font-bold tracking-tight">
              {creditBalance === null ? '—' : `${creditBalance.toLocaleString('ko-KR')} 크레딧`}
            </span>
          </section>

          <section className="flex flex-col gap-3">
            <h3 className="text-sm font-bold text-neutral-900">충전하기</h3>
            {packages === null && (
              <p className="text-xs text-neutral-400">불러오는 중...</p>
            )}
            {packages?.length === 0 && (
              <p className="text-xs text-neutral-400">판매 중인 상품이 없어요.</p>
            )}
            <ul className="flex flex-col gap-2">
              {packages?.map((pkg) => (
                <li
                  key={pkg.id}
                  className="flex items-center justify-between rounded-2xl bg-white p-4 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]"
                >
                  <div className="flex flex-col">
                    <span className="text-sm font-bold text-neutral-900">{pkg.name}</span>
                    <span className="text-xs text-neutral-500">
                      {pkg.creditAmount.toLocaleString('ko-KR')} 크레딧 · {formatKrw(pkg.priceKrw)}
                    </span>
                  </div>
                  <button
                    type="button"
                    disabled={purchasingId === pkg.id}
                    className="rounded-full bg-rose-500 px-4 py-2 text-xs font-bold text-white disabled:opacity-50"
                    onClick={() => handlePurchase(pkg)}
                  >
                    {purchasingId === pkg.id ? '처리 중...' : '충전'}
                  </button>
                </li>
              ))}
            </ul>
          </section>

          <section className="flex flex-col gap-3">
            <h3 className="text-sm font-bold text-neutral-900">결제내역</h3>
            {payments === null && (
              <p className="text-xs text-neutral-400">불러오는 중...</p>
            )}
            {payments?.length === 0 && (
              <p className="text-xs text-neutral-400">아직 결제 내역이 없어요.</p>
            )}
            <ul className="flex flex-col gap-2">
              {payments?.map((payment) => (
                <li
                  key={payment.id}
                  className="flex items-center justify-between rounded-2xl bg-white p-4 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_8px_20px_-8px_rgba(0,0,0,0.1)]"
                >
                  <div className="flex flex-col">
                    <span className="text-sm font-semibold text-neutral-900">
                      {payment.creditAmount.toLocaleString('ko-KR')} 크레딧 · {formatKrw(payment.amountKrw)}
                    </span>
                    <span className="text-[11px] text-neutral-400">
                      {new Date(payment.createdAt).toLocaleString('ko-KR')}
                    </span>
                  </div>
                  <span className="rounded-full bg-neutral-100 px-3 py-1 text-[11px] font-semibold text-neutral-600">
                    {STATUS_LABEL[payment.status]}
                  </span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </main>
  );
}
