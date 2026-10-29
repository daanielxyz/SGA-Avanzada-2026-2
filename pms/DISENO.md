# Diseño visual del PMS (DEC-84)

Cómo diseñamos las pantallas de `pms/`, qué hay que diseñar ahora y cómo se audita. Lo lee el equipo pms y Claude antes de tocar el aspecto del PMS.

## Flujo por pantalla
1. **Layout a mano** (el equipo): la distribución de la pantalla, con los datos que de verdad trae el Response (ver «Datos de cada pantalla») y sus estados: cargando, vacío, error de la API y sin permiso.
2. **Mockup en Claude Design**: a partir del layout, siempre sobre la misma base visual (la primera vez se crea con las pantallas del sprint 1).
3. **Revisión contra el contrato**: todo dato del mockup que la API no trae (por ejemplo, el nombre del titular en el listado) es un cambio de backend; se acuerda el lunes («contrato primero», `docs/SPRINTS.md`) o se quita del mockup.
4. **Código**: se implementa en `pms/` con las skills de diseño elegidas (ver «Skills»), reutilizando la base de `src/index.css`.
5. **Auditoría**: el equipo le pasa a Claude la pantalla ya en código y el mockup; Claude la audita con esas skills y propone los arreglos antes del PR.

Desde el sprint 2, los pasos 1 a 3 se hacen el lunes para las pantallas del sprint. Si el diseño no alcanza, la pantalla se entrega con la base y se pule después: el diseño no frena la demo.

## Reglas
- Nada del alojamiento escrito en el código: nombre, logo y colores de marca llegan de la API o de variables (DEC-83, BL-05). En los mockups se puede usar «Puerta al Sol» como ejemplo, pero no en `pms/src`.
- Colores, tipografía, espaciados, radios y sombras son variables de CSS (`:root` en `src/index.css`); un componente nuevo se agrega a la base, no se copia entre pantallas.
- Las pruebas buscan por rol y por texto (Testing Library), nunca por clases ni estilos.
- Accesible: contraste suficiente, foco visible, todo se usa con teclado, cada campo con su etiqueta.
- Los errores del backend (400/401/403/404/409) se muestran entendibles y junto a lo que los causó («Terminado», `docs/SPRINTS.md`).
- Recepción y administración trabajan sobre todo en computador (tablas densas); el personal de servicio, en el celular (preparación y novedades, desde el sprint 2): esas pantallas se diseñan primero para el celular.

## Pantallas por diseñar ahora (sprint 1)
| # | Pantalla | Ruta | Estados que debe mostrar el mockup |
|---|---|---|---|
| 1 | **Iniciar sesión** | `/ingresar` | normal; enviando; correo o contraseña errados (401); sesión perdida (venció o se cerró, DEC-71) con su aviso |
| 2 | **Marco del panel** (cabecera y menú) | todas | cabecera con el nombre del alojamiento, el usuario, su rol y «Cerrar sesión»; menú por grupos (Recepción, Operación, Configuración, Mi cuenta) en las tres variantes de rol (`src/navegacion.ts`); opción activa; en el celular, menú plegable |
| 3 | **Listado de reservas** | `/reservas` | filtros; tabla; cargando; sin resultados; filtro inválido (409, por ejemplo un rango de fechas al revés); paginación de 10 |
| 4 | **Detalle de una reserva** | `/reservas/{codigo}` | datos de la reserva, ocupantes, desglose por noche; reserva no encontrada (404); un lugar reservado para las acciones del sprint 2 (confirmar, cancelar, no-show), sin diseñarlas todavía |
| 5 | **Páginas de estado** (una sola plantilla) | `/`, en construcción, sin permiso, no encontrada | bienvenida con el menú; «en construcción»; sin permiso (403); ruta que no existe |

Ya en el sprint 4: activar la cuenta y recuperar la contraseña, con la misma plantilla de «Iniciar sesión».

