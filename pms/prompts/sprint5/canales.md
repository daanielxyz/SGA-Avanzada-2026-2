# Prompt para Claude Design: canales de venta (sprint 5, Reyes)

**Adjuntos**, todos juntos en `pms/diseno/referencias-sprint5/` (fuera de git):
- `base.css`: copia de `pms/src/index.css`, el CSS real del PMS con todo lo de los sprints anteriores (también el asistente). Manda sobre el `base.css` de cualquier mockup viejo.
- `brandbook.png`.
- `reservas.png` (listado con filtros y paginación) y `detalles.png` (cabecera, menú y bloques del PMS real).
- `usuarios.png`: captura de `/usuarios` a 1280 px tomada del PMS en local, como administrador (lista de configuración con diálogos, como la que se pide aquí).
- `S3Llegada.html` y `S3Llegada.png`: un aviso de rechazo junto a la acción que lo causó.

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 5 · canales» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S5`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px. Los diálogos en el celular ocupan toda la pantalla.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien usa esto es el **administrador**, sobre todo en el computador. Español de Colombia, sin términos técnicos: nunca `EXTERNO`, `CREAR_RESERVA`, `ENTRANTE` ni `CONFLICTO`; siempre su texto (tablas de abajo).
- Oliva y Terracota no se usan como color de texto. Ningún estado depende solo del color: lleva texto o icono.
- Los conflictos son lo urgente de esta pantalla: un canal externo (Booking, Airbnb) vendió una noche que ya estaba ocupada y el sistema **la rechazó** para no vender dos veces. El huésped cree que tiene reserva; alguien tiene que llamarlo o reubicarlo.

## Cómo funcionan los canales
- Un **canal** es por dónde entra una reserva. Hay tres tipos:
  - «Portal web»: la web de reservas del hotel.
  - «Directo»: la reserva que crea recepción (teléfono, mostrador, WhatsApp).
  - «Externo»: una plataforma como Booking o Airbnb, que se conecta sola al sistema con una clave.
- El administrador **registra** un canal con nombre (hasta 80 caracteres) y tipo. Solo el externo lleva **referencia de la clave** (hasta 120): el **nombre** con el que quedó guardada la clave en el servidor (por ejemplo `BOOKING_API_KEY`), **nunca la clave misma**. La pantalla lo dice junto al campo: «Escribe el nombre con el que el equipo técnico guardó la clave, no la clave». Un canal nace activo.
- **Desactivar** un canal: deja de crear reservas nuevas; las que ya trajo siguen igual. No se puede volver a activar desde aquí (va en «Lo que falta»). Un canal inactivo se ve en la lista, en gris, sin acciones.
- **Conflicto**: cuando un canal externo manda una reserva que no cabe (noche ocupada, apartamento bloqueado o fuera de servicio, más personas que la capacidad, sin tiempo de preparación), el sistema no la crea, no toca la reserva que ya estaba y deja un conflicto **pendiente** con el motivo. El administrador lo **resuelve** escribiendo qué decidió (hasta 500 caracteres, obligatorio): «Llamé al huésped y lo pasamos al APT-104 por Booking». Resolver no cambia ninguna reserva: solo deja registrado quién lo revisó, cuándo y qué decidió. Si hace falta mover algo, se hace aparte en Reservas.
- **Bitácora**: cada mensaje con un canal externo queda registrado (consultas de disponibilidad, reservas que llegan, cancelaciones), también los rechazados, para poder demostrar qué pasó. Se consulta por canal y entre dos fechas, del más reciente al más antiguo, de 10 en 10.

### Textos de la bitácora
| Operación | Texto |
|---|---|
| Consultar disponibilidad | «Consultó disponibilidad» |
| Crear reserva | «Envió una reserva» |
| Cancelar reserva | «Canceló una reserva» |
| Publicar disponibilidad | «Le enviamos la disponibilidad» (saliente; todavía no hay eventos de este tipo) |

| Resultado | Texto |
|---|---|
| Exitoso | «Hecho» |
| Duplicado | «Repetido» (la reserva ya había llegado; no se creó otra) |
| Conflicto | «Rechazado por disponibilidad» (generó un conflicto) |
| Rechazado | «Rechazado» (otra regla: falta una tarifa, canal inactivo…) |
| Error | «Error» |

Cada evento tiene además un **sentido** («Recibido del canal» o «Enviado al canal»), un **detalle** (el código de la reserva creada o el motivo del rechazo) y el **mensaje original** del canal, en texto. El mensaje trae datos del huésped: va plegado («Ver mensaje del canal»), en letra monoespaciada y con desplazamiento propio, nunca abierto por defecto.

## Datos de ejemplo
Hoy es 17 nov 2026, 10:40 a. m. Alojamiento «Puerta al Sol».

Canales:
| Nombre | Tipo | Referencia de la clave | Estado |
|---|---|---|---|
| Portal web | Portal web | — | Activo |
| Recepción | Directo | — | Activo |
| Booking | Externo | `BOOKING_API_KEY` | Activo |
| Airbnb | Externo | `AIRBNB_API_KEY` | Inactivo |

Conflictos pendientes (2):
- Booking · reserva externa `BK-558120` · APT-103 · del 20 al 23 nov (3 noches) · detectado el 16 nov a las 9:12 p. m. · «El apartamento APT-103 ya está reservado en esas fechas (RES-2026-00042)».
- Booking · reserva externa `BK-558377` · APT-108 · del 28 al 30 nov · detectado hoy a las 7:05 a. m. · «El apartamento APT-108 admite máximo 2 ocupantes».

Bitácora de Booking, del 10 al 17 nov: 14 eventos; entre ellos, «Envió una reserva · Hecho · RES-2026-00051», «Envió una reserva · Rechazado por disponibilidad · El apartamento APT-103 ya está reservado…», «Envió una reserva · Repetido · RES-2026-00051», «Consultó disponibilidad · Hecho · 6 apartamentos disponibles», «Canceló una reserva · Hecho · Reserva RES-2026-00049 cancelada».

## Pantallas

### 1. S5Canales
Ruta `/canales`, grupo «Configuración», título «Canales de venta». Dos bloques, en este orden:

1. **Conflictos pendientes**, con su número en el título («Conflictos pendientes · 2»). Una tarjeta o fila por conflicto: canal, reserva externa, apartamento, fechas (entrada y salida, con el número de noches), cuándo se detectó (relativo y exacto: «hace 13 horas · 16 nov, 9:12 p. m.») y el motivo tal cual. Botón «Resolver». Los más antiguos primero. Si no hay: «No hay conflictos pendientes. Cuando un canal externo intente vender una noche ocupada, aparecerá aquí». Este bloque no se pagina.
2. **Canales**, con el botón «Registrar canal». Tabla de 10 en 10 con paginador: nombre, tipo, referencia de la clave (solo los externos; «—» en los demás), estado («Activo» / «Inactivo») y acciones «Ver bitácora» (solo externos) y «Desactivar» (solo activos). En el celular, tarjetas.

Hoja **S5CanalesEstados**: cargando; error con «Reintentar» (cada bloque por separado: si falla uno, el otro se ve); sin canales registrados; sin permiso (403) para quien no es administrador.

### 2. S5DialogoRegistrarCanal
Nombre, tipo (tres opciones, cada una con una línea que explica qué es) y, **solo si el tipo es externo**, la referencia de la clave con su aviso. Botones «Registrar canal» y «Cancelar». Estados: errores por campo (nombre vacío o largo; referencia que falta en un externo), enviando, rechazo general.

### 3. S5DialogoDesactivarCanal
«Booking dejará de crear reservas nuevas. Las reservas que ya trajo no cambian. Esta acción no se puede deshacer desde el PMS». Botones «Desactivar canal» y «Cancelar». Rechazo: «El canal ya está inactivo».

### 4. S5DialogoResolverConflicto
Arriba, el resumen del conflicto (canal, reserva externa, apartamento, fechas y motivo), sin editar. Campo «¿Qué se decidió?» (obligatorio, hasta 500, con contador) con un ejemplo de ayuda debajo. Aviso: «Esto no cambia ninguna reserva. Si hay que mover o crear una, hazlo en Reservas». Botones «Marcar como resuelto» y «Cancelar». Al terminar, el conflicto sale de la lista con el aviso «Conflicto resuelto». Estados: campo vacío, enviando, rechazo («Este conflicto ya lo resolvió otra persona»: se cierra y se recarga la lista).

### 5. S5Bitacora
Ruta `/canales/{id}/bitacora`, con «← Canales de venta» para volver. Título «Bitácora de Booking» y el tipo y estado del canal. Filtro de fechas «Desde» y «Hasta» (por defecto, los últimos 7 días, ambos incluidos) con «Consultar». Lista de 10 en 10, del más reciente al más antiguo, con paginador: fecha y hora, sentido, operación, resultado (con icono o etiqueta, no solo color) y detalle; debajo, plegado, «Ver mensaje del canal». En el celular, tarjetas.

Hoja **S5BitacoraEstados**: cargando; sin eventos en esas fechas; «Hasta» antes de «Desde» (error junto al campo, sin consultar); error con «Reintentar»; canal que no existe (404, con «← Canales de venta»).

---

## Para Claude Code (no va a Claude Design)
- Endpoints, todos solo de administración (`PermisosPorRol`; el menú ya tiene la opción en `src/navegacion.ts`):
  - `GET /api/canales?pagina=` → `PaginaResponse<CanalResponse>` (`id`, `nombre`, `tipo` PORTAL | DIRECTO | EXTERNO, `referenciaCredencial`, `activo`).
  - `POST /api/canales` (`RegistrarCanalRequest`: `nombre`, `tipo`, `referenciaCredencial`) → 201. Que solo el externo traiga la referencia, y siempre la traiga, lo valida el dominio: 409 (CAN-01 · CAN-02). Al registrar, la pantalla manda `referenciaCredencial` solo si el tipo es EXTERNO.
  - `PUT /api/canales/{id}/desactivar` → `CanalResponse`; 409 si ya está inactivo.
  - `GET /api/canales/{id}/eventos?desde=&hasta=&pagina=` (`desde` y `hasta` obligatorios, ISO, ambos incluidos) → `PaginaResponse<EventoCanalResponse>` (`id`, `operacion`, `sentido`, `fechaHora`, `cargaUtil`, `resultado`, `detalle`); 404 si el canal no existe.
  - `GET /api/conflictos/pendientes?pagina=` → `PaginaResponse<ConflictoResponse>` (`id`, `canalId`, `idExterno`, `apartamento`, `entrada`, `salida` exclusiva, `detectadoEn`, `motivo`, `estado`, y `resueltoPor`, `resueltoEn`, `decision` en null mientras está pendiente).
  - `PUT /api/conflictos/{id}/resolver` (`ResolverConflictoRequest`: `decision`, hasta 500) → `ConflictoResponse`; 409 si ya estaba resuelto.
- `ConflictoResponse` no trae el nombre del canal, solo `canalId`: se toma de la lista de canales ya cargada. Si el canal no está en la página cargada, se muestra el id; si eso molesta en la revisión, es una propuesta de núcleo (agregar `canalNombre`), no un cambio por cuenta propia.
- `cargaUtil` es el cuerpo que mandó el canal, sin credenciales (BIT-04), pero con datos del huésped: se muestra como texto (nunca como HTML), plegado.
- La salida del conflicto es exclusiva (EST-01): «del 20 al 23 nov» son 3 noches; la noche del 23 no se cuenta.
- Cliente nuevo `src/api/canales.ts`, con el mismo estilo de `src/api/usuarios.ts`. La pantalla de bitácora se conecta como una ruta más en `PANTALLAS` de `src/App.tsx`.
- **Manual del asistente** (DEC-127): al terminar, se actualiza `backend/src/main/resources/asistente/manual/general.md` (Canales deja de estar «en construcción») y se agrega una sección `canales.md` para administración, en el mismo PR.
- **Para el núcleo, si en la revisión hace falta** (no se dibuja ni se programa sin acordarlo): reactivar un canal; ver los conflictos ya resueltos (hoy solo se listan los pendientes); el nombre del canal en el conflicto.
