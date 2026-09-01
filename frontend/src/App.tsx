import { Route, Routes } from 'react-router-dom';
import BottomNav from './components/BottomNav';
import Header from './components/Header';
import HomePage from './pages/HomePage';
import MySajuPage from './pages/MySajuPage';
import PersonaDetailPage from './pages/PersonaDetailPage';
import PlaceholderPage from './pages/PlaceholderPage';
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
        <Route
          path="/mypage"
          element={<PlaceholderPage title="마이페이지 (로그인 도입 후 제공 예정)" />}
        />
      </Routes>
      <BottomNav />
    </>
  );
}
