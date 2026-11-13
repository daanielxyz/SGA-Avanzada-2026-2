# Pantallas del sprint 4: cómo se hace cada una

Guía para quien hace por primera vez una pantalla del PMS. Este sprint son tres personas (reparto en `pms/DISENO.md`, «Sprint 4: reparto»). Se sigue el flujo de DEC-84, el mismo que se usó en los sprints 2 y 3. Los prompts de esta carpeta reemplazan el layout a mano (paso 1): ya traen las pantallas, los datos que de verdad da la API y los estados.

| Prompt | Pantallas | Quién |
|---|---|---|
| `apartamentos.md` | `/apartamentos`: lista, crear, editar, capacidad, fotos, mascotas, bloqueos, retirar | Arias |
| `temporadas-tarifas.md` | `/tarifas`: calendario de temporadas y tarifas por apartamento | Arias |
| `usuarios-perfil.md` | `/usuarios`, `/perfil`, activar la cuenta y recuperar la contraseña | Reyes |
| `alojamiento.md` | `/alojamiento`: datos, ubicación, parámetros, servicios, medios de pago y marca | Daniel |
| `politica.md` | `/politica`: versiones de la política de cancelación y publicar una nueva | Daniel |
| `nueva-reserva-principal.md` | Paso 3 de «Nueva reserva»: huésped principal y apellidos del titular (DEC-97/98) | Daniel |

## Antes de empezar (una vez)
1. **El clon**: en `CLAUDE.local.md`, en la raíz del clon, deja esta línea mientras trabajes en el PMS (si venías del equipo web, cámbiala y vuelve a ponerla al terminar):
   ```markdown
   Soy el equipo **pms**. Solo edito lo que docs/EQUIPOS.md asigna a pms; lo del núcleo, solo en una rama feature/nucleo/….
   ```
2. **Las referencias de diseño**: la carpeta `pms/diseno/` no va a git (`pms/.gitignore`). Pídele a Daniel el zip `referencias-sprint4.zip` y descomprímelo en `pms/diseno/` de tu clon: queda la carpeta `pms/diseno/referencias-sprint4/`, que trae:
   - `brandbook.png`: la marca Meridian.
   - `mockups-meridian-sprint3-preparacion/preparacion/`: el `base.css` más reciente (tiene todo lo de los sprints anteriores) y su `LEEME.md`.
   - `mockups-meridian-sprint3-llegada/llegada/`: `LEEME.md`, `S3Llegada.html`, `S3DialogoVehiculos.html`, `S3DialogoTra.html` y sus imágenes en `png/`.
   - `mockups-meridian-sprint2/mockups/`: `S2Ocupacion.html` y `S2Paso3Personas.html` con sus imágenes en `png/`.
   - `capturas/`: capturas del PMS real a 1280 px: `reservas.png` (listado con filtros y paginación), `detalle.png` (detalle de una reserva), `ingresar.png` (iniciar sesión), `paso3.png` (paso 3 de «Nueva reserva»), y `politica-detalle.png` («Ver política» en el detalle). Mandan sobre los mockups viejos: el código ya corrigió cosas.
3. **La fuente Satoshi**: si `pms/public/fuentes/Satoshi-Variable.woff2` no está, se baja de Fontshare (`pms/DISENO.md`, «Marca del producto»).
4. **Probar en local**: el backend con tu `backend/.env` (`cd backend && ./gradlew bootRun`) y el PMS con `cd pms && npm install && npm run dev`. Las cuentas de prueba están en `README.md`; para estas pantallas entra como administrador.

