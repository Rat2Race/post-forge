import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    open: true,
    proxy: {
      // dev에서 CORS 우회를 위해 프록시 적용
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/articles': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/comments': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
