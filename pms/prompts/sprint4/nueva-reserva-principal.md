# Prompt para Claude Design: huésped principal y apellidos en «Nueva reserva» (sprint 4, Daniel, DEC-97/98)

**Adjuntos**: `brandbook.png`; `base.css` y `LEEME.md` de la preparación; de `mockups-meridian-sprint2/mockups/`, `S2Paso3Personas.html`, `S2Paso3Errores.html` y sus PNG; de `capturas/`, `paso3.png` (el paso 3 tal como está programado hoy, con dos ocupantes).

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Es un ajuste del paso 3 «Personas» de «Nueva reserva». La captura `paso3.png` es como está hoy y manda sobre `S2Paso3Personas`: cambia solo lo que se pide aquí. Mismo tema oscuro y mismo `base.css`.

Entrega: `S4Paso3Personas.html` (computador, 1280 px) y `S4Paso3PersonasCelular.html` (390 px), la hoja `S4Paso3Estados` y su `png/`, `base.css` con lo nuevo al final bajo «Sprint 4 · nueva reserva» y un `LEEME.md` con lo cambiado y «Lo que falta o quedó dudoso».

## Reglas
- No inventes datos ni acciones. Español de Colombia, sin términos técnicos. Oliva y Terracota no se usan como color de texto.

## Qué cambia
1. **Titular con nombres y apellidos aparte**: el campo «Nombre completo» del titular pasa a tres: «Nombres» (hasta 120), «Primer apellido» (hasta 60) y «Segundo apellido» (hasta 60, opcional). Así quedan bien en el reporte a Migración (TRA).
2. **Huésped principal**: es quien responde por el grupo en el TRA. Debe ser mayor de edad el día de entrada. Cada ocupante adulto tiene una opción «Huésped principal» (un solo elegido en todo el grupo, como un botón de opción). Los menores no la tienen. Por defecto queda elegido el primer adulto. Si el titular también se aloja (la casilla «El titular también se aloja», que ya existe), queda elegido él.
3. Si nadie del grupo será mayor de edad al entrar, el error que ya existe se mantiene: «El grupo necesita un mayor de edad (18 años o más al entrar): será el huésped principal.»

## Datos de ejemplo
Entrada 20 nov 2026. Titular: Ana María Ríos Gómez, CC 1094 888 777. Ocupantes: Ana María Ríos Gómez (adulta, se aloja), Julián Pérez Ríos (adulto), Sofía Pérez Ríos (9 años, TI).

Hoja **S4Paso3Estados**: el titular sin primer apellido (error junto al campo); el principal elegido es Julián; un grupo con un solo adulto (la opción ya elegida y sin poder quitarla).

---

## Para Claude Code (no va a Claude Design)
- `TitularReservaRequest` ya recibe `nombres`, `primerApellido` y `segundoApellido` (DEC-98); `OcupanteRequest.principal` ya existe (DEC-97). Mandar `nombre` también, armado con las partes, mientras la API lo acepte.
- En `src/reserva/nueva/formulario.ts` y `PasoPersonas.tsx`. La regla de edad usa la fecha de entrada, como `mensajeDocumentoPorEdad`.
- Después de crear, «Hacer principal» de la llegada sigue sirviendo para cambiarlo.
