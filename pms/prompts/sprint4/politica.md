# Prompt para Claude Design: política de cancelación (sprint 4, Daniel)

**Adjuntos**: los mismos de `apartamentos.md`, más la captura `politica-detalle.png` (el diálogo «Ver política» del detalle de una reserva).

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos; «Ver política» ya muestra los tramos y esta pantalla debe contarlos igual. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 4 · política» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S4`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien usa esto es el **administrador**, en el computador. Español de Colombia, sin términos técnicos: nunca `VALOR_TOTAL` ni `ANTICIPO_EXIGIDO`.
- Oliva y Terracota no se usan como color de texto.

## Cómo funciona
La política no se edita: se **publica una versión nueva**, que rige para las reservas que se creen desde ese momento. Cada reserva guarda la versión con la que se hizo. Una versión tiene:
- **Tramos**: «si cancela con al menos N horas de antelación a la hora de entrada, se retiene…». Debe haber un tramo desde 0 horas y dos tramos no empiezan en las mismas horas. Hay un mínimo de tramos (2, configurable en Alojamiento).
- Cada retención (de un tramo o del no-show) es **un porcentaje o un monto fijo**, nunca los dos, sobre una base: el valor total de la reserva, lo pagado o el anticipo exigido.
- **No-show**: lo que se retiene si el huésped no llega.

## Datos de ejemplo
Hoy es 17 nov 2026. Versión vigente: la 3 (la política no tiene nombre, solo número de versión).
- Desde 48 horas: no se retiene nada (0 % del valor total).
- Desde 24 horas: 50 % del valor total.
- Desde 0 horas: 100 % del anticipo exigido.
- No-show: 100 % de lo pagado.
Versiones 1 y 2 anteriores.

## Pantallas

### 1. S4Politica
Ruta `/politica`, grupo «Configuración», título «Política de cancelación». Arriba, la **vigente** contada en frases, de la mayor antelación a la menor, igual que «Ver política» en el detalle, con «Versión 3». Botón «Publicar nueva versión». Debajo, «Versiones anteriores», de 10 en 10, cada una plegada con sus tramos.

Hoja **S4PoliticaEstados**: cargando; error con «Reintentar»; sin política publicada.

### 2. S4PublicarPolitica
Formulario que parte de la vigente. Cada tramo en un renglón: «Con al menos [48] horas de antelación, retener [0] [% / $] de [el valor total ▾]», con ✕ para quitarlo; «Agregar tramo». El renglón del no-show aparte. A la derecha, la **vista previa** en frases como la verá recepción. Aviso antes de publicar: «Rige para las reservas nuevas. Las reservas ya hechas conservan su versión». Errores junto al renglón: falta el tramo desde 0 horas, dos tramos con las mismas horas, porcentaje y monto a la vez, menos tramos que el mínimo. Estados: normal, enviando, publicada.

---

## Para Claude Code (no va a Claude Design)
- Endpoints: `GET /api/politicas?pagina=` (versiones), `GET /api/politicas/vigente`, `GET /api/politicas/{id}`, `POST /api/politicas` (`PublicarPoliticaRequest`: `tramos` con `antelacionMinHoras` y `penalizacion`, y `penalizacionNoShow`; `penalizacion` = `base` VALOR_TOTAL | PAGADO | ANTICIPO_EXIGIDO más `porcentaje` o `montoFijo`).
- Ya existen `src/api/politicas.ts` y el texto de los tramos de «Ver política» (DEC-104): reutilízalos para la vista previa.
