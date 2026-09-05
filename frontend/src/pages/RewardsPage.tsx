import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageTitle from '../components/PageTitle';
import {
  checkInAttendance,
  fetchAttendanceStatus,
  type AttendanceStatus,
} from '../lib/attendance';
import { useUser } from '../contexts/useUser';
import { getAuthToken } from '../lib/auth';

export default function RewardsPage() {
  const { creditBalance, refreshBalance } = useUser();
  const [attendance, setAttendance] = useState<AttendanceStatus | null>(null);
  const [checkingIn, setCheckingIn] = useState(false);
  const [justCheckedDay, setJustCheckedDay] = useState<number | null>(null);
  const [popups, setPopups] = useState<{ id: number; text: string; className: string }[]>([]);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchAttendanceStatus().then(setAttendance);
  }, []);

  async function handleCheckIn() {
    setCheckingIn(true);
    const result = await checkInAttendance();
    if (result) {
      setAttendance({ checkedInToday: true, streak: result.streak, baseReward: 0, bonusReward: 0 });
      void refreshBalance();

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
      fetchAttendanceStatus().then(setAttendance);
    }
    setCheckingIn(false);
  }

  if (!getAuthToken()) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-center text-sm text-slate-500">
        로그인하면 출석 보상을 받을 수 있어요.
        <Link to="/login" className="font-semibold text-violet-500 underline">
          로그인하기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <div className="flex flex-col gap-1">
        <PageTitle>내 혜택</PageTitle>
        <p className="text-xs text-slate-400">매일 출석하고, 다양한 혜택을 받아보세요</p>
      </div>

      <section className="flex items-center justify-between rounded-3xl border border-violet-900/50 bg-gradient-to-br from-violet-950 to-slate-900 p-5">
        <div className="flex flex-col gap-1">
          <span className="text-xs text-violet-300">보유 크레딧</span>
          <span className="text-2xl font-bold text-white">
            {creditBalance === null ? '—' : creditBalance.toLocaleString('ko-KR')}
          </span>
        </div>
        <Link to="/shop" className="rounded-full bg-violet-500 px-4 py-2 text-xs font-bold text-white">
          크레딧 충전
        </Link>
      </section>

      <div className="grid grid-cols-2 gap-3">
        {attendance && (
          <section className="relative col-span-2 flex flex-col gap-4 overflow-hidden rounded-3xl border border-slate-800 bg-slate-900 p-5 transition-colors hover:bg-slate-800/70 sm:col-span-1">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-white">출석 체크</h3>
              <span className="text-xs text-slate-400">
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
                            : 'border-slate-700 text-slate-500',
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

        <section className="col-span-2 flex flex-col gap-2 rounded-3xl border border-slate-800 bg-slate-900 p-5 opacity-60 transition-colors hover:bg-slate-800/70 sm:col-span-1">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-white">친구 초대</h3>
            <span className="rounded-full bg-slate-800 px-2 py-0.5 text-[10px] font-semibold text-slate-500">
              준비중
            </span>
          </div>
          <p className="text-xs text-slate-500">친구를 초대하면 서로 크레딧 보상을 받을 수 있어요</p>
        </section>
      </div>

      <section className="flex flex-col gap-1.5 rounded-2xl border border-slate-800 bg-slate-900 p-4 text-[11px] text-slate-500">
        <h4 className="mb-1 text-xs font-bold text-slate-300">안내사항</h4>
        <p>· 출석 보상은 매일 자정(한국시간)에 갱신됩니다</p>
        <p>· 연속 출석이 끊기면 1일차부터 다시 시작됩니다</p>
        <p>· 7일 연속 출석 완료 시 다음 날 새 출석판이 시작됩니다</p>
        <p>· 출석으로 받은 크레딧은 즉시 사용 가능합니다</p>
      </section>
    </main>
  );
}
