import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  checkInAttendance,
  fetchAttendanceStatus,
  type AttendanceStatus,
} from '../lib/attendance';
import {
  fetchCurrentUser,
  getAuthToken,
  logout,
  type CurrentUser,
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

  const [attendance, setAttendance] = useState<AttendanceStatus | null>(null);
  const [checkingIn, setCheckingIn] = useState(false);
  const [justCheckedDay, setJustCheckedDay] = useState<number | null>(null);
  const [popups, setPopups] = useState<{ id: number; text: string; className: string }[]>([]);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchCurrentUser().then(setUser);
  }, []);

  useEffect(() => {
    if (!user) return;
    fetchBalance().then((b) => setCreditBalance(b?.creditBalance ?? null));
    fetchPackages().then(setPackages);
    fetchPaymentHistory().then(setPayments);
    fetchAttendanceStatus().then(setAttendance);
  }, [user]);

  async function handlePurchase(pkg: CreditPackage) {
    setPurchasingId(pkg.id);
    const payment = await purchasePackage(pkg.id);
    if (payment) {
      setPayments((prev) => [payment, ...(prev ?? [])]);
    }
    setPurchasingId(null);
  }

  async function handleCheckIn() {
    setCheckingIn(true);
    const result = await checkInAttendance();
    if (result) {
      setAttendance({ checkedInToday: true, streak: result.streak, baseReward: 0, bonusReward: 0 });
      setCreditBalance((prev) => (prev ?? 0) + result.baseReward + result.bonusReward);

      // 방금 채워진 원에 팝 이펙트 + "+N" 문구를 띄운다. 보너스가 있는
      // 날이면(7일째 등) 기본/보너스를 각각 따로 보여준다.
      setJustCheckedDay(((result.streak - 1) % 7) + 1);
      const effects = [{ id: Date.now(), text: `+${result.baseReward}`, className: 'text-violet-300' }];
      if (result.bonusReward > 0) {
        effects.push({
          id: Date.now() + 1,
          text: `+${result.bonusReward} 보너스`,
          className: 'text-amber-400',
        });
      }
      setPopups(effects);
      setTimeout(() => setPopups([]), 1300);
      setTimeout(() => setJustCheckedDay(null), 500);
    } else {
      // 이미 체크했거나(다른 탭 등) 네트워크 실패 — 실제 최신 상태로 다시 맞춘다.
      fetchAttendanceStatus().then(setAttendance);
    }
    setCheckingIn(false);
  }

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-2xl font-bold tracking-tight text-white">
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
        <section className="flex flex-col items-center gap-3 rounded-3xl border border-neutral-800 bg-neutral-900 p-8 text-center">
          <p className="text-sm text-neutral-400">
            로그인하면 크레딧, 상담 기록을 계정에 저장할 수 있어요.
          </p>
          <Link
            to="/login"
            className="rounded-full bg-violet-600 px-6 py-3 text-sm font-bold text-white"
          >
            로그인하기
          </Link>
        </section>
      )}

      {user && (
        <>
          <section className="flex flex-col gap-3 rounded-3xl border border-neutral-800 bg-neutral-900 p-5">
            <p className="text-sm text-neutral-100">
              <span className="font-bold">{user.nickname || '(닉네임 없음)'}</span>
              님, {user.provider} 계정으로 로그인됐어요.
            </p>
            {user.isAdmin && (
              <Link
                to="/admin"
                className="rounded-full bg-violet-600 py-3 text-center text-sm font-semibold text-white"
              >
                관리자 화면
              </Link>
            )}
            <button
              type="button"
              className="rounded-full border border-neutral-800 py-3 text-sm font-semibold text-neutral-500"
              onClick={() => {
                void logout();
                setUser(null);
                setCreditBalance(null);
                setPackages(null);
                setPayments(null);
              }}
            >
              로그아웃
            </button>
          </section>

          <section className="flex flex-col gap-1 rounded-3xl border border-violet-900/50 bg-gradient-to-br from-violet-950 to-neutral-900 p-5 text-white">
            <span className="text-xs text-violet-300">보유 크레딧</span>
            <span className="text-3xl font-bold tracking-tight">
              {creditBalance === null ? '—' : `${creditBalance.toLocaleString('ko-KR')} 크레딧`}
            </span>
          </section>

          {attendance && (
            <section className="relative flex flex-col gap-4 overflow-hidden rounded-3xl border border-neutral-800 bg-neutral-900 p-5">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-white">출석 체크</h3>
                <span className="text-xs text-neutral-400">
                  {attendance.streak}일째 {attendance.checkedInToday ? '연속 출석 중' : '도전 중'}
                </span>
              </div>

              <div className="flex justify-between">
                {Array.from({ length: 7 }, (_, i) => i + 1).map((day) => {
                  const activePosition = ((attendance.streak - 1) % 7) + 1;
                  const filledCount = attendance.checkedInToday ? activePosition : activePosition - 1;
                  const filled = day <= filledCount;
                  const isToday = !attendance.checkedInToday && day === activePosition;
                  return (
                    <div key={day} className="flex flex-col items-center gap-1">
                      <div
                        className={[
                          'flex h-8 w-8 items-center justify-center rounded-full border text-xs font-bold transition-colors duration-300',
                          filled
                            ? 'border-violet-500 bg-violet-500 text-white'
                            : isToday
                              ? 'border-violet-400 text-violet-300'
                              : 'border-neutral-700 text-neutral-500',
                          justCheckedDay === day ? 'animate-check-pop' : '',
                        ].join(' ')}
                      >
                        {filled ? '✓' : day}
                      </div>
                      <span className={`text-[9px] ${day === 7 ? 'text-amber-400' : 'text-transparent'}`}>
                        +3
                      </span>
                    </div>
                  );
                })}
              </div>

              <button
                type="button"
                disabled={attendance.checkedInToday || checkingIn}
                onClick={handleCheckIn}
                className="rounded-full bg-violet-500 py-3 text-sm font-bold text-white disabled:opacity-40"
              >
                {attendance.checkedInToday
                  ? '오늘 체크 완료'
                  : checkingIn
                    ? '처리 중...'
                    : `출석 체크하고 +${attendance.baseReward + attendance.bonusReward} 받기`}
              </button>

              {popups.length > 0 && (
                <div className="pointer-events-none absolute inset-x-0 top-14 flex flex-col items-center gap-1">
                  {popups.map((p) => (
                    <span
                      key={p.id}
                      className={`animate-float-up-fade text-lg font-extrabold drop-shadow-[0_2px_4px_rgba(0,0,0,0.6)] ${p.className}`}
                    >
                      {p.text}
                    </span>
                  ))}
                </div>
              )}
            </section>
          )}

          <section className="flex flex-col gap-3">
            <h3 className="text-sm font-bold text-white">충전하기</h3>
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
                  className="flex items-center justify-between rounded-2xl border border-neutral-800 bg-neutral-900 p-4"
                >
                  <div className="flex flex-col">
                    <span className="text-sm font-bold text-white">{pkg.name}</span>
                    <span className="text-xs text-neutral-500">
                      {pkg.creditAmount.toLocaleString('ko-KR')} 크레딧 · {formatKrw(pkg.priceKrw)}
                    </span>
                  </div>
                  <button
                    type="button"
                    disabled={purchasingId === pkg.id}
                    className="rounded-full bg-violet-500 px-4 py-2 text-xs font-bold text-white disabled:opacity-50"
                    onClick={() => handlePurchase(pkg)}
                  >
                    {purchasingId === pkg.id ? '처리 중...' : '충전'}
                  </button>
                </li>
              ))}
            </ul>
          </section>

          <section className="flex flex-col gap-3">
            <h3 className="text-sm font-bold text-white">결제내역</h3>
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
          </section>
        </>
      )}
    </main>
  );
}
