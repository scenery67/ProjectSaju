import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [react(), tailwindcss()],
  // GitHub Pages serves a project site under /<repo>/, not the domain root —
  // but keep local dev at '/' so `npm run dev` URLs don't change.
  base: command === 'build' ? '/ProjectSaju/' : '/',
  server: {
    port: 5173,
  },
}))
