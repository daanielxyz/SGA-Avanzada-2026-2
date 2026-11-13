# Prompt para Claude Design: temporadas y tarifas (sprint 4, Arias)

**Adjuntos**: los mismos de `apartamentos.md` (`brandbook.png`, `base.css` y `LEEME.md` de la preparación, `S3Llegada.html`, `S3DialogoVehiculos.html`, `S2Ocupacion.html` con sus PNG y las capturas `reservas.png` y `detalle.png`). Si ya tienes los mockups de apartamentos, adjunta también `S4Apartamentos.html` y `S4Apartamento.html` para que sigan iguales.

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 4 · temporadas y tarifas» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S4`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px. Los diálogos en el celular ocupan toda la pantalla.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien usa esto es el **administrador**, en el computador. Español de Colombia, sin términos técnicos. Los valores en pesos colombianos sin decimales («$ 180.000»).
- Oliva y Terracota no se usan como color de texto. Ningún estado depende solo del color.
- **Lo ya reservado no cambia**: cada reserva guardó el valor de sus noches al crearse. Cambiar una tarifa o una temporada solo afecta las reservas nuevas. La pantalla lo dice donde se cambia un valor.

## Cómo funcionan
- **Temporadas**: hay una **temporada base**, que cubre todos los días que no caen en otra; no tiene fechas ni estancia mínima, y no se puede desactivar. Las demás tienen nombre (hasta 80), desde y hasta, y una estancia mínima en noches (0 = sin mínimo). Dos temporadas activas no se pueden solapar. Una temporada se puede desactivar (no se reactiva) o cambiar de fechas. No se renombran ni se borran.
- **Tarifas**: un valor **por ocupante facturable por noche** para cada apartamento en cada temporada. Cambiar una tarifa crea una versión nueva que rige desde hoy; las anteriores quedan en el historial. Un apartamento necesita tarifa en todas las temporadas activas para ponerse a la venta.

## Datos de ejemplo
Hoy es 17 nov 2026. Alojamiento «Puerta al Sol», 10 apartamentos. Administración: Andrés Felipe Gómez.

| Temporada | Desde | Hasta | Estancia mínima | Estado |
|---|---|---|---|---|
| Temporada base | — | — | — | Siempre activa |
| Temporada alta fin de año | 15 dic 2026 | 15 ene 2027 | 3 noches | Activa |
| Semana Santa | 29 mar 2027 | 5 abr 2027 | 2 noches | Activa |
| Puente de noviembre | 14 nov 2026 | 17 nov 2026 | 0 | Inactiva |

Tarifas por ocupante y noche (base / alta / Semana Santa): APT-101 Penthouse Mirador $ 120.000 / $ 180.000 / $ 160.000; APT-102 Apartamento Jardín $ 90.000 / $ 135.000 / $ 120.000; APT-103 Estudio Terraza $ 80.000 / $ 120.000 / sin tarifa.

## Pantallas

### 1. S4Temporadas: el calendario y la tabla de tarifas
Ruta `/tarifas`, grupo «Configuración», título «Temporadas y tarifas». Dos partes:
- **Temporadas**: lista con nombre, fechas («15 dic 2026 – 15 ene 2027»), estancia mínima y estado. La base va primero, con la pista «Cubre los días que no caen en otra temporada». Cada temporada activa no base tiene «Cambiar fechas» y «Desactivar». Las inactivas en gris debajo de «Ver inactivas». Botón «Nueva temporada».
- **Tarifas**: tabla con un apartamento por fila y una columna por temporada activa; cada celda muestra el valor («$ 180.000 por persona y noche» abreviado como «$ 180.000») o «Sin tarifa» marcado (con ícono y palabra, no solo color). Tocar una celda abre el diálogo 4. Al lado de cada apartamento, «Historial». Un apartamento con alguna celda «Sin tarifa» lleva la pista «No se puede poner a la venta». De 10 en 10 con paginador, como los apartamentos. En el celular, una tarjeta por apartamento con sus temporadas en renglones.

Hoja **S4TemporadasEstados**: cargando; error con «Reintentar»; solo la temporada base; un apartamento sin ninguna tarifa.

### 2. S4DialogoTemporada: nueva temporada
Nombre, desde, hasta, estancia mínima y, debajo, la tarifa de cada apartamento para esa temporada (un campo por apartamento, opcional: el que quede vacío queda «Sin tarifa»). Rechazos: «La temporada Semana Santa se solapa con Temporada alta fin de año» (junto a las fechas), «El fin de la temporada no puede ser anterior al inicio». Estados: normal, enviando, rechazo.

### 3. S4DialogoFechasTemporada y S4DialogoDesactivarTemporada
- **Cambiar fechas**: desde y hasta, con el aviso «Las reservas ya hechas conservan el valor con que se reservaron». Mismos rechazos de solapamiento.
- **Desactivar**: «Temporada alta fin de año deja de aplicarse a las reservas nuevas: esas noches se cobran con la temporada base. No se puede volver a activar».

### 4. S4DialogoTarifa
«APT-101 · Temporada alta fin de año». Valor por persona y noche (mayor que cero), con el valor vigente y desde cuándo rige («Hoy: $ 180.000, desde el 2 nov 2026»). Aviso: «Rige para las reservas nuevas desde hoy. Las reservas ya hechas no cambian». Estados: normal, enviando, rechazo.

### 5. S4HistorialTarifas
Panel o diálogo «Historial de tarifas · APT-101»: por cada versión, temporada, valor, versión y desde cuándo rigió, de la más reciente a la más antigua, de 10 en 10.

---

## Para Claude Code (no va a Claude Design)
- Endpoints: `GET /api/temporadas` (`CalendarioResponse` con `temporadas`: `id`, `nombre`, `fechaInicio`, `fechaFin`, `esBase`, `estanciaMinimaNoches`, `activa`), `POST /api/temporadas` (`AgregarTemporadaRequest` con `tarifas`: `apartamento` y `valorPorOcupante`; responde `TemporadaCreadaResponse`), `PUT /api/temporadas/{id}/fechas` y `…/desactivar`; `GET /api/apartamentos/{codigo}/tarifas?pagina=` (vigentes), `PUT /api/apartamentos/{codigo}/tarifas/{temporada}` (`DefinirTarifaRequest`: `valorPorOcupante`) y `GET …/tarifas/historial?pagina=`.
- `TarifaResponse.temporada` es el id (`TEM-1`): el nombre se toma de `GET /api/temporadas`. Nunca se muestra el id.
- La tabla necesita las tarifas de los apartamentos de la página (una llamada por apartamento, 10 como mucho).
- Dinero: `number` de la API, sin decimales, con el formato de `src/formato.ts`.
