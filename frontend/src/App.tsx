import { Route, Routes, useLocation } from 'react-router-dom';
import BottomNav from './components/BottomNav';
import Header from './components/Header';
import AdminPage from './pages/AdminPage';
import AuthCallbackPage from './pages/AuthCallbackPage';
import ConsultationListPage from './pages/ConsultationListPage';
import ConsultationPage from './pages/ConsultationPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import MyPage from './pages/MyPage';
import MySajuPage from './pages/MySajuPage';
import PersonaDetailPage from './pages/PersonaDetailPage';
import ResultPage from './pages/ResultPage';

// 로그인 화면은 참고 사이트처럼 사이트 전체 내비게이션 없이 독립된
// 화면으로 둔다 — 아직 "앱 안"에 들어오기 전이라는 느낌을 준다.
const CHROME_FREE_PATHS = ['/login'];

export default function App() {
  const location = useLocation();
  const showChrome = !CHROME_FREE_PATHS.includes(location.pathname);

  return (
    <>
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
      </Routes>
      {showChrome && <BottomNav />}
    </>
  );
}
