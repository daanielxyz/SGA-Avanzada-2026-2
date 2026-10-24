# Equipos, ramas y propiedad del código

Cómo se reparte el trabajo para que dos equipos avancen en paralelo sin pisarse (DEC-77). Lo leen las personas **y Claude** antes de tocar cualquier archivo. El plan por semanas está en `docs/SPRINTS.md`.

## Productos
Un solo backend (este repositorio, `src/`) y dos frontends en React, cada uno en su carpeta:

| Producto | Carpeta | Para quién | Habla con |
|---|---|---|---|
| **Web de reservas** | `web/` | El huésped: busca, cotiza, reserva, paga y ve su cuenta | `/api/portal/**`, `/api/auth/huesped/**`, `/api/mi-cuenta`, `/api/mis-reservas`, `/api/apartamentos/disponibles`, `/api/cotizaciones` |
| **PMS** | `pms/` | Recepción y administración: toda la gestión del hotel | El resto de `/api/**` (con JWT del personal) |
| **Backend (API)** | `src/` | Ambos frontends y los canales externos | PostgreSQL, correo, Channex |

## Equipos
| Equipo | Producto | Integrantes | Prefijo de rama |
|---|---|---|---|
| **web** | Web de reservas + canales de venta (portal y Channex) | `TODO(equipo)` | `feature/web/…` |
| **pms** | PMS: operación y configuración del hotel, TRA | `TODO(equipo)` | `feature/pms/…` |

Cada equipo trabaja con su propia cuenta de GitHub y de Claude. Cada clon dice a Claude qué equipo es en un archivo **local, que no va a git**: `CLAUDE.local.md` en la raíz, con una sola línea:

```markdown
Soy el equipo **pms**. Solo edito lo que docs/EQUIPOS.md asigna a pms; lo del núcleo, solo en una rama feature/nucleo/….
```

## Qué toca cada uno
Regla general: **cada equipo edita solo lo suyo**. Lo del otro equipo se lee y se usa, nunca se modifica: si se necesita un cambio allí, se le pide al dueño (issue o mensaje) con el contrato que se necesita (endpoint, Request, Response). Las pruebas pertenecen al dueño del código que prueban.

### Equipo web
- `web/` completo.
- Canales externos: `domain/canal/`, `application/canal/`, `infrastructure/rest/canal/`, `infrastructure/externos/ReceptorCanalExterno.java` y los adaptadores de Channex (`infrastructure/externos/channex/`), `infrastructure/seguridad/AutenticadorCanal.java`.
- Portal y cuenta del huésped: `infrastructure/rest/portal/`, `infrastructure/rest/cuenta/`, `infrastructure/rest/acceso/AccesoHuespedController.java` y sus Request, `infrastructure/seguridad/{AccesoHuesped, CodigoAcceso, CodigoAccesoRepository}.java`.
- Casos de uso: `application/reserva/{ReservarDesdePortal, PagarEnLinea, ListarReservasDeTitular}UseCase.java`, `application/titular/{ActualizarCuentaTitular, CambiarCorreoTitular}UseCase.java`, `domain/servicio/PagoEnLineaDomainService.java`, `infrastructure/externos/PasarelaSimulada.java`.

### Equipo pms
- `pms/` completo.
- REST de la operación: `infrastructure/rest/{alojamiento, apartamento, folio, novedad, politica, reserva, tarifa, titular, usuario}/`, `infrastructure/rest/acceso/{SesionController, RecuperacionController}.java` y sus Request.
- Seguridad del personal: el resto de `infrastructure/seguridad/` (usuarios, sesiones, contraseñas, tokens).
- Casos de uso de `application/{alojamiento, apartamento, folio, novedad, politica, tarifa}/`, `application/titular/` (salvo los dos del equipo web) y de `application/reserva/` los de recepción: `ConfirmarReserva`, `ConsultarReserva`, `CorregirRegistro`, `DeclararNoShow`, `IndicarHoraLlegada`, `ListarReservas`, `ModificarReserva`, `RegistrarLlegada`, `RegistrarSalida`.
- Lo nuevo de TRA (B5) y de IA (B4d).

