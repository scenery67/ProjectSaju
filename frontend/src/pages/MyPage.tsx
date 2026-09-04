import AccountMenu from '../components/AccountMenu';

// 하단 탭("마이페이지")으로 들어오면 전체 화면으로, 헤더의 계정 버튼으로는
// 드롭다운으로 — 내용은 AccountMenu 하나를 공유한다.
export default function MyPage() {
  return (
    <main className="flex flex-1 flex-col px-4 pb-6 pt-5">
      <h2 className="mb-4 text-2xl font-bold tracking-tight text-white">마이페이지</h2>
      <AccountMenu />
    </main>
  );
}
