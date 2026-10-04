# SGA — Sistema de Gestión de Alojamiento

Proyecto de Programación Avanzada (Universidad del Quindío). Backend de un único alojamiento ("Puerta al Sol", 10 apartamentos) aunque en este caso solo se usara un alojamiento la idea es generar un producto comerciable a diferentes alojamientos por medio de SaaS que gestiona apartamentos, tarifas por temporada, disponibilidad, reservas, check-in/out, folio y 3 canales de venta (PORTAL, DIRECTO, EXTERNO). **Riesgo central: vender dos veces la misma noche.**
Modelo completo (clases, agregados, servicios, reglas): `docs/MODELO.md` — léelo solo cuando necesites atributos/firmas/reglas. Fuente de las reglas detalladas: `docs/Entidad__Objeto_de_valor_corregido.xlsx` (buscar por ID: `RES-01`, `FOL-03`, `RN-17`).

## Stack
Java 25 · Spring Boot 4.1.x · **Gradle Groovy** (`build.gradle`, nunca `.kts`) · H2 en memoria (`jdbc:h2:mem:sgadb`) · JUnit 5 · Lombok (solo fuera de `domain`).
Boot 4: `spring-boot-starter-webmvc` (no `-web`), `spring-boot-h2console` explícito, un `*-test` por cada starter.
Paquete raíz `co.edu.uniquindio.sga`. Nada de secretos en git.

## Arquitectura hexagonal
`infrastructure → application → domain` (las dependencias solo apuntan al dominio).
- **domain**: Java puro (sin Spring, JPA, Lombok, Jackson, Bean Validation, DTOs). Entidades sin setters, cambios por métodos del negocio, colecciones con `List.copyOf`. VO = `record` que valida en el constructor; valores cerrados = `enum` con su lógica. Excepciones de negocio extienden `ReglaDominioException`. Contiene también los **puertos** (`XRepository`, uno por agregado) y los servicios de dominio.
- **application**: un caso de uso por clase, en verbo (`CrearReserva`). Flujo: cargar por puertos → invocar dominio → guardar → efectos. **Cero `if` de negocio.** Config llega por constructor.
- **infrastructure**: REST + DTO (`record`), adaptadores JPA con entidades JPA **separadas** del dominio y mapeadores, seguridad JWT, externos, config. Usuario/Rol viven aquí.
- Agregado: una raíz, hijos sin repositorio, referencias a otros agregados **solo por id tipado**. Una transacción = un agregado (excepción documentada D-02: llegada y salida tocan Reserva+Apartamento(+Folio)).
- Servicio de dominio: sin estado, solo reglas que cruzan agregados, **recibe los datos por parámetro** (no usa repositorios).
- Validación: formato en el borde (400); reglas de negocio siempre en el dominio (409).
- Reloj inyectado (`fechaHoy`/`ahora` por parámetro); nunca `now()` en el dominio.
- Todo valor que cambia entre alojamientos es **configuración** (`ParametrosAlojamiento`, props `sga.*`), no constante.
- Eliminación lógica; listados paginados de 10; `Dinero` = `BigDecimal` COP sin decimales (nunca float/double), redondeo al final de cada cargo.

## Paquetes
`domain/{compartido, apartamento, reserva, folio, titular, tarifa, politica, canal, alojamiento, novedad, servicio}` · `application/<agregado>` · `infrastructure/{rest, persistencia, seguridad, externos, config}`. Pruebas en `src/test` espejo de `domain`.

## Convenciones
- Código del dominio **en español** (Apartamento, Reserva, Estancia, Folio, Cargo, Pago, Saldo, Titular, Ocupante, Noche, Canal, Novedad; check-in = *Registro*, check-out = *Salida*). Nada de room/booking/user. Estructura y palabras clave en inglés. No mezclar idiomas en un nombre.
- Ids tipados con código de negocio (`ReservaId` = `RES-2026-00042`, `ApartamentoId` = `APT-101`).
- Comentar la regla que implementa cada método (`// RN-02 · CAP-02`); pruebas con `@Tag("RN-02")`, caso feliz **y** violación.
- API por acciones de negocio: `PUT /api/reservas/{codigo}/cancelar`. Prohibido `PATCH /estado` y `DELETE` físico.
- Commits pequeños, en español, descriptivos; nunca «cambios»/«avance».

## Reglas transversales
- **Estancia** = `[entrada, salida)`: la noche de salida no se ocupa ni se cobra. Solapan si `entradaA < salidaB && entradaB < salidaA`.
- **Ocupante facturable** si a la fecha de entrada alcanza el umbral de edad; todos cuentan para capacidad. La edad se calcula, no se guarda.
- **Reserva** (nace PENDIENTE): PENDIENTE→CONFIRMADA | CANCELADA · CONFIRMADA→EN_CURSO | CANCELADA | NO_SHOW · EN_CURSO→FINALIZADA. Terminales: FINALIZADA, CANCELADA, NO_SHOW. Activas: PENDIENTE, CONFIRMADA, EN_CURSO. Cualquier otra transición se rechaza.
- **Apartamento (estado operativo)**: PREPARADO→OCUPADO→PENDIENTE_PREPARACION→EN_PREPARACION→PREPARADO; FUERA_DE_SERVICIO desde cualquier estado no ocupado (solo admin). Bloqueo y estado operativo son independientes.
- Al crear se **congelan** el valor (desglose por noche) y la versión de la política de cancelación; solo una modificación explícita recalcula (diferencia = ajuste en el folio).
- Folio: saldo = cargos − pagos (calculado). Cargos y pagos no se editan ni borran: se corrigen con movimiento inverso. No cierra con saldo ≠ 0 sin autorización registrada.
- Canal externo: canal + idExterno único (idempotencia); colisión → se rechaza y se registra `ConflictoCanal`, nunca sobrescribe.
- Integraciones externas siempre tras un puerto, con implementación local alternativa y sin impedir reservar/cobrar. La IA es no bloqueante.

## Pendientes (no resolver por cuenta propia; dejar `// TODO(equipo)` o preguntame cuando sea necesario)
- Tarifa de Temporada Media y tramos restantes de la política de cancelación: por definir. 
