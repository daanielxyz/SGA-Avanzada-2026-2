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
- **Listado** (`GET /api/reservas`, DEC-72): filtros opcionales `desde`, `hasta`, `estado`, `apartamento`, `canal` (de origen), `titular` (su id) y `pagina`, de la más reciente a la más antigua (DEC-86), de 10 en 10. Cada fila es un `ReservaResponse`.
- **`ReservaResponse`**: `codigo` (RES-2026-00042), `apartamento` (APT-101), `titularId`, `titularNombre` (solo en el listado de recepción y en el detalle; en las demás respuestas, nulo), `entrada`, `salida` (exclusiva: esa noche no se ocupa), `estado` (PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW), `canalOrigen` (PORTAL, DIRECTO, EXTERNO), `ocupantes` (nombre, fecha de nacimiento, tipo y número de documento, nacionalidad), `horaEstimadaLlegada`, `valor` (desglose congelado: por cada noche, `noche`, `temporada`, `tarifa` por ocupante facturable, `ocupantesFacturables` y `subtotal`; más el `total`), `politicaId`, `creadaEn`, `vehiculos`, `mascotas` y `avisoCuenta` (solo llega al crear; en el listado y el detalle viene vacío).
- **Detalle** (`GET /api/reservas/{codigo}`): el mismo `ReservaResponse`, o 404 `NO_ENCONTRADO`. No trae el folio ni el saldo: esos son de otro endpoint, en el sprint 2.
- **Errores**: todos llegan como `ErrorResponse` con `estado`, `codigo` (`VALIDACION`, `NO_AUTENTICADO`, `SIN_PERMISO`, `NO_ENCONTRADO`, `REGLA_NEGOCIO`…), `mensaje` y `detalles` (los errores por campo en un 400).
- **No existe** hoy: buscar por código en el listado (para eso se abre el detalle directamente), buscar el titular por nombre ni el total de noches (se calcula con `salida − entrada`).
- **Nombre del titular**: se muestra junto al id, porque solo con el id recepción no identifica rápido una reserva. Es el nombre vigente del titular, no uno congelado en la reserva.
- El contrato completo está en `docs/api/openapi.yaml` y en `/swagger-ui.html` (DEC-79 · DEC-86).

## Marca del producto (Meridian)
El PMS lleva la marca del **producto**, Meridian, «software de gestión hotelera» (brandbook en `pms/diseno/brandbook.png`). No es la marca de un alojamiento, así que sí puede ir en `pms/src`. El nombre y el logo del alojamiento siguen llegando de la API (DEC-83).
- **Paleta**: Azul Medianoche `#0F2A3A` y Arena Cálida `#D9C9B0` (primarios); Verde Oliva `#6B7F6B` y Terracota Suave `#C98F6B` (secundarios); Gris Piedra `#E8E6E1` y Gris Carbón `#1F1F1F` (neutros).
- **Tipografía**: Satoshi Semibold/Medium para títulos (H1 32/40, H2 20/28) e Inter Regular/Medium para la interfaz (cuerpo 16/24, etiquetas 14/20). Las dos fuentes se sirven desde `pms/public`, no desde un CDN.
- **Contraste**: los valores del brandbook están mal. Medidos con la fórmula WCAG dan: Medianoche sobre Arena **9,2:1** (el brandbook dice 12,6), Oliva sobre Arena **2,7:1** (dice 4,8) y Terracota sobre Arena **1,7:1** (dice 4,6). En la interfaz, entonces:
  - Medianoche y Carbón sirven para texto sobre Arena, Piedra o blanco (más de 9:1).
  - Oliva y Terracota **no** sirven como color de texto sobre fondos claros. Se usan como relleno de etiquetas de estado o de botones con texto Medianoche o Carbón encima (5,4:1 y 6:1 con Terracota; con Oliva, Carbón da solo 3,8:1, así que no sirve para texto pequeño). También pueden ir como texto sobre Medianoche (Terracota 5,4:1).
  - El rojo de error y los colores de estado de la reserva no salen del brandbook: se definen en la base y se revisan con `impeccable audit`.
