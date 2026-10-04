import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // ws: true обязателен — без него апгрейд соединения до WebSocket
      // не пройдёт через dev-сервер.
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      }
    }
  }
})