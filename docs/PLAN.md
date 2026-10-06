# Plan incremental del SGA

Estado: `[x]` hecho · `[ ]` pendiente · `[~]` en curso. Actualizar al cerrar cada incremento. Decisiones en `docs/DECISIONES.md`.

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
- [x] **A1-E · Primer corte de extremo a extremo** (DEC-31..35): `Apartamento.crear`; casos de uso `CrearApartamento` y `ConsultarApartamento` con `CrearApartamentoCommand`/`ApartamentoResult`; `POST /api/apartamentos` (201 + Location) y `GET /api/apartamentos/{codigo}` con `CrearApartamentoRequest`/`ApartamentoResponse` y Bean Validation; `ErrorResponse` + `ManejadorErrores` (400/404/409). 16 pruebas: dominio, casos de uso con repositorios en memoria y MockMvc contra H2. Sin seguridad todavía (B3). El bean `Clock` pasa al primer caso de uso que lo necesite (`CrearReserva`, B1).
- [x] **A4 · Reserva** (DEC-36..40): `EstadoReserva.puedePasarA/retieneDisponibilidad`, `Ocupante.esDuplicadoDe` (OCU-05), `Registro`, `Salida`, `Reserva.crear/modificar/indicarHoraEstimadaLlegada/confirmar/cancelar/declararNoShow/expirar/registrarLlegada/registrarSalida/retieneNochesDe` con `creadaEn` (RN-21), RP-01 vía `CalendarioTemporadas.estanciaMinimaPara`, `Dinero.porcentaje` (RES-15); `DisponibilidadDomainService` (+ `Disponibilidad.exigir`), `BloqueoOperativoDomainService` (BLO-01), `BajaApartamentoDomainService` (APA-16) + persistencia V4 (`@Version`, `UNIQUE(canal_id, id_externo)`, registro/salida como columnas) y `db/postgresql/V4_1` con el `EXCLUDE`. 63 pruebas (RN-01/02/04/07/08/09/10/12/14/19/20/21/22, RES-15/16, OCU-02/05/12, RP-01, CORI-03, SAL-01/04/05, BLO-01, APA-16, EDO-02/03). Sin casos de uso ni REST: `CrearReserva` necesita la política vigente (A5). Pendiente: probar el `EXCLUDE` contra PostgreSQL real.
- [x] **A5 · Política y Folio** (DEC-41..44): `PoliticaCancelacion.crear/nuevaVersion/penalizacionPara` con tramos por horas y `Penalizacion` (porcentaje o monto fijo sobre `BaseRetencion`) configurables por el alojamiento, vigente = versión más alta; `ParametrosAlojamiento.minimoTramosCancelacion` (POL-01); `Folio.abrir/agregarServicioAdicional/registrarPago/registrarDevolucion/revertirPago/revertirCargo/ajustarPorModificacion/liquidarPenalidad/saldo/totalPagado/cerrar`, `Cargo` con `SentidoAjuste` y `Saldo` (DEC-20 aplicado); `CancelacionDomainService` y `NoShowDomainService` (Reserva + Folio) + persistencia V5 (incluye `minimo_tramos_cancelacion`). 40 pruebas (RN-13/14/15/16/17, POL-01/02/03/04/06, FOL-01/02/05/06, CAR-02/03/04/06/07, PAG-03/05/06/07, TPAG-02, MPAG-02, SLD-03, RES-16). Los valores de los tramos ya no son `TODO(equipo)`: los configura cada alojamiento (los iniciales llegan en B1).
- [ ] **A6 · Llegada y salida (D-02)**: `RegistroLlegadaDomainService`, `SalidaOperativaDomainService`. Anulación del registro (REG-06, DEC-40).
- [ ] **A7 · Canales, Titular, Novedad**: `Canal.desactivar`, `ConflictoCanal.resolver`, `EventoCanal`, `SincronizacionCanalDomainService`, `Titular.actualizarDatos`, `Novedad`. + persistencia V6. Titular entre los ocupantes (OCU-02 · TIT-01, DEC-38).

