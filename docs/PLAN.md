# Plan incremental del SGA

Estado: `[x]` hecho · `[ ]` pendiente · `[~]` en curso. Decisiones en `docs/DECISIONES.md`.

> **Desde el 26 de octubre de 2026 se trabaja por sprints y por equipos (DEC-77).** Este plan queda como la historia del backend hasta B4b. Lo pendiente (B4c Channex, B4d IA, B5 TRA y los frontends `web/` y `pms/`) está repartido en `docs/SPRINTS.md`, y lo que es de cada equipo en `docs/EQUIPOS.md`.

## Definición de terminado (cada incremento)
1. Un incremento = un agregado o pieza transversal = un commit (lo hace el usuario).
2. Antes de codificar, leer del Excel las reglas del agregado por ID.
3. Métodos de negocio con `// RN-xx · XXX-nn` y Javadoc completo (DEC-08); sin setters.
4. Pruebas espejo de `domain`, `@Tag("RN-xx")`, caso feliz **y** violación. Reloj por parámetro.
5. `./gradlew build` verde; informe y pausa para revisión antes del siguiente.
6. Lo indefinido → `// TODO(equipo)` y se pregunta.

## Etapa A — Dominio + persistencia por agregado
- [x] **A00 · Esqueleto**: paquetes, entidades con atributos y constructor, VO con validación, enums, puertos y servicios vacíos. Compila.
- [x] **A0 · Compartido**: `Dinero` (sumar, restar, multiplicar, esCero/esPositivo; redondeo HALF_UP al construir; nunca negativo, DEC-20), `Estancia` (noches, cantidadNoches, seSolapaCon), `Noche`. 15 pruebas (DIN-01..03, RN-03/EST-02, EST-03, EST-04, NOC-01, NOC-02).
- [x] **A1 · Apartamento**: `Capacidad.admite`, `Imagen` (URL http(s)), `EstadoOperativo.puedePasarA/permiteRegistro`, `Bloqueo.cubre/levantar`, `Apartamento.activar/retirarDeVenta/cambiarCapacidad/cambiarEstadoOperativo/registrarBloqueo/levantarBloqueo/tieneBloqueoEn/admite`, invariantes (máx. 10 imágenes, una principal, ≥1 característica). 55 pruebas (CAP, DOR, IMG, CARAC, EOPE, BLO, APA-11/12/16, TAR-03, RN-02/07/11). DEC-21..24.
- [x] **A1-P · Primer corte de persistencia**: PostgreSQL + Flyway en Gradle, properties main/test, `V1__apartamento.sql`, puerto `guardar/buscarPorCodigo`, adaptador JPA de Apartamento, 4 pruebas de ida y vuelta en H2 (Flyway + validate). Pendiente: probar contra PostgreSQL real.
- [x] **A2 · Alojamiento**: `ParametrosAlojamiento` (con mínimos configurables), `Ubicacion`, `ServicioAdicional`, `Alojamiento` (parámetros, ubicación, habilitar/deshabilitar/aceptar medios, servicios) + persistencia V2. 41 pruebas (ALO-02/03/04/06, MPAG-01/02/06, SERV-01/02/04/05, UBI-01/03, TPRE-01, PORC-01). DEC-25..27.
- [x] **A3 · Temporada y Tarifa**: `CalendarioTemporadas` (raíz, DEC-28) con `Temporada` interna, `Tarifa` versionada, `Ocupante` adelantado (DEC-29), `Cotizacion`/`LineaCotizacion` validadas, `TarificacionDomainService` (RN-05, 06) y `ActivadorApartamentoService` (TAR-03, APA-11, TEM-04) + persistencia V3 (incluye `minimo_temporadas`). 48 pruebas. DEC-28..30. `TODO(equipo)` Temporada Media.
- [x] **A1-E · Primer corte de extremo a extremo** (DEC-31..35): `Apartamento.crear`; casos de uso `CrearApartamentoUseCase` y `ConsultarApartamentoUseCase` con `CrearApartamentoCommand`/`ApartamentoResult`; `POST /api/apartamentos` (201 + Location) y `GET /api/apartamentos/{codigo}` con `CrearApartamentoRequest`/`ApartamentoResponse` y Bean Validation; `ErrorResponse` + `ManejadorErrores` (400/404/409). 16 pruebas: dominio, casos de uso con repositorios en memoria y MockMvc contra H2. Sin seguridad todavía (B3). El bean `Clock` pasa al primer caso de uso que lo necesite (`CrearReservaUseCase`, B1).
- [x] **A4 · Reserva** (DEC-36..40): `EstadoReserva.puedePasarA/retieneDisponibilidad`, `Ocupante.esDuplicadoDe` (OCU-05), `Registro`, `Salida`, `Reserva.crear/modificar/indicarHoraEstimadaLlegada/confirmar/cancelar/declararNoShow/expirar/registrarLlegada/registrarSalida/retieneNochesDe` con `creadaEn` (RN-21), RP-01 vía `CalendarioTemporadas.estanciaMinimaPara`, `Dinero.porcentaje` (RES-15); `DisponibilidadDomainService` (+ `Disponibilidad.exigir`), `BloqueoOperativoDomainService` (BLO-01), `BajaApartamentoDomainService` (APA-16) + persistencia V4 (`@Version`, `UNIQUE(canal_id, id_externo)`, registro/salida como columnas) y `db/postgresql/V4_1` con el `EXCLUDE`. 63 pruebas (RN-01/02/04/07/08/09/10/12/14/19/20/21/22, RES-15/16, OCU-02/05/12, RP-01, CORI-03, SAL-01/04/05, BLO-01, APA-16, EDO-02/03). Sin casos de uso ni REST: `CrearReservaUseCase` necesita la política vigente (A5). Pendiente: probar el `EXCLUDE` contra PostgreSQL real.
- [x] **A5 · Política y Folio** (DEC-41..44): `PoliticaCancelacion.crear/nuevaVersion/penalizacionPara` con tramos por horas y `Penalizacion` (porcentaje o monto fijo sobre `BaseRetencion`) configurables por el alojamiento, vigente = versión más alta; `ParametrosAlojamiento.minimoTramosCancelacion` (POL-01); `Folio.abrir/agregarServicioAdicional/registrarPago/registrarDevolucion/revertirPago/revertirCargo/ajustarPorModificacion/liquidarPenalidad/saldo/totalPagado/cerrar`, `Cargo` con `SentidoAjuste` y `Saldo` (DEC-20 aplicado); `CancelacionDomainService` y `NoShowDomainService` (Reserva + Folio) + persistencia V5 (incluye `minimo_tramos_cancelacion`). 40 pruebas (RN-13/14/15/16/17, POL-01/02/03/04/06, FOL-01/02/05/06, CAR-02/03/04/06/07, PAG-03/05/06/07, TPAG-02, MPAG-02, SLD-03, RES-16). Los valores de los tramos ya no son `TODO(equipo)`: los configura cada alojamiento (los iniciales llegan en B1).
- [x] **A6 · Llegada y salida (D-02)** (DEC-45, DEC-46): `RegistroLlegadaDomainService.procesarCheckIn` (apartamento PREPARADO → OCUPADO, reserva EN_CURSO), `SalidaOperativaDomainService.procesarCheckOut` (cierra el folio si sigue abierto o acepta uno cerrado, con o sin autorización; reserva FINALIZADA, apartamento PENDIENTE_PREPARACION), `Reserva.corregirRegistro` (REG-06) y `validarSalida` + persistencia V6 (`reserva_registro` con historial, migra las columnas de V4). 15 pruebas (RN-10/11/17, REG-04/06, SAL-01/02/03/04, AUTC-04, FOL-02).
- [x] **A7 · Canales, Titular, Novedad** (DEC-47..50): `Canal.desactivar/exigirReservasExternas` (referencia a la credencial, no el secreto), `ConflictoCanal.registrar/resolver`, `EventoCanal` (bitácora con `OperacionCanal` y `ResultadoEvento`), `SincronizacionCanalDomainService.integrarReservaExterna` → `IntegracionExterna` (EXITOSO, DUPLICADO o CONFLICTO), `Titular` (nombre, documento y al menos un contacto; `actualizarDatos`), `Novedad.registrar/avanzar` con historial; TIT-01 en `Reserva.crear/modificar`; `alojamientoId` en Canal y Titular (DEC-26); `ReservaRepository.buscarPorCanalEIdExterno` + persistencia V7. 32 pruebas (RN-18/19, CAN-01/02/05, CONF-01/02/03/04, BIT-01/02, TIT-01/05, NOV-01/04/06, CORI-03).

