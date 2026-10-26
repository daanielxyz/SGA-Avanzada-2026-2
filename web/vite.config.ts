/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// En desarrollo, Vite reenvía todo lo que empieza por /api al backend. Así el navegador cree que la web y la API
// están en el mismo sitio y no hace falta CORS. SGA_API_URL cambia la dirección del backend si no es la de siempre.
export default defineConfig(({ mode }) => {
  const entorno = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': entorno.SGA_API_URL ?? 'http://localhost:8080',
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/pruebas/preparar.ts'],
    },
  }
})
