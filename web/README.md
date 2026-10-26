# Web de reservas (equipo web)

La web donde el huésped busca, cotiza, reserva y ve su cuenta. React + Vite + TypeScript; solo habla con la API del backend (DEC-77).

## Correrla en local
1. Arrancar el backend (en la raíz del repositorio) en `http://localhost:8080`.
2. En esta carpeta: `npm install` (solo la primera vez) y luego `npm run dev`.
3. Abrir `http://localhost:5173`.

Vite reenvía las rutas `/api` al backend, así que no hace falta CORS en desarrollo. Si el backend corre en otra dirección, se define `SGA_API_URL` (por ejemplo `SGA_API_URL=http://localhost:9090`).

## Comandos
- `npm run dev`: la web en modo desarrollo, se recarga sola al guardar.
- `npm test`: las pruebas (Vitest).
- `npm run build`: revisa los tipos y genera la versión final en `dist/`.
- `npm run lint`: revisa errores comunes en el código.
