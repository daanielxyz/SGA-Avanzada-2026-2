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
- **Política en el detalle**: la reserva trae `politicaId` y, con él, recepción pide `GET /api/politicas/{id}` (DEC-104) para mostrar los tramos y la penalización del no-show. El historial de versiones y publicar siguen siendo del administrador.
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

## Cambios de backend: decisiones de la reunión del 6 nov
Mientras se hacía el frontend del sprint 2, los mockups se adaptaron al backend que había, y lo que hacía falta se anotó aquí para decidirlo con todo el equipo. Todos son del núcleo: cada uno va en su rama `feature/nucleo/…`, que revisa y aprueba el equipo web (DEC-77).

| # | Cambio | Decisión | Qué hace el PMS hoy |
|---|---|---|---|
| 1 | Saber cuánto se retendrá **antes** de cancelar | **Hecho** por el equipo web: `GET /api/reservas/{codigo}/cancelacion` (DEC-91) | Falta usarlo: ver «Ajustes del PMS» |
| 1b | Lo mismo para el **no-show**: DEC-91 solo cubre cancelar | **Hecho** (DEC-102) | Muestra «Se retendrá $ X» antes de declarar, con los mismos estados de cancelar (11 nov) |
| 2 | `titularNombre` en las reservas de `OcupacionResponse` | **Sí** | La barra del tablero muestra el `titularId` |
| 3 | Un `motivo` en los errores 409 de `REGLA_NEGOCIO` (campo nuevo, sin quitar nada) | **Hecho** (DEC-94) | Muestra el `mensaje` y solo las salidas que resuelven ese rechazo (`salidasDelRechazo`); si otro turno ya cambió la reserva, lo dice en palabras de recepción |
| 4 | Decir qué noche se vendió | **No**: con el `motivo` del #3 basta | El aviso habla de las fechas de la reserva |
| 5 | Cupos libres del parqueadero por fechas | **Se pospone** | Muestra el error de la API al crear |
| 6 | Monto del anticipo exigido en la respuesta | **Hecho** (DEC-95) | Usa `anticipoExigido` de la API, sin calcularlo |
| 7 | Nombre legible de la política para recepción | **Hecho** (DEC-104): `GET /api/politicas/{id}` para recepción | Todavía muestra el id: falta usarlo en el detalle. La API manda tramos (horas de antelación y lo que se retiene), sin un nombre, así que va con mockup en la tarea 3 |
| 8 | Mensajes de los 409 pensados para recepción | **Baja prioridad**: el #3 resuelve la mayoría. Pasa al sprint 4 (entrevista) | Los muestra tal cual |
| 9 | Reservar a nombre de alguien que no se aloja | **Lo hace el equipo web** (decisión del 8 nov). **Sí, cambia la regla TIT-01**: el titular puede no alojarse (una madre reserva para sus hijos y sigue siendo la titular). Al crear, el titular ya no tiene que estar entre los ocupantes ni ser facturable. Además, en el registro de llegada recepción puede cambiar el titular si quien reservó no debe serlo. Es del núcleo (dominio, `CrearReservaUseCase`, reservas del portal y de canales, pruebas de TIT-01 y `docs/MODELO.md`); en el PMS se quita la validación «El titular debe estar entre los ocupantes» de `src/reserva/nueva/formulario.ts` y la casilla «El titular también se aloja» queda solo para llenar el primer ocupante | **Hecho** (DEC-97, 11 nov): el titular puede no alojarse; se revisa que el grupo traiga un mayor de edad |
| 10 | Nombre de la temporada en el desglose | **Sí**, en el sprint 4: le cede el lugar a la entrevista | Muestra el id (`TEM-1`) |
| 11 | Tarifa de la Temporada Media | **Hecho**: no era un pendiente, cada alojamiento define sus temporadas y sus tarifas al configurar el PMS (sprint 4). Se quitó de `CLAUDE.md` y de `Tarifa.java` | — |
| 12 | Quién revierte un cargo o un pago y quién registra una devolución (decisión del equipo pms, 7 nov) | **Sí**, en el paso 3 del sprint 3: el reverso de un cargo, el de un pago y la devolución guardan su autor, que sale de `UsuarioActual` y nunca del Request; `CargoResponse` y `PagoResponse` lo agregan (campo nuevo, no rompe a la web). Rama `feature/nucleo/autor-reversos`, con su migración y su DEC | El reverso guarda el motivo y la fecha, pero no quién lo hizo |

