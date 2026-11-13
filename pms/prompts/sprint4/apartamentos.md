# Prompt para Claude Design: apartamentos (sprint 4, Arias)

**Adjuntos**: `brandbook.png`; de `mockups-meridian-sprint3-preparacion/preparacion/`, `base.css` y `LEEME.md`; de `mockups-meridian-sprint3-llegada/llegada/`, `S3Llegada.html`, `S3DialogoVehiculos.html` y sus PNG; de `mockups-meridian-sprint2/mockups/`, `S2Ocupacion.html` y su PNG; de `capturas/`, `reservas.png` y `detalle.png`.

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos (tabla, filtros, paginador, botones). Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 4 · apartamentos» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S4`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px. Los diálogos en el celular ocupan toda la pantalla.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien usa esto es el **administrador**, en el computador. Español de Colombia, sin términos técnicos. Nunca muestres valores como «PENDIENTE_PREPARACION».
- Oliva y Terracota no se usan como color de texto. Ningún estado depende solo del color: cada uno lleva su palabra.
- Las fotos **no se suben**: la API recibe la dirección (URL) de una imagen ya publicada. El campo dice «Dirección de la imagen» con la pista «Pega el enlace de una foto ya publicada (https://…)».

## Qué es un apartamento
Código (`APT-101`, lo pone el administrador al crearlo y no cambia), nombre, descripción, capacidad (personas), dormitorios, características (lista de textos: «Balcón», «Cocina integral», «Wifi»), fotos (hasta 10, una es la principal), límite de mascotas (solo si el hotel acepta mascotas), bloqueos (fechas en que no se vende, con motivo) y dos estados aparte:
- **A la venta / Retirado** (`activo`). Nace retirado. Para ponerlo a la venta necesita al menos una foto y tarifa en todas las temporadas activas. No se puede retirar si tiene reservas activas o futuras.
- **Estado operativo**: Preparado, Ocupado, Pendiente de preparación, En preparación, Fuera de servicio. Aquí solo se muestra; se cambia en «Preparación».

## Datos de ejemplo
Hoy es 17 nov 2026. Alojamiento «Puerta al Sol», 10 apartamentos (`APT-101` a `APT-110`). Administración: Andrés Felipe Gómez.

| Código | Nombre | Capacidad | Dormitorios | Venta | Estado operativo |
|---|---|---|---|---|---|
| APT-101 | Penthouse Mirador | 6 | 3 | A la venta | Preparado |
| APT-102 | Apartamento Jardín | 4 | 2 | A la venta | Ocupado |
| APT-103 | Estudio Terraza | 2 | 1 | A la venta | En preparación |
| APT-110 | Mirador del Lago | 4 | 2 | Retirado | Fuera de servicio |

`APT-101`: descripción de 3 renglones, características «Balcón», «Vista a la montaña», «Cocina integral», «Wifi», 4 fotos (la primera principal), límite de 2 mascotas, un bloqueo vigente del 24 al 27 nov «Mantenimiento de la cubierta» y uno pasado.

## Pantallas

### 1. S4Apartamentos: la lista
Ruta `/apartamentos`, grupo «Configuración». Título «Apartamentos» con la cantidad y el botón «Nuevo apartamento». Tabla de 10 en 10 con paginador (como el de Reservas): foto principal en miniatura, código, nombre, capacidad («6 personas»), dormitorios, «A la venta» o «Retirado», estado operativo y «Ver». Sin buscador ni filtros (la API no los tiene). En el celular, tarjetas.

Hoja **S4ApartamentosEstados**: cargando; sin apartamentos («Todavía no hay apartamentos» con «Nuevo apartamento»); error con «Reintentar»; apartamento sin foto (miniatura vacía con un ícono, no una imagen rota).

### 2. S4Apartamento: el detalle de uno
Ruta `/apartamentos/APT-101`, con «← Apartamentos» arriba. Cabecera con código, nombre, «A la venta» o «Retirado» y el estado operativo. Secciones, cada una con su acción:
- **Datos**: nombre, descripción, dormitorios y características, con «Editar» (diálogo 3).
- **Capacidad**: «6 personas», con «Cambiar capacidad» (diálogo 4).
- **Fotos**: cuadrícula con la principal marcada («Principal»). En cada foto: «Hacer principal» y «Quitar». Arriba, «Agregar foto» (diálogo 5) y la cuenta «4 de 10».
- **Mascotas**: «Hasta 2 mascotas», con «Cambiar» (diálogo 6). Si el hotel no acepta mascotas, la sección dice «El hotel no acepta mascotas (se cambia en Alojamiento)» sin acción.
- **Bloqueos**: los vigentes primero, con fechas, motivo y «Levantar» (confirmación). Los pasados, en gris, debajo de «Ver anteriores». «Nuevo bloqueo» (diálogo 7).
- **Venta**: «Retirar de la venta» o «Poner a la venta», como acción secundaria al final, cada una con su confirmación (diálogo 8).

Hoja **S4ApartamentoEstados**: cargando; no existe (404); error con «Reintentar»; un apartamento retirado (el aviso «No aparece en la web ni se puede reservar» arriba).

### 3. S4DialogoDatos: crear y editar
Dos variantes del mismo formulario:
- **Nuevo apartamento** (desde la lista): código (`APT-` y número, con la pista «No se puede cambiar después»), nombre (hasta 120), descripción (hasta 2000, opcional), capacidad, dormitorios, características (agregar una por una como fichas con ✕; al menos una) y fotos (al menos una dirección, una marcada como principal). Al crear, va al detalle con el aviso «APT-111 quedó creado y retirado de la venta. Para ponerlo a la venta necesita tarifa en todas las temporadas».
- **Editar** (desde «Datos»): nombre, descripción, dormitorios y características. El código y la capacidad no se cambian aquí.

Estados: errores por campo (código repetido, «El apartamento debe declarar al menos una característica», «Exactamente una imagen debe ser la principal»), enviando y rechazo general.

### 4. S4DialogoCapacidad
Campo «Capacidad (personas)». Si al guardar hay reservas que ya no caben, la API responde con su lista: el diálogo muestra «Quedó en 4 personas. Estas reservas tienen más ocupantes: RES-2026-00042, RES-2026-00051. Revísalas con recepción», con un enlace a cada una. Estados: normal, enviando, resultado con reservas afectadas.

### 5. S4DialogoFoto
«Dirección de la imagen» y una vista previa al pegarla (si no carga, «No se pudo ver la imagen: revisa el enlace»). Rechazos: más de 10 fotos, la misma dirección repetida. Al quitar la única foto de un apartamento a la venta, el rechazo «Un apartamento a la venta no puede quedarse sin imágenes».

### 6. S4DialogoMascotas
«¿Cuántas mascotas admite este apartamento?», número desde 0 («0: no admite»).

### 7. S4DialogoBloqueo
Desde, hasta y motivo (hasta 500, obligatorio). Pista: «Esas noches no se venden en ningún canal». Si la API lo rechaza (por ejemplo, fechas al revés), su mensaje va junto a las fechas.

### 8. S4DialogoVenta
- **Retirar**: «APT-101 deja de aparecer en la web y no se podrá reservar. Las reservas que ya tiene no cambian». Rechazo: «No se puede retirar un apartamento con reservas activas o futuras».
- **Poner a la venta**: rechazos «Un apartamento sin imágenes no puede publicarse» y «El apartamento necesita tarifa en todas las temporadas para activarse», este con un enlace «Ir a temporadas y tarifas». Cualquier otro rechazo sale con el mensaje de la API (por ejemplo, el mínimo de temporadas o de capacidades distintas que pide el hotel).

---

## Para Claude Code (no va a Claude Design)
- Endpoints: `GET /api/apartamentos?pagina=` (`PaginaResponse` de `ApartamentoResponse`), `GET` y `PUT /api/apartamentos/{codigo}` (`ActualizarApartamentoRequest`), `POST /api/apartamentos` (`CrearApartamentoRequest`), `PUT …/capacidad` (`CambioCapacidadResponse` con `reservasAfectadas`), `POST …/imagenes` y `PUT …/imagenes/quitar` y `…/imagenes/principal` (`ImagenApartamentoRequest`, por `url`), `PUT …/limite-mascotas`, `POST …/bloqueos` (`RegistrarBloqueoRequest`: `inicio`, `fin`, `motivo`) y `PUT …/bloqueos/{id}/levantar`, `PUT …/retirar` y `…/activar`. Si aceptar mascotas está apagado, sale de `GET /api/alojamiento` (`parametros.aceptaMascotas`).
- Ya existe `src/api/apartamentos.ts` (lo usan la preparación y la reserva): amplíalo, no lo dupliques. `EstadoApartamento` y `estadosApartamento.ts` ya traen los textos del estado operativo.
- La ruta del detalle (`/apartamentos/:codigo`) se agrega en `src/App.tsx` como las de reservas, solo para el administrador.
