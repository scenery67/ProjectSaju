import { CheckCircle2, TriangleAlert } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { confirmPurchase } from '../lib/billing';

// 토스 결제창에서 성공적으로 돌아왔을 때의 착지 화면:
// #/payment/success?paymentKey=...&orderId=...&amount=...
// 여기서 파라미터만 보고 크레딧을 주면 안 된다 — 서버가 토스에 직접
// 재확인한 뒤에만 지급하도록 confirmPurchase를 호출한다.
export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const paymentKey = searchParams.get('paymentKey');
  const orderId = searchParams.get('orderId');
  const amount = searchParams.get('amount');
  const hasRequiredParams = Boolean(paymentKey && orderId && amount);

  // 파라미터가 애초에 없는 경우는 렌더 시점에 바로 알 수 있으니(외부 상태
  // 동기화가 필요 없음) 초기값으로 바로 반영한다 — 이펙트 안에서 동기적으로
  // setState 하지 않는다.
  const [status, setStatus] = useState<'confirming' | 'done' | 'failed'>(
    hasRequiredParams ? 'confirming' : 'failed',
  );
  const [creditAmount, setCreditAmount] = useState<number | null>(null);

  useEffect(() => {
    if (!hasRequiredParams) return;
    confirmPurchase(orderId!, paymentKey!, Number(amount)).then((result) => {
      if (result) {
        setCreditAmount(result.creditAmount);
        setStatus('done');
      } else {
        setStatus('failed');
      }
    });
  }, [hasRequiredParams, orderId, paymentKey, amount]);

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-10 text-center">
      {status === 'confirming' && (
        <p className="text-sm text-slate-400">결제를 확인하는 중이에요...</p>
      )}
      {status === 'done' && (
        <>
          <CheckCircle2 className="h-14 w-14 text-emerald-400" strokeWidth={1.5} />
          <p className="text-sm font-bold text-white">
            {creditAmount?.toLocaleString('ko-KR')} 크레딧이 충전됐어요!
          </p>
        </>
      )}
      {status === 'failed' && (
        <>
          <TriangleAlert className="h-14 w-14 text-amber-400" strokeWidth={1.5} />
          <p className="text-sm font-bold text-white">결제 확인에 실패했어요.</p>
          <p className="text-xs text-slate-400">
            결제는 됐는데 크레딧 반영이 안 됐다면, 마이페이지 결제내역을 확인해주세요.
          </p>
        </>
      )}
      <Link to="/mypage" className="rounded-full bg-violet-500 px-6 py-3 text-sm font-bold text-white">
        마이페이지로
      </Link>
    </main>
  );
}
