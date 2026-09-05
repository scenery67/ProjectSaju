import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Emoji from '../components/Emoji';
import { useUser } from '../contexts/useUser';
import { getAuthToken } from '../lib/auth';
import { fetchPackages, purchasePackage, startTossCheckout, type CreditPackage } from '../lib/billing';

// 할인율/보너스를 스키마에 별도로 두지 않고(V3 마이그레이션 참고) 기준
// 단가로부터 계산한다 — 실제 판매가가 이 단가보다 저렴한 만큼을
// "보너스 크레딧"/"할인율"로 환산해 보여준다.
const BASE_UNIT_PRICE_KRW = 290;

function formatKrw(amount: number): string {
  return `${amount.toLocaleString('ko-KR')}원`;
}

export default function ShopPage() {
  const { creditBalance } = useUser();
  const [packages, setPackages] = useState<CreditPackage[] | null>(null);
  const [purchasingId, setPurchasingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!getAuthToken()) return;
    fetchPackages().then(setPackages);
  }, []);

  async function handlePurchase(pkg: CreditPackage) {
    setPurchasingId(pkg.id);
    setError(null);
    const payment = await purchasePackage(pkg.id);
    if (!payment) {
      setError('결제 준비에 실패했어요. 잠시 후 다시 시도해주세요.');
      setPurchasingId(null);
      return;
    }
    try {
      await startTossCheckout({ id: payment.id, amountKrw: payment.amountKrw }, pkg.name, '고객');
    } catch {
      setError('결제창을 여는 데 실패했어요.');
      setPurchasingId(null);
    }
  }

  if (!getAuthToken()) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-center text-sm text-neutral-500">
        로그인하면 크레딧을 충전할 수 있어요.
        <Link to="/login" className="font-semibold text-violet-500 underline">
          로그인하기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-5 px-4 pb-6 pt-5">
      <div className="flex flex-col gap-1">
        <h2 className="text-2xl font-bold tracking-tight text-white">상점</h2>
        <p className="text-xs text-neutral-400">사주 추가 상담에 쓰는 크레딧을 충전하세요</p>
      </div>

      <section className="flex items-center justify-between rounded-3xl border border-violet-900/50 bg-gradient-to-br from-violet-950 to-neutral-900 p-5">
        <div className="flex flex-col gap-1">
          <span className="text-xs text-violet-300">보유 크레딧</span>
          <span className="text-2xl font-bold text-white">
            {creditBalance === null ? '—' : creditBalance.toLocaleString('ko-KR')}
          </span>
        </div>
        <Link
          to="/payments"
          className="rounded-full border border-violet-700 px-4 py-2 text-xs font-semibold text-violet-300"
        >
          이용 내역
        </Link>
      </section>

      <div className="flex flex-col gap-1">
        <h3 className="text-sm font-bold text-white">크레딧 충전</h3>
        <p className="text-[11px] text-neutral-500">사주 상담에 바로 쓰는 크레딧 · 질문 1회 {BASE_UNIT_PRICE_KRW}크레딧</p>
      </div>

      {packages === null && <p className="text-xs text-neutral-400">불러오는 중...</p>}
      {error && <p className="text-xs font-medium text-violet-500">{error}</p>}

      <ul className="grid grid-cols-2 gap-3">
        {packages?.map((pkg, i) => {
          const unitPrice = pkg.priceKrw / pkg.creditAmount;
          const discountPercent = Math.round((1 - unitPrice / BASE_UNIT_PRICE_KRW) * 100);
          const bonusCredits = pkg.creditAmount - Math.round(pkg.priceKrw / BASE_UNIT_PRICE_KRW);
          const isFirst = i === 0;
          const isLast = i === packages.length - 1;

          return (
            <li
              key={pkg.id}
              className={`flex flex-col gap-2 rounded-2xl border p-4 ${
                isLast ? 'border-amber-700/60 bg-amber-950/10' : 'border-neutral-800 bg-neutral-900'
              }`}
            >
              <span className="text-sm font-bold text-white">{pkg.name}</span>
              <div className="flex flex-col">
                <span className="text-xs text-neutral-400">기본 {pkg.creditAmount.toLocaleString('ko-KR')}크레딧</span>
                {bonusCredits > 0 && (
                  <span className="text-xs font-semibold text-violet-400">
                    +{bonusCredits.toLocaleString('ko-KR')} 보너스
                  </span>
                )}
              </div>
              <span className="text-base font-bold text-white">{formatKrw(pkg.priceKrw)}</span>
              <span
                className={`w-fit rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                  isLast
                    ? 'bg-amber-900/60 text-amber-300'
                    : isFirst
                      ? 'bg-neutral-800 text-neutral-400'
                      : 'bg-violet-900/60 text-violet-300'
                }`}
              >
                {isLast ? '최대 혜택' : isFirst ? '기본 패키지' : `${discountPercent}% 절약`}
              </span>
              <button
                type="button"
                disabled={purchasingId === pkg.id}
                onClick={() => handlePurchase(pkg)}
                className="mt-1 rounded-full bg-violet-500 py-2 text-xs font-bold text-white disabled:opacity-50"
              >
                {purchasingId === pkg.id ? '처리 중...' : '구매하기'}
              </button>
            </li>
          );
        })}
      </ul>

      <section className="flex flex-col gap-2 rounded-2xl border border-neutral-800 bg-neutral-900 p-4">
        <h4 className="flex items-center gap-1.5 text-xs font-bold text-white">
          <Emoji name="clipboard" className="h-4 w-4" />
          유의사항
        </h4>
        <ul className="flex flex-col gap-1.5 text-[11px] leading-relaxed text-neutral-500">
          <li>상기 결제 금액은 VAT가 포함된 금액입니다.</li>
          <li>답변 품질과 같은 주관적인 사유로는 환불이 불가합니다.</li>
          <li>크레딧 패키지 결제 등은 전자상거래법에 따른 청약철회 규정이 적용될 수 있으나, 사용 여부 등에 따라 제한될 수 있습니다.</li>
          <li>추가 문의 사항은 설정 화면의 문의하기로 연락해주세요.</li>
        </ul>
      </section>
    </main>
  );
}