## Etapa B — Aplicación, REST e infraestructura (con `domain` completo, replicando el patrón de A1-E)
- **B1 · Casos de uso** (`@Service` + `@Transactional`) con su `XCommand`/`XResult` (DEC-31). Dividido en cuatro incrementos, un commit cada uno:
- [x] **B1a · Base, alojamiento, política, apartamento, temporadas y tarifas** (DEC-51..55): bean `Clock` en zona de Colombia (EST-06), `ServiciosDominioConfig`, `Pagina<T>` (DEC-18), `GeneradorCodigos` con secuencias de la BD (DEC-52); `InicializarAlojamientoUseCase` desde `sga.*` al arrancar (ALO-01 · DEC-53: alojamiento + política inicial + calendario con la base); `CAP-05` como parámetro `minimoCapacidadesDistintas` exigido al activar o al cambiar la capacidad de un activo (DEC-51); `APA-15` (advertir reservas afectadas); `TAR-03` al crear temporada (DEC-54) + persistencia V8 (columna del parámetro, secuencias, índice de apartamentos por alojamiento). 62 pruebas (CAP-05, APA-11/15/16, TAR-01/02/03/05, TEM-02/03/05/06/07/08/10, ALO-01/03/06, MPAG-01/06, SERV-01/02/04/05, POL-01/03/04/06, BLO-01/06, EOPE-02, UBI-03).
  - DTO de aplicación de B1a:

    | Caso de uso | Command | Result |
    |---|---|---|
    | `InicializarAlojamientoUseCase` (arranque, DEC-53) | `InicializarAlojamientoCommand` (datos, `ParametrosCommand`, servicios, medios, tramos, no-show, temporada base) | `AlojamientoResult` |
    | `ConsultarAlojamientoUseCase` (CU-50) | — | `AlojamientoResult` (datos, `ParametrosResult`, servicios, medios) |
    | `CambiarParametrosAlojamientoUseCase` / `CambiarUbicacionAlojamientoUseCase` (CU-30) | `CambiarParametrosAlojamientoCommand` / `CambiarUbicacionAlojamientoCommand` | `AlojamientoResult` |
    | `HabilitarMedioPagoUseCase` / `DeshabilitarMedioPagoUseCase` (CU-53) | `MedioPagoCommand` | `AlojamientoResult` |
    | `AgregarServicioAdicionalUseCase` / `CambiarValorServicioUseCase` / `DesactivarServicioUseCase` | `AgregarServicioAdicionalCommand` / `CambiarValorServicioCommand` / `DesactivarServicioCommand` | `AlojamientoResult` |
    | `PublicarPoliticaUseCase` (CU-35) / `ConsultarPoliticaVigenteUseCase` | `PublicarPoliticaCommand` (tramos, `PenalizacionCommand`) | `PoliticaResult` |
    | `ListarApartamentosUseCase` (CU-31) | — (página) | `Pagina<ApartamentoResult>` |
    | `ActivarApartamentoUseCase` / `RetirarApartamentoUseCase` (CU-31) | — (código) | `ApartamentoResult` |
    | `CambiarCapacidadUseCase` (APA-15) | `CambiarCapacidadCommand` | `CambioCapacidadResult` (apartamento + reservas afectadas) |
    | `CambiarEstadoOperativoUseCase` (CU-40 · CU-42) | `CambiarEstadoOperativoCommand` | `ApartamentoResult` |
    | `RegistrarBloqueoUseCase` (CU-32) / `LevantarBloqueoUseCase` (CU-52) | `RegistrarBloqueoCommand` / `LevantarBloqueoCommand` | `ApartamentoResult` |
    | `ConsultarCalendarioUseCase` / `CambiarFechasTemporadaUseCase` / `DesactivarTemporadaUseCase` (CU-33) | `CambiarFechasTemporadaCommand` / `DesactivarTemporadaCommand` | `CalendarioResult` |
    | `AgregarTemporadaUseCase` (CU-33 · TAR-03) | `AgregarTemporadaCommand` (+ tarifa por apartamento) | `TemporadaCreadaResult` |
    | `DefinirTarifaUseCase` / `ConsultarTarifasUseCase` (CU-34) | `DefinirTarifaCommand` | `TarifaResult` / lista |