### Núcleo (compartido)
Lo que usan los dos y donde está el riesgo de vender dos veces la misma noche. **Solo se cambia en una rama `feature/nucleo/…`, en un PR pequeño que revisa y aprueba el otro equipo**, y nunca mezclado con trabajo propio:
- `domain/` (salvo lo asignado arriba), `application/compartido/`.
- `application/reserva/{CrearReserva, CancelarReserva, VencerReserva, BuscarDisponibles, BuscarReservasVencidas, CotizarEstancia}UseCase.java`, `CargadorReserva`, `AvisosReserva`, `TitularParaReserva`.
- `infrastructure/persistencia/` y `src/main/resources/db/` (migraciones), `infrastructure/config/`, `infrastructure/externos/{NotificadorLocal, NotificadorSmtp}.java`.
- `infrastructure/rest/{ManejadorErrores, ErrorResponse, PaginaResponse, UsuarioActual, AlojamientoActual}.java`, `infrastructure/seguridad/{PermisosPorRol, SeguridadConfig}.java`.
- `build.gradle`, `settings.gradle`, `src/*/resources/application*.properties`, `CLAUDE.md`, `docs/EQUIPOS.md`, `docs/MODELO.md`, `docs/modelo-relacional.drawio`.

Excepción práctica: agregar **una línea** propia en `PermisosPorRol` o en `ManejadorErrores` para un endpoint nuevo del equipo puede ir en la rama del equipo, avisándolo en el PR.

## Ramas
- `main`: lo que ve el cliente. Solo recibe `develop` al **cierre de cada sprint**, con la etiqueta `sprint-N`.
- `develop`: integración. Todo entra por PR, con `./gradlew build` verde (y `npm run build` y `npm test` si se tocó `web/` o `pms/`).
- `feature/<equipo>/<tema>`: una por tarea, corta (menos de una semana), en minúsculas y con guiones: `feature/web/busqueda-disponibilidad`, `feature/pms/registro-llegada`, `feature/nucleo/cors-frontends`.
- `fix/<equipo>/<tema>`: corrección de algo ya integrado.
- La rama personal `Arias` se integra a `develop` y deja de usarse.

Flujo de cada tarea: actualizar `develop` → crear la rama → trabajar → traer `develop` a la rama al menos una vez al día → PR a `develop` → el otro equipo revisa si toca núcleo → integrar y borrar la rama.

## Lo que más choca y cómo evitarlo
- **Migraciones de Flyway**: el número `V<n>` se elige al integrar, no al empezar. Antes del PR, traer `develop` y renombrar la migración al siguiente número libre. Nunca editar una migración ya integrada.
- **Decisiones (`DEC-nn`)**: igual, el siguiente número libre al integrar. Cada equipo escribe sus DEC en `docs/DECISIONES.md` en un PR propio o junto con su tarea.
- **`docs/PLAN.md`**: es la historia del backend hasta B4b. Desde el sprint 1, cada equipo marca su avance **solo en su sección** de `docs/SPRINTS.md`.
- **Contratos de la API**: si un equipo cambia un endpoint que usa el otro (ruta, Request o Response), es un cambio de núcleo: PR propio, aviso y revisión del otro equipo. Agregar campos es seguro; quitar o renombrar, no.

## Reglas para Claude (en ambos clones)
1. Al empezar, leer `CLAUDE.local.md` para saber qué equipo es. Si no existe, preguntar y no editar hasta saberlo.
2. Antes de editar un archivo, comprobar en este documento que es del equipo. Si es del otro equipo, no editarlo: explicar qué cambio hace falta y redactar el pedido. Si es del núcleo, editarlo solo si la rama actual es `feature/nucleo/…`.
3. Nunca trabajar directo en `main` ni en `develop`; si la rama actual es una de ellas, proponer la rama `feature/<equipo>/<tema>` antes de cambiar nada.
4. Los commits, los PR y los merges los hace la persona, no Claude.
5. Al cerrar una tarea, actualizar solo la sección del equipo en `docs/SPRINTS.md`.
