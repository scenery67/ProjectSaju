import { Route, Routes } from 'react-router-dom';
import BottomNav from './components/BottomNav';
import Header from './components/Header';
import AdminPage from './pages/AdminPage';
import AuthCallbackPage from './pages/AuthCallbackPage';
import ConsultationPage from './pages/ConsultationPage';
import HomePage from './pages/HomePage';
import MyPage from './pages/MyPage';
import MySajuPage from './pages/MySajuPage';
import PersonaDetailPage from './pages/PersonaDetailPage';
import ResultPage from './pages/ResultPage';

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/persona/:personaId" element={<PersonaDetailPage />} />
        <Route path="/persona/:personaId/result" element={<ResultPage />} />
        <Route path="/my-saju" element={<MySajuPage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/consultation/:sessionId" element={<ConsultationPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
      </Routes>
      <BottomNav />
    </>
  );
}
