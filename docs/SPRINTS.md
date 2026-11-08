# Sprints (MVP1)

Cinco sprints de una semana, del 26 de octubre al 27 de noviembre de 2026 (DEC-77). Quién toca qué está en `docs/EQUIPOS.md`. Estado: `[x]` hecho · `[~]` en curso · `[ ]` pendiente. **Cada equipo actualiza solo su sección.**

## Cómo iteramos con el cliente (LEAN)
Cada sprint es una vuelta de **construir → medir → aprender**:
1. **Hipótesis** (lunes): qué creemos que le sirve al cliente y cómo lo vamos a saber. Ejemplo: «recepción encuentra una reserva en menos de 30 segundos».
2. **Construir** lo mínimo que la pone a prueba, integrado en `develop`.
3. **Medir** en el cierre (viernes): demo con el cliente sobre `main` y, si se puede, que él mismo lo use. Se anota lo que dijo y la medida.
4. **Aprender**: se decide **seguir, ajustar o descartar**, y el backlog se reordena. Lo que el cliente pida entra a `docs/BACKLOG.md`, nunca directo al sprint en curso.

Reglas:
- **Capacidad**: alrededor del 20 % de cada sprint (desde el 2) queda libre para lo que salió del cierre anterior.
- **Requisitos nuevos**: los de la entrevista entran a `docs/BACKLOG.md` y se priorizan con el cliente en el cierre 1. Desde ahí se reparten entre los equipos según el producto (web o pms).
- **Cierre**: el jueves se integra todo lo terminado en `develop`; el viernes, antes de la demo, `develop` pasa a `main` con la etiqueta `sprint-N`. Lo que no terminó no se muestra: vuelve al backlog o al sprint siguiente.

## Cada tarea es un corte vertical (backend + pantalla)
El cliente solo puede probar lo que ve en una pantalla, así que cada tarea del sprint se entrega completa: **la pantalla en `web/` o `pms/` y lo que haga falta del backend para que funcione**, y la hace un solo equipo de punta a punta. La mayoría de los endpoints ya existen (B1–B4b); cuando falte uno, lo construye el mismo equipo dentro de la tarea.
- **Lunes, contrato primero**: si la tarea necesita un endpoint nuevo o un cambio, se acuerdan la ruta, el Request y el Response antes de programar. La pantalla puede avanzar contra datos simulados mientras el backend se termina.
- **Si toca el núcleo** (dominio, migraciones, seguridad común): ese cambio va primero, en su propia rama `feature/nucleo/…` y en un PR pequeño, a más tardar el miércoles, para que el otro equipo alcance a revisarlo. La tarea del equipo se apoya en él después.
- **Terminado** quiere decir: `cd backend && ./gradlew build` verde con pruebas del caso feliz y de la violación de cada regla nueva; `npm run build` y `npm test` verdes en el frontend tocado; los mensajes de error del backend (400/401/403/404/409) se muestran entendibles en la pantalla; integrado en `develop` y probado allí con el otro frontend también corriendo.
- **Probar en local**: el backend corre con PostgreSQL y las variables de entorno de cada quien; cada frontend corre con `npm run dev` y su proxy de Vite reenvía `/api` al backend (`SGA_API_URL` si no está en `http://localhost:8080`), así que no hace falta CORS; `VITE_API_URL` va vacía y solo se usa, junto con `SGA_CORS_ORIGENES`, cuando un frontend se publica en otra dirección que la API (DEC-82).

