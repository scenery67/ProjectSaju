import { Route, Routes, useLocation } from 'react-router-dom';
import BottomNav from './components/BottomNav';
import Header from './components/Header';
import { UserProvider } from './contexts/UserProvider';
import AdminPage from './pages/AdminPage';
import AuthCallbackPage from './pages/AuthCallbackPage';
import ConsultationListPage from './pages/ConsultationListPage';
import ConsultationPage from './pages/ConsultationPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import MyPage from './pages/MyPage';
import MySajuPage from './pages/MySajuPage';
import NotificationsPage from './pages/NotificationsPage';
import PaymentFailPage from './pages/PaymentFailPage';
import PaymentHistoryPage from './pages/PaymentHistoryPage';
import PaymentSuccessPage from './pages/PaymentSuccessPage';
import PersonaDetailPage from './pages/PersonaDetailPage';
import ResultPage from './pages/ResultPage';
import RewardsPage from './pages/RewardsPage';
import SettingsPage from './pages/SettingsPage';
import ShopPage from './pages/ShopPage';

// 로그인 화면과 결제 결과 화면은 참고 사이트처럼 사이트 전체 내비게이션
// 없이 독립된 화면으로 둔다 — 외부(토스)에서 돌아오는 짧은 처리 화면이라
// 하단 탭 등 평소 UI가 어색하다.
const CHROME_FREE_PATHS = ['/login', '/payment/success', '/payment/fail'];

export default function App() {
  const location = useLocation();
  const showChrome = !CHROME_FREE_PATHS.includes(location.pathname);

  return (
    <UserProvider>
      {showChrome && <Header />}
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/persona/:personaId" element={<PersonaDetailPage />} />
        <Route path="/persona/:personaId/result" element={<ResultPage />} />
        <Route path="/my-saju" element={<MySajuPage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/consultations" element={<ConsultationListPage />} />
        <Route path="/consultation/:sessionId" element={<ConsultationPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route path="/payment/success" element={<PaymentSuccessPage />} />
        <Route path="/payment/fail" element={<PaymentFailPage />} />
        <Route path="/shop" element={<ShopPage />} />
        <Route path="/rewards" element={<RewardsPage />} />
        <Route path="/payments" element={<PaymentHistoryPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
      </Routes>
      {showChrome && <BottomNav />}
    </UserProvider>
  );
}
