# Prompt para Claude Design: titulares (sprint 5, Arias)

**Adjuntos**, todos juntos en `pms/diseno/referencias-arias/` (fuera de git):
- `base.css`: copia de `pms/src/index.css`, el CSS real del PMS con todo lo de los sprints anteriores. Manda sobre el `base.css` de cualquier mockup viejo.
- `brandbook.png`.
- `reservas.png` (listado de reservas con filtros y paginación) y `detalle.png` (detalle de una reserva: cabecera, menú, bloques y la tabla de ocupantes), capturas del PMS real a 1280 px.
- `usuarios.png`: captura de `/usuarios` a 1280 px (lista con buscador de configuración, diálogos y estados con símbolo).

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 5 · titulares» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S5`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px. Los diálogos en el celular ocupan toda la pantalla.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien usa esto es **recepción** (y administración), sobre todo en el computador del mostrador, a veces con el huésped al frente. Español de Colombia, sin términos técnicos: nunca `CC`, `PASAPORTE` ni `CONFIRMADA` sueltos como dato crudo; los tipos de documento van como «CC», «CE», «Pasaporte» y «TI», y los estados de reserva como en la captura de reservas.
- Oliva y Terracota no se usan como color de texto. Ningún estado depende solo del color: lleva texto o símbolo.
- El titular es quien responde por la reserva y su folio; puede no alojarse. No es lo mismo que un huésped (ocupante): esta pantalla no lista huéspedes.

## Cómo funcionan los titulares
- Un **titular** se crea solo cuando se hace su primera reserva, y se reutiliza si vuelve con el mismo documento. Esta pantalla **no crea titulares**.
- **Buscar**: un solo campo, «Busca por nombre o documento». Cada palabra que se escriba debe estar en el nombre o en el documento, sin importar mayúsculas, tildes ni los puntos del número: «rios ana» encuentra a «Ana María Ríos» y «1.094.000» encuentra «1094000». Sin texto, se ven todos. Siempre por nombre, de 10 en 10.
- Sus **datos**: nombres, primer apellido, segundo apellido (opcional), documento (tipo y número), correo y teléfono. Debe tener al menos uno de los dos contactos.
- **Editar** cambia nombres, apellidos, correo y teléfono. El **documento no se edita aquí**: si quedó mal escrito, se corrige desde una de sus reservas con «Corregir documento» (ya existe en el detalle de la reserva). La pantalla lo dice junto al documento.
- **Validar identidad**: recepción revisó en persona el documento original y coincide. Se hace una sola vez y no se deshace. Desde entonces el titular ya no puede cambiar su nombre desde su cuenta en la web; solo el hotel. Se muestra como «✓ Identidad validada · 14 nov 2026, 3:24 p. m., por María Fernanda Ospina»; sin validar, «Sin validar».
- **Sus reservas**: las del titular, de la más reciente a la más antigua, de 10 en 10, con el mismo formato del listado de reservas; cada una abre su detalle.

## Datos de ejemplo
Hoy es 17 nov 2026. Alojamiento «Puerta al Sol». Recepción: María Fernanda Ospina.

| Nombre | Documento | Correo | Teléfono | Identidad |
|---|---|---|---|---|
| Ana María Ríos Pérez | CC 1.094.000 | ana.rios@ejemplo.co | 300 123 4567 | Validada el 14 nov 2026, 3:24 p. m., por María Fernanda Ospina |
| Andrés Felipe Gómez | Pasaporte AB123456 | — | +1 305 555 0142 | Sin validar |
| Carlos Restrepo Arango | CC 75.080.112 | carlos.restrepo@ejemplo.co | — | Sin validar |
| Luz Marina Cardona | CE 4.521.889 | luz.cardona@ejemplo.co | 315 987 6543 | Sin validar |

Reservas de Ana María Ríos Pérez (3): RES-2026-00051 · APT-103 · 20–23 nov · Confirmada · $ 1.200.000; RES-2026-00042 · APT-101 · 14–17 nov · En curso · $ 1.250.000; RES-2026-00017 · APT-102 · 2–4 oct · Finalizada · $ 480.000.

## Pantallas

### 1. S5Titulares
Ruta `/titulares`, grupo «Recepción», título «Titulares». Arriba, el buscador (un campo y «Buscar»; con texto, «Limpiar»). Debajo, tabla de 10 en 10 con paginador: nombre, documento, contacto (correo y teléfono, el que haya) e identidad («✓ Validada» o «Sin validar»). Toda la fila abre el detalle. En el celular, tarjetas.

Hoja **S5TitularesEstados**: cargando; sin resultados para lo buscado («Ningún titular coincide con “rodriguez”», con «Limpiar búsqueda»); todavía no hay titulares («Los titulares aparecen cuando se hace su primera reserva»); error con «Reintentar»; sin permiso (403) para el personal de servicio.

### 2. S5Titular (detalle)
Ruta `/titulares/{id}`, con «← Titulares» para volver (conserva la búsqueda). Título con el nombre completo y, al lado, el estado de la identidad. Dos bloques:
1. **Datos**: nombres y apellidos, documento (con la pista «Si está mal escrito, corrígelo desde una de sus reservas con “Corregir documento”»), correo y teléfono. Acciones «Editar datos» y, si no está validada, «Validar identidad».
2. **Reservas**, con su número en el título: tabla de 10 en 10 con código, apartamento, entrada y salida, estado y total; el código abre la reserva. Sin reservas: «Este titular todavía no tiene reservas».

Hoja **S5TitularEstados**: cargando; titular que no existe (404, con «← Titulares»); error con «Reintentar» en cada bloque por separado; datos guardados (aviso «Datos guardados»); identidad recién validada.

### 3. S5DialogoEditarTitular
Nombres, primer apellido, segundo apellido (opcional), correo y teléfono, con el documento como texto de solo lectura. Si la identidad está validada, una línea: «La identidad está validada: el titular ya no puede cambiar su nombre desde la web». Botones «Guardar cambios» y «Cancelar». Estados: errores por campo (nombres vacíos, correo mal escrito, sin ningún contacto: «Deja al menos un correo o un teléfono»), enviando, rechazo general.

### 4. S5DialogoValidarIdentidad
«¿Revisaste el documento original de Ana María Ríos Pérez?» con el documento en grande (CC 1.094.000) y el texto «Confirma solo si lo tienes en la mano y coincide. No se puede deshacer, y desde entonces su nombre solo lo cambia el hotel». Botones «Sí, validar identidad» y «Cancelar». Estados: enviando; rechazo («La identidad ya estaba validada»: se cierra y se recarga).

---

## Para Claude Code (no va a Claude Design)
- Endpoints, todos de recepción y administración (`/api/titulares/**` y `/api/reservas/**` en `PermisosPorRol`; el menú ya tiene la opción en `src/navegacion.ts`):
  - `GET /api/titulares?buscar=&pagina=` → `PaginaResponse<TitularResponse>` (DEC-131): `id`, `nombre` (completo), `nombres`, `primerApellido`, `segundoApellido`, `tipoDocumento`, `numeroDocumento`, `correo`, `telefono`, `validado`, `validadoEn`, `validadoPor`, `validadoPorNombre`. `buscar` hasta 100 caracteres (400).
  - `GET /api/titulares/{id}` → `TitularResponse`; 404 si no existe. Ya existe en `src/api/titulares.ts`.
  - `PUT /api/titulares/{id}` (`ActualizarTitularRequest`: `nombres`, `primerApellido`, `segundoApellido`, `correo`, `telefono`; nombres hasta 120 y apellidos hasta 60). 409 sin ningún contacto o sin apellido (DEC-49); el documento no viaja (TIT-05).
  - `PUT /api/titulares/{id}/validar` → `TitularResponse`; 409 `IDENTIDAD_VALIDADA` si ya estaba. Ya existe `validarTitular` en `src/api/titulares.ts`.
  - Las reservas: `GET /api/reservas?titular={id}&pagina=` (ya existe `listarReservas`).
- Los números de documento llegan sin puntos; la pantalla los muestra con separador de miles solo si son solo dígitos (CC, CE, TI); el pasaporte, tal cual.
- **Manual del asistente** (DEC-127): al terminar, se actualiza `backend/src/main/resources/asistente/manual/general.md` (Titulares deja de estar «en construcción») y se agrega lo de titulares a la sección de recepción, en el mismo PR (es núcleo: `feature/nucleo/…` o junto al PR de la pantalla, como hizo canales).
- **Para el núcleo, si en la revisión hace falta** (no se dibuja ni se programa sin acordarlo): registrar un titular sin reserva desde aquí (`POST /api/titulares` ya existe, pero el prompt no lo pide); cuántas reservas tiene cada titular en la lista.