## Lo que cambió en el backend para la Entrega 2 (DEC-86)
Integrado en `develop` (PR #18). Ninguna pantalla existente se rompe: ningún frontend usaba lo que cambió de forma. El contrato completo está en `docs/api/openapi.yaml`.

**Los dos equipos, en local**
- Traer `develop` aplica la migración `V15__parqueadero_y_mascotas`. La siguiente migración nueva es la `V16` o la que siga libre al integrar.
- Los datos de prueba y Swagger ahora están **encendidos por defecto**. Sobre una base vacía quedan 10 apartamentos, un usuario por rol, reservas en todos los estados y por los tres canales, un bloqueo y un apartamento fuera de servicio. Las cuentas y su clave están en `README.md`. `SGA_DATOS_DEMO=false` los apaga.
- En una base que ya tenía datos, esa carga puede fallar y dejar en el log `Datos de prueba (12.3): no se cargaron`, sin afectar el arranque: por ejemplo, si `APT-101` ya no está en `PENDIENTE_PREPARACION` o sus fechas chocan con una reserva. Para tener el escenario completo, borrar y crear otra vez la base `sga`.
- Las reservas de prueba ocupan noches: si una búsqueda o una demo no encuentra un apartamento, revisar `GET /api/ocupacion` antes de suponer un error. La `PENDIENTE` de prueba vence sola a las 24 horas.

**Contratos que cambiaron** (ningún frontend los consumía todavía)
| Endpoint | Cambio |
|---|---|
| `PUT /api/apartamentos/{codigo}/estado-operativo` | Ya no existe. Se usa `iniciar-preparacion`, `marcar-preparado`, `marcar-pendiente-preparacion` y `declarar-fuera-de-servicio`, sin cuerpo. `OCUPADO` solo lo pone registrar la llegada |
| `GET /api/apartamentos/disponibles` | Responde `PaginaResponse`, con imagen principal, valor estimado y ubicación. Filtros opcionales: `fechasNacimiento`, `capacidadMinima`, `precioMinimo`, `precioMaximo`, `caracteristicas`, `servicios` |
| `GET /api/canales`, `GET /api/conflictos/pendientes`, `GET /api/apartamentos/{codigo}/tarifas` | Responden `PaginaResponse`: los datos vienen en `contenido`, con `?pagina=` |
| `GET /api/reservas` | Ordena de la más reciente a la más antigua |
| Todo error | 404 de ruta, 405 y 500 también llegan como `ErrorResponse`, con nuevos códigos `METODO_NO_PERMITIDO` y `ERROR_INTERNO`; un choque de doble venta en PostgreSQL es 409 `CONCURRENCIA` |

**Campos nuevos, compatibles** (agregar campos no rompe a nadie): `vehiculos` y `mascotas` en `POST /api/reservas` y en la respuesta de la reserva; `cupoParqueadero`, `aceptaMascotas`, `limiteMascotas` y `cargoMascota` en los parámetros de `GET /api/alojamiento`.

**PMS**
- Endpoints nuevos para las pantallas que vienen: `GET /api/ocupacion` (tablero), `GET /api/apartamentos/pendientes-preparacion`, `PUT /api/apartamentos/{codigo}` e imágenes (`POST …/imagenes`, `PUT …/imagenes/quitar`, `PUT …/imagenes/principal`), `PUT /api/alojamiento/datos`, `GET /api/apartamentos/{codigo}/tarifas/historial` y `GET /api/politicas`.
- `PUT /api/alojamiento/parametros` reemplaza todos los parámetros: **la pantalla debe enviar los cuatro campos de parqueadero y mascotas**. Si faltan, se apagan esas reglas.
- `POST /api/reservas` acepta `vehiculos` y `mascotas`; cada regla puede responder 409 (RP-02 y RP-03), y cada mascota agrega un cargo al folio.

**Web**
- El portal (`/api/portal/**`) no cambió de forma, pero ahora lista 10 apartamentos. `APT-110` aparece aunque esté fuera de servicio: el enunciado lo pide así si no tiene bloqueo (sección 3.3).
- El portal todavía no envía `vehiculos` ni `mascotas`, ni tiene los filtros nuevos: si se quieren, `ReservarDesdePortalCommand` y `/api/portal/apartamentos/disponibles` deben ampliarse, o la pantalla puede usar `GET /api/apartamentos/disponibles`, que es público.
- `GET /api/apartamentos/{codigo}/calendario?desde&hasta` es público y da las noches libres para el calendario del detalle (CU-08).
- `GET /api/alojamiento`, que la web consume, sigue siendo público y ahora incluye también los parámetros nuevos: se suma a lo pendiente de `GET /api/portal/alojamiento` (DEC-85).

**Por confirmar con la Ficha** (`TODO(equipo)`): los apartamentos `APT-105` a `APT-110` y los valores iniciales de parqueadero y mascotas son de relleno.

## Sprint 1 · 26 – 30 oct · «Ver y encontrar»
Hipótesis: el huésped entiende qué hay disponible y cuánto cuesta; recepción encuentra cualquier reserva rápido.
- **web**
  - [x] Crear `web/` (React + Vite + TypeScript), cliente de la API y despliegue local.
  - [x] Búsqueda de disponibilidad por fechas y ocupantes, y cotización con el desglose por noche (`feature/web/busqueda-disponibilidad`, DEC-80). Ajustada tras medirla (1:19, la meta era 1 minuto): se cotiza con adultos y niños, sin fechas de nacimiento (`feature/web/pulir-busqueda`, DEC-85).
  - [x] Catálogo del portal: el inicio muestra todos los apartamentos con lo que paga un adulto su próxima noche libre, la búsqueda por fechas queda como filtro y cada apartamento tiene su página con fechas y personas que recalculan el precio; de 10 en 10, probado con datos extremos y ajustado al celular (`feature/web/catalogo-apartamentos`, `feature/web/movil`, DEC-85).
  - [x] Revisar que `web/` cumpla DEC-83: el nombre del hotel y la edad desde la que se paga salen de la API; nada de Puerta al Sol en el código.
  - [~] Definir con el cliente qué datos del apartamento ve el público (pendiente de DEC-67) y ajustar la respuesta pública: supuesto en DEC-80, se valida en el cierre 1.
  - [x] Núcleo: `feature/nucleo/contrato-api`, la documentación OpenAPI de la API para que ambos frontends la consulten.
  - [x] Núcleo: `feature/nucleo/cotizar-por-cantidad` y `feature/nucleo/proxima-noche-libre`: cotizar sabiendo cuántos pagan y la próxima noche libre de cada apartamento (DEC-85).
  - [x] Diseño base de la web: un esqueleto con variables para la marca de cada hotel, portada y panel de reserva (`feature/web/diseno-base`, DEC-87; `web/DISENO.md`).
  - [x] Núcleo: `feature/nucleo/marca-alojamiento`, la marca de cada alojamiento por la API, cargada en los datos demo (BL-05, DEC-87).
  - [x] Núcleo: `feature/nucleo/datos-demo`, apartamentos, temporadas y tarifas de ejemplo con `SGA_DATOS_DEMO=true` para probar a mano y para la demo (DEC-81).
- **pms**
  - [x] Crear `pms/` (React + Vite + TypeScript), inicio de sesión del personal y menú según el rol (`feature/pms/estructura-pms`).
  - [x] Diseño base del PMS: layouts a mano, mockups en Claude Design de las pantallas del sprint 1 y su base visual (variables de CSS y componentes comunes) aplicada a las pantallas existentes (`feature/pms/diseno-base`, DEC-84; flujo y pantallas en `pms/DISENO.md`).
  - [x] Listado de reservas con filtros y detalle de una reserva, ya con el diseño base y con el nombre del titular en el Response (`feature/pms/listado-reservas`).
  - [x] Núcleo: `feature/nucleo/entrega-2`, lo que faltaba del backend para la Entrega 2 (DEC-86).
  - [x] Núcleo: `feature/nucleo/cors-frontends`, CORS para los orígenes de `web/` y `pms/` por variable de entorno (DEC-82).
  - [x] Núcleo: `feature/nucleo/estructura-carpetas`, con las ramas de los dos equipos ya integradas: el backend pasa a `backend/` (DEC-83).
- **Cierre 1 (vie 30 oct)**: demo de las dos pantallas; priorizar con el cliente el backlog de la entrevista.

## Sprint 2 · 3 – 6 nov (lunes festivo) · «Reservar»
Hipótesis: un huésped reserva y paga solo, sin llamar; recepción crea y confirma reservas directas sin papel.
- **web**
  - [x] Reservar desde el portal, pago en línea (pasarela simulada) y pantalla de confirmación (`feature/web/reservar`, DEC-88).
  - [x] Ingreso del huésped con PIN, «mi cuenta» y «mis reservas» (`feature/web/cuenta-huesped`, DEC-90).
  - [x] Volver desde el pago con la clave de la reserva, pago en línea que ya no se hace con solo el código, y límite de mascotas por apartamento (`feature/nucleo/volver-y-mascotas`, DEC-93).
- **pms**
  - [x] Tablero de ocupación por apartamento y fechas (`GET /api/ocupacion`, DEC-86; `feature/pms/tablero-ocupacion`).
  - [x] Confirmar, cancelar y declarar no-show, con registrar pago y el bloque Pago en el detalle (`feature/pms/acciones-reserva`).
  - [x] Crear reserva directa en cuatro pasos, con «Nueva reserva» en el listado (`feature/pms/crear-reserva`).
- **Cierre 2 (vie 6 nov)**.

## Sprint 3 · 9 – 13 nov · «Operar la estancia»
Hipótesis: recepción hace llegada, cobros y salida en el sistema; el huésped resuelve solo lo de su cuenta desde el celular.
- **web**
  - [ ] Cambio de correo con verificación y edición de los datos de la cuenta (los endpoints ya existen).
  - [ ] Pulir la web: diseño adaptable al celular, textos y mensajes de error.
  - [ ] Pulir las pantallas del huésped con sesión: entrar con PIN, mi cuenta, mis reservas y el detalle de cada reserva.
  - [ ] Requisitos de la entrevista del producto web priorizados en el cierre 1.
- **pms**
  - [~] Entrevista a la recepcionista: anotada en `docs/BACKLOG.md` (BL-07 a BL-23). Entran a este sprint BL-07 a BL-12, BL-22 y BL-23; para hacerles espacio, el nombre de la temporada y los mensajes de los 409 pasan al sprint 4 (`feature/pms/entrevista`).
  - [~] Ajustes del PMS por DEC-89, DEC-91 y DEC-93: anticipo con extras, carros y motos, límite de mascotas por apartamento y extras en el detalle hechos (`feature/pms/ajustes-extras`); falta la retención antes de cancelar (`pms/DISENO.md`).
  - [ ] Núcleo, acordado el 6 nov: retención antes del no-show, `titularNombre` en el tablero, `motivo` en los 409, anticipo exigido en la respuesta, política legible para recepción, el titular que no se aloja (cambia TIT-01) y quitar el pendiente de la tarifa de Temporada Media, que configura cada alojamiento (`pms/DISENO.md`, «Cambios de backend»).
  - [ ] Núcleo, de la entrevista: datos del TRA en cada ocupante (BL-07), notas de la reserva (BL-08), saldo de cada reserva en el tablero (BL-09) y la estancia cobrada en la llegada, salvo autorización (BL-22).
  - [ ] Registro de llegada y corrección del registro, con el cambio de titular si quien reservó no es quien llega (decisión del 6 nov), los datos del TRA (BL-07), el desglose y el cobro de la estancia antes de entregar (BL-10, BL-22), quitar o agregar vehículos y mascotas antes de llegar (BL-23) y la reserva de mostrador (BL-11).
  - [ ] Folio: cargos (con cantidad, BL-12), pagos, devoluciones y reversos; cierre del folio y salida, bloqueada si faltan datos del TRA (BL-07).
  - [ ] Tablero coloreado por la cuenta (BL-09) y notas en el detalle (BL-08).
  - [ ] Probar en un celular real las pantallas del sprint 2.
- **Cierre 3 (vie 13 nov)**: preguntar al cliente por el IVA según el documento del huésped y las retenciones de empresa (BL-13).

## Sprint 4 · 17 – 20 nov (lunes festivo) · «Configurar el hotel»
Hipótesis: el administrador deja el hotel listo para vender sin ayuda técnica.
- **web**
  - [ ] Requisitos de la entrevista y del cierre 3 para la web.
  - [ ] Prueba con un huésped real de punta a punta (buscar, reservar, pagar, ver su cuenta) y ajustes de lo que salga.
- **pms**
  - [ ] Apartamentos, temporadas y tarifas, política de cancelación, parámetros y medios de pago.
  - [ ] Núcleo, pasado del sprint 3: nombre de la temporada en el desglose y mensajes de los 409 pensados para recepción (`pms/DISENO.md`, #8 y #10).
  - [ ] Usuarios del personal, novedades de mantenimiento y pantallas de canales (conflictos y bitácora, con la API del equipo web).
- **Cierre 4 (vie 20 nov)**: decidir con el cliente qué parte de Channex es imprescindible para la entrega.

## Sprint 5 · 23 – 27 nov · «Vender en plataformas y cumplir»
Hipótesis: una reserva de Booking o Airbnb entra sola y sin doble venta, y el hotel cumple con el reporte de TRA.
- **web**
  - [ ] B4c Channex: cuenta sandbox, mapeo de apartamentos y tarifas (decisión con el cliente) y entrada de reservas desde Channex.
  - [ ] B4c Channex: publicar disponibilidad y tarifas, reintentos y bitácora; prueba de punta a punta en el sandbox. Si no alcanza, el cierre 4 decide si pasa al MVP2.
- **pms**
  - [ ] B5 TRA: titular validado y envío del reporte (los datos del ocupante se piden en la llegada desde el sprint 3, BL-07); SIRE para extranjeros y adjuntos si hay capacidad (BL-18, BL-19).
  - [ ] B4d IA no bloqueante, si queda capacidad (decidir el uso con el cliente).
- **Cierre final (vie 27 nov)**: entrega del MVP1.

## Registro de cierres
Copiar esta plantilla por cada cierre:

```markdown
### Cierre N · fecha
- Hipótesis y medida: … → resultado: …
- Qué dijo el cliente: …
- Decisión: seguir / ajustar / descartar …
- Al backlog: …
```
