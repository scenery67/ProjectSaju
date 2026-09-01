import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter } from 'react-router-dom';
import App from './App.tsx';
import './index.css';

// HashRouter (not BrowserRouter): GitHub Pages has no server-side rewrite for
// SPA routes, so a direct link or page refresh on e.g. /persona/breakup would
// 404. Hash routes (/#/persona/breakup) always resolve client-side.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HashRouter>
      <App />
    </HashRouter>
  </StrictMode>,
);