## Paso a paso de cada pantalla
1. **Rama**: `git checkout develop && git pull && git checkout -b feature/pms/<tema>` (por ejemplo `feature/pms/apartamentos`).
2. **Claude Design** (claude.ai → Claude Design):
   - Crea un proyecto nuevo, por ejemplo «Meridian sprint 4 · apartamentos».
   - Pega **todo** el texto del prompt (desde la línea «Continúa el diseño de Meridian…»).
   - Adjunta los archivos que pide su sección «Adjuntos».
   - Revisa lo que entrega. Si algo no cuadra con el prompt (un dato inventado, un botón que la API no tiene, texto en inglés, un estado que falta), pídele la corrección en el mismo chat. No le pidas cosas que no estén en el prompt: si crees que falta algo, anótalo y se habla en el equipo.
   - Descarga el resultado y descomprímelo en `pms/diseno/mockups-meridian-sprint4-<tema>/`.
3. **Revisión contra el contrato** (paso 3 de DEC-84), en Claude Code dentro del clon:
   > Lee `pms/prompts/sprint4/LEEME.md`, `pms/prompts/sprint4/<prompt>.md` y `pms/DISENO.md`. Revisa los mockups de `pms/diseno/mockups-meridian-sprint4-<tema>/` contra `docs/api/openapi.yaml`, como se hizo en el sprint 3, y anota lo resuelto en una sección «Sprint 4: <tema>» de `pms/DISENO.md`.

   Todo dato del mockup que la API no trae se quita o se vuelve una propuesta de núcleo para Reyes. Nunca se agrega al backend por cuenta propia.
4. **Código**: «Programa la pantalla siguiendo los mockups y lo resuelto en `pms/DISENO.md`, con sus pruebas». Claude debe reutilizar lo que ya existe en `pms/src`: el cliente de la API (`src/api/`), los diálogos, los campos (`src/reserva/nueva/Campo.tsx`), el paginador, los mensajes de error y las variables de `src/index.css`. La pantalla se conecta en `PANTALLAS` de `src/App.tsx`. El menú ya tiene la opción en `src/navegacion.ts`.
5. **Probar en el navegador** contra tu backend local: el caso feliz, cada rechazo (400 y 409) con su mensaje junto al campo o la acción que lo causó, y la pantalla a 1280, 1024 y 390 px.
6. **Auditoría** (paso 5): «Audita la pantalla con `impeccable` (audit, harden, adapt) y `break-ui`, como en el sprint 3». Arregla lo que salga en P1 y P2.
7. **Antes del PR**:
   ```
   cd pms
   npx vitest run --maxWorkers=2
   npx oxlint src
   npm run build
   ```
   Todo en verde. Marca tu tarea en la sección pms del sprint 4 en `docs/SPRINTS.md`. Trae develop (`git fetch origin && git merge origin/develop`), haz el commit (un solo `-m` con el título en español, por ejemplo `feat: Pantalla de apartamentos`), `git push -u origin feature/pms/<tema>` y abre el PR a develop en GitHub. Daniel lo revisa.

## Reglas que Claude Design y Claude Code deben respetar
- Español de Colombia, sin términos técnicos. Nunca se muestra un valor crudo de la API (`EN_PREPARACION`, `VALOR_TOTAL`, `TEM-1`): siempre su texto («En preparación», «Valor total», «Temporada alta»).
- Solo los datos y las acciones que trae el prompt, que salen de `docs/api/openapi.yaml`.
- Nada del alojamiento escrito en el código («Puerta al Sol» solo como dato de ejemplo en el mockup).
- Mismo tema oscuro y componentes de `base.css`. Un componente nuevo se agrega a la base, no se copia. Oliva y Terracota no van como color de texto.
- Estados de cada pantalla: cargando, vacío, error de la API con «Reintentar», sin permiso (403) y los rechazos propios de cada acción.
- Recepción y administración usan sobre todo el computador: estas pantallas se diseñan a 1280 px primero y además a 390 px (sufijo `Celular`).
- Los errores llegan como `ErrorResponse` (`codigo`, `mensaje`, `motivo` y `detalles` por campo en un 400). Se muestran junto a lo que los causó.
- Un cambio de configuración nunca cambia lo ya reservado: las reservas guardan su valor y su versión de la política (CLAUDE.md, «Reglas transversales»). Donde aplique, la pantalla lo dice.
