import { Route, Routes } from 'react-router-dom';
import BottomNav from './components/BottomNav';
import Header from './components/Header';
import HomePage from './pages/HomePage';
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
        <Route
          path="/my-saju"
          element={<PlaceholderPage title="내 사주" />}
        />
        <Route path="/mypage" element={<PlaceholderPage title="마이페이지" />} />
      </Routes>
      <BottomNav />
    </>
  );
}
