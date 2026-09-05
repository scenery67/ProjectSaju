import { Frown } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';

// 토스 결제창에서 실패/취소로 돌아왔을 때: #/payment/fail?code=...&message=...
// (사용자가 결제창을 닫거나 카드가 거절된 경우 등) — 서버를 부를 필요 없이
// 토스가 준 사유만 보여주면 된다.
export default function PaymentFailPage() {
  const [searchParams] = useSearchParams();
  const message = searchParams.get('message');

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-10 text-center">
      <Frown className="h-14 w-14 text-slate-500" strokeWidth={1.5} />
      <p className="text-sm font-bold text-white">결제가 완료되지 않았어요.</p>
      {message && <p className="text-xs text-slate-400">{message}</p>}
      <Link to="/mypage" className="rounded-full bg-violet-500 px-6 py-3 text-sm font-bold text-white">
        마이페이지로
      </Link>
    </main>
  );
}