## Datos de cada pantalla (lo que trae la API hoy)
- **Iniciar sesión**: `POST /api/auth/login` con correo y contraseña; devuelve el token, su vencimiento, el usuario, el nombre y el rol (`SesionResponse`).
- **Marco del panel**: el nombre del alojamiento sale de `GET /api/alojamiento` (público); no hay logo ni colores de marca en la API todavía (BL-05).
- **Listado** (`GET /api/reservas`, DEC-72): filtros opcionales `desde`, `hasta`, `estado`, `apartamento`, `canal` (de origen), `titular` (su id) y `pagina`, ordenado por entrada, de 10 en 10. Cada fila es un `ReservaResponse`.
- **`ReservaResponse`**: `codigo` (RES-2026-00042), `apartamento` (APT-101), `titularId` (**solo el id, no el nombre**), `entrada`, `salida` (exclusiva: esa noche no se ocupa), `estado` (PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW), `canalOrigen` (PORTAL, DIRECTO, EXTERNO), `ocupantes` (nombre, fecha de nacimiento, tipo y número de documento, nacionalidad), `horaEstimadaLlegada`, `valor` (desglose congelado: por cada noche, `noche`, `temporada`, `tarifa` por ocupante facturable, `ocupantesFacturables` y `subtotal`; más el `total`), `politicaId`, `creadaEn` y `avisoCuenta` (solo llega al crear; en el listado y el detalle viene vacío).
- **Detalle** (`GET /api/reservas/{codigo}`): el mismo `ReservaResponse`, o 404 `NO_ENCONTRADO`. No trae el folio ni el saldo: esos son de otro endpoint, en el sprint 2.
- **Errores**: todos llegan como `ErrorResponse` con `estado`, `codigo` (`VALIDACION`, `NO_AUTENTICADO`, `SIN_PERMISO`, `NO_ENCONTRADO`, `REGLA_NEGOCIO`…), `mensaje` y `detalles` (los errores por campo en un 400).
- **No existe** hoy: buscar por código en el listado (para eso se abre el detalle directamente), buscar el titular por nombre ni el total de noches (se calcula con `salida − entrada`).
- Si el mockup del listado muestra el nombre del titular, hace falta agregarlo al Response (un campo nuevo, sin quitar ninguno) o pedirlo aparte: se decide en `feature/pms/listado-reservas`.
- El contrato completo está en `/swagger-ui.html` con `SGA_DOCUMENTACION_API=true` (DEC-79).

## Marca del producto (Meridian)
El PMS lleva la marca del **producto**, Meridian, «software de gestión hotelera» (brandbook en `pms/diseno/brandbook.png`). No es la marca de un alojamiento, así que sí puede ir en `pms/src`. El nombre y el logo del alojamiento siguen llegando de la API (DEC-83).
- **Paleta**: Azul Medianoche `#0F2A3A` y Arena Cálida `#D9C9B0` (primarios); Verde Oliva `#6B7F6B` y Terracota Suave `#C98F6B` (secundarios); Gris Piedra `#E8E6E1` y Gris Carbón `#1F1F1F` (neutros).
- **Tipografía**: Satoshi Semibold/Medium para títulos (H1 32/40, H2 20/28) e Inter Regular/Medium para la interfaz (cuerpo 16/24, etiquetas 14/20). Las dos fuentes se sirven desde `pms/public`, no desde un CDN.
- **Contraste**: los valores del brandbook están mal. Medidos con la fórmula WCAG dan: Medianoche sobre Arena **9,2:1** (el brandbook dice 12,6), Oliva sobre Arena **2,7:1** (dice 4,8) y Terracota sobre Arena **1,7:1** (dice 4,6). En la interfaz, entonces:
  - Medianoche y Carbón sirven para texto sobre Arena, Piedra o blanco (más de 9:1).
  - Oliva y Terracota **no** sirven como color de texto sobre fondos claros. Se usan como relleno de etiquetas de estado o de botones con texto Medianoche o Carbón encima (5,4:1 y 6:1 con Terracota; con Oliva, Carbón da solo 3,8:1, así que no sirve para texto pequeño). También pueden ir como texto sobre Medianoche (Terracota 5,4:1).
  - El rojo de error y los colores de estado de la reserva no salen del brandbook: se definen en la base y se revisan con `impeccable audit`.

## Skills
Las skills salen del documento «Playbooks Web y SaaS con Claude Code». El PMS es la parte *app* del playbook 2 (SaaS), pero los mockups de Claude Design hacen de referencia, como en el playbook 1.2. Por eso hay **una sola skill de dirección visual (`impeccable`)**, y en el código manda el mockup, no el gusto de la skill.

