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
- [x] **A1-E · Primer corte de extremo a extremo** (DEC-31..35): `Apartamento.crear`; casos de uso `CrearApartamento` y `ConsultarApartamento` con `CrearApartamentoCommand`/`ApartamentoResult`; `POST /api/apartamentos` (201 + Location) y `GET /api/apartamentos/{codigo}` con `CrearApartamentoRequest`/`ApartamentoResponse` y Bean Validation; `ErrorResponse` + `ManejadorErrores` (400/404/409). 16 pruebas: dominio, casos de uso con repositorios en memoria y MockMvc contra H2. Sin seguridad todavía (B3). El bean `Clock` pasa a A4, el primer caso de uso que lo necesita.
- [ ] **A4 · Reserva**: `EstadoReserva`, `Ocupante` (falta OCU-05 y OCU-12 al agregarlo), `Registro`, `Salida`, `creadaEn` para `expirar` (RN-21), estancia mínima de la temporada (RP-01, TEM-07), `Reserva.crear/modificar/confirmar/cancelar/declararNoShow/expirar/registrarLlegada/registrarSalida`; `DisponibilidadDomainService`, `BloqueoOperativoDomainService`, `BajaApartamentoDomainService`. + persistencia V4 con `@Version`, `UNIQUE(canal, idExterno)` y `EXCLUDE` (DEC-17).
- [ ] **A5 · Política y Folio**: `retencionPara/nuevaVersion`; `Folio.agregarCargo/registrarPago/ajustarMovimiento/saldo/cerrar`; `CancelacionDomainService`, `NoShowDomainService`. + persistencia V5. `TODO(equipo)` tramos. Aplicar DEC-20: sentido del AJUSTE y VO `Saldo`.
- [ ] **A6 · Llegada y salida (D-02)**: `RegistroLlegadaDomainService`, `SalidaOperativaDomainService`.
- [ ] **A7 · Canales, Titular, Novedad**: `Canal.desactivar`, `ConflictoCanal.resolver`, `EventoCanal`, `SincronizacionCanalDomainService`, `Titular.actualizarDatos`, `Novedad`. + persistencia V6.

## Etapa B — Aplicación, REST e infraestructura (con `domain` completo, replicando el patrón de A1-E)
- [ ] **B1 · Casos de uso** (`@Service` + `@Transactional`) con su `XCommand`/`XResult` (DEC-31), generador de códigos de negocio tras un puerto. Crear el alojamiento con valores iniciales de `sga.*` (DEC-25) respetando ALO-01 (uno solo). CAP-05 (≥2 apartamentos con capacidades distintas) y APA-15 (advertir reservas afectadas al cambiar capacidad) van aquí. TAR-03 al crear temporada: se crea junto con las tarifas de todos los apartamentos activos o no se crea. Prueba de conflicto de bloqueo optimista entre dos transacciones.
- [ ] **B2 · REST** por acciones de negocio con `XRequest`/`XResponse` por endpoint (DEC-31, DEC-32), errores por el manejador global de A1-E (DEC-33), `Pagina<T>` de 10. Pruebas MockMvc por endpoint.
- [ ] **B3 · Seguridad JWT** (`Usuario`, `Rol`).
- [ ] **B4 · Externos** tras puertos con implementación local (canales, IA no bloqueante) y planificador de `VencerReservasPendientes`.

## Verificación
- Cada incremento: `./gradlew build`.
- Casos de uso: prueba con repositorio falso en memoria (sin Spring). REST: MockMvc contra H2 (códigos 2xx/400/404/409 y forma del JSON).
- Persistencia: prueba de ida y vuelta en H2 por agregado; contra PostgreSQL real (instalado o Docker), Flyway aplica `V1..Vn` y `ddl-auto=validate` confirma el esquema.