- **Tema**: por ahora solo oscuro, con los colores de los mockups; más adelante se agrega el claro. Los componentes usan variables semánticas (`--superficie`, `--texto`, `--acento`…), así que agregarlo será dar otros valores a esas variables. Sobre el fondo oscuro, **Oliva como texto da 3,45:1 y no sirve**; Terracota (5,4:1), Arena (9,15:1) y Piedra (11,9:1) sí sobre Medianoche.
- **Archivos**: el logo está en `public/marca/` (símbolo y palabra, en Arena y con fondo transparente; falta el SVG). Las fuentes se sirven desde `public/fuentes/`. **Inter** (licencia OFL) va en git con su licencia. **Satoshi** no: su licencia (ITF FFL) permite usarla en nuestra aplicación, pero no publicarla en un repositorio ni modificarla o convertirla. Cada clon la descarga de [Fontshare](https://www.fontshare.com/fonts/satoshi) y deja `Fonts/WEB/fonts/Satoshi-Variable.woff2` en `public/fuentes/`; sin ella, los títulos salen con la fuente del sistema y `vite build` avisa que no la encontró. Al publicar el PMS, el archivo hay que ponerlo en el servidor. La foto genérica del inicio de sesión todavía no existe: se usa el degradado de prueba (`--fondo-entrada`).

## Mockups
Están en `pms/diseno/mockups/`, solo en cada clon: la carpeta `pms/diseno/` está en `pms/.gitignore`, así que quien los necesite los pide al equipo. Trae las imágenes para verlos y los `.html` con `base.css` para leer los valores exactos (no se abren solos en el navegador). Los datos son de ejemplo. Lo que se resolvió al compararlos con el contrato (paso 3):
- **Menú**: manda `src/navegacion.ts`. El mockup de Administración trae «Tarifas», «Temporadas» y «Políticas» y no trae «Canales de venta».
- **Política en el detalle**: la reserva solo trae `politicaId` y `/api/politicas` queda para el administrador (`PermisosPorRol`), así que se muestra el id, no «Flexible · 48 h». Para mostrar el texto hace falta un cambio de backend.
- **Agregado al detalle**: `vehiculos` y `mascotas` (el mockup no los tiene).
- **Corregido en el listado**: a 1280 px la tabla del mockup se salía y cortaba la columna Total, así que Entrada y Salida van en una sola columna, para dejarle espacio al nombre del titular. Cuando el contenido mide menos de 960 px (ventanas medianas o con zoom) se muestran las tarjetas del celular, en dos columnas desde 640 px; las fechas son campos de fecha; el filtro de apartamento trae su lista de `GET /api/apartamentos`; en el celular, los filtros activos con ✕ miden 44 px.

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

## Sprint 2: acciones, crear reserva y tablero
Mockups en `pms/diseno/mockups-meridian-sprint2/` (48 pantallas, con su LEEME). Excepciones a este archivo solo para este sprint:
- **DEC-84**: si algo no queda listo el viernes, no se entrega a medias; se dice en el cierre y se termina después (esto reemplaza «el diseño no frena la demo»).
- **Layouts**: Claude Design propuso la distribución, sin layouts a mano. Los mockups son borradores.
- **Backend**: no se cambia por los mockups; ver «Para hablar con el equipo».

| # | Rama | Pantallas | Estado |
|---|---|---|---|
| 1 | `feature/pms/acciones-reserva` | Botones según el estado, bloque Pago, diálogos de registrar pago, confirmar, cancelar y no-show | Hecho |
| 2 | `feature/pms/crear-reserva` | `/reservas/nueva` en cuatro pasos y «Nueva reserva» en el listado | Hecho |
| 3 | `feature/pms/tablero-ocupacion` | `/ocupacion`, opción en el menú y en la bienvenida | Hecho |

Lo resuelto al comparar los mockups con el contrato (paso 3) y en su crítica:
- **Detalle**: política por su id y titular con nombre e id, como en el sprint 1. El nombre del apartamento sale de `GET /api/apartamentos/{codigo}`; si falla, queda el código. «Vehículos» y «Mascotas» se conservan.
- **Pago**: el saldo llega positivo, así que su título dice «Saldo pendiente» o «Saldo a favor». El anticipo es el valor congelado por `anticipoPct`, sin mascotas. Las mascotas y otros servicios van en «Otros cargos», porque suman al saldo y no al total. En una reserva terminada, el anticipo se cambia por «Valor retenido», y «Registrar pago» solo aparece si todavía debe.
- **Confirmar**: al terminar se cierra y deja el aviso en el detalle. Cancelar y no-show sí muestran su resultado, porque hablan de dinero.
- **Cancelar y no-show**: sin «Se retendrá $ X» ni sus estados, que necesitan el cambio 1 de la lista de abajo; se avisa que puede haber retención.
- **Desglose**: en pantallas angostas quedan Noche, Temporada y Subtotal, con la tarifa bajo la fecha, para que las noches sumen el total.
- **Crear reserva**: elegir un apartamento avanza de una vez y, si las personas ya están listas, llega al paso 4. Los pasos van en `?paso=`, para que «Atrás» del navegador no borre el formulario, y se pregunta antes de recargar o seguir un enlace. Los datos personales no se guardan en el navegador. Un rechazo al crear muestra el mensaje de la API con tres salidas: otro apartamento, cambiar fechas o revisar personas. Vehículos solo aparece si `cupoParqueadero` es mayor que 0, y mascotas si `aceptaMascotas` está activo. El cargo por mascota se cobra una vez por estancia y va en una línea aparte del total. Edad del titular según `umbralEdadFacturable`.
- **Tablero**: el campo es «Última noche» y a la API se le envía un día más, porque `hasta` es exclusiva; el rango va en la dirección. La barra lleva código y `titularId`. Primera columna de 200 px y al menos 52 px por noche, para que quepan las 14 a 1280 px. Debajo de 640 px de contenido se muestra la lista por apartamento, con el código y el `titularId`.

## Para hablar con el equipo (backend)
Los mockups del sprint 2 son borradores: mientras se termina el frontend, **se adaptan al backend que existe y el backend no se cambia por ellos**. Esto se discute con todo el equipo cuando el frontend esté listo; nada de esta lista está aprobado.

| # | Posible cambio | Por qué serviría | Qué hace el frontend mientras tanto |
|---|---|---|---|
| 1 | Consultar cuánto se retendrá antes de cancelar o declarar no-show | Recepción conocería el costo antes de decidir | El valor retenido se muestra solo después, con `CancelacionResponse` |
| 2 | `titularNombre` en las reservas de `OcupacionResponse` | Recepción identificaría más rápido cada reserva en el tablero | La barra muestra el código y el `titularId` |
| 3 | Un `motivo` en los errores 409 de `REGLA_NEGOCIO` | Cada error podría salir junto a su causa, sin comparar textos | Se muestra el `mensaje` de la API en un aviso general, con las acciones para corregir |
| 4 | Indicar qué noche se vendió | Se podría marcar en el desglose, como en el mockup | El aviso se refiere a las fechas de la reserva |
| 5 | Cupos libres del parqueadero por fechas | Se avisaría antes de enviar la reserva | Se muestra el error de la API al crear |
| 6 | Monto del anticipo exigido en la respuesta | La pantalla no repetiría ese cálculo | La pantalla lo calcula con `anticipoPct`; el backend decide al confirmar |
| 7 | Nombre legible de la política para recepción | Hoy recepción solo puede ver el id | Se muestra `politicaId` |
| 8 | Mensajes de los 409 pensados para recepción | Hoy algunos son técnicos: «Transición de reserva no permitida: CANCELADA → CANCELADA», «desde 2029-07-12T22:00» | Se muestran tal cual; los botones ya evitan la mayoría de esos casos |
| 9 | Reservar para otra persona: un titular que no se aloja | Hoy el backend exige que el titular sea un ocupante que paga (TIT-01), así que la casilla «El titular también se aloja» casi no tiene uso | Sin marcarla, hay que escribir su documento en otro ocupante; si no, se avisa antes de enviar |
| 10 | Nombre de la temporada en el desglose | Hoy llega su id (`TEM-1`), no «Alta» o «Baja» | Se muestra lo que llega |

## Estado
- [x] Flujo acordado (DEC-84).
- [x] Elegir las skills con el documento del equipo.
- [x] Copiar las skills a `pms/.claude/skills/` y que Claude revise cada `SKILL.md`.
- [x] Layouts a mano de las pantallas 1 a 5.
- [x] Mockups en Claude Design y base visual.
- [x] Revisión de los mockups contra el contrato (paso 3).
- [x] Base visual en `src/index.css` y pantallas 1, 2 y 5 en código (`feature/pms/diseno-base`).
- [x] Fuentes Satoshi e Inter en `public/fuentes/` (Satoshi fuera de git).
- [x] Pantallas 3 y 4 en código, con el nombre del titular (`feature/pms/listado-reservas`).
- [x] Auditoría de las cinco pantallas con las skills elegidas (paso 5). Quedó pendiente: márgenes laterales para la muesca con el celular en horizontal (hay que probarlo en un teléfono), pasar a variables los tamaños de letra y los espacios fijos (20 % libre del sprint siguiente) y la dominancia del azul (se muestra al cliente en el cierre).