| Skill | Para qué en el PMS | Paso del flujo |
|---|---|---|
| `impeccable` | Dirección de la app. Se usan solo `shape`, `critique`, `audit`, `harden` y `adapt`. **No** se usa `init`: su contexto ya es este archivo más `docs/SPRINTS.md`, sin crear `PRODUCT.md` | 1 (`shape` sobre el layout, antes de Claude Design) · 3 (`critique` del mockup) · 5 (`audit`, `harden`, `adapt`) |
| `image-to-code` | Pasar el mockup a código sin desviarse. Solo sus reglas de análisis y fidelidad (secciones 8–9, 21–28); **no** genera imágenes ni usa su «motor de variación» (sección 12) | 4 |
| `emil-design-eng` | Pulir los componentes de la base: tabla, filtros, botones, etiqueta de estado, formularios y mensajes | 4 (al crear o tocar un componente de `src/index.css`) |
| `break-ui` | Probar con datos extremos: 0 reservas, nombres de ocupantes larguísimos, 10 ocupantes, valores en COP de 9 dígitos, la última página, códigos largos | 5 |
| `mobile-native` | Menú plegable en el celular y, desde el sprint 2, las pantallas del personal de servicio (áreas táctiles de 44 px, inputs de 16 px, `svh` y zona segura) | 4 y 5, solo en pantallas para el celular |

Para construir y cerrar cada tarea siguen las de flujo de código, que son globales (`lean-build`, `verify-and-stop`; `investigate-first` y `surgical-patch` si hay errores). No van en esta carpeta.

**No se usan** (con su motivo):
- `design-taste-frontend`, `minimalist-ui`, `high-end-visual-design` e `industrial-brutalist-ui`: son una segunda dirección visual y chocarían con el mockup y con `impeccable`. Las de landing son de la web, no del PMS.
- `impeccable polish` / `bolder` (y en general los comandos de `impeccable` que rediseñan): «mejorarían» el mockup aprobado. Solo se usan si el equipo decide rediseñar.
- `pick-ui-library`: la base es CSS propia (DEC-84). Una librería (por ejemplo, un selector de fechas) sería una decisión nueva (`DEC-nn`), y entonces se invoca a mano.
- `prototype`: las variantes ya salen de Claude Design.
- `animate`, `review-animations`, `find-animation-opportunities` y `apple-design`: es una herramienta de trabajo con tablas densas, así que el movimiento se limita al foco y a la carga, con `prefers-reduced-motion`. Se pueden reconsiderar en el sprint 4.
- `brandkit` e `imagegen-frontend-web`: la marca ya existe y Claude Code no genera imágenes.

**Dónde están**: en `pms/.claude/skills/<nombre>/SKILL.md`. Así Claude solo las carga cuando se trabaja dentro de `pms/` (aparecen como `pms:impeccable`) y no le llegan al equipo web. Ya están revisadas: los comandos y las secciones citadas arriba existen. Cómo se usan aquí:
- **`impeccable`** trae un ejecutable (`scripts/bin/`, 19 MB) que su «Setup» manda correr y que, si falta, se descarga de GitHub. **No se ejecuta**: se usa el camino que la misma skill prevé cuando el lanzador no está disponible, y su contexto se toma de este archivo. El ejecutable no va a git (`pms/.gitignore`). Tampoco se usan sus archivos de `agents/`, que son para Codex.
- **`break-ui`** quiere un selector «Demo / Worst case» en la pantalla. Aquí los datos extremos van en los datos simulados de las pruebas (Testing Library) o en una ruta solo de desarrollo; nunca llegan a la versión publicada.
- **`image-to-code`** dice ser para Codex y pide generar imágenes. Aquí el mockup de Claude Design es la imagen de referencia.

## Estado
- [x] Flujo acordado (DEC-84).
- [x] Elegir las skills con el documento del equipo.
- [x] Copiar las skills a `pms/.claude/skills/` y que Claude revise cada `SKILL.md`.
- [ ] Layouts a mano de las pantallas 1 a 5.
- [ ] Mockups en Claude Design y base visual.
- [ ] Base visual en `src/index.css` y aplicada a las pantallas existentes (`feature/pms/diseno-base`).
- [ ] Auditoría de las pantallas con las skills elegidas.