- [x] **B1b · Reserva** (DEC-56..59): `CrearReservaUseCase` con su folio y su titular nuevo o actualizado, `CotizarEstanciaUseCase` (CU-09), `BuscarDisponiblesUseCase` (CU-07, solo activos), `ModificarReservaUseCase` (+ ajuste en el folio), `IndicarHoraLlegadaUseCase`, `ConfirmarReservaUseCase` (`folio.totalPagado()`), `CancelarReservaUseCase`, `DeclararNoShowUseCase`, `ConsultarReservaUseCase` (CU-49), `BuscarReservasVencidasUseCase` + `VencerReservaUseCase` (RN-21, anula sin penalidad; `CancelacionDomainService.procesarVencimiento`), `ReservaRepository.buscarPendientesCreadasAntesDe`, código `RES-aaaa-nnnnn` del generador + persistencia V9 (secuencias de titular, folio y reserva; índice por estado y creación) y prueba de bloqueo optimista entre dos transacciones. 22 pruebas (RN-01/02/05/06/09/13/14/21/22, RES-15/16, TIT-01/05, FOL-01, CAR-06, CORI-03, COT-02, DISP-02, POL-06, DEC-17).
  - DTO de aplicación de B1b, en `application/reserva`:

    | Caso de uso | Command | Result |
    |---|---|---|
    | `CrearReservaUseCase` (CU-10 · CU-11) | `CrearReservaCommand` (apartamento, fechas, canal, `TitularCommand`, `OcupanteCommand`[], hora opcional) | `ReservaResult` (estado, grupo, `CotizacionResult` congelada, política) |
    | `CotizarEstanciaUseCase` (CU-09) | `CotizarEstanciaCommand` (apartamento, fechas, fechas de nacimiento) | `CotizacionResult` (desglose por noche, total) |
    | `BuscarDisponiblesUseCase` (CU-07) | `BuscarDisponiblesCommand` (fechas, tamaño del grupo) | lista de `ApartamentoDisponibleResult` |
    | `ModificarReservaUseCase` (CU-17) | `ModificarReservaCommand` (apartamento, fechas, ocupantes) | `ReservaResult` |
    | `IndicarHoraLlegadaUseCase` (RN-09) | `IndicarHoraLlegadaCommand` | `ReservaResult` |
    | `ConfirmarReservaUseCase` (CU-18) / `ConsultarReservaUseCase` (CU-49) | — (código) | `ReservaResult` |
    | `CancelarReservaUseCase` (CU-14 · CU-15) / `DeclararNoShowUseCase` (CU-19) / `VencerReservaUseCase` (CU-20) | — (código) | `CancelacionResult` (estado, retenido, saldo y situación) |
    | `BuscarReservasVencidasUseCase` (CU-20, planificador) | — (alojamiento) | lista de códigos |
