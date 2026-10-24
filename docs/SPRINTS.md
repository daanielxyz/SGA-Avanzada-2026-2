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

## Sprint 3 · 9 – 13 nov · «Operar la estancia» y «Vender en plataformas»
Hipótesis: recepción hace llegada, cobros y salida en el sistema; una reserva de Booking o Airbnb entra sola y sin doble venta.
- **web**
  - [ ] B4c Channex (parte 1): cuenta sandbox, mapeo de apartamentos y tarifas (decisión con el cliente) y entrada de reservas desde Channex.
- **pms**
  - [ ] Registro de llegada y corrección del registro.
  - [ ] Folio: cargos, pagos, devoluciones y reversos; cierre del folio y salida.
- **Cierre 3 (vie 13 nov)**.

## Sprint 4 · 17 – 20 nov (lunes festivo) · «Configurar el hotel»
Hipótesis: el administrador deja el hotel listo para vender sin ayuda técnica.
- **web**
  - [ ] B4c Channex (parte 2): publicar disponibilidad y tarifas, reintentos y bitácora; prueba de punta a punta en el sandbox.
  - [ ] Pulir la web: diseño adaptable al celular, textos y mensajes de error.
- **pms**
  - [ ] Apartamentos, temporadas y tarifas, política de cancelación, parámetros y medios de pago.
  - [ ] Usuarios del personal, novedades de mantenimiento y pantallas de canales (conflictos y bitácora, con la API del equipo web).
- **Cierre 4 (vie 20 nov)**.

## Sprint 5 · 23 – 27 nov · «Cumplir y entregar»
Hipótesis: el producto está listo para usarse en el hotel real.
- **web**
  - [ ] Ajustes del cierre 4 y requisitos nuevos priorizados.
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
