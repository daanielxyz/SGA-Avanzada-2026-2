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
- [ ] **A1 · Apartamento**: `Capacidad.admite`, `EstadoOperativo.puedePasarA/permiteRegistro`, `Bloqueo.cubre/levantar`, métodos de `Apartamento`, invariantes (1..10 imágenes con una principal, ≥1 característica).
- [ ] **A1-P · Primer corte de persistencia**: PostgreSQL + Flyway en Gradle, properties main/test, `V1__apartamento.sql`, adaptador JPA de Apartamento, prueba de ida y vuelta en H2.
- [ ] **A2 · Alojamiento**: parámetros, medios de pago (≥2), servicios adicionales (≥1), ubicación. + persistencia V2.
- [ ] **A3 · Temporada y Tarifa**: `cubre/seSolapaCon`, base obligatoria, `nuevaVersion`; `TarificacionDomainService` (RN-05, 06), `ActivadorApartamentoService` (TAR-03, APA-11). + persistencia V3. `TODO(equipo)` Temporada Media.
- [ ] **A4 · Reserva**: `EstadoReserva`, `Ocupante`, `Registro`, `Salida`, `Reserva.crear/modificar/confirmar/cancelar/declararNoShow/expirar/registrarLlegada/registrarSalida`; `DisponibilidadDomainService`, `BloqueoOperativoDomainService`, `BajaApartamentoDomainService`. + persistencia V4 con `@Version`, `UNIQUE(canal, idExterno)` y `EXCLUDE` (DEC-17).
- [ ] **A5 · Política y Folio**: `retencionPara/nuevaVersion`; `Folio.agregarCargo/registrarPago/ajustarMovimiento/saldo/cerrar`; `CancelacionDomainService`, `NoShowDomainService`. + persistencia V5. `TODO(equipo)` tramos. Aplicar DEC-20: sentido del AJUSTE y VO `Saldo`.
- [ ] **A6 · Llegada y salida (D-02)**: `RegistroLlegadaDomainService`, `SalidaOperativaDomainService`.
- [ ] **A7 · Canales, Titular, Novedad**: `Canal.desactivar`, `ConflictoCanal.resolver`, `EventoCanal`, `SincronizacionCanalDomainService`, `Titular.actualizarDatos`, `Novedad`. + persistencia V6.

## Etapa B — Aplicación, REST e infraestructura (con `domain` completo)
- [ ] **B1 · Casos de uso** (`@Service` + `@Transactional`), `Clock` y `ParametrosAlojamiento` (`sga.*`) inyectados, generador de códigos de negocio tras un puerto.
- [ ] **B2 · REST** por acciones de negocio, DTO `record`, 400 en el borde / 409 por `ReglaDominioException`, `Pagina<T>` de 10.
- [ ] **B3 · Seguridad JWT** (`Usuario`, `Rol`).
- [ ] **B4 · Externos** tras puertos con implementación local (canales, IA no bloqueante) y planificador de `VencerReservasPendientes`.

## Verificación
- Cada incremento: `./gradlew build`.
- Persistencia: prueba de ida y vuelta en H2 por agregado; contra PostgreSQL real (instalado o Docker), Flyway aplica `V1..Vn` y `ddl-auto=validate` confirma el esquema.
