# SGA — Sistema de Gestión de Alojamiento

Proyecto de Programación Avanzada (Universidad del Quindío). Backend de un único alojamiento ("Puerta al Sol", 10 apartamentos) aunque en este caso solo se usara un alojamiento la idea es generar un producto comerciable a diferentes alojamientos por medio de SaaS que gestiona apartamentos, tarifas por temporada, disponibilidad, reservas, check-in/out, folio y 3 canales de venta (PORTAL, DIRECTO, EXTERNO). **Riesgo central: vender dos veces la misma noche.**
Modelo completo (clases, agregados, servicios, reglas): `docs/MODELO.md` — léelo solo cuando necesites atributos/firmas/reglas. Fuente de las reglas detalladas: `docs/Entidad__Objeto_de_valor_corregido.xlsx` (buscar por ID: `RES-01`, `FOL-03`, `RN-17`).
**Al iniciar trabajo nuevo, leer `docs/SPRINTS.md`** (en qué sprint vamos y qué le toca al equipo), `docs/PLAN.md` (historia del backend por incrementos) y `docs/DECISIONES.md` (decisiones acordadas `DEC-nn`: estructura, persistencia, Javadoc). Desde el sprint 1, el avance se anota en `SPRINTS.md`.

## Trabajo en equipo (DEC-77) — leer antes de editar
Dos equipos en paralelo, cada uno con su cuenta de GitHub y de Claude: **web** (web de reservas en `web/` y canales de venta) y **pms** (gestión del hotel en `pms/`), sobre este mismo backend. `docs/EQUIPOS.md` dice qué archivos son de cada equipo, cuáles son del núcleo compartido y cómo se nombran las ramas; `docs/SPRINTS.md` tiene el plan por semanas y `docs/BACKLOG.md` lo pendiente con el cliente.
- Al empezar, leer `CLAUDE.local.md` (local, no va a git) para saber qué equipo es este clon; si no existe, preguntar y no editar nada.
- Editar solo archivos del propio equipo. Lo del otro equipo no se toca: se explica qué cambio hace falta. El núcleo solo se cambia en una rama `feature/nucleo/…`.
- Nunca trabajar directo en `main` ni en `develop`: ramas `feature/<equipo>/<tema>`. Commits, PR y merges los hace la persona.
- Cada tarea es un corte vertical: la pantalla del frontend del equipo más lo que falte del backend, con sus pruebas (ver «Terminado» en `docs/SPRINTS.md`).
- Al terminar una tarea, marcarla solo en la sección del equipo en `docs/SPRINTS.md`. Números de migración y de `DEC-nn`: el siguiente libre al integrar.

## Stack
Java 25 · Spring Boot 4.1.x · **Gradle Groovy** (`build.gradle`, nunca `.kts`) · JPA/Hibernate · Spring Security con JWT (resource server, DEC-67) · PostgreSQL (principal) + Flyway · H2 en memoria solo para pruebas (DEC-11) · JUnit 5 · Lombok (solo fuera de `domain`).
Boot 4: `spring-boot-starter-webmvc` (no `-web`), `spring-boot-starter-flyway`, `spring-boot-starter-security-oauth2-resource-server`, un `*-test` por cada starter; `@DataJpaTest` está en `org.springframework.boot.data.jpa.test.autoconfigure`.
Paquete raíz `co.edu.uniquindio.sga_avanzada_2026_2` (clase principal `SgaAvanzada20262Application`). Nada de secretos en git.