- [x] **B1c · Folio, llegada y salida** (DEC-60, DEC-61): `ConsultarFolioUseCase`, `RegistrarPagoUseCase`, `RegistrarDevolucionUseCase`, `RevertirPagoUseCase`, `RevertirCargoUseCase`, `CerrarFolioUseCase` (CU-28), `CargarServicioAdicionalUseCase` (CAR-05, solo EN_CURSO; `CargoServicioDomainService` + `Alojamiento.servicioCobrable`), `RegistrarLlegadaUseCase` (CU-21), `CorregirRegistroUseCase` (REG-06) y `RegistrarSalidaUseCase` (CU-22 · CU-38). Sin migración. 29 pruebas (RN-10/11/12/15/16/17, REG-01/02/03/04/06, SAL-01/02/03/04/06, FOL-01/02/03/05/06, PAG-02/03/05/06/07, TPAG-02, MPAG-02, CAR-03/05, SERV-02/04, AUTC-01/04, D-02, DEC-44/60), entre ellas `EstanciaCompletaTest` con Spring y H2. Las tablas de DTO de A5 y A6 de abajo quedaron como se implementaron.
- [x] **B1d · Canales, Titular y Novedad** (DEC-62, DEC-63): `RegistrarCanalUseCase`, `DesactivarCanalUseCase`, `ListarCanalesUseCase`, `RecibirReservaExternaUseCase` (CU-12), `RegistrarEventoCanalUseCase`, `ConsultarBitacoraUseCase` (CU-45), `ConsultarConflictosPendientesUseCase`, `ResolverConflictoUseCase`, `RegistrarTitularUseCase`, `ActualizarTitularUseCase`, `ConsultarTitularUseCase`, `RegistrarNovedadUseCase` (CU-43), `AvanzarNovedadUseCase` (CU-55) y `ConsultarNovedadesUseCase` (CU-44). Persistencia V10 (secuencias de canal, conflicto, evento y novedad; índice de canal por alojamiento) y puertos de listado paginados. 20 pruebas (RN-01/02/18/19, CAN-01/02/04/05, CONF-01/02/04, BIT-01/03, CORI-03, TIT-01/03/05, NOV-01/03/04/05/06, FOL-01, DEC-18/26/48/49/62/63), dos de ellas con Spring y H2 en `EstanciaCompletaTest`. La tabla de DTO de A7 de abajo quedó como se implementó. Con esto termina B1.
  - DTO de aplicación de A5 (Política y Folio), en `application/politica` y `application/folio`; el folio se identifica por el código de su reserva (DEC-61):

    | Caso de uso | Command | Result |
    |---|---|---|
    | `PublicarPoliticaUseCase` (CU-35, admin) — hecho en B1a | `PublicarPoliticaCommand` (tramos: horas, base, % o monto fijo; penalización de no-show) | `PoliticaResult` |
    | `ConsultarPoliticaVigenteUseCase` (la que acepta el huésped al reservar) — hecho en B1a | — | `PoliticaResult` |
    | `ConsultarFolioUseCase` — hecho en B1c | — (código de la reserva) | `FolioResult` (`CargoResult`[], `PagoResult`[], total pagado, saldo y situación, cerrado, `AutorizacionResult`) |
    | `RegistrarPagoUseCase` / `RegistrarDevolucionUseCase` — hechos en B1c | `RegistrarPagoCommand` (reserva, medio, monto, fecha) | `FolioResult` |
    | `RevertirPagoUseCase` / `RevertirCargoUseCase` — hechos en B1c | `RevertirPagoCommand` (reserva, pago) / `RevertirCargoCommand` (reserva, cargo, motivo) | `FolioResult` |
    | `CargarServicioAdicionalUseCase` (CAR-05 · DEC-60) — hecho en B1c | `CargarServicioAdicionalCommand` (reserva, servicio) | `FolioResult` |
    | `CerrarFolioUseCase` (CU-28) — hecho en B1c | `CerrarFolioCommand` (reserva, autor, motivo de autorización opcional) | `FolioResult` |
    | `CancelarReservaUseCase` / `DeclararNoShowUseCase` — hechos en B1b | — (código) | `CancelacionResult` (estado, retenido, saldo) |
  - DTO de aplicación de A6 (Llegada y salida), en `application/reserva`, hechos en B1c; el Command lleva el `autor`, que en B3 sale del usuario autenticado (DEC-61):

    | Caso de uso | Command | Result |
    |---|---|---|
    | `RegistrarLlegadaUseCase` (CU-21) | `RegistrarLlegadaCommand` (código, autor) | `EstanciaResult` (estado de la reserva, estado operativo del apartamento, `RegistroResult`[], `SalidaResult`, folio cerrado, saldo y situación) |
    | `CorregirRegistroUseCase` (REG-06) | `CorregirRegistroCommand` (código, fechaHora correcta, motivo, autor) | `EstanciaResult` |
    | `RegistrarSalidaUseCase` (CU-22, y CU-38 con autorización) | `RegistrarSalidaCommand` (código, autor, motivo de autorización opcional) | `EstanciaResult` |
  - DTO de aplicación de A7 (Canales, Titular, Novedad), en `application/canal`, `application/titular` y `application/novedad`, hechos en B1d; donde hay `autor`, en B3 sale del usuario autenticado (DEC-61):

    | Caso de uso | Command | Result |
    |---|---|---|
    | `RegistrarCanalUseCase` / `DesactivarCanalUseCase` / `ListarCanalesUseCase` (CU-54) | `RegistrarCanalCommand` (alojamiento, nombre, tipo, referencia de credencial) | `CanalResult` / lista |
    | `RecibirReservaExternaUseCase` (CU-12; lo invoca el adaptador del canal, B4) | `RecibirReservaExternaCommand` (canal, idExterno, apartamento, fechas, `TitularCommand`, `OcupanteCommand`[], carga útil) | `IntegracionExternaResult` (resultado, código de reserva, id del conflicto, detalle) |
    | `RegistrarEventoCanalUseCase` (BIT-03; lo invoca el adaptador del canal, B4) | `RegistrarEventoCanalCommand` (canal, operación, sentido, carga útil, resultado, detalle) | `EventoCanalResult` |
    | `ConsultarConflictosPendientesUseCase` / `ResolverConflictoUseCase` (CONF-04) | `ResolverConflictoCommand` (id, decisión, autor) | lista de `ConflictoResult` / `ConflictoResult` |
    | `ConsultarBitacoraUseCase` (CU-45) | `ConsultarBitacoraCommand` (canal, día desde, día hasta, página) | `Pagina<EventoCanalResult>` |
    | `RegistrarTitularUseCase` / `ActualizarTitularUseCase` / `ConsultarTitularUseCase` | `RegistrarTitularCommand` (alojamiento, nombre, documento, correo, teléfono) / `ActualizarTitularCommand` (id, nombre, correo, teléfono) | `TitularResult` |
    | `RegistrarNovedadUseCase` (CU-43) / `AvanzarNovedadUseCase` (CU-55) | `RegistrarNovedadCommand` (apartamento, descripción, gravedad, autor) / `AvanzarNovedadCommand` (id, autor) | `NovedadResult` (con `CambioResult`[] del historial) |
    | `ConsultarNovedadesUseCase` (CU-44) | — (apartamento, página) | `Pagina<NovedadResult>` |
