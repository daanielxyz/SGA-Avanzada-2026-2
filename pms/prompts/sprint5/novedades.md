# Prompt para Claude Design: novedades de mantenimiento (sprint 5, Arias)

**Adjuntos**, todos juntos en `pms/diseno/referencias-arias/` (fuera de git):
- `base.css`: copia de `pms/src/index.css`, el CSS real del PMS con todo lo de los sprints anteriores. Manda sobre el `base.css` de cualquier mockup viejo.
- `brandbook.png`.
- `reservas.png`: listado de reservas con filtros de varios valores («Activas», varios apartamentos) y paginación, captura del PMS real a 1280 px.
- `preparacion.png` y `preparacion-celular.png`: la pantalla «Preparación de apartamentos» del personal de servicio, a 1280 y 390 px: tarjetas por apartamento y botones grandes.

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 5 · novedades» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S5`. Cada pantalla a **390 px primero** y, con sufijo `Computador`, a 1280 px. Los diálogos en el celular ocupan toda la pantalla.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien más usa esto es el **personal de servicio**, desde el **celular**, recorriendo los apartamentos; también la administración, desde el computador. Botones grandes, textos cortos. Español de Colombia, sin términos técnicos: nunca `ABIERTA`, `EN_REVISION` ni `ALTA` como dato crudo; siempre su texto (tablas de abajo).
- Oliva y Terracota no se usan como color de texto. Ningún estado ni gravedad depende solo del color: lleva texto o símbolo.

## Cómo funcionan las novedades
- Una **novedad** es un daño, un faltante o algo raro encontrado en un apartamento: «El grifo del baño gotea», «Falta una toalla», «Vidrio de la ventana roto». Tiene apartamento, descripción (hasta 2000 caracteres) y gravedad.
- **Reportar** una novedad **no bloquea el apartamento ni cambia su preparación**: solo queda registrada para atenderla. Si el apartamento no se puede usar, la administración lo declara fuera de servicio en Apartamentos (ya existe). El diálogo lo dice.
- Cada novedad avanza en un solo sentido: «Abierta» → «En revisión» → «Cerrada». Una cerrada no se reabre ni se borra. Cada paso guarda quién lo dio y cuándo.
- La lista junta las novedades de **todos** los apartamentos, de la más reciente a la más antigua, de 10 en 10. Filtros de varios valores, como en Reservas: estado (con el atajo «Por atender» = Abierta y En revisión, que es lo que se ve al entrar), gravedad y apartamento.

| Estado | Texto | Acción para pasar al siguiente |
|---|---|---|
| ABIERTA | «Abierta» | «Pasar a revisión» |
| EN_REVISION | «En revisión» | «Cerrar» |
| CERRADA | «Cerrada» | ninguna |

| Gravedad | Texto | Para qué |
|---|---|---|
| ALTA | «Alta» | Impide usar algo importante o es un riesgo |
| MEDIA | «Media» | Molesta, pero el apartamento se puede usar |
| BAJA | «Baja» | Un detalle o un faltante menor |

## Datos de ejemplo
Hoy es 17 nov 2026, 10:40 a. m. Alojamiento «Puerta al Sol». Personal de servicio: Luz Marina Cardona. Administración: Andrés Felipe Gómez. Apartamentos APT-101 a APT-110.

| Apartamento | Descripción | Gravedad | Estado | Pasos |
|---|---|---|---|---|
| APT-104 | Vidrio de la ventana del cuarto roto, con riesgo de cortarse | Alta | Abierta | Reportada hoy 9:15 a. m. por Luz Marina Cardona |
| APT-101 | El grifo del lavamanos gotea aunque esté cerrado | Media | En revisión | Reportada el 15 nov 4:02 p. m. por Luz Marina Cardona; a revisión el 16 nov 8:30 a. m. por Andrés Felipe Gómez |
| APT-107 | Falta una toalla de manos | Baja | Abierta | Reportada el 16 nov 11:48 a. m. por Luz Marina Cardona |
| APT-102 | La bombilla de la lámpara de la sala no prende | Baja | Cerrada | Reportada el 10 nov; a revisión el 10 nov; cerrada el 11 nov 9:05 a. m. por Andrés Felipe Gómez |

## Pantallas

### 1. S5Novedades
Ruta `/novedades`, grupo «Operación», título «Novedades» con el botón «Reportar novedad». Debajo, los filtros (estado con «Por atender» marcado al entrar, gravedad y apartamento), plegables en el celular como en Reservas. Una tarjeta por novedad: apartamento en grande, gravedad (etiqueta con símbolo), estado, la descripción (dos renglones y «Ver más» si es larga), «Reportada hace 1 hora por Luz Marina Cardona» y el botón de la acción siguiente («Pasar a revisión» o «Cerrar»). Tocar la tarjeta abre el historial. En el computador, tabla densa con las mismas columnas y paginador.

Hoja **S5NovedadesEstados**: cargando; nada por atender («No hay novedades por atender. Si encuentras algo en un apartamento, repórtalo aquí»); ninguna coincide con los filtros (con «Limpiar filtros»); error con «Reintentar»; sin permiso (403) para recepción; el aviso tras avanzar («APT-104 · Vidrio roto pasó a revisión»).

### 2. S5DialogoReportarNovedad
Apartamento (lista de los apartamentos), descripción (obligatoria, con contador de 2000) y gravedad (tres opciones, cada una con su línea de «Para qué»). El aviso: «Reportarla no bloquea el apartamento. Si no se puede usar, avisa a Administración». Botones «Reportar» y «Cancelar». Al terminar, la novedad aparece arriba de la lista con «Novedad reportada». Estados: errores por campo (sin apartamento, sin descripción, sin gravedad), enviando, rechazo general.

### 3. S5DialogoAvanzarNovedad
Confirmación corta antes de avanzar: «¿Pasar a revisión la novedad de APT-104?» o «¿Cerrar la novedad de APT-101? Una novedad cerrada no se puede reabrir», con la descripción y la gravedad. Botones «Pasar a revisión» o «Cerrar novedad», y «Cancelar». Estados: enviando; rechazo («Esta novedad ya avanzó: puede que otra persona la haya movido», y la lista se recarga).

### 4. S5HistorialNovedad
Diálogo con el apartamento, la descripción completa, la gravedad y los pasos en orden (una línea por paso: estado, quién y cuándo; «Una persona que ya no está en el sistema» si no hay nombre). Desde aquí también está el botón de la acción siguiente. Botón «Cerrar» del diálogo.

---

## Para Claude Code (no va a Claude Design)
- Endpoints, todos de personal de servicio y administración (`PermisosPorRol`; el menú ya tiene la opción en `src/navegacion.ts`):
  - `GET /api/novedades?estado=&gravedad=&apartamento=&pagina=` → `PaginaResponse<NovedadResponse>` (DEC-132). Cada filtro con varios valores, repetidos o con comas (como DEC-119). Valores: estado ABIERTA | EN_REVISION | CERRADA; gravedad BAJA | MEDIA | ALTA; apartamento `APT-n`.
  - `NovedadResponse`: `id`, `apartamento`, `descripcion`, `gravedad`, `estado`, `historial` (del registro al paso más reciente): cada paso con `estado`, `autor`, `autorNombre` (nulo si esa cuenta ya no existe) y `fechaHora`.
  - `POST /api/apartamentos/{codigo}/novedades` (`RegistrarNovedadRequest`: `descripcion` hasta 2000, `gravedad`) → 201 con la novedad; 404 si el apartamento no existe.
  - `PUT /api/novedades/{id}/avanzar` → la novedad con el paso nuevo; 409 si ya está cerrada; 404 si no existe.
  - Los apartamentos de la lista: `listarCodigosDeApartamentos` (ya existe en `src/api/apartamentos.ts`).
- «Por atender» es el atajo de `SeleccionVarios` (`src/componentes/SeleccionVarios.tsx`, DEC-119), como «Activas» en Reservas; los filtros viven en la dirección (`?estado=ABIERTA,EN_REVISION`).
- **Manual del asistente** (DEC-127): al terminar, se pone al día `backend/src/main/resources/asistente/manual/novedades.md` y `casos-servicio.md`, en el mismo PR.
- **Para el núcleo, si en la revisión hace falta** (no se dibuja ni se programa sin acordarlo): editar la descripción o la gravedad de una novedad; adjuntar una foto del daño (subir archivos sigue para después de la entrega); reportar desde la tarjeta de «Preparación».
