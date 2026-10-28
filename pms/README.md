# PMS (equipo pms)

La gestión del alojamiento para recepción, el personal de servicio y la administración. React + Vite + TypeScript; solo habla con la API del backend (DEC-77).

## Correrlo en local
1. Arrancar el backend (en la raíz del repositorio) en `http://localhost:8080`.
2. En esta carpeta: `npm install` (solo la primera vez) y luego `npm run dev`.
3. Abrir `http://localhost:5174` y entrar con una cuenta del personal (en local, el primer administrador del `.env`: `SGA_ADMIN_CORREO` y `SGA_ADMIN_CLAVE`).

La web de reservas usa el puerto 5173, así que los dos frontends pueden correr a la vez.

## Variables
Las mismas que `web/`, con el mismo significado (DEC-81):
- `SGA_API_URL`: la lee Vite para el proxy. Vite reenvía las rutas `/api` al backend, así que no hace falta CORS en desarrollo. Solo se define si el backend corre en otra dirección (por ejemplo `SGA_API_URL=http://localhost:9090`).
- `VITE_API_URL`: la lee el navegador para llamar directo a la API. En desarrollo va vacía, para pasar por el proxy; solo se define al construir el PMS para publicarlo en otra dirección que la API, y entonces el backend necesita ese origen en `SGA_CORS_ORIGENES`.

## Sesión
- `POST /api/auth/login` devuelve el token, que el cliente de la API (`src/api/cliente.ts`) envía en cada petición. La sesión se guarda en el navegador hasta que vence.
- Si el backend responde 401 a una petición con token (venció, se cerró en otra pestaña, o al usuario lo eliminaron o le cambiaron el rol, DEC-71), el PMS vuelve a la entrada y lo explica.
- El menú muestra a cada rol solo lo que puede usar, con la misma tabla del backend (`PermisosPorRol`, DEC-67). Si cambia un permiso allá, se cambia también en `src/navegacion.ts`.

## Comandos
- `npm run dev`: el PMS en modo desarrollo, se recarga solo al guardar.
- `npm test`: las pruebas (Vitest).
- `npm run build`: revisa los tipos y genera la versión final en `dist/`.
- `npm run lint`: revisa errores comunes en el código.
