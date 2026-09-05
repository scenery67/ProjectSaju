import { CreditCard, Gift, Inbox, Megaphone, type LucideIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageTitle from '../components/PageTitle';
import { getAuthToken } from '../lib/auth';
import {
  fetchNotifications,
  markAllNotificationsRead,
  type NotificationEntry,
} from '../lib/notifications';

const TYPE_ICON: Record<NotificationEntry['type'], LucideIcon> = {
  PAYMENT_COMPLETED: CreditCard,
  ATTENDANCE_BONUS: Gift,
  ADMIN_ANNOUNCEMENT: Megaphone,
};

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR');
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationEntry[] | null>(null);
  const [tab, setTab] = useState<'unread' | 'read'>('unread');

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchNotifications().then((list) => {
      setNotifications(list);
      // 목록을 불러온 뒤 전부 읽음 처리 — 다음에 열면 배지가 사라져 있다.
      markAllNotificationsRead();
    });
  }, []);

  if (!getAuthToken()) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-center text-sm text-neutral-500">
        로그인하면 알림을 볼 수 있어요.
        <Link to="/login" className="font-semibold text-violet-500 underline">
          로그인하기
        </Link>
      </main>
    );
  }

  const unread = notifications?.filter((n) => !n.read) ?? [];
  const read = notifications?.filter((n) => n.read) ?? [];
  const visible = tab === 'unread' ? unread : read;

  return (
    <main className="flex flex-1 flex-col gap-4 px-4 pb-6 pt-5">
      <div className="flex flex-col gap-1">
        <PageTitle>알림</PageTitle>
        <p className="text-xs text-neutral-400">공지와 크레딧 소식을 여기서 확인하세요.</p>
      </div>

      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setTab('unread')}
          className={`rounded-full px-4 py-1.5 text-xs font-bold ${
            tab === 'unread' ? 'bg-violet-600 text-white' : 'border border-neutral-800 text-neutral-400'
          }`}
        >
          안 읽음
        </button>
        <button
          type="button"
          onClick={() => setTab('read')}
          className={`rounded-full px-4 py-1.5 text-xs font-bold ${
            tab === 'read' ? 'bg-violet-600 text-white' : 'border border-neutral-800 text-neutral-400'
          }`}
        >
          읽음 {read.length > 0 && read.length}
        </button>
      </div>

      {notifications === null && <p className="text-xs text-neutral-400">불러오는 중...</p>}

      {notifications !== null && visible.length === 0 && (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 rounded-3xl bg-white/[0.03] p-10 text-center">
          <Inbox className="h-9 w-9 text-neutral-500" strokeWidth={1.5} />
          <p className="text-sm font-bold text-white">
            {tab === 'unread' ? '읽지 않은 알림이 없습니다' : '읽은 알림이 없습니다'}
          </p>
          <p className="text-xs text-neutral-500">새 알림이 도착하면 이곳에 표시됩니다.</p>
        </div>
      )}

      {visible.length > 0 && (
        <ul className="flex flex-col gap-2">
          {visible.map((n) => {
            const Icon = TYPE_ICON[n.type];
            return (
              <li
                key={n.id}
                className="flex gap-3 rounded-2xl border border-neutral-800 bg-neutral-900 p-4"
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-violet-950">
                  <Icon className="h-4.5 w-4.5 text-violet-300" strokeWidth={2} />
                </span>
                <div className="flex flex-1 flex-col gap-1">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-white">{n.title}</span>
                    <span className="text-[10px] text-neutral-500">{formatDate(n.createdAt)}</span>
                  </div>
                  <p className="text-xs text-neutral-400">{n.body}</p>
                  {n.creditAmount !== null && (
                    <span className="mt-1 w-fit rounded-full bg-amber-900/40 px-2.5 py-1 text-[11px] font-bold text-amber-300">
                      +{n.creditAmount.toLocaleString('ko-KR')} 크레딧
                    </span>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </main>
  );
}