- [x] **B2 · REST** por acciones de negocio con `XRequest`/`XResponse` por endpoint (DEC-31, DEC-32), errores por el manejador global de A1-E (DEC-33), `PaginaResponse<T>` de 10. Pruebas MockMvc por endpoint. Un valor de enum inválido en el Request (`base`, `estado`) debe dar 400, no llegar al caso de uso. Convenciones en DEC-64. Dividido en tres (decisión del equipo):
  - [x] **B2a · Configuración**: `AlojamientoController`, `PoliticaController`, `ApartamentoController` (listado, activar, retirar, capacidad, estado operativo, bloqueos), `TemporadaController` y `TarifaController`, con sus Request/Response (tabla de B1a abajo, ya implementada). Piezas comunes: `AlojamientoActual`, `PaginaResponse`, y en `ManejadorErrores` el 409 `CONCURRENCIA` y el 400 de parámetros de URL. 29 pruebas netas.
  - [x] **B2b · Operación** (DEC-65): `DisponibilidadController` (búsqueda y cotización), `ReservaController` (crear, consultar, modificar, hora de llegada, confirmar, cancelar, no-show), `EstanciaController` (llegada, corrección del registro, salida) y `FolioController` (consulta, pagos, devoluciones, reversos, servicios, cierre), con sus Request/Response (tablas de B1b, A5 y A6 abajo, ya implementadas). `UsuarioActual` entrega un autor provisional hasta B3. 29 pruebas.
  - [x] **B2c · Canales, conflictos, bitácora, titulares y novedades** (DEC-66): `CanalController`, `ContratoCanalController` (el endpoint que usan los canales, aún sin credencial), `ConflictoController`, `TitularController` y `NovedadController`, con sus Request/Response (tabla de A7 abajo, ya implementada), y `ReceptorReservaExterna` en `infrastructure/externos`, que deja en la bitácora los mensajes rechazados. 18 pruebas.
  - Endpoints y DTO REST de B1a, hechos en B2a (el alojamiento es el del despliegue, ALO-01: su id sale de `sga.alojamiento.id`, no de la URL ni del cuerpo):

    | Endpoint | Request | Response |
    |---|---|---|
    | `GET /api/alojamiento` | — | `AlojamientoResponse` |
    | `PUT /api/alojamiento/parametros` · `PUT /api/alojamiento/ubicacion` | `ParametrosRequest` · `UbicacionRequest` | `AlojamientoResponse` |
    | `PUT /api/alojamiento/medios-pago/{medio}/habilitar` · `…/deshabilitar` | — | `AlojamientoResponse` |
    | `POST /api/alojamiento/servicios` · `PUT …/servicios/{id}/valor` · `PUT …/servicios/{id}/desactivar` | `ServicioAdicionalRequest` · `ValorServicioRequest` | `AlojamientoResponse` (201 al agregar) |
    | `GET /api/apartamentos?pagina=` | — | `Pagina<ApartamentoResponse>` |
    | `PUT /api/apartamentos/{codigo}/activar` · `…/retirar` | — | `ApartamentoResponse` |
    | `PUT /api/apartamentos/{codigo}/capacidad` | `CambiarCapacidadRequest` | `CambioCapacidadResponse` |
    | `PUT /api/apartamentos/{codigo}/estado-operativo` | `CambiarEstadoOperativoRequest` | `ApartamentoResponse` |
    | `POST /api/apartamentos/{codigo}/bloqueos` · `PUT …/bloqueos/{id}/levantar` | `RegistrarBloqueoRequest` | `ApartamentoResponse` |
    | `GET /api/temporadas` · `POST /api/temporadas` | `AgregarTemporadaRequest` | `CalendarioResponse` · `TemporadaCreadaResponse` (201) |
    | `PUT /api/temporadas/{id}/fechas` · `PUT /api/temporadas/{id}/desactivar` | `CambiarFechasTemporadaRequest` | `CalendarioResponse` |
    | `GET /api/apartamentos/{codigo}/tarifas` · `PUT /api/apartamentos/{codigo}/tarifas/{temporada}` | `DefinirTarifaRequest` | lista de `TarifaResponse` · `TarifaResponse` |
    | `POST /api/politicas` · `GET /api/politicas/vigente` | `PublicarPoliticaRequest` | `PoliticaResponse` (201 + Location a la vigente) |
  - Endpoints y DTO REST de B1b, hechos en B2b (un conflicto de bloqueo optimista, DEC-59, responde 409 `CONCURRENCIA`, DEC-64):

    | Endpoint | Request | Response |
    |---|---|---|
    | `GET /api/apartamentos/disponibles?entrada&salida&ocupantes` | — | lista de `ApartamentoDisponibleResponse` |
    | `POST /api/cotizaciones` | `CotizarEstanciaRequest` | `CotizacionResponse` (200) |
    | `POST /api/reservas` · `GET /api/reservas/{codigo}` | `CrearReservaRequest` | `ReservaResponse` (201 + Location) |
    | `PUT /api/reservas/{codigo}/modificar` | `ModificarReservaRequest` | `ReservaResponse` |
    | `PUT /api/reservas/{codigo}/hora-llegada` · `PUT …/confirmar` | `HoraLlegadaRequest` | `ReservaResponse` |
  - Endpoints y DTO REST de A5 (Política y Folio). El `autor` de los Commands nunca viene en el Request: lo pone el controlador con `UsuarioActual`, que hasta B3 devuelve `USR-PROVISIONAL` (DEC-65). Folio hecho en B2b; cada `POST` responde 201 con el folio completo:

    | Endpoint | Request | Response |
    |---|---|---|
    | `POST /api/politicas` (hecho en B2a) | `PublicarPoliticaRequest` | `PoliticaResponse` (201 + Location) |
    | `GET /api/politicas/vigente` (hecho en B2a) | — | `PoliticaResponse` |
    | `GET /api/reservas/{codigo}/folio` | — | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/pagos` | `RegistrarPagoRequest` | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/devoluciones` | `RegistrarPagoRequest` | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/pagos/{id}/revertir` | — | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/cargos/{id}/revertir` | `RevertirCargoRequest` (motivo) | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/servicios` | `CargarServicioRequest` (servicioId) | `FolioResponse` |
    | `PUT /api/reservas/{codigo}/folio/cerrar` | `CerrarFolioRequest` (motivo de autorización opcional; solo admin, B3) | `FolioResponse` |
    | `PUT /api/reservas/{codigo}/cancelar` · `PUT …/no-show` | — | `CancelacionResponse` |
  - Endpoints y DTO REST de A6 (Llegada y salida), hechos en B2b:

    | Endpoint | Request | Response |
    |---|---|---|
    | `PUT /api/reservas/{codigo}/registrar-llegada` | — | `EstanciaResponse` |
    | `PUT /api/reservas/{codigo}/corregir-registro` | `CorregirRegistroRequest` (fechaHora, motivo) | `EstanciaResponse` |
    | `PUT /api/reservas/{codigo}/registrar-salida` | `RegistrarSalidaRequest` (motivo de autorización opcional; solo admin, B3) | `EstanciaResponse` |
  - Endpoints y DTO REST de A7 (Canales, Titular, Novedad), hechos en B2c:

    | Endpoint | Request | Response |
    |---|---|---|
    | `GET /api/canales` · `POST /api/canales` · `PUT /api/canales/{id}/desactivar` | `RegistrarCanalRequest` | lista de `CanalResponse` · `CanalResponse` |
    | `POST /api/canales-externos/{id}/reservas` (contrato del canal; la ruta y la clave del canal son de B4a, DEC-73) | `ReservaExternaRequest` | `ReservaExternaResponse` (201, o 200 si es duplicado, o 409 con el conflicto; un rechazo por otra regla es el 409 `REGLA_NEGOCIO` de siempre, y el adaptador lo anota con `RegistrarEventoCanalUseCase`, DEC-62) |
    | `GET /api/conflictos/pendientes` · `PUT /api/conflictos/{id}/resolver` | `ResolverConflictoRequest` (decisión) | lista de `ConflictoResponse` · `ConflictoResponse` |
    | `GET /api/canales/{id}/eventos?desde&hasta&pagina` (días) | — | `Pagina<EventoCanalResponse>` |
    | `POST /api/titulares` (409 `DUPLICADO` si el documento ya existe) · `GET /api/titulares/{id}` · `PUT /api/titulares/{id}` | `TitularRequest` · `ActualizarTitularRequest` | `TitularResponse` |
    | `POST /api/apartamentos/{codigo}/novedades` · `PUT /api/novedades/{id}/avanzar` | `RegistrarNovedadRequest` | `NovedadResponse` |
    | `GET /api/apartamentos/{codigo}/novedades?pagina=` | — | `Pagina<NovedadResponse>` |
- [x] **B3 · Seguridad JWT** (`Usuario`, `Rol`). Dividido en cinco incrementos, un commit cada uno (DEC-68 · DEC-69 · DEC-71):
  - [x] **B3a · Acceso y permisos** (DEC-67): Spring Security con su soporte JWT; `Usuario` y `Rol` con la tabla `usuario` (V11, `seq_usuario`); `POST /api/auth/login`; primer administrador al arrancar desde variables de entorno; `PermisosPorRol` con la tabla de permisos de todos los endpoints; 401 `NO_AUTENTICADO` y 403 `SIN_PERMISO`; `UsuarioActual` entrega el usuario del token; AUTC-01 y EOPE-04 dentro del endpoint. 25 pruebas (CU-02, USU-01/02/05, ROL-02/04, CORR-01, AUTC-01, EOPE-04, BLO-04, REG-06, NOV-05, SAL-02, RN-17, CAN-03, DEC-22/61/66/67).
    - DTO de B3a: ningún Request ni Response existente cambió de forma.

      | Endpoint | Request | Response |
      |---|---|---|
      | `POST /api/auth/login` (público) | `IniciarSesionRequest` (correo, contraseña) | `SesionResponse` (token, tipo, vencimiento, usuario, nombre, rol) |
  - [x] **B3b · Cuenta del titular y acceso del huésped** (DEC-69): una reserva solo completa el contacto que le falta al titular (cambia DEC-58) y avisa si llegó otro correo; correo y teléfono obligatorios para el portal y recepción, opcionales para los canales externos; los dos correos de cada reserva (`AvisosReserva`) por el puerto `Notificador` con su implementación local; inicio de sesión del huésped con correo, documento y PIN (tabla `codigo_acceso`, V12); cambio de correo con PIN por SMS y al correo actual, sin sesión; el huésped ve sus reservas, su folio y su cuenta y cancela solo lo suyo. 19 pruebas (DEC-18/68/69, USU-04/08, ROL-04, TIT-05).
    - DTO de B3b: `ReservaResponse` suma `avisoCuenta`; el contrato del canal usa `TitularExternoRequest` en lugar de `TitularReservaRequest`, que ahora exige correo y teléfono.

      | Endpoint | Request | Response |
      |---|---|---|
      | `POST /api/auth/huesped/pin` (público) | `SolicitarPinRequest` (correo, número de documento) | `MensajeResponse` (202, igual exista o no la cuenta) |
      | `POST /api/auth/huesped/login` (público) | `IniciarSesionHuespedRequest` (correo, número de documento, PIN) | `SesionResponse` |
      | `POST /api/auth/huesped/cambio-correo` · `PUT /api/auth/huesped/cambio-correo` (públicos) | `SolicitarCambioCorreoRequest` (documento, correo nuevo) · `ConfirmarCambioCorreoRequest` (documento, PIN) | `MensajeResponse` |
      | `GET /api/mi-cuenta` · `PUT /api/mi-cuenta` (huésped) | `ActualizarCuentaRequest` (nombre, teléfono) | `CuentaResponse` |
      | `GET /api/mis-reservas?pagina=` (huésped) | — | `Pagina<ReservaResponse>` |
      | `GET /api/reservas/{codigo}`, su folio y `PUT …/cancelar` (el huésped, solo las propias) | — | sin cambios |
  - [x] **B3c · Reserva desde el portal y pago en línea** (DEC-70): `ReservarDesdePortalUseCase` (público, canal PORTAL, hora de llegada obligatoria), `PagarEnLineaUseCase` con el puerto `PasarelaPago` y su `PasarelaSimulada`, `PagoEnLineaDomainService` (valida antes de cobrar; registra con el medio `PAGO_EN_LINEA` y confirma), parámetros `pagoEnLinea` (ANTICIPO o TOTAL) y `plazoPagoEnLinea` (30 min) + persistencia V13, vencimiento según el canal (`plazoParaConfirmar`, `Reserva.vencio`) y `PlanificadorVencimientos` cada minuto. 13 pruebas (CU-10, CU-20, RN-09/21, RES-15, MPAG-02, DEC-57/68/69).
    - DTO de B3c: `ParametrosRequest` y `AlojamientoResponse` suman `pagoEnLinea` y `plazoPagoEnLineaMinutos` (un `PUT /api/alojamiento/parametros` sin ellos ahora es 400).

      | Endpoint | Request | Response |
      |---|---|---|
      | `POST /api/portal/reservas` (público) | `ReservaPortalRequest` (apartamento, fechas, titular con correo y teléfono, ocupantes, hora de llegada) | `ReservaPortalResponse` (201: la reserva, `montoAPagar`, `pagarAntesDe`) |
      | `POST /api/portal/reservas/{codigo}/pago` (público) | `PagoEnLineaRequest` (`tokenPago`) | `ReservaResponse` (CONFIRMADA) · 409 `PAGO_RECHAZADO` |
  - [x] **B3d · Personal interno** (DEC-71): gestión de usuarios por el administrador (CU-06 · USU-03 · USU-05 · ROL-03), alta sin contraseña con código de activación de 24 h, perfil y cambio de contraseña (CU-03 · CU-04 · USU-02), recuperación con código de 15 min (CU-05 · USU-06 · USU-08), cierre de sesión (CU-48) y cortes de sesión al eliminar, cambiar el rol o la contraseña (`ValidadorSesion`); el último administrador no se puede quitar. Persistencia V14 (`codigo_usuario`, `token_revocado`, versión de sesiones). 9 pruebas (CU-03/04/05/06/48, USU-01/02/03/05/06/08, ROL-03, DEC-71).
    - DTO de B3d: ningún Request ni Response existente cambió de forma.

      | Endpoint | Request | Response |
      |---|---|---|
      | `POST /api/usuarios` · `GET /api/usuarios?pagina=` · `PUT /api/usuarios/{id}` · `PUT /api/usuarios/{id}/eliminar` (administrador) | `CrearUsuarioRequest` (datos de USU-01 y rol interno, sin contraseña) · `ModificarUsuarioRequest` (datos y rol) | `UsuarioResponse` · `Pagina<UsuarioResponse>` |
      | `GET`/`PUT /api/usuarios/perfil` · `PUT /api/usuarios/perfil/contrasena` (personal) | `DatosPersonalesRequest` · `CambiarContrasenaRequest` (actual, nueva) | `UsuarioResponse` · `MensajeResponse` |
      | `POST /api/auth/activar` · `POST /api/auth/restablecer-contrasena` · `POST /api/auth/recuperar-contrasena` (públicos) | `FijarContrasenaRequest` (correo, código, contraseña) · `RecuperarContrasenaRequest` (correo) | `MensajeResponse` (la recuperación, 202 igual exista o no el correo) |
      | `POST /api/auth/logout` (con sesión, también el huésped) | — | `MensajeResponse` |
  - [x] **B3e · Listado de reservas para recepción** (DEC-72): `ListarReservasUseCase` y `GET /api/reservas` con filtros opcionales (fechas que se cruzan con la estancia, estado, apartamento, canal, titular), de 10 en 10 por fecha de entrada; `FiltroReservas` (VO) y `ReservaRepository.buscar` con `Specification`. Sin migración. 6 pruebas (CU-25, EST-03, ROL-04, USU-04, DEC-18/72). Con esto termina B3.
    - DTO de B3e: ningún Request ni Response cambió; el listado devuelve `ReservaResponse`.

      | Endpoint | Request | Response |
      |---|---|---|
      | `GET /api/reservas?desde&hasta&estado&apartamento&canal&titular&pagina` (recepción y administrador) | — (parámetros) | `Pagina<ReservaResponse>` |
- **B4 · Externos** tras puertos con implementación local. Dividido en cuatro incrementos, un commit cada uno (DEC-73 · DEC-74). El planificador de vencimientos se adelantó a B3c (DEC-70).
  - [x] **B4a · Contrato del canal externo** (DEC-73): clave del canal en `X-Canal-Clave` (`AutenticadorCanal`, CAN-03), rutas en `/api/canales-externos/{canalId}`, `ConsultarDisponibilidadCanalUseCase` (CU-13) y `CancelarReservaExternaUseCase` (CU-16) con su bitácora, y `ReceptorCanalExterno` (antes `ReceptorReservaExterna`) para las tres operaciones. Sin migración. 6 pruebas (CU-13/16, CAN-03/05, RN-12, BIT-01, DEC-73).
    - DTO de B4a: ningún Request ni Response nuevo; la disponibilidad devuelve `ApartamentoDisponibleResponse` y la cancelación `CancelacionResponse`.

      | Endpoint | Request | Response |
      |---|---|---|
      | `GET /api/canales-externos/{canalId}/disponibilidad?entrada&salida&ocupantes` | — (parámetros) | lista de `ApartamentoDisponibleResponse` |
      | `POST /api/canales-externos/{canalId}/reservas` | `ReservaExternaRequest` | `ReservaExternaResponse` (201 · 200 · 409) |
      | `PUT /api/canales-externos/{canalId}/reservas/{idExterno}/cancelar` | — | `CancelacionResponse` |
  - [x] **B4b · Correo SMTP** (DEC-74 · DEC-76): `NotificadorSmtp` tras `Notificador` (Gmail para pruebas), activado con `SGA_CORREO_MODO=smtp`, no bloqueante y solo tras confirmar la transacción; `NotificadorLocal` sigue como alternativa. SMS sin proveedor. El pago en línea sigue con `PasarelaSimulada`: la pasarela real es del MVP2. Además, los casos de uso pasan a `XUseCase` (DEC-75). Sin migración. 5 pruebas (DEC-68, DEC-76).
    - DTO de B4b: ninguno; no cambia ningún endpoint.
  - [ ] **B4c · Channex**: adaptador de entrada para las reservas de las plataformas (aviso de Channex más la consulta de revisiones pendientes y su confirmación), sobre `RecibirReservaExternaUseCase` y `CancelarReservaExternaUseCase`; publicar disponibilidad y tarifas hacia Channex (`PUBLICAR_DISPONIBILIDAD`, saliente, con reintentos y bitácora). Por decidir: cómo se mapean apartamentos a tipos de habitación y la tarifa por ocupante facturable a sus planes de tarifas, y qué pasa con una modificación que llega desde la plataforma.
  - [ ] **B4d · IA no bloqueante**: un puerto de IA con implementación local; decidir para qué se usa (p. ej. tarifa sugerida, resumen diario o clasificación de novedades) y con qué proveedor. La operación nunca depende de ella.
- [ ] **B5 · TRA** (CU-VA-01 · DEC-68), de último porque es lo más complejo y un adicional del producto: nombres y apellidos separados en titular y ocupantes; ciudad de residencia, ciudad de procedencia y motivo del viaje de cada ocupante, que recepción toma en la llegada; recepción valida la identidad del titular (tipo de documento, nombres y apellidos) y el titular queda **validado** para las próximas estadías, con esos datos editables solo por recepción o administración; envío del reporte tras un puerto, sin bloquear la llegada, y **nunca de un titular sin validar** (se advierte que primero hay que validarlo).

## MVP2
- **Pasarela de pago real** (Wompi: tarjeta, PSE y Nequi; DEC-74): adaptador tras `PasarelaPago`, cobro inmediato, pago pendiente con su aviso de eventos firmado y consulta de respaldo, la reserva no vence con un pago en curso, y pago tardío (reactivar si hay cupo o reembolso por recepción). Hace falta la cuenta sandbox de Wompi. Mientras tanto queda `PasarelaSimulada`.
- **Facturación electrónica DIAN** (candidato: Factus, tiene sandbox; DEC-74): factura al cerrar el folio y notas crédito para corregir. Antes hace falta en el modelo: impuestos de los cargos, datos fiscales del titular (tipo de documento, dígito de verificación) y la resolución de numeración de cada alojamiento.

## Pendientes por definir (no tienen incremento todavía)
- **Proveedor de SMS** (DEC-74): sin decidir; sigue la implementación local.
- **TRA**: ya tiene incremento (B5) y campos acordados (DEC-68); falta confirmar con la lista oficial del reporte si pide algo más (p. ej. nacionalidad o fecha de nacimiento obligatorias).
- **Avisar al docente**: USU-02 deja de aplicar al huésped y se retira CU-01 (DEC-68).
- **Por validar con el docente**: REG-06 (corrección del registro, aplicada como DEC-45), CU-56 (anular también una *salida*: hoy no existe), TIT-06 (cambio de titular: no implementado), CAN-05 (canal desactivado), TEM-06.
- **Gravedad de las novedades** (`TODO(equipo)`, DEC-09) y **Temporada Media** (`TODO(equipo)`).
- **Datos del portal**: `GET /api/apartamentos` y `GET /api/apartamentos/{codigo}` son públicos y hoy muestran también el estado operativo y los bloqueos; definir qué ve el público (DEC-67).
- **Valores iniciales de `sga.parametros`** (DEC-53): umbral 12 años, entrada 15:00, salida 11:00, preparación 3 h, confirmación 24 h, no-show 22:00, anticipo 30 %, medios EFECTIVO y TRANSFERENCIA, servicio Parqueadero sin cargo. Son supuestos para arrancar; el equipo puede cambiarlos en `application.properties` o el administrador en la aplicación.

## Verificación
- Cada incremento: `./gradlew build`.
- Casos de uso: prueba con repositorio falso en memoria (sin Spring). REST: MockMvc contra H2 (códigos 2xx/400/401/403/404/409 y forma del JSON), como administrador salvo que la prueba diga otro rol (`SesionDePrueba`, DEC-67).
- Persistencia: prueba de ida y vuelta en H2 por agregado; contra PostgreSQL real (instalado o Docker), Flyway aplica `V1..Vn` y `ddl-auto=validate` confirma el esquema.