## Etapa B — Aplicación, REST e infraestructura (con `domain` completo, replicando el patrón de A1-E)
- [ ] **B1 · Casos de uso** (`@Service` + `@Transactional`) con su `XCommand`/`XResult` (DEC-31), generador de códigos de negocio tras un puerto. Crear el alojamiento con valores iniciales de `sga.*` (DEC-25) respetando ALO-01 (uno solo). CAP-05 (≥2 apartamentos con capacidades distintas) y APA-15 (advertir reservas afectadas al cambiar capacidad) van aquí. TAR-03 al crear temporada: se crea junto con las tarifas de todos los apartamentos activos o no se crea. Prueba de conflicto de bloqueo optimista entre dos transacciones. Política inicial del alojamiento desde `sga.*` (DEC-25, DEC-41) y caso de uso para que el admin publique versiones nuevas (CU-35). `CrearReserva` (con su folio, DEC-44)/`ModificarReserva` (+ `Folio.ajustarPorModificacion`)/`ConfirmarReserva` (`folio.totalPagado()`)/`CancelarReserva`/`DeclararNoShow` según el flujo de MODELO §3 (DEC-36, DEC-37) con el bean `Clock` en zona de Colombia (EST-06), y `ReservaRepository.buscarPendientesCreadasAntesDe` para el planificador.
  - DTO de aplicación de A5 (Política y Folio), en `application/politica` y `application/folio`:

    | Caso de uso | Command | Result |
    |---|---|---|
    | `PublicarPolitica` (CU-35, admin) | `PublicarPoliticaCommand` (tramos: horas, base, % o monto fijo; penalización de no-show) | `PoliticaResult` |
    | `ConsultarPoliticaVigente` (la que acepta el huésped al reservar) | — | `PoliticaResult` |
    | `ConsultarFolio` | — | `FolioResult` (cargos, pagos, saldo y situación) |
    | `RegistrarPago` / `RegistrarDevolucion` | `RegistrarPagoCommand` (medio, monto, fecha) | `FolioResult` |
    | `RevertirPago` / `RevertirCargo` | `RevertirPagoCommand` / `RevertirCargoCommand` (id, motivo) | `FolioResult` |
    | `CerrarFolio` | `CerrarFolioCommand` (autorización opcional: autor, motivo) | `FolioResult` |
    | `CancelarReserva` / `DeclararNoShow` | `CancelarReservaCommand` | `CancelacionResult` (estado, retenido, saldo) |
- [ ] **B2 · REST** por acciones de negocio con `XRequest`/`XResponse` por endpoint (DEC-31, DEC-32), errores por el manejador global de A1-E (DEC-33), `Pagina<T>` de 10. Pruebas MockMvc por endpoint.
  - Endpoints y DTO REST de A5 (Política y Folio):

    | Endpoint | Request | Response |
    |---|---|---|
    | `POST /api/politicas` | `PublicarPoliticaRequest` | `PoliticaResponse` (201 + Location) |
    | `GET /api/politicas/vigente` | — | `PoliticaResponse` |
    | `GET /api/reservas/{codigo}/folio` | — | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/pagos` | `RegistrarPagoRequest` | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/devoluciones` | `RegistrarPagoRequest` | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/pagos/{id}/revertir` | — | `FolioResponse` |
    | `POST /api/reservas/{codigo}/folio/cargos/{id}/revertir` | `RevertirCargoRequest` (motivo) | `FolioResponse` |
    | `PUT /api/reservas/{codigo}/folio/cerrar` | `CerrarFolioRequest` (autorización opcional) | `FolioResponse` |
    | `PUT /api/reservas/{codigo}/cancelar` · `PUT …/no-show` | — | `CancelacionResponse` |
- [ ] **B3 · Seguridad JWT** (`Usuario`, `Rol`).
- [ ] **B4 · Externos** tras puertos con implementación local (canales, IA no bloqueante) y planificador de `VencerReservasPendientes`.

## Verificación
- Cada incremento: `./gradlew build`.
- Casos de uso: prueba con repositorio falso en memoria (sin Spring). REST: MockMvc contra H2 (códigos 2xx/400/404/409 y forma del JSON).
- Persistencia: prueba de ida y vuelta en H2 por agregado; contra PostgreSQL real (instalado o Docker), Flyway aplica `V1..Vn` y `ddl-auto=validate` confirma el esquema.