## Ajustes del PMS por DEC-89, DEC-91 y DEC-93
El equipo web cambió el núcleo después del sprint 2 del PMS. No hay nada que decidir: hay que hacerlo, y va primero en el sprint 3. **Hechos**: el anticipo, la nueva reserva, el límite de mascotas y el detalle en `feature/pms/ajustes-extras`; cancelar en `feature/pms/previa-cancelacion`.
- Cómo quedó: el bloque Pago muestra Estancia, Parqueadero, Cargo por mascotas y Consumos por separado, y el anticipo sale de la estancia más los extras de la reserva (sin consumos). Los consumos son los servicios del folio que no son los extras: el backend carga los extras con los conceptos «Parqueadero» y «Mascotas» («Mascotas (n)» antes de DEC-89), así que si cambian esos nombres hay que cambiar `pms/src/reserva/pago.ts`. En la nueva reserva, carros y motos llevan su precio en la pista y se revisa que no pasen del cupo total; el total ya suma los extras. El límite de mascotas sale de `GET /api/apartamentos/{codigo}`; si no carga, rige el del hotel y el backend lo exige igual.
- Auditoría (`impeccable audit`, sin su ejecutable, 17/20) y sus cinco P2 corregidos: los extras de una reserva cancelada o con no-show dicen «anulado»; carros y motos van en el grupo «Parqueadero» con un solo error para los dos; el texto dice «El anticipo incluye el parqueadero y las mascotas»; el campo de mascotas espera el límite del apartamento y avisa si se descartan las escritas; la hora y las mascotas van en una fila de dos y el total se anuncia (`aria-live`). Quedan dos P3: el «$» se puede separar del número (`dinero()`, para la fase de estilos) y los consumos revertidos (en el folio, abajo).
- **Corregir cargos en el folio** (sprint 3): un cargo o un pago nunca se edita ni se borra (RN-16); se corrige con su movimiento inverso, que el backend ya tiene (`POST …/folio/cargos/{id}/revertir` con motivo y `…/pagos/{id}/revertir`) y que rechaza revertir dos veces lo mismo o revertir una corrección. En la pantalla: «Revertir» en cada cargo, con motivo obligatorio; «Corregir» es revertir y abrir el formulario de cargo nuevo ya lleno, para cambiar la cantidad o el valor. El cargo revertido se ve tachado junto a su reverso (`corrigeA`), y los consumos se suman netos de sus reversos. Cualquier cargo se revierte, pero solo los consumos se corrigen (decisión del 10 nov, que reemplaza la del 7 nov), y queda quién lo hizo (#12). Hecho en `feature/pms/folio`.
- **Anticipo con extras** (DEC-89): la confirmación exige el anticipo sobre la estancia más los extras (`valor.total + extras.total`). `pms/src/reserva/pago.ts` lo calcula solo sobre la estancia: con parqueadero o mascotas cobrados, el PMS muestra un anticipo menor y el backend rechaza la confirmación. Si se hace el #6, se usa el valor que mande la API.
- **Nueva reserva** (DEC-89): enviar `carros` y `motos` en vez de `vehiculos` (obsoleto: cuenta como carros), con el cobro de parqueadero de `cobroParqueadero`, `cargoCarro` y `cargoMoto`. Quitar «van aparte en el pago»: los extras ahora suman en el anticipo.
- **Límite de mascotas por apartamento** (DEC-93): usar el del apartamento (`limiteMascotas`, vacío = el del hotel) en vez del del hotel. El backend ya lo exige.
- **Cancelar** (DEC-91): mostrar «Se retendrá $ X» antes de confirmar, con sus estados (calculando, valor, nada que retener, error con «Reintentar» y el botón deshabilitado). Ya están en los mockups del sprint 2 (`S2DialogoCancelar`). **Hecho**: si la API responde 409 (la reserva ya no se puede cancelar) se muestra su mensaje sin «Reintentar»; el aviso dice que el valor es el de este momento. El no-show hace lo mismo desde el 11 nov (#1b, DEC-102).
- **Detalle**: mostrar `carros`, `motos` y los `extras` (parqueadero y mascotas) de la reserva.

## Acuerdos del equipo pms
Se anotan aquí para que cualquier clon los tenga:
- **Proxy en local**: `pms/` llama a `/api` por el proxy de Vite, sin CORS. `SGA_API_URL` (dirección del backend para el proxy) y `VITE_API_URL` (vacía en desarrollo) no se unifican: si se unifican, el navegador se salta el proxy (`pms/README.md`).
- **Decisiones**: antes de pedir el PR de una rama que toca `docs/DECISIONES.md`, comparar con `origin/develop` que no se borró ninguna decisión ajena. Si otro equipo usó el mismo número, se conservan las dos y la nuestra toma el siguiente libre.
- **Mockups y backend**: los mockups son borradores. Se adaptan al backend que existe; lo que pida un cambio de backend se anota para decidirlo con el equipo, no se cambia por un mockup.
- **Estilos al final**: los cambios visuales (colores, como la dominancia del azul, y detalles de estilo) se hacen cuando todo funcione, sin tocar funcionalidades. Para eso los tamaños, espacios y colores están en variables de `src/index.css`.
- **Antes de decir que algo funciona**: comprobar que el backend corre con el código actual de la rama. Al terminar cada tarea, decir qué cambió en los Request y los Response. Nada de secretos ni valores del `.env`.
- **Documentación sin choques** (pedido del equipo web, 8 nov): antes de tocar `docs/SPRINTS.md`, `docs/DECISIONES.md`, `docs/BACKLOG.md` o `docs/api/openapi.yaml`, revisar los PR abiertos y lo último de `develop`, y ajustar lo que haga falta. Cuando un cambio deba acordarse con el equipo web, avisarle a la persona antes de programarlo.
- **Trabajar entre dos sin pasarse el chat**: ninguna decisión se queda solo en el chat; va a este archivo o a `docs/SPRINTS.md`. Cada tarea va en su rama, y se marca `[~]` con quién la tiene antes de empezar. Al terminar una sesión, se actualiza «Próxima sesión» con lo hecho, la rama y lo que sigue.
- **Commits y PR**: los hace la persona; Claude no hace commits. Nunca se trabaja en `main` ni en `develop`.

## Entrevista a la recepcionista (6 nov)
Videos y fotos de una recepción real (otro hotel, con su PMS, el SIRE de Migración y una planilla de Excel). El material trae datos personales de huéspedes: no va a git ni a los mockups. Lo que cambia para el PMS:
- **Llegada**: pide por cada huésped los datos del TRA (BL-07). Se puede registrar con datos incompletos, pero la **salida queda bloqueada**: el botón deshabilitado, con la lista de lo que falta, y 409 en el backend. Recepción solo pregunta de dónde viene el huésped; con eso se proponen residencia y destino, que se pueden cambiar. El motivo del viaje es una lista: turismo y vacaciones; eventos; estudios; tratamiento médico; asuntos jurídicos; negocios; tránsito; compras; religión; visita a familiares o amigos. Antes de entregar el apartamento se revisa el desglose por noche y se registra el pago (BL-10).
- **Cobro** (BL-22): la estancia, con su parqueadero y sus mascotas, se paga en la llegada; sin eso el botón de registrar queda deshabilitado, salvo que recepción registre una autorización con motivo (por ejemplo, cuenta por cobrar a una empresa). Los consumos se pagan en cualquier momento hasta la salida. Si el huésped llega sin la mascota o con otro vehículo, recepción modifica la reserva antes de registrar y la diferencia entra al folio como ajuste (BL-23). El backend ya está (DEC-106): `PUT /api/reservas/{codigo}/modificar` acepta `carros`, `motos` y `mascotas`; si no se envían, se conservan.
- **Notas** de la reserva para el siguiente turno, en el detalle y en la llegada (BL-08). El backend ya está (DEC-105): `GET`/`POST /api/reservas/{codigo}/notas` y `PUT …/notas/{id}/atender`; arriba las pendientes, abajo las atendidas con quién y cuándo, y sin «Agregar nota» en una reserva terminada.
- **Tablero**: la barra se colorea por la cuenta (ya salió, al día, con saldo, debe todo; BL-09), además del estado. Los colores salen de la base y se revisan con `impeccable audit`; no se copian los del otro sistema.
- **Reserva de mostrador**: si la reserva directa entra hoy, al crearla se sigue con el pago, la confirmación y la llegada (BL-11).
- **Folio**: los consumos se cargan con cantidad (BL-12). El backend ya está (DEC-107): `POST …/folio/servicios` acepta `cantidad` (1 a 99) y cada cargo trae `cantidad`; `valor` ya es el total, así que se muestra «Desayuno × 2 · $50.000». Un cargo se corrige entero.
- **IVA** según el documento: se pregunta al cliente en el cierre 3 (BL-13).

## Próxima sesión: sprint 3 (9 – 13 nov)
**Para retomar**, una sesión nueva de Claude lee `CLAUDE.md`, `CLAUDE.local.md`, la sección pms del sprint actual en `docs/SPRINTS.md` y esta sección. Lo que falta para trabajar y no va a git: `CLAUDE.local.md`, `backend/.env` (cada quien con sus valores), `pms/diseno/` y la fuente Satoshi en `pms/public/fuentes/`.

**Ya hecho** (10 nov): entrevista (BL-07 a BL-23), ajustes del PMS por DEC-89, DEC-91 y DEC-93, y todo el núcleo del reparto: BL-07 (DEC-96) y #9 (DEC-97) del equipo web; BL-22, #12, BL-09, #1b, #7, BL-08, BL-23 y BL-12 de Reyes (DEC-99 a DEC-107), más los datos de demo para probar el folio y los extras (`feature/nucleo/demo-completa`).

**Folio, terminado** (`feature/pms/folio`, Daniel, 10 nov): pantalla `/reservas/{codigo}/folio` y sus diálogos (`src/folio/`), «Ver folio» y «Registrar salida» en el detalle, pruebas (`src/folio/movimientos.test.ts`, `src/paginas/Folio.test.tsx`), comprobado contra el backend de la rama y auditado (18/20, «Sprint 3: folio»). Integrado (PR #59).

**Registro de llegada, en curso** (`feature/pms/registro-llegada`, Daniel, 11 nov): mockups revisados contra el contrato y pantallas programadas con sus pruebas («Sprint 3: registro de llegada»). **Sigue**: probarlo contra el backend de la rama, auditarlo con las skills y llevar el #13, el #14 y el #15 a la reunión del equipo (quedan pendientes; no frenan la tarea). En la misma rama quedaron el #9 en «Nueva reserva» y el #1b en el diálogo de no-show.

**Tareas 3 y 4, programadas** (11 nov, en la misma copia de trabajo): mockups revisados, código y pruebas («Sprint 3: tablero por la cuenta y notas» y «Sprint 3: preparación de apartamentos»). **Sigue**: la reunión del equipo («Para la reunión del equipo») y probar todo contra el backend y en el celular antes de los PR.

**Datos de prueba** (`feature/nucleo/demo-completa`): el hotel cobra Desayuno ($25.000), Agua ($3.000) y Lavandería ($18.000), admite parqueadero (8 cupos, carro $15.000 y moto $8.000 por noche) y hasta 2 mascotas ($30.000). En una base nueva, la reserva confirmada de APT-102 trae 1 carro y 1 mascota (para corregirlos antes de la llegada, BL-23) y la que está en curso en APT-103 ya consumió «Desayuno × 2» y debe $50.000 (para el folio y la salida con saldo). En una base creada antes de V15, los servicios, el parqueadero y las mascotas se agregan solos al arrancar, una vez; las reservas de ejemplo solo salen borrando y creando de nuevo la base `sga`.

**Reparto** (8 nov, se puede cambiar). Si una tarea depende de otra que no ha entrado, se avanza contra el contrato acordado o se toma la siguiente.

| Quién | Tarea, en orden | Depende de |
|---|---|---|
| Daniel (pantallas) | 1. **Folio**: hecho (PR #59) | — |
| Daniel (pantallas) | 2. **Registro de llegada**: datos del TRA (completa lo que la salida exige), desglose, cobro de la estancia con autorización, cambio de titular y de huésped principal, vehículos y mascotas antes de llegar, y reserva de mostrador (BL-10, BL-11, BL-22, BL-23) | Nada: ya entraron sus reglas |
| Daniel (pantallas) | 3. **Tablero por la cuenta y notas** (BL-09, BL-08) | Nada: ya entraron |
| Daniel (pantallas) | 4. **Preparación de apartamentos** para el personal de servicio, pensada para el celular; no tenía sprint y sin ella un apartamento no se vuelve a entregar desde el PMS. Probar en un celular real las pantallas del sprint 2 | Nada |
| Reyes (núcleo) | Hecho lo del reparto (DEC-99 a DEC-107) y los datos de demo | — |

**Sprint 4 (17 – 20 nov)**: el reparto de esta sección se reemplazó por «Sprint 4: reparto», al final del archivo, con Arias en el equipo.

**#9 en «Nueva reserva»** (11 nov, en `feature/pms/registro-llegada`): se quitó «El titular debe estar entre los ocupantes»; «El titular también se aloja» solo llena el ocupante 1. En su lugar se revisa lo que exige DEC-97: el grupo trae un mayor de edad a la entrada (si no, 409 `HUESPED_PRINCIPAL_INVALIDO`). **Queda de DEC-97 y DEC-98 para «Nueva reserva»**: escoger al huésped principal al reservar (hoy lo elige el backend, el primer adulto, y se cambia en la llegada) y enviar los apellidos del titular aparte. Cambian el formulario, así que van con mockup en una tarea propia.

**Cómo se hace cada pantalla**: mismo flujo del sprint 2: lista de pantallas sacada del contrato, prompt para Claude Design, revisión de los mockups contra el contrato, código y auditoría. «Registrar pago» (`src/reserva/FormularioPago.tsx`) también registra devoluciones (`tipo`). Los consumos se suman netos de sus correcciones; cualquier cargo se revierte, pero solo los consumos se corrigen (decisión del 10 nov, «Sprint 3: folio»).

## Sprint 3: folio (`feature/pms/folio`)
Lista sacada del contrato (9 nov). Los mockups son borradores y se llaman `S3…`, como los del sprint 2. El folio tiene ruta propia, `/reservas/{codigo}/folio`, y se abre con «Ver folio» desde el bloque Pago del detalle.

| # | Pantalla | Endpoint | Qué manda en cada estado |
|---|---|---|---|
| 1 | **Folio**: resumen, cargos y pagos | `GET …/folio` | Cargando; 404; error con «Reintentar»; sin consumos; abierto; cerrado, sin acciones y con su autorización si la hubo |
| 2 | **Cargar consumo** | `POST …/folio/servicios` | Solo con la reserva EN_CURSO y el folio abierto. Lista: `serviciosAdicionales` de `GET /api/alojamiento`, activos y con `generaCargo`. `cantidad` de 1 a 99 (DEC-107) |
| 3 | **Revertir cargo** | `POST …/folio/cargos/{id}/revertir` | Motivo obligatorio, hasta 170 caracteres. No se ofrece en un AJUSTE que corrige otro ni en un cargo ya corregido |
| 4 | **Corregir consumo** | Revertir y luego cargar | Solo en `SERVICIO_ADICIONAL` que siga en el catálogo. Si se abandona después de revertir, el cargo queda revertido y se avisa |
| 5 | **Registrar pago** | `POST …/folio/pagos` | Ya existe: se reutiliza `FormularioPago` |
| 6 | **Revertir pago** | `POST …/folio/pagos/{id}/revertir` | Solo en un ABONO sin revertir. Es una confirmación: la API no recibe motivo |
| 7 | **Registrar devolución** | `POST …/folio/devoluciones` | Mismos campos del pago. Propone el saldo si está A_FAVOR. No supera lo abonado neto |
| 8 | **Cerrar folio** | `PUT …/folio/cerrar` | Solo en reservas CANCELADA o NO_SHOW: en EN_CURSO lo cierra la salida. Con saldo, solo el administrador, con motivo |
| 9 | **Registrar salida** | `PUT …/registrar-salida` | Solo EN_CURSO. Deshabilitada con la lista de `datosTraFaltantes`. Con saldo, solo el administrador, con motivo; recepción ve qué falta cobrar o devolver |

Lo que el contrato no da o da distinto a lo planeado:
- **Cantidad (BL-12)**: hecha en DEC-107. Un solo cargo con `cantidad` y `valor` ya total; el concepto no la trae, así que la pantalla muestra «Desayuno × 2». Se corrige entero: «Corregir» revierte el cargo y propone el mismo servicio y cantidad.
- **Corregir el hospedaje**: la decisión del 7 nov dice que se puede corregir cualquier cargo, pero solo se puede cargar de nuevo un servicio del catálogo. Hospedaje, parqueadero, mascotas y penalidad se pueden revertir y no volver a cargar desde el folio; su valor se cambia modificando la reserva, que deja el ajuste (RN-14; carros, motos y mascotas desde DEC-106). **Decidido (Daniel, 10 nov)**: solo tienen «Revertir», con el aviso «Desde el folio no se puede volver a cargar. Si el valor cambió, modifica la reserva». Esto reemplaza «cualquier cargo se puede corregir» del 7 nov.
- **Valor del consumo**: sale del catálogo y no llega en el Request, así que «Corregir» cambia el servicio o la cantidad, no el valor.
- **Autor de reversos y devoluciones (#12)**: hecho en DEC-100 y DEC-103. `autorNombre` llega en las correcciones, los reversos de pago, las devoluciones y la autorización de cierre; los abonos y los ajustes automáticos no lo traen. Si es nulo (persona eliminada o movimiento anterior a V23), se muestra `autor` o nada.
- **Motivo del reverso de un cargo**: no tiene campo propio; va en el concepto («Corrección de CAR-3: motivo»).
- **Devolución y reverso de pago**: un REVERSO no se revierte (la devolución equivocada se compensa con un abono). `reversaDe` vacío distingue la devolución del reverso de un abono.
- **Movimientos**: traen fecha, sin hora; se muestran en el orden en que llegan.
- **Datos del TRA**: la salida los exige, pero se llenan en el registro de llegada, que es la tarea siguiente; hasta entonces la pantalla solo lista lo que falta.

Mockups en `pms/diseno/mockups-meridian-sprint3/sprint3/` (20 pantallas, con su LEEME; se hicieron antes de DEC-100 y DEC-107). Lo resuelto al compararlos con el contrato (paso 3):
- **Corrección de un cargo**: la API manda «Corrección de CAR-5: motivo». El PMS cambia el código por el concepto del cargo que corrige (`corrigeA`), como en el mockup. Si el texto no empieza así, lo muestra tal cual.
- **Quién lo hizo** (falta en el mockup): «por {autorNombre}» como dato secundario en las correcciones, los reversos de pago, las devoluciones y la autorización de cierre. Sin nombre, no se muestra nada.
- **Pagos**: el reverso de un abono dice «Reverso del abono» y va debajo del abono, con el medio repetido. Un REVERSO sin `reversaDe` es una «Devolución», va en su orden y no tiene acciones.
- **Cantidad**: la pista dice «De 1 a 99» (DEC-107). La fila muestra «Agua × 2» cuando `cantidad` es mayor que 1.
- **Resumen**: «Cargos» es la suma neta (los AJUSTE que disminuyen restan) y debe dar `totalPagado` ± `saldo`.
- **Botones por estado**: «Registrar pago» sigue `puedeRegistrarPago`, así que también aparece en una reserva Cancelada o No-show que debe. «Cerrar folio» solo en Cancelada y No-show. «Cargar consumo» y «Registrar salida» solo en En curso. «Revertir» en cualquier estado mientras el folio esté abierto.
- **Admin o recepción**: los diálogos de cerrar y de salida eligen su variante con el rol de la sesión. El motivo solo se envía cuando hay saldo. Si recepción intenta salir con saldo, «Registrar pago» o «Registrar devolución» abre el formulario dentro del mismo diálogo, como en Confirmar.
- **Textos de los rechazos**: son los de la API (`mensaje` y `motivo`), no los del mockup.
- **TRA**: la lista sale de `datosTraFaltantes`, con los textos del backend (documento, primer apellido, nacionalidad, género, país y ciudad de residencia, procedencia, destino y motivo del viaje). Completarlos es del registro de llegada.
- **Detalle**: del mockup solo se toman «Registrar salida» junto al título y «Ver folio» en el bloque Pago. Lo demás queda como está en el código (extras, política, etc.).
- **Se aceptan** los botones agregados («Salir», «Salir sin cargar», «Seguir aquí», «Cerrar» en el resultado y «Autorizar y cerrar»).

Auditoría (paso 5, 10 nov): `impeccable audit`, `harden` y `adapt` sin su ejecutable (sin el detector automático) y `break-ui` con la API simulada en el navegador (montos de 9 dígitos, motivo de 170 caracteres, nombres de 70, 16 cargos, 10 ocupantes sin TRA), a 1280, 1024 y 390 px. Nota **18/20**: accesibilidad 3, rendimiento 4, tema 4, adaptable 3 → 4 tras los arreglos, integridad 4. Contrastes nuevos de 6,5:1 o más.
- **Corregido (P1)**: un nombre largo de quien corrige ensanchaba la columna Concepto y dejaba «Revertir» fuera del marco, incluso a 1280 px; a 1024 px se cortaban el valor y los botones. Ahora la tabla tiene columnas fijas (112, libre, 180, 140 y 210 px), el concepto y el autor se parten, y la tabla aparece desde 820 px de contenido (antes, las tarjetas).
- **Corregido (P2)**: el autor se salía de la tarjeta en el celular; si una acción fallaba (otro turno revirtió o cerró), el folio quedaba viejo: ahora se vuelve a leer al cerrar el diálogo; «Registrar salida» en el detalle no hacía nada si el pago no cargó: ahora queda deshabilitado; en la salida, lo que la bloquea va primero y el botón deshabilitado lo anuncia (`aria-describedby`); el administrador no ve el motivo de autorización mientras falten datos del TRA.
- **Queda (P3)**: en el celular la tabla pasa a tarjetas cambiando el `display`, y algunos lectores de pantalla (Safari) dejan de anunciarla como tabla; es el mismo patrón de los ocupantes del detalle. Al salir de «Corregir» sin cargar no queda un aviso en la página (el folio sí muestra el cargo revertido).

## Sprint 3: registro de llegada (`feature/pms/registro-llegada`)
Lista sacada del contrato (11 nov). Los mockups son borradores y se llaman `S3…`; su prompt está en `pms/diseno/prompt-llegada-sprint3.md`. La llegada tiene ruta propia, `/reservas/{codigo}/llegada`, para cobrar y registrar desde una sola pantalla (BL-10). Las notas del turno (BL-08) se le agregan en la tarea siguiente.

| # | Pantalla | Endpoint | Qué manda en cada estado |
|---|---|---|---|
| 1 | **Llegada**: lo que falta, cobro, huéspedes, titular, vehículos | `GET` reserva, folio, apartamento, titular y alojamiento · `PUT …/registrar-llegada` | Solo CONFIRMADA, desde la fecha de entrada (RN-10) y con el apartamento PREPARADO (RN-11). Sin la estancia y sus extras pagados, exige `motivoSinPago`, hasta 500 (DEC-99). Con el TRA incompleto sí se registra |
| 2 | **Reserva de mostrador** (BL-11) | `POST /api/reservas` · `PUT …/hora-llegada` · `…/confirmar` · `…/registrar-llegada` | Misma página con la reserva PENDIENTE y entrada hoy. Sin hora estimada se envía la actual. La confirmación exige el anticipo aunque haya motivo. Si se confirma y la llegada falla, queda CONFIRMADA y se reintenta |
| 3 | **Completar datos del TRA** | `PUT …/ocupantes/{id}/datos-tra` | Cualquier estado activo, por partes. Fecha de nacimiento y documento ya registrado, solo lectura. Tipo y número van juntos. Procedencia propone residencia y destino; del segundo huésped en adelante se propone lo del anterior |
| 4 | **Vehículos y mascotas** (BL-23) | `PUT …/modificar` · `POST /api/cotizaciones` para mostrar la diferencia | PENDIENTE o CONFIRMADA, con entrada desde hoy (RN-04) |
| 5 | **Cambiar titular** | `PUT …/cambiar-titular` | Estados activos. 409 `TITULAR_INVALIDO` si es el mismo |
| 6 | **Validar identidad** (DEC-98) | `GET`/`PUT /api/titulares/{id}/validar` | Solo si el documento del titular coincide con el de un ocupante. Una sola vez |
| 7 | **Hacer principal** | `PUT …/ocupantes/{id}/principal` | Solo mayores de 18 a la entrada, sin diálogo |
| 8 | **Corregir registro** | `PUT …/corregir-registro` | Solo administrador y EN_CURSO. Fecha entre la entrada y este momento, motivo hasta 500 |
| 9 | **Detalle** | — | «Registrar llegada», «Continuar llegada», «Corregir registro», huésped principal y estado del TRA con «Completar datos», también en EN_CURSO: la salida los exige |

Lo que el contrato da distinto a lo planeado:
- **Modificar recalcula toda la estancia** con las tarifas de hoy (TAR-06), no solo los extras: quitar una mascota puede cambiar el valor si la tarifa cambió. El diálogo muestra el total nuevo antes de guardar. La petición lleva todo el grupo con `nombres`, apellidos, documento y `principal`; sin `principal`, el backend vuelve a elegir al primer adulto. Los ids de los ocupantes pueden cambiar: se usa la respuesta.
- **Llegada tardía**: registrar se puede hasta el no-show, pero modificar rechaza una entrada ya pasada. Con la entrada vencida, el diálogo de vehículos explica que ya no se puede.
- **Falta por cobrar**: se calcula en la pantalla como `valor.total + extras.total − folio.totalPagado`, igual que el backend (`valorConExtras`); los consumos no cuentan.
- **Preparar el apartamento**: recepción no puede; es del personal de servicio y del administrador (tarea 4). Hasta entonces la llegada solo dice el estado.
- **Datos de prueba**: las reservas confirmadas de demo entran en 10 y 15 días, así que la llegada se prueba con una reserva de mostrador de hoy.

Por decidir con el equipo (no se programa hasta acordarlo):
- **#13 · Leer la llegada registrada**: `ReservaResponse` no trae los registros y `EstanciaResponse` solo llega al hacer la acción, así que al volver a la reserva no se puede ver a qué hora llegó, quién la registró ni el `motivoSinPago`, y el administrador corrige sin ver la hora actual. Propuesta: `GET /api/reservas/{codigo}/estancia`, solo agrega, con un caso de uso de lectura; `PermisosPorRol` ya lo cubre. Mientras tanto, «Corregir registro» no muestra la hora actual.
- **#14 · Corregir un documento mal escrito**: `datos-tra` no lo cambia y modificar lo tomaría como otra persona, que pierde sus datos del TRA. Hoy no hay cómo hacerlo desde el PMS.
- **#15 · Quién validó la identidad**: `TitularResponse.validadoPor` es el id del usuario, no su nombre. Mientras tanto la pantalla dice solo cuándo se validó. Propuesta: agregar `validadoPorNombre`, como `autorNombre` en el folio (DEC-100).

Mockups en `pms/diseno/mockups-meridian-sprint3-llegada/llegada/` (13 pantallas con sus versiones de celular y su LEEME). Lo resuelto al compararlos con el contrato (paso 3, 11 nov), siguiendo el «Lo que falta o quedó dudoso» del LEEME:
- **Conteo del TRA** (duda 1): «Faltan N datos» cuenta los de `datosTraFaltantes`; el documento cuenta como uno, y el país y la ciudad de residencia como dos. A Ana le faltan 6 y a Julián 8, como en el mockup.
- **Temporada** (2): se muestra lo que manda la API, como en el detalle; el nombre legible es el #10 del sprint 4.
- **Anticipo de la reserva confirmada** (3): no se muestra en la llegada; solo en la de mostrador (pendiente), como condición «Cubrir el anticipo».
- **Mostrador con el anticipo cubierto y la estancia sin pagar** (8): el botón dice «Confirmar y registrar llegada sin cobro».
- **Mostrador sin hora estimada**: antes de confirmar se envía la hora de este momento (RN-09), porque el huésped ya está en el mostrador.
- **«Completar datos» en un huésped completo** (9): no se ofrece. Lo que le quede por corregir es el #14.
- **Cupos que quedan** (10): el PMS solo conoce el cupo total; la falta de cupo para esas noches la dice el backend al guardar, arriba del diálogo.
- **Validar identidad**: se muestra «Validada» con la fecha; sin el nombre de quien validó (#15).
- **Se aceptan** los botones nombrados por el diseño (duda 7). «Guardar y seguir» pasa al siguiente huésped al que le falte algo, dando la vuelta; «Guardar» cierra.
- **Detalle**: «Carros», «Motos» y «Mascotas» pasan a un solo dato «Vehículos y mascotas», con «Cambiar» mientras la reserva se pueda modificar. La columna del TRA sale en las reservas activas.

Código (11 nov): página `src/paginas/Llegada.tsx`, diálogos en `src/reserva/llegada/` (`DialogoTra.tsx`, `DialogosLlegada.tsx`) y sus cuentas en `llegada.ts`. Pruebas: `src/reserva/llegada/llegada.test.ts` y `src/paginas/Llegada.test.tsx`. Falta: probarlo contra el backend de la rama y la parte de la auditoría que se mira en el navegador (`break-ui`, 1280, 1024 y 390 px).

Auditoría del código (paso 5, 11 nov, primera parte): `impeccable audit`, `harden` y `adapt` sin su ejecutable y sin el navegador, así que la nota es provisional: **17/20** (accesibilidad 3, rendimiento 4, tema 4, adaptable 3, integridad 3). Sin colores fijos: todo sale de las variables de la base.
- **Corregido (P2)**: al cerrar un diálogo cuyo botón ya no existe (huésped completo, identidad validada) el foco se perdía; ahora va al título. «Hacer principal» deja un aviso y lleva el foco al título. En el celular, con el motivo de «Entra sin pagar» abierto, el pie fijo tapaba media pantalla: ahora se queda al final. El texto de una reserva no-show decía «está no se presentó». La flecha de las fechas tenía un `aria-label` que los lectores ignoran.
- **Queda (P3)**: la tabla de huéspedes pasa a tarjetas en lo angosto cambiando el `display`, como la de ocupantes (Safari deja de anunciarla como tabla). Los campos del TRA van en dos columnas también a 390 px; hay que mirarlos con nombres largos en `break-ui`.

## Sprint 3: tablero por la cuenta y notas (tarea 3)
Lista sacada del contrato (11 nov); prompt en `pms/diseno/prompt-tablero-notas-sprint3.md`. Junta lo que queda del detalle por DEC-103 y DEC-104, porque se dibuja en las mismas pantallas.

| # | Pantalla | Endpoint | Qué manda en cada estado |
|---|---|---|---|
| 1 | **Tablero por la cuenta** (BL-09) | `GET /api/ocupacion` (DEC-101): `titularNombre`, `saldo`, `situacionSaldo`, `totalPagado` | El estado manda sobre la cuenta: NO_SHOW, no se presentó (apagada, último carril); FINALIZADA, ya salió; AL_DIA o A_FAVOR, al día («A favor $X · devolver»); PENDIENTE sin pagos, debe todo; PENDIENTE con pagos, con saldo. Ahora llegan también FINALIZADA y NO_SHOW; las canceladas no |
| 2 | **Notas del turno** en el detalle (BL-08) | `GET`/`POST …/notas`, `PUT …/notas/{id}/atender` (DEC-105) | Texto hasta 500. Pendientes arriba, atendidas abajo con quién y cuándo. Solo en reservas activas se escribe y se atiende; 409 `ESTADO_NO_PERMITIDO` y `YA_ATENDIDA`. Todas las respuestas traen la lista completa |
| 3 | **Notas en la llegada** | Los mismos | Solo las pendientes y «Agregar nota» |
| 4 | **Política aplicada** en el detalle (#7) | `GET /api/politicas/{id}` (DEC-104) | Tramos (`antelacionMinHoras`, `penalizacion` con `base` VALOR_TOTAL o PAGADO y `porcentaje` o `montoFijo`) y `penalizacionNoShow`. No trae nombre: se resume con el tramo que retiene |
| 5 | **Quién declaró el no-show** | `noShow` en `GET /api/reservas/{codigo}` (DEC-103) | `autorNombre` y `fechaHora`; nulo en los anteriores a V24 |

Lo que el contrato no da: el tablero no trae notas (no se dibujan ahí) y `antelacionMinHoras` se cuenta hasta la entrada.

Mockups en `pms/diseno/mockups-meridian-sprint3-tablero-notas/` (con su LEEME). Lo resuelto al compararlos con el contrato (11 nov):
- **Nombres de los apartamentos y paginador** (dudas 1 y 3): salen de la API; el paginador se queda, porque `GET /api/ocupacion` responde de 10 en 10.
- **Estado operativo en el tablero** (2): se queda bajo el nombre del apartamento, como en el sprint 2; ahora con el componente compartido `EstadoApartamento`.
- **Colores** (5): se aceptan. Al día usa el verde de confirmada; con saldo, el arena de pendiente; debe todo, un fondo terracota oscuro (`--barra-debe`) con borde terracota; ya salió, gris (`--barra-salio`); no se presentó, sin relleno y con borde punteado. Si la API no manda la cuenta, la barra conserva el color del estado.
- **Descripción de la barra**: va en `title` y en el nombre accesible del enlace. La tarjeta flotante del mockup (`.otip`) queda como mejora (P3).
- **Debe todo** (4): con el saldo, «Debe todo: $ 750.000».
- **Política** (10, 11): sin nombre, porque la API no lo trae. El resumen dice el primer tramo que retiene, con su base («del valor total» o «de lo pagado»). Se aceptan «El huésped no llega» y el aviso de la versión.
- **Notas en la llegada**: solo las pendientes y «Agregar nota»; si no hay, «Sin notas pendientes» y, si hay atendidas, «Ver todas en la reserva».

Código (11 nov): tablero en `src/paginas/Ocupacion.tsx` con la cuenta en `src/ocupacion/cuenta.ts` (no-show en el último carril, `barras.ts`); notas en `src/reserva/NotasTurno.tsx` y `src/api/notas.ts`; política en `src/reserva/PoliticaAplicada.tsx`, `politica.ts` y `src/api/politicas.ts`; quién declaró el no-show, en los datos del detalle. Pruebas: `cuenta.test.ts`, `barras.test.ts`, `politica.test.ts`, `Ocupacion.test.tsx` y `NotasYPolitica.test.tsx`. Falta probarlo contra el backend.

## Sprint 3: preparación de apartamentos (tarea 4)
Lista sacada del contrato (11 nov); prompt en `pms/diseno/prompt-preparacion-sprint3.md`. Pensada primero para el celular (personal de servicio, `mobile-native`).

| # | Pantalla | Endpoint | Qué manda en cada estado |
|---|---|---|---|
| 1 | **Por preparar** | `GET /api/apartamentos/pendientes-preparacion` (PENDIENTE_PREPARACION y EN_PREPARACION, de 10 en 10) | Personal de servicio y administrador |
| 2 | **Empezar a preparar** | `PUT /api/apartamentos/{codigo}/iniciar-preparacion` | PENDIENTE_PREPARACION → EN_PREPARACION |
| 3 | **Marcar como preparado** | `PUT …/marcar-preparado` | EN_PREPARACION → PREPARADO, con confirmación: el personal no lo puede devolver |
| 4 | **Todos los apartamentos** (administrador) | `GET /api/apartamentos` (público, trae `estadoOperativo`) · `PUT …/declarar-fuera-de-servicio` · `PUT …/marcar-pendiente-preparacion` | Fuera de servicio desde cualquier estado menos OCUPADO; volver de ahí a pendiente. Salirse de la ruta de preparación es solo del administrador (EOPE-04, 403) |
| 5 | **Bienvenida del personal de servicio** | El mismo listado, para la cantidad | — |

Mockups en `pms/diseno/mockups-meridian-sprint3-preparacion/` (con su LEEME). Lo resuelto al compararlos con el contrato (11 nov):
- **«Todos»** (dudas 1 y 3): salen los apartamentos de `GET /api/apartamentos`, ordenados por código.
- **El administrador también prepara** (4): sí, el backend se lo permite; en «Todos» ve la acción de preparación y además «Declarar fuera de servicio».
- **«Volver a preparación»** (6) va sin confirmación; marcar como preparado y declarar fuera de servicio sí la piden.
- **Menú** (2): sigue «Preparación de apartamentos»; el personal de servicio ve el título «Por preparar» y, en la bienvenida, un acceso grande con la cantidad.
- **Avisos en tiempo real** (8): no; la lista se vuelve a leer tras cada acción, con «Actualizar» o tras un rechazo.

Código (11 nov): `src/paginas/Preparacion.tsx`, la bienvenida en `src/paginas/Inicio.tsx` y la API en `src/api/apartamentos.ts`. Pruebas: `src/paginas/Preparacion.test.tsx`. Falta probarla contra el backend y en un celular real (`mobile-native`).

Por decidir con el equipo:
- **#17 · Motivo al declarar fuera de servicio** (duda 5 del LEEME): la API no lo recibe, aunque el bloqueo del tablero sí tiene motivo. Hoy se declara sin motivo.
- **#16 · Qué preparar primero**: el personal de servicio no puede leer reservas ni el tablero (403), así que no sabe qué apartamento recibe huésped hoy. Propuesta: agregar a `pendientes-preparacion` la próxima entrada de cada apartamento (fecha y hora estimada, sin datos del huésped). Mientras tanto, la lista va en el orden de la API, con los ya empezados primero.

## Para la reunión del equipo (preparado el 11 nov)
Todo lo que quedó abierto hasta la tarea 4. Lo del núcleo va en una rama `feature/nucleo/…` con su PR, y lo que cambia el contrato que usa la web se le avisa antes al equipo web (DEC-77).

| # | Qué hay que decidir | De quién sería | Qué hace el PMS mientras tanto | Propuesta |
|---|---|---|---|---|
| 13 | Leer la llegada registrada (hora, quién, motivo de entrar sin pagar) | Núcleo (un caso de uso de lectura) | No se ve después de registrar; «Corregir registro» no muestra la hora actual | `GET /api/reservas/{codigo}/estancia`, solo agrega |
| 14 | Corregir un documento mal escrito de un huésped | Núcleo (dominio) | No hay cómo | Una acción de recepción que cambie el documento y conserve los datos del TRA, con motivo |
| 15 | Quién validó la identidad del titular | Núcleo; la web lee `TitularResponse` | Solo dice cuándo | `validadoPorNombre`, como `autorNombre` en el folio |
| 16 | Qué apartamento preparar primero | Núcleo | La lista va en el orden de la API, con los ya empezados primero | La próxima entrada (fecha y hora estimada) en `pendientes-preparacion`, sin datos del huésped |
| 17 | Motivo al declarar fuera de servicio | Núcleo, si se quiere | Se declara sin motivo | Decidir si hace falta; si sí, opcional y visible en el tablero |
| 8 y 10 | Mensajes de los 409 para recepción y nombre de la temporada | Núcleo, sprint 4 (ya acordado) | Muestra el mensaje del backend y `TEM-1` | Confirmar que siguen en el sprint 4 |
| DEC-97/98 | Escoger al huésped principal al reservar y enviar los apellidos del titular aparte | pms, con mockup | El backend elige al primer adulto; se cambia en la llegada | Confirmar si entra en el sprint 4 |

Decisiones que tomó Daniel con Claude y conviene ratificar: las del paso 3 de la llegada, el tablero, las notas y la preparación (en sus secciones de arriba), sobre todo enviar la hora actual al confirmar una reserva de mostrador sin hora, y que el administrador también prepare apartamentos.

**Organización de las ramas**: la llegada, el #9, el #1b, el tablero, las notas, la política y la preparación quedaron en la misma copia de trabajo de `feature/pms/registro-llegada`, sin commit, porque tocan los mismos archivos (el detalle y `src/index.css`). Decidir si van en un solo PR o se separan por archivos al hacer los commits.

**Pruebas pendientes** (no son decisiones, pero van antes de los PR): todo contra el backend de la rama en el navegador, `break-ui` a 1280, 1024 y 390 px, y la preparación en un celular real.

## Pruebas en el navegador (12 nov)
Daniel recorrió la lista de pruebas contra el backend de la rama y en un celular real. Pasó casi todo; lo que falló o pidió cambios quedó así.

**Arreglado en el PMS** (con sus pruebas; 302 en total):
- TRA: «Guardar y seguir» ya no vuelve una y otra vez al mismo huésped (no repite los que ya se vieron en el diálogo); un huésped con todo completo tiene «Editar datos»; cerrar con cambios sin guardar (Esc o ✕) pregunta antes de descartarlos.
- Edad: antes del primer año sale en meses, «7 meses (bebé)», y no «0 años».
- Notas: si otro turno ya atendió la nota, el aviso queda arriba hasta que se lee («Entendido»), no un instante junto a la nota; la lista se relee al volver a la pestaña; los saltos de línea se respetan.
- Sesión: quien entra después de cerrar sesión empieza en Inicio; si otra pestaña cierra la sesión, esta vuelve a la entrada al instante (antes seguía abierta hasta la próxima petición, que salía sin token).
- Nueva reserva: desmarcar «El titular también se aloja» deja en blanco el ocupante 1; el teléfono se revisa en el paso 3 con la regla del backend (7 a 15 dígitos); el error lleva un sello «!» junto al campo y, si falta un mayor de edad, se marcan las fechas; Atrás y Siguiente quedan fijos al pie (en el celular, el resumen no, para no tapar media pantalla); salir con algo escrito pregunta con un diálogo de la página (al recargar o cerrar la pestaña sigue el aviso del navegador, que no se puede cambiar).
- Hora estimada de llegada: debajo del campo se lee en palabras, «Queda a las 3:00 p. m., en la tarde», porque el campo se ve en 24 horas o con a. m./p. m. según el equipo.
- Cambiar titular: no acepta el documento de un huésped con otro nombre, y ofrece «¿Es uno de los huéspedes?» para llenar sus datos tal como están en la reserva.
- Tablero en el celular: las fechas ya no se salen del recuadro (cada una en su línea si la pantalla es angosta).

**No eran fallas** (la prueba se hizo en otro orden o la lista no lo explicó bien):
- «Continuar llegada» solo sale en la reserva que entra hoy; la segunda reserva entraba mañana.
- Pasos 6 a 8 de la llegada: «Se libera al registrar la salida» solo sale con el apartamento OCUPADO, o sea que la llegada ya estaba registrada. Repetirlos con una reserva que entre hoy, antes de registrar la llegada.
- La llegada de una reserva que entra otro día no tiene botón a propósito: el detalle muestra «Registrar llegada» deshabilitado con «Disponible desde»; la prueba era abrir la dirección a mano. Ver #24.
- TRA, pasos 1, 3, 6 y 7: todos los huéspedes tenían documento, así que el documento salía solo de lectura. Solo pasa con reservas de la web o de un canal que lleguen sin documento.

**Decisiones nuevas para la reunión:**

| # | Qué hay que decidir | De quién sería | Qué hace el PMS mientras tanto | Propuesta |
|---|---|---|---|---|
| 18 | Documento obligatorio para todos los huéspedes al reservar | pms; núcleo para el registro civil | Opcional, como permite el backend (el TRA lo exige antes de la salida) | Obligatorio en el PMS, pero antes agregar `RC` (registro civil) a `TipoDocumento`: un niño menor de 7 años no tiene tarjeta de identidad y no se podría reservar |
| 19 | Dónde se valida la identidad | pms | Botón en el recuadro del titular, solo si se aloja (DEC-98) | DEC-98 ya dice que se valida el titular y solo cuando está presente. Pasar el botón a su fila en la tabla de huéspedes es solo de pantalla. Validar a cada huésped sería nuevo en el backend y no lo pide el TRA |
| 20 | Una reserva pendiente que llega a su día después de la hora límite | Núcleo | Se puede confirmar y luego declarar no-show | Que el backend no deje confirmarla después de la hora límite del día de entrada, y que el PMS solo ofrezca cancelarla |
| 21 | Un apartamento fuera de servicio se puede vender | Núcleo o pms | Sale en «Nueva reserva» si no tiene bloqueo ni reservas: estado operativo y bloqueo son independientes (CLAUDE.md) | Avisar en la tarjeta «Fuera de servicio ahora» si entra hoy o mañana; para no venderlo, el administrador lo bloquea |
| 22 | Filtros de varios valores en Reservas (dos estados, dos canales…) | Núcleo (la API recibe un valor por filtro) y pms | Un valor por filtro | Sprint 4: la API acepta la lista (`estado=CANCELADA&estado=NO_SHOW`) y el PMS los muestra como chips |
| 23 | Tablero: ventana con lo básico al tocar una reserva, rango que empiece donde empiezan las estancias en curso y columna de hoy más ancha | pms, con mockup (DEC-84) | Tocar lleva al detalle; el rango empieza hoy | Prompt a Claude Design para la ventana y el rango; la columna ancha quizá sobra si la ventana ya muestra los detalles |
| 24 | Abrir la llegada antes del día de entrada | pms | Solo el día de entrada | Dejar «Preparar llegada» desde la víspera para adelantar el TRA, con «Registrar llegada» deshabilitado hasta el día |

**Decididas el 12 nov** (Daniel con el equipo) y ya programadas, con sus pruebas:
- **#18, sí**: el documento es obligatorio para todos los huéspedes al reservar. Núcleo: `RC` (registro civil) en `TipoDocumento`, solo para ocupantes; el titular no se identifica con él (DEC-110, `feature/nucleo/decisiones-pruebas-sprint3`). PMS: «Registro civil» en la lista del ocupante y del TRA, y un adulto no puede llevarlo; el cambio de titular no ofrece a un niño con registro civil.
- **#19, sí** (ronda 2): «Validar identidad» pasó de la tarjeta del titular a su fila en la tabla de huéspedes de la llegada; solo sale si el titular se aloja y aún no está validado.
- **#20, sí**: el backend no confirma una pendiente después de la hora límite de llegada (409 `LLEGADA_VENCIDA`), salvo la creada después de esa hora (mostrador tarde). El PMS solo ofrece «Cancelar» y dice por qué.
- **#21, sí, y cambia una regla**: fuera de servicio (solo lo declara el administrador) no se vende en ninguna fecha; pendiente o en preparación sí. Una reserva que ya estaba ahí se puede seguir modificando. El diálogo de «Declarar fuera de servicio» lo dice.
- **Salida vencida** (salió en la conversación del #21): una reserva en curso cuya salida ya pasó no impide vender las noches siguientes (la disponibilidad solo mira las fechas), pero deja el apartamento ocupado, así que la llegada siguiente no se puede registrar. No se cierra sola, porque la salida cierra el folio y exige el TRA. El PMS la muestra: aviso «Salida vencida» en el detalle, la lista en la bienvenida de recepción y, en la llegada que espera ese apartamento, quién lo ocupa con el enlace para registrar su salida.
- **Salida y TRA**: el aviso «Faltan datos del TRA» de la salida tiene «Completar datos» por huésped; al cerrar los datos se vuelve a la salida.
- **Fechas**: los campos de fecha no dejan escribir un año de más de 4 cifras.
- **#22**: al sprint 4 (`docs/SPRINTS.md`).
- **#23, en parte**: tocar una reserva del tablero abre una ventana con lo básico y «Ver detalle completo»; sin rango elegido, el tablero empieza un día antes de la estancia en curso que empezó primero. La columna de hoy más ancha queda para el sprint 4, si después de probar la ventana todavía hace falta. Se hizo sin mockup (excepción a DEC-84, a pedido del equipo).
- **#24, sí**: «Preparar llegada» desde la víspera, en las pendientes y confirmadas; «Registrar llegada» sigue deshabilitado hasta el día.

**Sobre la sesión cerrada**: el equipo propuso una ventana «Tu sesión terminó» con un botón para entrar. Hoy el PMS lleva directo a la pantalla de entrada con ese mismo mensaje, que además no deja ver nada de la aplicación detrás. Con el arreglo de las pestañas pasa al instante. Si igual se quiere la ventana, es solo de pantalla.

**Para las pruebas finales**: modificar vehículos con una tarifa que cambió (se recalcula toda la estancia con la tarifa de hoy, una de las decisiones por ratificar), los pasos 6 a 8 de la llegada en el orden de la lista, los pasos 7 y 8 del cambio de titular, y volver a probar lo arreglado arriba.

**Ronda 2 (12 nov)**. Bien casi todo. Lo que salió y cómo quedó:
- **Documento según la edad**: un menor no tiene cédula de ciudadanía. La lista del ocupante (nueva reserva y TRA) ofrece solo los de su edad: menor, TI, registro civil, pasaporte o CE; adulto, todos menos registro civil. Si ya traía uno que no corresponde, se avisa junto al campo. Solo es del PMS: el backend no lo revisa (el equipo web puede hacer lo mismo).
- **Menú lateral y «Volver a reservas» fijos**: en escritorio el menú no baja con la página, y en el detalle de una reserva el enlace de volver queda arriba al bajar.
- **«Registrar llegada» deshabilitado** también en la pendiente que entra mañana, junto a «Preparar llegada», con «Disponible desde …, ya confirmada».
- **Tablero**: «Anterior» y «Siguiente» corren una semana, no el rango entero; «Hoy» vuelve a la vista de entrada (hoy y las estancias en curso enteras), conservando el apartamento filtrado.
- **Sin recargar**: la llegada (estado del apartamento), Preparación y la bienvenida se vuelven a leer cada 20 s y al volver a la pestaña. No es tiempo real: el backend no avisa; para eso haría falta un canal de eventos (núcleo, sprint 4).
- **Salida vencida en la bienvenida (falla)**: no era el código; en la base ya no había ninguna reserva en curso con la salida pasada. Se vuelve a probar cuando una en curso pase su salida sin registrarla.
- **Fuera de servicio en la llegada de mostrador (falla)**: el resultado esperado de la prueba estaba mal. Con #21 la reserva ya no se crea («El apartamento … está fuera de servicio»), así que no hay rechazo parcial.
- **Sesión en otra pestaña**: abrir el PMS en otra pestaña entraba con la misma cuenta, a propósito. El equipo decidió después una sesión por pestaña (#28, abajo).

**Ronda 3 (12 nov)**. Todo bien; salieron dos cosas, ya arregladas:
- **Menú lateral**: al quedar fijo, su fondo y su línea se cortaban donde terminaba el menú. Ahora los pinta el panel en toda la columna.
- **Cambiar titular por un huésped elegido de la lista**: decía «Ese documento es de David…» aunque se hubiera elegido a David, porque el nombre del formulario (separado o corregido por recepción) no era idéntico al de la reserva. Si se eligió de la lista, es esa persona; si se escribe a mano, el nombre se compara con el de la reserva y con el del TRA.
- Quedan por probar cuando se den: salida vencida, pendiente vencida (#20) y los pasos 3 y 4 de «Sin recargar».

## Decisiones del equipo sobre lo abierto (12 nov)
Respuestas del equipo a la página de decisiones del sprint 3 (#13 a #17, #25 a #28, R1 a R9). Lo del núcleo va en el sprint 4, cada cosa en su rama `feature/nucleo/…`; el PR de DEC-110 no cambia.

| # | Decisión | Dónde queda |
|---|---|---|
| 13 | A: `GET /api/reservas/{codigo}/estancia` (registros vigente y anulados, salida, saldo); el detalle muestra la llegada y el motivo de entrar sin pagar, y «Corregir registro» la hora vigente | Núcleo + pms, sprint 4 |
| 14 | A: acción «Corregir documento» con motivo, que conserva los datos del TRA y deja quién y cuándo. Prioridad media: si el sprint 4 se llena, pasa al 5 | Núcleo + pms, sprint 4 |
| 15 | A: `validadoPorNombre` en `TitularResponse`, nunca para la cuenta del huésped; avisar a la web | Núcleo + pms, sprint 4 |
| 16 | A: la próxima entrada (fecha y hora estimada, sin datos del huésped) en `pendientes-preparacion`; el PMS ordena por eso. Prioridad alta | Núcleo + pms, sprint 4 |
| 17 | **A** (no la recomendada): motivo opcional al declarar fuera de servicio, visible en el tablero y en la preparación | Núcleo + pms, sprint 4 |
| 8 y 10 | Siguen en el sprint 4 | Núcleo |
| DEC-97/98 | A: escoger al huésped principal y apellidos del titular aparte en «Nueva reserva», con mockup | pms, sprint 4 |
| Ramas | A: un solo PR del PMS con commits por tema; el del núcleo (DEC-110) primero | Hecho al subir |
| 25 | A: la web no ofrece pagar una pendiente que pasó su hora límite; se le avisa en el PR de DEC-110 | Web |
| 26 | B: el documento según la edad lo revisan los frontends; la regla se le pasa a la web | Nada en el núcleo |
| 27 | A: se queda la lectura cada 20 s; el canal de eventos sale del sprint 4 | Hecho |
| 28 | **B** (no la recomendada): una sesión por pestaña. Hecho: la sesión va en `sessionStorage`; otra pestaña pide entrar, cerrar la pestaña cierra la sesión y la que quedó en `localStorage` se borra | Hecho (pms) |
| R1, R4, R5, R6, R8, R9 | Se mantienen | — |
| R2 | El anticipo sigue siendo obligatorio para confirmar, sin excepción. Lo que cambia: registrar la llegada sin la estancia pagada completa («Confirmar y registrar llegada sin cobro» y la llegada con `motivoSinPago`) solo lo autoriza el administrador, como la salida con saldo. Cambia DEC-99, que se lo dejaba a recepción | Núcleo + pms, sprint 4 |
| R3 | Cambiar vehículos o mascotas solo debe cambiar el valor de esos extras; las noches conservan la tarifa con la que se reservó. Hoy se recalcula todo con la tarifa vigente: hace falta en el núcleo un «modificar solo extras» que no toque las noches | Núcleo + pms, sprint 4 |
| R7 | Los colores, tamaños y ubicaciones se deciden al final, en la fase de estilos | Fase de estilos |

## Sprint 4: reparto (17 – 20 nov)
Desde este sprint el PMS lo hacen tres personas: **Daniel**, **Reyes** y **Arias** (terminó lo del equipo web y se suma). La hipótesis del sprint es que el administrador deja el hotel listo para vender sin ayuda técnica, así que van primero las pantallas de configuración. El lunes 16 es festivo: el martes 17 se acuerdan los contratos del núcleo y se lanzan los prompts en Claude Design.

**Para la sesión de Claude de cada uno**: leer `CLAUDE.md`, `CLAUDE.local.md` (debe decir equipo **pms**), la sección pms del sprint 4 en `docs/SPRINTS.md`, esta sección y `pms/prompts/sprint4/LEEME.md`. Cada pantalla tiene su prompt para Claude Design en esa carpeta, con lo que debe adjuntarse y una parte final «Para Claude Code». Las referencias de diseño (`pms/diseno/`) no van a git: Daniel las comparte en `referencias-sprint4.zip`.

| Quién | Tarea, en orden | Rama | Depende de |
|---|---|---|---|
| Arias | 1. **Apartamentos** (`apartamentos.md`) | `feature/pms/apartamentos` | Nada: la API ya está |
| Arias | 2. **Temporadas y tarifas** (`temporadas-tarifas.md`) | `feature/pms/temporadas-tarifas` | Nada |
| Reyes | 3. **Núcleo** (abajo), una rama por cosa, PR a más tardar el miércoles 18 | `feature/nucleo/…` | Nada |
| Reyes | 4. **Usuarios, mi perfil, activar y recuperar** (`usuarios-perfil.md`) | `feature/pms/usuarios` | Nada |
| Daniel | 5. **Alojamiento** (`alojamiento.md`) y **política de cancelación** (`politica.md`) | `feature/pms/alojamiento`, `feature/pms/politica` | Nada |
| Daniel | 6. **La pantalla de lo que entre del núcleo**, a medida que se integra | `feature/pms/decisiones-sprint4` | Cada PR de Reyes |
| Daniel | 7. **Huésped principal y apellidos en «Nueva reserva»** (`nueva-reserva-principal.md`) | `feature/pms/nueva-reserva-principal` | Nada |
| Daniel | Revisar los mockups (paso 3) y los PR de Arias y Reyes | — | — |

Pasan al sprint 5, o los toma quien termine primero: **novedades de mantenimiento** (la API las lista por apartamento: o diez llamadas, o un listado general en el núcleo) y **titulares** (la API no tiene buscador por nombre ni documento). Las pantallas de **canales de venta** (conflictos y bitácora) van con Channex en el sprint 5.

**Núcleo de Reyes**, en orden. Cada cosa con su `DEC-nn` (el siguiente libre al integrar), sus pruebas (caso feliz y violación), `docs/api/openapi.yaml` regenerado y, si cambia algo que lee la web, el aviso en el PR:
1. **#16 · Próxima entrada en la preparación**: `GET /api/apartamentos/pendientes-preparacion` trae de cada apartamento la fecha de la próxima entrada de una reserva activa y su hora estimada, sin ningún dato del huésped (el personal de servicio no ve reservas). Ordenada por esa fecha (los sin próxima entrada al final). Solo agrega campos.
2. **R2 · Llegada sin el pago completo, solo con el administrador**: `PUT /api/reservas/{codigo}/registrar-llegada` con `motivoSinPago` (la estancia no está pagada completa) responde 403 o 409 si quien lo hace no es administrador; recepción sí registra la llegada de una reserva pagada. El anticipo para confirmar sigue obligatorio (RES-15). Reemplaza «la autorización la da quien registra la llegada» de DEC-99.
3. **#17 · Motivo al declarar fuera de servicio**: `PUT /api/apartamentos/{codigo}/declarar-fuera-de-servicio` acepta un cuerpo opcional con `motivo` (hasta 500), que llega en `ApartamentoResponse` y en el tablero (`GET /api/ocupacion`) mientras siga fuera de servicio. Lleva migración.
4. **#13 · Leer la llegada registrada**: `GET /api/reservas/{codigo}/estancia` con los registros (vigente y anulados: hora, autor con su nombre, `motivoSinPago`), la salida si la hubo y el saldo. Solo para el personal: el huésped no lo ve (DEC-99).
5. **#15 · Quién validó la identidad**: `validadoPorNombre` en `TitularResponse` para el personal; en `/api/mi-cuenta` del huésped va vacío (como DEC-103). Avisar a la web.
6. **R3 · Vehículos y mascotas sin recalcular las noches**: cambiar carros, motos o mascotas de una reserva solo ajusta esos cargos; el valor de las noches queda como se congeló al reservar, aunque la tarifa haya cambiado. Hoy pasa por `PUT …/modificar` (DEC-106), que recalcula todo con la tarifa vigente: hace falta una acción propia, por ejemplo `PUT /api/reservas/{codigo}/extras`.
7. **#10 · Nombre de la temporada en el desglose**: cada noche del valor trae el nombre de la temporada además del id. Solo agrega.
8. **#8 · Mensajes de los 409 para recepción**: textos sin códigos internos ni palabras técnicas. Cambia textos que ve la web (el `motivo` de DEC-94 no cambia, solo `mensaje`): avisar a la web.
9. Si cabe: **#22**, filtros de varios valores en `GET /api/reservas` (`estado=PENDIENTE&estado=CONFIRMADA`), y **#14**, «Corregir documento» con motivo, autor y fecha, que conserva los datos del TRA.

### Avance de Daniel (13 nov) y próxima sesión
- **Hecho e integrado**: alojamiento (PR #75), política de cancelación (PR #76), las pantallas de DEC-111 a DEC-118 (PR #84) y la tarea 7, huésped principal y apellidos en «Nueva reserva» (`feature/pms/nueva-reserva-principal`). Cada una con su sección abajo: lo resuelto contra el contrato, la prueba en el navegador y la auditoría.
- **Sigue** (desde el 13 nov, «Sprint 4: nuevo reparto»): la IA no bloqueante del PMS (B4d), adelantada del sprint 5. La planeación, las herramientas y las carpetas se deciden en una sesión nueva, empezando por una lluvia de ideas; el uso se lleva al cliente en el cierre 4.
- **Revisar**: los PR de Arias (el resto del sprint 4) y de Reyes (la base publicada).
- **Para el núcleo, sin urgencia**: ahora son de Arias («Para Arias», tarea 3).
- **Al cerrar el sprint**: limpiar las ramas viejas y el worktree `../SGA-nucleo`.
- **Cómo se trabaja con Claude** (además de lo de arriba): el ejecutable de `impeccable` no se corre; la auditoría lee sus guías a mano. La prueba en el navegador la hace Claude con el Chromium de Playwright que ya está en el equipo, desde su carpeta temporal (no queda nada en el repositorio), contra el backend local y `npm run dev`. Antes de probar, el backend debe estar reiniciado con lo último de develop. Commits, merges y PR los hace Daniel desde GitHub Desktop: «Merge into current branch… → `origin/develop`»; si hay conflictos, Claude los resuelve y Daniel da «Continue merge».
- **Base local de Daniel**: la política tiene una versión 2 igual a la 1 (se publicó para probar); APT-101 se declaró fuera de servicio y se devolvió a «Preparado».

## Sprint 4: nuevo reparto (13 nov)
Al núcleo de Reyes no le falta nada (DEC-111 a DEC-120 integrados), Arias terminó sus dos pantallas y Daniel las suyas. Desde aquí **Arias termina lo que queda del sprint 4 del PMS**, y **Daniel y Reyes adelantan el sprint 5**. La tabla de «Sprint 4: reparto» queda como historia.

### Para Arias
Al empezar: `CLAUDE.md`, `CLAUDE.local.md` (equipo **pms**), la sección pms del sprint 4 en `docs/SPRINTS.md`, esta sección y `pms/prompts/sprint4/LEEME.md` (el paso a paso de cada pantalla). Cada tarea, en su rama desde develop y con su PR. En orden:
1. **Usuarios, mi perfil, activar la cuenta y recuperar la contraseña** (`pms/prompts/sprint4/usuarios-perfil.md`, `feature/pms/usuarios`). Era de Reyes, que no la había empezado. Es la más grande: Claude Design con su prompt y sus adjuntos, la revisión contra el contrato (paso 3), el código, la prueba en el navegador y la auditoría, como en apartamentos. El backend ya existe.
2. **Pantallas de DEC-119 y DEC-120** (`feature/pms/decisiones-119-120`). Sin mockup: siguen los componentes de la base, como en «Sprint 4: lo que entró del núcleo». Leer las dos en `docs/DECISIONES.md`.
   - DEC-119: los filtros de Reservas pueden ser de selección múltiple (`estado`, `apartamento` y `canal` repetidos o con comas); por ejemplo «Activas» = pendiente, confirmada y en curso.
   - DEC-120: «Corregir documento» con motivo (obligatorio, hasta 500) en el detalle del huésped (`PUT /api/reservas/{codigo}/ocupantes/{ocupanteId}/documento`) y en el del titular (`PUT /api/reservas/{codigo}/titular/documento`), y la lista de correcciones con quién y cuándo (`ReservaResponse.correccionesDocumento`, marcando las del titular). Sus 409 salen junto a la acción.
3. **Núcleo sin urgencia**, una rama `feature/nucleo/…` por cosa, cada una con su `DEC-nn`, sus pruebas (caso feliz y violación) y `docs/api/openapi.yaml` regenerado. Los detalles están anotados en cada sección de este archivo:
   - que la API exija al menos una foto y que `quitarImagen` rechace la última (Apartamentos);
   - que `RegistrarBloqueoUseCase` rechace un bloqueo que ya empezó, con el reloj inyectado (Apartamentos);
   - el largo del nombre de un medio de pago (400 y no 500; Alojamiento);
   - que los rechazos de temporadas digan el nombre y no `TEM-n` (Temporadas y tarifas). Al integrarlo, el PMS ya no necesita cambiar los ids por nombres.

   Subir fotos desde el equipo sigue para después de la entrega.
4. **Novedades de mantenimiento y titulares**. Primero su núcleo: un listado general de novedades (hoy la API las lista por apartamento) y un buscador de titulares por nombre o documento. Luego las pantallas, con un prompt para Claude Design escrito como los de `pms/prompts/sprint4/`, que revisa Daniel antes de lanzarlo.

Al terminar: el cierre 4 con el cliente (vie 20 nov). Revisar los PR de los demás es de todos, a medida que lleguen.

### Daniel y Reyes: el sprint 5 adelantado
- **Reyes · publicar la base de datos** (BL-06). Su sesión de Claude parte de cero y decide el plan, el proveedor y las herramientas; lo decidido queda como `DEC-nn`. Lo que ya se sabe:
  - El backend lee la base de `DB_URL`, `DB_USER` y `DB_PASSWORD`, y Flyway crea las tablas al arrancar. Una base sola no se le puede mostrar al cliente: decidir si el alcance incluye publicar el backend y los frontends (DEC-82, DEC-83).
  - **Copia de respaldo desde el principio**: automática y probada (restaurar una vez para saber que sirve), antes de que la base tenga datos que importen.
  - **Seguridad**: ningún secreto en git (DEC-78). Claves, contraseñas y direcciones de la base van en variables de entorno o en el panel del proveedor, nunca en un archivo del repositorio ni en un mensaje del PR. Si el proveedor lo permite, la base no se abre a todo internet, y la aplicación no entra con el usuario administrador de la base. Antes de cada PR, revisión con la skill `/security-review` y con la que encuentre su sesión para auditar seguridad.
  - `SGA_DATOS_DEMO`: en un servidor real va en `false`. Si la base es para mostrársela al cliente, decidir si lleva los datos de prueba y cambiar su clave (`SGA_DEMO_CLAVE`).
  - Migraciones: la base publicada aplica las nuevas en cada despliegue; una migración ya aplicada nunca se edita. Si cambia `application.properties` o la configuración, es núcleo (`feature/nucleo/…`).
- **Daniel · IA no bloqueante** (B4d): un asistente en el PMS que resuelve dudas de quien lo use (cómo hacer algo, qué hacer en un caso) y lo que salga de la lluvia de ideas. La planeación, las herramientas y las carpetas se deciden en su sesión. Lo que ya se sabe (`CLAUDE.md`): la IA va tras un puerto (núcleo, en `domain/`), con una implementación local para cuando no haya clave; nunca impide operar; la clave va por variable de entorno; el endpoint nuevo entra en `PermisosPorRol` y `openapi.yaml` se regenera. A un servicio externo no se le mandan datos personales de los huéspedes sin decidirlo a propósito. El uso se le presenta al cliente en el cierre 4.

**Dónde se pueden cruzar**:
- las migraciones de Arias con la base publicada de Reyes (número: el siguiente libre al integrar);
- el panel del asistente de Daniel y las pantallas de usuarios de Arias en el marco del PMS (`App.tsx`, `navegacion.ts`): el asistente va en su propio componente, con una sola línea en el marco;
- `application.properties`, que pueden tocar Reyes y Daniel, cada uno en su rama de núcleo con un PR pequeño.

## Sprint 4: alojamiento (`feature/pms/alojamiento`)
Mockups en `pms/diseno/mockups-meridian-sprint4-alojamiento/` (segunda entrega, con los mínimos abiertos y un servicio desactivado). Revisados contra `docs/api/openapi.yaml` y el dominio el 13 nov. Pantalla en `src/paginas/Alojamiento.tsx`, bloques en `src/alojamiento/`.

Lo que el diseño dejó abierto, resuelto con el contrato:
- **Descripción** obligatoria (`minLength 1`), sin largo máximo: sin contador. Nombre y ciudad hasta 120, dirección hasta 200, normas opcional.
- **Ubicación**: latitud de −90 a 90 y longitud de −180 a 180, las dos obligatorias. Se acepta la coma decimal, y si se pega «4.6372, -75.5701» (como lo copia Google Maps) se reparte entre los dos campos.
- **Reglas**: siempre se envían los 21 parámetros, aunque mascotas esté oculto (si se oculta, sus campos no frenan el guardado). Los cinco mínimos se editan en «Avanzado», que se abre solo si uno tiene error. Plazos de confirmación y de pago en línea, y el mínimo de tramos, desde 1 (`ParametrosAlojamiento`). Los cupos son carros y motos (`Reserva.vehiculos()`), y el cargo de mascota es por cada una, una vez por estancia: los textos de efecto lo dicen así. Pago en línea y cobro del parqueadero, de lo que traiga la API.
- **Servicios**: la API devuelve también los desactivados (`activo: false`): van al final, con «Desactivado» y sin acciones. Un servicio «Incluido» se crea con valor 0 y no ofrece «Cambiar valor». «Desactivar» también puede rechazarse por el mínimo de servicios activos (409): el mensaje sale en su diálogo.
- **Medios de pago**: la API solo devuelve los habilitados, en mayúsculas; el que se deshabilita sale de la lista y se vuelve a habilitar escribiéndolo en «Agregar». El mensaje del 409 se muestra tal como llega, porque el mínimo es configurable. «Deshabilitar» no pide confirmación (el prompt no la pide).
- **Marca**: fuentes escritas a mano (letras, números y espacios, hasta 60); radios de 0 a 99 px, enviados como «8px»; logo y portada opcionales. Contraste WCAG de siete pares, cortado (no redondeado) a un decimal; desde 4,5:1 «se lee bien». Si el alojamiento no tiene marca, el formulario parte de unos valores neutros, no de los de ningún hotel.
- **Cambios sin guardar**: se pregunta al salir de la pantalla y también al cambiar de pestaña o volver a la lista en el celular, porque cada bloque se vuelve a cargar al abrirlo.
- **Menú**: el de `src/navegacion.ts`; el del mockup era de ejemplo.
- Al guardar otro nombre, la cabecera del panel se actualiza sin recargar.

**Para el núcleo (Reyes), sin urgencia**: el nombre de un medio de pago se guarda en `VARCHAR(40)` (`alojamiento_medio_pago.nombre`) y la API no valida el largo, así que uno más largo da un 500. El PMS no deja escribir más de 40. **Hecho** en DEC-121 (`feature/nucleo/largo-medio-pago`): el backend responde 400.

**Probada en el navegador** (13 nov, Chromium contra el backend local con los datos demo, como administrador): guardar datos, marca y reglas; descripción vacía, latitud fuera de rango, anticipo de más de 100 y color mal escrito marcados junto al campo; pegar las coordenadas de Google Maps; la pregunta al cambiar de pestaña con cambios; el 409 por el mínimo de medios de pago junto al bloque, y volver a habilitar un medio escribiéndolo. Sin desbordes a 1280, 1024 y 390 px. Los datos demo quedaron como estaban (solo `normas` pasó de vacío a nulo, que es lo mismo).

**Auditoría** (`impeccable` audit, harden y adapt; `break-ui` con el GET del alojamiento reemplazado por el peor caso: nombre de 120, dirección de 200, 14 servicios con nombres largos, 6 medios de pago, sin ubicación, sin marca, vacío y uno; a 1280, 820, 640 —200 % de zoom— y 320 px). El detector no encontró nada. Arreglado:
- P1: tras un error de validación el foco va al primer campo marcado, y el aviso «Revisa los campos marcados» se quita al corregirlos (antes se quedaba).
- P2: las pestañas se recorren con las flechas, Inicio y Fin, con un solo punto de tabulación; entre 800 y 900 px se desplazan en vez de salirse.
- P2: en el celular, cada servicio muestra nombre y valor arriba, las dos etiquetas juntas y las acciones abajo (antes quedaban revueltas).
- P2: la latitud y la longitud vacías ya no muestran un ejemplo en gris que parecía un dato guardado.
- P3: el triángulo de «Avanzado» junto al título, los números sin unidad del mismo ancho que los que la tienen y el contraste en una sola fila (par a la izquierda, medida a la derecha) cuando cabe.

Quedan, sin arreglar a propósito: el rótulo «Alojamiento» sobre el título viene del mockup; los nombres de los medios de pago salen sin tildes («Tarjeta credito») porque la API los guarda en mayúsculas sin ellas.

## Sprint 4: política de cancelación (`feature/pms/politica`)
Mockups en `pms/diseno/mockups-meridian-sprint4-politica/`, revisados contra el contrato el 13 nov. Están bien; lo abierto se resuelve así:
- **Frase del tramo de en medio**: la de «Ver política» (`src/reserva/politica.ts`), «Entre 24 y 48 horas antes de la entrada», no la del mockup. Las dos pantallas cuentan los tramos con la misma función.
- **Base «el anticipo exigido»**: falta en `politica.ts` (hoy dice «el 100 %» sin la base); se agrega con esta pantalla.
- **Monto fijo**: la base es un tope (`Penalizacion.calcular`): «Se retienen $ 50.000, sin pasar del valor total». El renglón usa el selector [% / $] del mockup, así que «porcentaje y monto a la vez» no se puede escribir; si la API lo rechaza, el mensaje sale arriba.
- **Versiones anteriores**: `GET /api/politicas` trae los tramos de cada versión (de la más alta a la más baja, con la vigente incluida): al abrir una se cuentan sus tramos. No hay fecha de publicación en el contrato: no se muestra.
- **Sin política publicada**: el formulario parte de un tramo desde 0 horas y el no-show, los dos en 100 % del valor total, para que se ajusten.
- **Validación**: tramo desde 0 horas, horas repetidas y mínimo de tramos (el de `parametros.minimoTramosCancelacion`) se revisan al escribir, como en el mockup; los renglones se muestran en el orden en que se escriben y la vista previa los ordena.
- **Salir con cambios**: se pregunta, como en Alojamiento.

Programada en `src/paginas/Politica.tsx` (`/politica`) y `src/paginas/PublicarPolitica.tsx` (`/politica/publicar`, solo el administrador); la lógica del formulario en `src/politica/formulario.ts`. Las frases salen de un solo componente, `src/politica/PoliticaContada.tsx`, que ahora usa también «Ver política» del detalle. La vigente se lee de `GET /api/politicas/vigente` (404 = todavía no hay) y las anteriores de la lista sin la vigente. Al publicar se vuelve a `/politica` con «Versión N publicada».

**Probada en el navegador** (13 nov, contra el backend local): la vigente y las anteriores plegadas; horas repetidas marcadas en los dos renglones con «Publicar» deshabilitado; la pregunta al salir con cambios; publicar (201) y volver con el aviso. Sin desbordes a 1280, 1024 y 390 px. En la base local quedó una versión 2 igual a la 1.

**Auditoría** (referencias de `impeccable` audit, harden y adapt leídas a mano, sin su ejecutable; `break-ui` sobre los datos de prueba): arreglados el triángulo de las versiones plegadas y el error de horas, que ahora está enlazado a su campo para los lectores de pantalla.

## Sprint 4: apartamentos (`feature/pms/apartamentos`, Arias)
Mockups en `pms/diseno/mockups-meridian-sprint4-apartamentos/S4Apartamentos/` (8 pantallas, cada una a 1280 y 390 px, con su LEEME). Revisados el 13 nov contra `docs/api/openapi.yaml` y el dominio (`Apartamento`, `Bloqueo`). Lo resuelto, con el número de la duda del LEEME:
- **Cantidad y paginador** (1): el título lleva `totalElementos` y el paginador es el de Reservas, con `GET /api/apartamentos?pagina=`. Trae también los retirados.
- **Sin foto** (2): la miniatura punteada con el ícono sale cuando `imagenes` viene vacío o la imagen no carga.
- **Bloqueos** (4): `fin` es exclusivo, como la estancia: «24 → 27 nov» deja libre la noche del 27. El diálogo dice «Desde» y «Hasta», con la pista «Esas noches no se venden en ningún canal. La noche de “Hasta” ya se vende». `vigente` = no levantado. Arriba van los vigentes que no han terminado (`fechaFin` > hoy), con «Levantar». En «Ver anteriores» van los levantados y los ya terminados, en gris y sin acción.
- **Fotos al crear** (6): acordado el 13 nov, todo apartamento tiene al menos una foto, como dice el prompt. El formulario la exige («Agrega al menos una foto»), aunque la API hoy acepta crear sin fotos (`minItems: 0`). La primera que se agrega queda marcada como principal, y con botones de opción nunca hay dos. El rechazo «Exactamente una imagen debe ser la principal» queda solo por si llega de la API.
- **Quitar fotos** (7): sin confirmación, porque se vuelve a agregar con la misma dirección. Se puede quitar la principal: la API pasa a principal la primera de las que quedan. La única foto no se puede quitar: su «Quitar» no aparece, y la pista dice «Agrega otra antes de quitar esta». Desde DEC-123 la API también lo rechaza, esté a la venta o retirado; si llega el rechazo, sale junto a la foto.
- **Vista previa** (8): solo en el diálogo de foto. Si no carga, «Agregar foto» sigue habilitado: puede ser una dirección que el navegador no muestra pero sí existe.
- **Al crear** (9): nace en «Pendiente de preparación» y «Retirado» (`Apartamento.crear`); la cabecera muestra los dos.
- **Mascotas** (10, 16): `limiteMascotas` vacío = usa el límite del hotel (`GET /api/alojamiento`, `parametros.limiteMascotas`). La sección dice «Hasta N mascotas (el límite del hotel)». Si `aceptaMascotas` está apagado, el texto del mockup sin acción. El rechazo del diálogo (número negativo) va junto al campo.
- **Mensajes** (11, 12): los textos propios del mockup se quedan. Los rechazos salen con el `mensaje` de la API junto a la acción que los causó: código repetido (409 `DUPLICADO`, junto al código), foto repetida o más de 10 (junto a la dirección), fechas del bloqueo (junto a las fechas), venta (en el diálogo). Ningún texto se compara para decidir nada.
- **Enlaces** (14): «Ir a temporadas y tarifas» lleva a `/tarifas` y cada reserva afectada a `/reservas/:codigo`.
- **Menú** (15): ya está en `src/navegacion.ts`; no se cambia.

**Para el núcleo, hecho** en DEC-123 (`feature/nucleo/al-menos-una-foto`): la API exige al menos una foto al crear y `quitarImagen` rechaza la última aunque esté retirado. Va en el dominio (409), no como `minItems: 1` en el Request.

Propuesta para el equipo (no está en el prompt, no se dibuja por ahora): DEC-113 ya trae `motivoFueraDeServicio` en `ApartamentoResponse`. Se podría mostrar bajo «⊘ Fuera de servicio» en el detalle.

Programada en `src/paginas/Apartamentos.tsx` (`/apartamentos`) y `src/paginas/FichaApartamento.tsx` (`/apartamentos/:codigo`, solo el administrador); los diálogos y la lógica sin pantalla en `src/apartamentos/` (`formulario.ts` con sus pruebas) y la API en `src/api/apartamentos.ts`. Pruebas: `src/paginas/Apartamentos.test.tsx` y `src/apartamentos/formulario.test.ts`.

**Probada en el navegador** (13 nov, contra el backend local con los datos demo, como administrador): lista y detalle a 1280, 1024 y 390 px sin desbordes; crear (errores por campo, código repetido junto al código, aviso de que nace retirado); poner a la venta sin tarifa (mensaje de la API y «Ir a temporadas y tarifas»); bloqueo con las fechas al revés y uno válido; agregar foto con vista previa, hacer principal y cambiar la capacidad. Las fotos de los datos demo (`picsum.photos`) no cargaban ese día (respondía 522): la pantalla las deja como recuadro vacío mientras esperan.

**Auditoría** (`impeccable` audit, harden y adapt; su detector sin hallazgos. `break-ui` con un apartamento real de peor caso en la base local: código de 9 cifras, nombre de 120, descripción de 2000 con una dirección sin espacios, 10 fotos con 3 rotas, 15 características con una de 100 sin espacios, japonés y emoji, 6 bloqueos con un motivo de 500 y 3 levantados):
- P2: en la lista, con un código largo se cortaba «Ver»: el nombre se queda con el ancho que sobra y se recorta (como el titular en Reservas).
- P2: al levantar un bloqueo o quitar una foto, el botón usado desaparece; el foco vuelve al título y no al comienzo de la página.
- P2: el ✕ de una característica mide 44 px con el dedo.
- P3: el ✕ de las características es el ícono del PMS, no un carácter; sin la barra roja a la izquierda de las fotos con error (los campos ya se marcan).

Queda, a propósito: una descripción de 2000 caracteres se lee completa en el detalle (es la pantalla del administrador); el rótulo «Apartamento» sobre el título viene del mockup.

**Ajustes tras la prueba de Arias** (13 nov):
- **Código repetido**: se avisa al salir del campo, sin esperar a «Crear» (`GET /api/apartamentos/APT-n`: 200 es que existe; 404, libre). Al crear, la API lo vuelve a revisar.
- **Bloqueo en un calendario** (`src/apartamentos/CalendarioBloqueo.tsx`), como el de la web con la marca de Meridian: un clic elige la primera noche y el siguiente el día en que vuelve a venderse; un mes a la vez, hasta 12 meses adelante, con las flechas del teclado. Las noches pasadas no se eligen; las que tienen una reserva activa (de `GET /api/ocupacion` del apartamento) salen tachadas y las ya bloqueadas con las rayas del tablero. Como la API rechaza un bloqueo encima de una reserva activa, el rango no puede pasar por encima de una; el día en que vuelve a venderse sí puede ser la llegada de una reserva.
- **Bloqueos en el pasado**: la API acepta uno que empezó antes de hoy (por ejemplo en marzo), que no tiene sentido. La pantalla ya no lo deja. **Hecho** en DEC-122 (`feature/nucleo/bloqueo-sin-pasado`): la API responde 409.

**Para el núcleo (Reyes), sin urgencia**:
- Que `RegistrarBloqueoUseCase` rechace un bloqueo cuyo inicio ya pasó (409), con el reloj inyectado. Hoy solo lo cuida el PMS.
- Subir las fotos desde el dispositivo (pedido por Arias el 13 nov): hoy la API solo recibe la dirección de una foto ya publicada. Hace falta un endpoint que reciba el archivo (multipart), lo valide (tipo, tamaño), lo guarde tras un puerto (`AlmacenImagenes`, con implementación local en disco y luego una nube) y devuelva su dirección. Es para después de la entrega universitaria; el PMS solo cambiaría el diálogo de foto.

## Sprint 4: temporadas y tarifas (`feature/pms/temporadas-tarifas`, Arias)
Mockups en `pms/diseno/mockups-meridian-sprint4-tarifas/S4Tarifas/` (7 pantallas, cada una a 1280 y 390 px, con su LEEME). Revisados el 13 nov contra `docs/api/openapi.yaml` y el dominio (`CalendarioTemporadas`, `Temporada`, `Tarifa`, `ActivadorApartamentoService`). Lo resuelto, con el número de la duda del LEEME:
- **Tarifas de la temporada nueva** (1, 3): la API exige la tarifa de cada apartamento **a la venta** (TAR-03: `buscarActivos` son los de `activo`, que es «A la venta»); la de los retirados es opcional. El diálogo trae todos los apartamentos (recorre las páginas de `GET /api/apartamentos`): los que están a la venta llevan el campo obligatorio y la marca «A la venta»; los retirados, «(opcional)». Por eso un apartamento a la venta nunca queda «Sin tarifa», y la pista «No se puede poner a la venta» solo sale en los retirados (APA-11 pide tarifa en todas las temporadas activas para activarlo).
- **Fechas** (4, 5): la temporada incluye los dos días (`[desde, hasta]`, TEM-01), al revés del bloqueo. Dos temporadas activas no pueden compartir ni un día: si una termina el 15, la siguiente empieza el 16. El diálogo lo dice en la pista. Antes de enviar, la pantalla revisa el solapamiento con las temporadas activas que ya conoce y lo dice con el nombre y las fechas de la otra. El mensaje de la API trae ids (`La temporada TEM-5 se solapa con TEM-2`): si aun así llega, se cambian los ids por los nombres; nunca se muestra un id.
- **Historial** (6): `GET …/tarifas/historial` ya viene de la más reciente a la más antigua (`vigenteDesde` y luego versión). La versión se cuenta por apartamento y temporada (`Tarifa.nuevaVersion`). «✓ Vigente» se queda: es la que está en `GET …/tarifas` (las vigentes). El nombre de cada temporada sale de `GET /api/temporadas`, también de las inactivas.
- **Rechazos de la tarifa** (8): valor mayor que cero, revisado en la pantalla (TAR-02); el 409 de la API (temporada inactiva) sale en el diálogo con su mensaje.
- **Textos y botones** (9, 11): se quedan los del mockup.
- **Desactivar en rojo** (10): se queda, porque no se deshace.
- **Ver inactivas** (12): plegado al entrar.
- **Nombre repetido** (13): la API no lo impide; la pantalla tampoco. Nombre vacío: «Escribe el nombre de la temporada.».
- **Menú y enlace** (14): ya están en `src/navegacion.ts` y en el diálogo de venta de apartamentos.
- Las tarifas vigentes de un apartamento pueden traer las de temporadas inactivas. La tabla solo muestra las columnas de las activas, y el historial solo marca «✓ Vigente» las de temporadas activas.

Programada en `src/paginas/Tarifas.tsx` (`/tarifas`, solo el administrador). Los diálogos y la lógica sin pantalla están en `src/tarifas/` (`Dialogos.tsx` y `logica.ts`, con sus pruebas), y la API en `src/api/tarifas.ts`. `Dialogo` tiene ahora la opción `ancho` (720 px en el computador) para el historial. `listarTodosLosApartamentos` devuelve la ficha completa (también la usa Preparación). Pruebas: `src/paginas/Tarifas.test.tsx` y `src/tarifas/logica.test.ts`.

**Probada en el navegador** (13 nov, contra el backend local con los datos demo, como administrador):
- La pantalla a 1280, 1024 y 390 px, sin desbordes.
- Nueva temporada:
  - vacía: un error junto a cada campo, también la tarifa de los 10 apartamentos a la venta (de 13);
  - solapada: la pantalla nombra la otra temporada y sus fechas;
  - válida: se creó.
- La API, sin la revisión de la pantalla, responde `409 La temporada TEM-5 se solapa con TEM-4`, que la pantalla convierte en nombres.
- Tarifa en 0 (error junto al campo) y una válida: la tabla cambia sin recargar.
- Historial, cambiar fechas (al revés y válidas) y desactivar.
- Quedó la temporada «Prueba del PMS 2035», inactiva, en la base local.

**Auditoría** (`impeccable` detect: sin hallazgos en lo nuevo; los 4 que reporta son de secciones anteriores de `index.css`). Para `break-ui`, dos temporadas de peor caso en 2041, ya desactivadas: un nombre de 77 caracteres, 99 noches de estancia y $ 9.999.999 por noche.
- P1: con 5 temporadas, a 1280 la página se ensanchaba a 1875 px. El texto para lectores de pantalla del encabezado «Historial» (posición absoluta) se salía de la caja que se desplaza. Los marcos de las tablas quedaron relativos.
- P2: un nombre largo ensanchaba su columna a 624 px; ahora parte en renglones (de 120 a 200 px).
- P2: las celdas de la tabla de temporadas tenían `display: flex` y desarmaban la tabla; el arreglo va en un `div` interno. El nombre del apartamento, que es el encabezado de su fila, se veía como encabezado de columna.
- P3: con el dedo, cada valor de la tabla mide 44 px de alto.

Queda, a propósito: con 5 o más temporadas, a 1280 la tabla de tarifas se desplaza de lado dentro de su caja, como las otras tablas anchas del PMS.

**Para el núcleo (Reyes), sin urgencia**: que los rechazos de temporadas nombren la temporada y no su id (`CalendarioTemporadas.validarSinSolape` y `exigirTemporadaActiva`). Hoy el PMS cambia `TEM-n` por el nombre; otro cliente vería el id.

## Sprint 4: lo que entró del núcleo (`feature/pms/decisiones-sprint4`)
Cambios chicos sobre pantallas que ya existen, sin mockup nuevo: siguen los componentes de la base.
- **DEC-111 · orden de la preparación**: el backend ya ordena por la entrada más cercana. El PMS solo separa «Ya empezados» y «Pendientes» con un orden estable, así que dentro de cada grupo se respeta el de la API; bajo «Pendientes» dice «Empieza por el de arriba: es el que recibe huéspedes primero».
- **DEC-112 · llegada sin pago, solo el administrador**: recepción ya no ve «Entra sin pagar la estancia» ni el motivo; la pista del botón dice «Falta cobrar la estancia. Solo el administrador puede dejar entrar sin pagarla.» Si el backend responde 403, sale arriba como cualquier rechazo de la llegada.
- **DEC-113 · motivo de fuera de servicio**: «Declarar fuera de servicio» pide un motivo opcional (hasta 500, `CampoMotivo` con `opcional`); sin motivo no se envía cuerpo. El motivo se ve en la tarjeta de la preparación y en el tablero (en la cuadrícula, dos renglones y el texto completo al pasar el puntero; en el celular, completo).
- **DEC-114 · llegada registrada**: el detalle de una reserva en curso o finalizada lee `GET /api/reservas/{codigo}/estancia` y muestra la llegada con su hora y quién la registró, la corrección con su motivo y la hora anterior, el motivo de entrar sin pagar y la salida. «Corregir registro» parte de la hora vigente. Las demás reservas no la piden.
- **DEC-115 · quién validó**: en la llegada, «Validada · El {fecha}, por {nombre}» con `validadoPorNombre`; sin nombre (cuenta borrada), solo la fecha.
- **DEC-116 · vehículos y mascotas sin recalcular**: el diálogo llama a `PUT /api/reservas/{codigo}/extras` con los tres números. Ya no cotiza la estancia ni advierte «Cambiaron las tarifas»: la diferencia es solo la de los extras, con los precios de hoy, y dice «El valor de las noches no cambia». Se puede mientras la reserva esté pendiente o confirmada, también si llega después de su fecha; en otro estado, el diálogo explica que se carga como consumo en el folio. Se quitó `modificacionCon`, que solo servía para mandar la reserva completa a `/modificar`.
- **DEC-117 · nombre de la temporada**: `TablaDesglose` (detalle, llegada y «Nueva reserva») muestra `temporadaNombre`; si una respuesta vieja no lo trae, el id.
- **DEC-118 · mensajes de los 409**: nada en el PMS; los mensajes ya llegan en palabras de recepción. `src/reserva/mensajes.ts` sigue cambiando solo los de otro turno (por `motivo`, DEC-94).

**Probado en el navegador** (13 nov, contra el backend local con develop): motivo de fuera de servicio en el diálogo, la preparación y el tablero (APT-101 quedó otra vez «Preparado»); recepción sin «Entra sin pagar» y el administrador con ella; la llegada registrada y corregida en el detalle de RES-2026-00046 y «Corregir registro» con su hora; el diálogo de vehículos de una confirmada con la entrada pasada. Sin desbordes a 1280 y 390 px.

## Sprint 4: huésped principal y apellidos en «Nueva reserva» (`feature/pms/nueva-reserva-principal`)
Mockups en `pms/diseno/mockups-meridian-sprint4-nueva-reserva/S4Paso3/` (computador, celular y la hoja de estados, con su LEEME). Revisados el 13 nov contra `docs/api/openapi.yaml` y el dominio (`TitularReservaRequest`, `OcupanteRequest`, `Reserva.marcarPrincipal`). Cuadran con el prompt; lo que el LEEME dejó dudoso, con su número:
- **Datos de ejemplo** (1): las fechas de nacimiento inventadas solo son del mockup.
- **Nombres aparte de los ocupantes** (2): no. El prompt solo separa los del titular. El ocupante del titular sí se envía con `nombres` y apellidos aparte (la API lo acepta, BL-07), así que su nombre completo puede pasar de 120. Los demás siguen con «Nombre completo»; separarlos va con el TRA del sprint 5.
- **Cuándo sale la opción** (3, 4): solo en los ocupantes que tendrán 18 años o más el día de entrada; sin fecha de nacimiento, no. Vale el que eligió recepción mientras siga siendo adulto; si no eligió, o deja de serlo (se corrige su fecha o la de entrada, o se quita del grupo), el primer adulto, que es el titular cuando se aloja. Marcar o desmarcar «El titular también se aloja» vuelve a ese de siempre (`principalDelGrupo`).
- **Error de «sin mayor de edad»** (5): se queda donde ya estaba en la app, bajo las pistas de «Ocupantes» y con las fechas marcadas.
- **Aviso general** (6): el de la app, «Revisa 1 campo marcado» en singular.
- **Textos** (7, 8): se quedan los del mockup. Los menores no llevan texto: la pista general lo explica.
- **Tipos de documento y formato de fecha** (9, 10): los de la app.
- **Un solo adulto**: su opción queda marcada, sin deshabilitar, con la pista «Es el único mayor de edad del grupo, así que no se puede cambiar».

Programada en `src/reserva/nueva/PasoPersonas.tsx` y `formulario.ts` (`nombreDelTitular`, `principalDelGrupo`, `titularParaEnviar`, `ocupantesParaEnviar`, con sus pruebas). `Campo` tiene ahora la opción `contador` (lo escrito contra su `maxLength`, a la derecha del error). Al titular se le envían `nombres`, `primerApellido` y `segundoApellido`, y también `nombre` armado mientras quepa en 120 (la API usa las partes cuando llegan). Cada ocupante lleva `principal`, `true` solo en el elegido. Después de crear, «Hacer principal» de la llegada sigue sirviendo para cambiarlo.

**Probada en el navegador** (13 nov, contra el backend local, como recepción): el titular sin primer apellido (error junto al campo, con el contador); Ana María por defecto y luego Julián, también con las flechas; Sofía, de 9 años, sin la opción; crear (RES-2026-00053): el detalle muestra a Julián como huésped principal y al titular «Ana María Ríos Gómez». La reserva quedó cancelada en la base local. Sin desbordes a 1280, 1024 y 390 px.

**Auditoría** (referencias de `impeccable` audit, harden y adapt y `craft-floor` leídas a mano, sin su ejecutable; `break-ui` con 8 ocupantes, nombres de 120 y apellidos de 60 con tildes y guiones, un correo de 85 sin espacios, «Jo», «Ñ», japonés y emoji, a 1280, 820, 640, 320 px y con la letra al doble):
- P1: al seguir con errores, el foco se quedaba en «Siguiente»; ahora va al primer campo marcado.
- P2: a 320 px «Siguiente: resumen →» se salía 11 px del pie (venía de antes). En el celular los botones del pie toman su ancho con menos relleno.

Queda, a propósito: el nombre largo del ocupante del titular se corta dentro de su campo de solo lectura (se lee completo en los campos del titular).

## Sprint 4: usuarios, mi perfil y la cuenta (`feature/pms/usuarios`, Arias)
Mockups en `pms/diseno/mockups-meridian-sprint4-usuarios/S4Usuarios/` (13 hojas, cada una a 1280 y 390 px, con su LEEME). Revisados el 13 nov contra `docs/api/openapi.yaml` y el backend (`GestionUsuarios`, `CuentaPersonal`, `RecuperacionContrasena`, `CodigosDelPersonal`). Lo resuelto, con el número de la duda del LEEME:
- **«Iniciar sesión»** (1): a Claude Design no le llegó `ingresar.png` y dibujó otra plantilla. Manda la pantalla programada: se queda la de siempre y solo se le agregan los enlaces «Olvidé mi contraseña» y «Activar mi cuenta» (el correo con el código no trae un enlace, así que la persona necesita una forma de llegar a `/activar`) y el aviso de éxito al volver. Activar y Recuperar usan esa misma plantilla. El «Ver» del mockup es el «Mostrar» de la entrada, ahora en `componentes/CampoClave.tsx`.
- **Datos obligatorios** (2, 14): la API pide los cinco datos (`@NotBlank`) y la fecha de nacimiento anterior a hoy (`@Past`); la pantalla lo revisa antes de enviar. El correo, con arroba y dominio con punto (el `@Email` del backend acepta `a@b`). Sin más reglas de formato para el teléfono ni el documento.
- **Código errado o vencido** (3, 4, 5): la API responde lo mismo (401 `CREDENCIALES_INVALIDAS`) si el correo no existe, el código no coincide, venció o ya tuvo 5 intentos fallidos, así que no hay un estado aparte de «demasiados intentos». El administrador **no puede reenviar** el código de activación (no hay endpoint), pero «Olvidé mi contraseña» sí sirve para una cuenta sin activar: por eso, al fallar el código de Activar, la pantalla ofrece «Pedir un código nuevo» (va a `/recuperar` con el correo) en vez de «Pídele al administrador uno nuevo». En Recuperar, el paso 2 tiene «Pedir otro código o usar otro correo» (vuelve al paso 1); el backend no envía más de 3 códigos en 15 minutos y no lo dice.
- **Enlaces** (6): «Volver a iniciar sesión» en Activar y Recuperar. El 403 es la página «Sin permiso» de siempre.
- **Cambiar la contraseña en Mi perfil** (7): la API cierra todas las sesiones de esa persona, esta incluida. Al cambiarla, el PMS vuelve a «Iniciar sesión» con «Tu contraseña cambió. Entra con la nueva.» (`terminar` en `sesion/contexto.ts`), y la pantalla lo avisa antes con una pista.
- **La contraseña actual errada** responde 401 (`CREDENCIALES_INVALIDAS`), y el PMS cerraba la sesión ante cualquier 401 con token. Ahora `api/cliente.ts` no la cierra cuando el 401 es por una contraseña mal escrita: el error sale junto al campo.
- **«Le enviamos… un código»** (8): dentro del diálogo, como en el mockup, con «Cerrar»; al cerrarlo, un aviso sobre la lista.
- **Cambio de rol** (9): el aviso va dentro del diálogo en cuanto se elige otro rol, sin confirmación aparte. Si el administrador cambia **su propio** rol, el aviso dice «Tendrás que volver a entrar» y, al guardar, el PMS vuelve a «Iniciar sesión» con el rol nuevo dicho.
- **Eliminar** (10): se puede eliminar una cuenta sin activar. El rechazo (el último administrador, una cuenta ya eliminada) sale en el diálogo con el mensaje de la API. La propia cuenta no ofrece «Eliminar».
- **Orden** (11): el de la API, por nombre, con las eliminadas en su lugar.
- **Menú** (12): el de `src/navegacion.ts`.
- **Textos** (3, 13): se quedan los del mockup, salvo los de arriba. Las líneas de cada rol dicen lo que de verdad abre su menú (`navegacion.ts`).
- **Nombre en la cabecera**: al cambiar el nombre propio (en Mi perfil o en Usuarios), la cabecera lo toma sin volver a entrar (`renombrar`).

Programada en `src/paginas/Usuarios.tsx` (`/usuarios`, solo el administrador), `Perfil.tsx` (`/perfil`, todos), `ActivarCuenta.tsx` (`/activar`) y `RecuperarContrasena.tsx` (`/recuperar`), las dos fuera de la sesión. Los diálogos, los campos de la persona, el estado de la cuenta y la lógica sin pantalla están en `src/usuarios/`, y la API en `src/api/usuarios.ts`. Pruebas: `Usuarios.test.tsx`, `Perfil.test.tsx`, `CuentaSinSesion.test.tsx` y `src/usuarios/logica.test.ts`.

**Probada en el navegador** (13 nov, Chrome contra el backend local, con el correo en modo local para leer los códigos del log): crear (vacío, correo repetido, creado), editar con cambio de rol, activar con el código errado y con el bueno, entrar con la cuenta nueva, Mi perfil (contraseña actual errada sin perder la sesión, guardar datos, cambiar la contraseña y volver a entrar), recuperar en dos pasos (reglas, código errado, hecho) y eliminar. Sin desbordes a 1280, 1024, 390 y 320 px y sin errores en la consola. En la base local quedaron cuentas de prueba eliminadas.

**Auditoría** (guías de `impeccable` a mano y `break-ui` con un nombre de 109 caracteres y un correo de 106). Arreglado:
- P2: un correo largo partía en mitad de una palabra; ahora parte en la arroba o en un punto (`Correo`, con `<wbr>`), en la tabla y en el diálogo de eliminar, donde además se salía del borde.
- P2: el recuadro del navegador alrededor de «Rol» y el documento corrido 2 px (los `fieldset` sin reiniciar).
- P2: el aviso de «¿Se venció el código?» con el botón al lado dejaba el texto en una columna angosta; el botón va debajo, a todo el ancho.
- P3: entre 760 y 1024 px la tabla partía los nombres; ahí se ve en tarjetas, como Temporadas y tarifas.

**Para el núcleo (sin urgencia, se suma a la tarea 3 de «Para Arias»)**:
- Que una contraseña actual errada en `PUT /api/usuarios/perfil/contrasena` no responda 401 (es el estado de «no hay sesión»): un 409 o un 400 en el campo `actual`. El PMS ya lo maneja, pero cualquier otro cliente cerraría la sesión. **Hecho** en DEC-124: 400 con el código `CREDENCIALES_INVALIDAS` y el detalle en `actual`.
- El correo de activación dice el rol crudo (`RECEPCIONISTA`) en `CodigosDelPersonal.texto`: que diga «Recepción».
- Que el administrador no pueda eliminarse a sí mismo (hoy solo lo impide si es el último administrador). La pantalla no lo ofrece.
- Reenviar el código de activación desde Usuarios: hoy se resuelve con «Olvidé mi contraseña».
- Ya hecho por Reyes en DEC-118 (#83): los rechazos de temporadas dicen el nombre y no `TEM-n`; se quita de la tarea 3.
## Sprint 4: filtros de varios valores y corregir documento (`feature/pms/decisiones-119-120`, Arias)
Sin mockup: siguen los componentes de la base, como en «Sprint 4: lo que entró del núcleo».
- **DEC-119 · filtros de varios valores**: en Reservas, Estado, Apartamento y Canal son un botón que dice lo elegido («Todos», «Activas», «APT-101, APT-102» o «3 elegidos») y abre una lista de casillas (`componentes/SeleccionVarios.tsx`, la lógica del texto en `componentes/eleccion.ts`). Estado trae dos atajos: «Activas» (pendiente, confirmada y en curso) y «Terminadas» (finalizada, cancelada y no se presentó). Los valores van separados por comas en la dirección y en la consulta; una dirección con el parámetro repetido (`estado=A&estado=B`) también se lee. Cada filtro aplicado sigue siendo una sola etiqueta para quitarlo («Estado: Activas»). En el computador la lista flota sobre la tabla; en el celular empuja hacia abajo. Se cierra con Escape, al tocar fuera o con «Listo».
- **DEC-120 · corregir documento**: en el detalle de una reserva activa, cada huésped con documento tiene «Corregir documento» junto a «Editar datos», y el titular lo tiene en «Datos de la reserva» (su documento actual se lee de `GET /api/titulares/{id}`, porque la reserva no lo trae). El diálogo (`reserva/DialogoCorregirDocumento.tsx`) muestra el documento actual y pide el nuevo y el motivo (obligatorio, hasta 500). Antes de enviar revisa que no sea el mismo y que el tipo sirva para la edad del huésped (#18); al titular no le ofrece registro civil (TIT-01). Los 409 de la API (otro del grupo ya lo tiene, otro titular ya lo tiene) salen en el diálogo. Al corregir, la sección «Documentos corregidos (n)» (`reserva/CorreccionesDocumento.tsx`) lista cada corrección, la más reciente primero: de quién (con «Titular» si fue el del titular), cómo estaba (tachado) y cómo quedó, el motivo, quién y cuándo. Se ve también en las reservas ya terminadas, que no ofrecen corregir. El diálogo del TRA dice dónde corregir un documento ya registrado.
- **Prueba de la llegada** («una pendiente que entra hoy ofrece Continuar llegada»): fallaba después de las 10 p. m., cuando pasa la hora límite de la reserva de prueba; ahora fija el reloj al mediodía.

**Probada en el navegador** (13 nov, Chrome contra el backend local): «Activas» y dos apartamentos, con sus etiquetas y la dirección; los filtros en el celular; corregir el documento de un huésped (el mismo documento se marca junto al campo), el historial y el diálogo del titular. Sin desbordes a 1280 y 390 px y sin errores en la consola. En la base local, RES-2026-00016 quedó con dos correcciones de prueba («Prueba del PMS»): el documento se cambió y se devolvió como estaba.
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
- [x] Tamaños de letra, interlineados y espacios en variables de `src/index.css` (`feature/pms/variables-y-traspaso`): ningún componente escribe píxeles de letra; quedan escritos a mano solo espacios propios de un componente (por ejemplo, el hueco del botón «Ver» en la contraseña). Las capturas de las pantallas salen idénticas antes y después.
- [ ] Dominancia del azul: se ve con el cliente en la fase de estilos, al final.
- [ ] Probar en un celular real, con los márgenes de la muesca: sprint 3.
- [x] Auditoría de las cinco pantallas con las skills elegidas (paso 5). Quedó pendiente: márgenes laterales para la muesca con el celular en horizontal (hay que probarlo en un teléfono), pasar a variables los tamaños de letra y los espacios fijos (20 % libre del sprint siguiente) y la dominancia del azul (se muestra al cliente en el cierre).
