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
- **Terminado** quiere decir: `./gradlew build` verde con pruebas del caso feliz y de la violación de cada regla nueva; `npm run build` y `npm test` verdes en el frontend tocado; los mensajes de error del backend (400/401/403/404/409) se muestran entendibles en la pantalla; integrado en `develop` y probado allí con el otro frontend también corriendo.
- **Probar en local**: el backend corre con PostgreSQL y las variables de entorno de cada quien; cada frontend apunta a la API con `VITE_API_URL` (se define en `feature/nucleo/cors-frontends`).

## Sprint 1 · 26 – 30 oct · «Ver y encontrar»
Hipótesis: el huésped entiende qué hay disponible y cuánto cuesta; recepción encuentra cualquier reserva rápido.
- **web**
  - [ ] Crear `web/` (React + Vite + TypeScript), cliente de la API y despliegue local.
  - [ ] Búsqueda de disponibilidad por fechas y ocupantes, y cotización con el desglose por noche.
  - [ ] Definir con el cliente qué datos del apartamento ve el público (pendiente de DEC-67) y ajustar la respuesta pública.
  - [ ] Núcleo: `feature/nucleo/contrato-api`, la documentación OpenAPI de la API para que ambos frontends la consulten.
- **pms**
  - [ ] Crear `pms/` (React + Vite + TypeScript), inicio de sesión del personal y menú según el rol.
  - [ ] Listado de reservas con filtros y detalle de una reserva.
  - [ ] Núcleo: `feature/nucleo/cors-frontends`, CORS para los orígenes de `web/` y `pms/` por variable de entorno.
- **Cierre 1 (vie 30 oct)**: demo de las dos pantallas; priorizar con el cliente el backlog de la entrevista.

## Sprint 2 · 3 – 6 nov (lunes festivo) · «Reservar»
Hipótesis: un huésped reserva y paga solo, sin llamar; recepción crea y confirma reservas directas sin papel.
- **web**
  - [ ] Reservar desde el portal, pago en línea (pasarela simulada) y pantalla de confirmación.
  - [ ] Ingreso del huésped con PIN, «mi cuenta» y «mis reservas».
- **pms**
  - [ ] Tablero de ocupación por apartamento y fechas (endpoint nuevo si hace falta).
  - [ ] Crear reserva directa; confirmar, cancelar y declarar no-show.
- **Cierre 2 (vie 6 nov)**.

## Sprint 3 · 9 – 13 nov · «Operar la estancia»
Hipótesis: recepción hace llegada, cobros y salida en el sistema; el huésped resuelve solo lo de su cuenta desde el celular.
- **web**
  - [ ] Cambio de correo con verificación y edición de los datos de la cuenta (los endpoints ya existen).
  - [ ] Pulir la web: diseño adaptable al celular, textos y mensajes de error.
  - [ ] Requisitos de la entrevista del producto web priorizados en el cierre 1.
- **pms**
  - [ ] Registro de llegada y corrección del registro.
  - [ ] Folio: cargos, pagos, devoluciones y reversos; cierre del folio y salida.
- **Cierre 3 (vie 13 nov)**.

## Sprint 4 · 17 – 20 nov (lunes festivo) · «Configurar el hotel»
Hipótesis: el administrador deja el hotel listo para vender sin ayuda técnica.
- **web**
  - [ ] Requisitos de la entrevista y del cierre 3 para la web.
  - [ ] Prueba con un huésped real de punta a punta (buscar, reservar, pagar, ver su cuenta) y ajustes de lo que salga.
- **pms**
  - [ ] Apartamentos, temporadas y tarifas, política de cancelación, parámetros y medios de pago.
  - [ ] Usuarios del personal, novedades de mantenimiento y pantallas de canales (conflictos y bitácora, con la API del equipo web).
- **Cierre 4 (vie 20 nov)**: decidir con el cliente qué parte de Channex es imprescindible para la entrega.

## Sprint 5 · 23 – 27 nov · «Vender en plataformas y cumplir»
Hipótesis: una reserva de Booking o Airbnb entra sola y sin doble venta, y el hotel cumple con el reporte de TRA.
- **web**
  - [ ] B4c Channex: cuenta sandbox, mapeo de apartamentos y tarifas (decisión con el cliente) y entrada de reservas desde Channex.
  - [ ] B4c Channex: publicar disponibilidad y tarifas, reintentos y bitácora; prueba de punta a punta en el sandbox. Si no alcanza, el cierre 4 decide si pasa al MVP2.
- **pms**
  - [ ] B5 TRA: datos del ocupante en la llegada, titular validado y envío del reporte.
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