## Arquitectura hexagonal
`infrastructure → application → domain` (las dependencias solo apuntan al dominio).
- **domain**: Java puro (sin Spring, JPA, Lombok, Jackson, Bean Validation, DTOs). Entidades sin setters, cambios por métodos del negocio, colecciones con `List.copyOf`. VO = `record` que valida en el constructor; valores cerrados = `enum` con su lógica. Excepciones de negocio extienden `ReglaDominioException`. Contiene también los **puertos** (`XRepository`, uno por agregado) y los servicios de dominio.
- **application**: un caso de uso por clase, en verbo (`CrearReservaUseCase`). Entrada `XCommand`, salida `XResult` (`record`, sin HTTP — DEC-31). Flujo: cargar por puertos → invocar dominio → guardar → efectos. **Cero `if` de negocio.** Config llega por constructor. Se permiten `@Service` y `@Transactional` (DEC-10).
- **infrastructure**: REST con `XRequest` (Bean Validation, 400) / `XResponse` y manejador global de errores 400/404/409 (DEC-31..33; convenciones en DEC-64); el controlador nunca recibe ni devuelve entidades del dominio. Adaptadores JPA con entidades JPA **separadas** del dominio y mapeadores, seguridad JWT, externos, config. Usuario/Rol viven aquí. Seguridad (DEC-67): todo endpoint nuevo se agrega a `PermisosPorRol` (si no, queda solo para el administrador); 401 `NO_AUTENTICADO`, 403 `SIN_PERMISO`; el autor sale de `UsuarioActual`, nunca del Request; las pruebas REST entran como administrador (`SesionDePrueba`). El huésped entra con PIN y su id es el de su titular: lo suyo se revisa con `UsuarioActual.exigirAccesoA` (DEC-69); correos y SMS salen por el puerto `Notificador`. Cada token se revisa en cada petición (`ValidadorSesion`: cierre de sesión, usuario eliminado, rol o contraseña cambiados; DEC-71). Los canales externos no usan JWT: entran por `/api/canales-externos/{canalId}` con su clave en `X-Canal-Clave` (`AutenticadorCanal`, DEC-73). Secretos por variables de entorno (`SGA_JWT_SECRETO`, `SGA_ADMIN_CORREO`, `SGA_ADMIN_CLAVE`, `SGA_CORREO_USUARIO`, `SGA_CORREO_CLAVE`). El correo real sale por `NotificadorSmtp` con `SGA_CORREO_MODO=smtp` (DEC-76); si no, `NotificadorLocal`.
- Agregado: una raíz, hijos sin repositorio, referencias a otros agregados **solo por id tipado**. Una transacción = un agregado (excepciones documentadas: D-02, llegada y salida tocan Reserva+Apartamento(+Folio); DEC-44, crear, cancelar, no-show y pago en línea (DEC-70) tocan Reserva+Folio; DEC-62, la reserva de un canal externo guarda además su conflicto o su evento de bitácora).
- Servicio de dominio: sin estado, solo reglas que cruzan agregados, **recibe los datos por parámetro** (no usa repositorios).
- Validación: formato en el borde (400); reglas de negocio siempre en el dominio (409).
- Reloj inyectado (`fechaHoy`/`ahora` por parámetro); nunca `now()` en el dominio.
- Todo valor que cambia entre alojamientos es **configuración** (`ParametrosAlojamiento`, guardado con el Alojamiento; `sga.*` solo valores iniciales — DEC-25), no constante.
- Eliminación lógica; listados paginados de 10; `Dinero` = `BigDecimal` COP sin decimales (nunca float/double), redondeo al final de cada cargo.

## Paquetes
`domain/{compartido, apartamento, reserva, folio, titular, tarifa, politica, canal, alojamiento, novedad, servicio}` · `application/{<agregado>, compartido}` · `infrastructure/{rest, persistencia, seguridad, externos, config}`. Pruebas en `src/test` espejo de `domain`.

## Convenciones
- Código del dominio **en español** (Apartamento, Reserva, Estancia, Folio, Cargo, Pago, Saldo, Titular, Ocupante, Noche, Canal, Novedad; check-in = *Registro*, check-out = *Salida*). Nada de room/booking/user. Estructura y palabras clave en inglés. No mezclar idiomas en un nombre, salvo el sufijo del rol técnico (`ReservaRepository`, `ReservaJpa`, `CrearReservaUseCase` — DEC-75, `CrearReservaRequest/Command`, `ReservaResult/Response` — DEC-32).
- Ids tipados con código de negocio (`ReservaId` = `RES-2026-00042`, `ApartamentoId` = `APT-101`).
- Comentar la regla que implementa cada método (`// RN-02 · CAP-02`); pruebas con `@Tag("RN-02")`, caso feliz **y** violación.
- API por acciones de negocio: `PUT /api/reservas/{codigo}/cancelar`. Prohibido `PATCH /estado` y `DELETE` físico.
- Commits pequeños, en español, descriptivos; nunca «cambios»/«avance».
- Frontends (`web/`, `pms/`): React + Vite + TypeScript; solo hablan con la API, nunca con la base.

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
- Tarifa de Temporada Media: por definir.
- La política de cancelación (tramos por horas, porcentaje o monto fijo, base de cálculo, no-show) la configura cada alojamiento (DEC-41); sus valores iniciales ya están en `sga.politica.*` (DEC-53). 
