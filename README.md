# SGA — Sistema de Gestión de Alojamiento

Backend y frontends de **Puerta al Sol**: apartamentos, tarifas por temporada, disponibilidad, reservas, llegada y salida, folio y tres canales de venta. Proyecto de Programación Avanzada, Universidad del Quindío.

Java 25 · Spring Boot 4.1 · Gradle · JPA/Hibernate · PostgreSQL + Flyway (H2 en memoria para las pruebas, DEC-11) · arquitectura hexagonal.

## Correr el backend

Necesita Java 25 y PostgreSQL con una base `sga` y su usuario `sga`.

1. Copiar `.env.example` como `.env` (no va a git) y poner la clave de PostgreSQL en `DB_PASSWORD` y un `SGA_JWT_SECRETO` de mínimo 32 caracteres.
2. Arrancar: `./gradlew bootRun`. Queda en `http://localhost:8080` y Flyway crea el esquema.

Al arrancar sobre una base vacía quedan solos los datos de prueba de la sección 12.3 del proyecto: los 10 apartamentos con sus tarifas, un usuario por rol, reservas en todos los estados y por los tres canales, un bloqueo vigente y un apartamento fuera de servicio. Para volver a empezar de cero, se borra y se crea otra vez la base `sga`.

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin.demo@example.com` | `Demo12345` |
| Recepcionista | `recepcion.demo@example.com` | `Demo12345` |
| Personal de servicio | `servicio.demo@example.com` | `Demo12345` |
| Huésped (entra con PIN) | `ana.demo@example.com`, `luis.demo@example.com` | el PIN sale en el log |

Estas cuentas solo existen con los datos de prueba. En un servidor real se apagan con `SGA_DATOS_DEMO=false`, y la clave se cambia con `SGA_DEMO_CLAVE`.

## Probar la API

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`, con el botón *Authorize* para pegar el token. Se apaga con `SGA_DOCUMENTACION_API=false`.
- **Contrato**: [`docs/api/openapi.yaml`](docs/api/openapi.yaml).
- **Colección de pruebas**: [`docs/api/SGA-Entrega2.postman_collection.json`](docs/api/SGA-Entrega2.postman_collection.json). Se importa en Postman y se ejecuta completa con *Run collection* contra la aplicación recién arrancada sobre una base vacía: flujo completo de una reserva, un 409 por cada regla que expone la API, y los 401 y 403.
- **Pruebas automáticas**: `./gradlew test`.

## Frontends

`web/` (reservas del huésped) y `pms/` (gestión del hotel), con React + Vite + TypeScript. Cada uno explica cómo correrlo en su `README.md`.

## Documentación

| Archivo | Qué tiene |
|---|---|
| `docs/MODELO.md` | Agregados, clases, servicios y reglas del dominio |
| `docs/DECISIONES.md` | Decisiones acordadas (`DEC-nn`) |
| `docs/Entidad__Objeto_de_valor_corregido.xlsx` | Identificación, reglas, casos de uso, catálogo de servicios, matriz de trazabilidad y API |
| `docs/SPRINTS.md`, `docs/BACKLOG.md`, `docs/EQUIPOS.md` | Plan por semanas, pendientes y reparto entre equipos |
