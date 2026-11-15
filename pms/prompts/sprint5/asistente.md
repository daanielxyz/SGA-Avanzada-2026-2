# Prompt para Claude Design: asistente del PMS (sprint 5, Daniel)

**Adjuntos** (todo está en `pms/diseno/` de Daniel):
- `referencias-sprint4/brandbook.png`.
- `mockups-meridian-sprint4-nueva-reserva/S4Paso3/base.css`: el más reciente, con todo lo de los sprints anteriores.
- `referencias-sprint4/capturas/detalle.png` y `reservas.png`: el PMS real en el computador, con la cabecera y el menú.
- `mockups-meridian-sprint3-preparacion/preparacion/png/S3Preparacion.png`: la preparación en el celular, que es donde más lo usa el personal de servicio.
- `referencias-sprint4/mockups-meridian-sprint3-llegada/llegada/S3Llegada.html` y `png/S3Llegada.png`: un aviso de rechazo (rojo) junto a una acción.

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos (cabecera, menú, avisos, botones). Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 5 · asistente» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S5`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Lo usan las tres personas del personal: **recepción** y **administración**, sobre todo en el computador, y el **personal de servicio**, sobre todo en el celular. Español de Colombia, tuteando, sin términos técnicos: nunca «IA», «modelo», «tokens», «API» ni «Gemini» en la pantalla.
- Oliva y Terracota no se usan como color de texto.
- Es una ayuda, no un chat social: sobrio, sin avatares de robot, sin emojis, sin animaciones de «escribiendo» exageradas.

## Cómo funciona
El asistente responde dudas de uso del PMS según el rol de quien pregunta: cómo se hace algo, qué hacer en un caso y por qué el sistema rechazó una acción. **Solo explica**: no hace nada en el sistema. Cada respuesta trae:
- el **texto**, en Markdown sencillo (párrafos, listas numeradas y negritas; nada más);
- las **fuentes**: las secciones del manual en que se basa (0, 1 o varias), cada una con su título y, si tiene, la pantalla a la que lleva;
- el **origen**: respondió el asistente (lo normal) o respondió **el manual**, porque el asistente no estaba disponible. En ese caso llega además un **aviso** corto que lo explica, que se muestra tal cual.

La conversación solo vive mientras el panel está abierto: al cerrar sesión o recargar se pierde. No se guarda nada. Una respuesta tarda de 1 a 3 segundos, y hasta unos 10 para administración.

## Datos de ejemplo
- Recepcionista: Laura Restrepo, en el detalle de la reserva RES-2026-00042.
- Pregunta: «¿Cómo declaro el no-show de una reserva?».
- Respuesta: «Para declarar el no-show: 1. La reserva debe estar **confirmada**. 2. Desde la hora límite de llegada del día de entrada (aquí, las 10:00 p. m.), pulsa «Declarar no-show» en el detalle. 3. Revisa cuánto se retendrá y vuelve a pulsar «Declarar no-show». Una vez declarado, no se puede revertir.» Fuentes: «Cancelar una reserva o declarar el no-show» y «Qué hacer en cada caso (recepción)».
- Respuesta del manual, con el aviso «El asistente no respondió: te respondo con el manual.»: el texto empieza por «Esto dice el manual en «Registrar la llegada»:» y sigue con la sección.
- Sin respuesta: «No encontré eso en el manual del PMS. Pregúntale al administrador.», sin fuentes.
- Personal de servicio, en «Por preparar», en el celular: «Marqué el 204 como preparado pero falta cambiar las sábanas.» → «No se puede devolver a pendiente desde la preparación. Avisa al administrador y a recepción para que no lo entreguen todavía.»

## Pantallas

### 1. S5Asistente (panel abierto, con conversación)
- **El botón para abrirlo** va en la cabecera del PMS, junto a «Cerrar sesión», con el ícono de ayuda y el texto «Ayuda» (en el celular, solo el ícono, con «Ayuda» para lectores de pantalla). Está en todas las pantallas con sesión.
- En el computador, el panel se abre a la **derecha**, de unos 400 px, de toda la altura, sin tapar la cabecera; el contenido del PMS sigue visible y usable a la izquierda (no hay velo). En el celular ocupa toda la pantalla, con «Cerrar» arriba.
- Encabezado del panel: «Ayuda del PMS», «Empezar de nuevo» (borra la conversación; solo si hay mensajes) y cerrar (✕).
- La conversación: la pregunta de la persona a la derecha y la respuesta a la izquierda, con su texto y, debajo, «Según el manual:» con las fuentes como enlaces que llevan a la pantalla (y cierran el panel en el celular). Una fuente sin pantalla va como texto.
- Abajo, el campo para escribir (crece hasta unas 5 líneas; Enter envía y Mayús+Enter salta de línea) con el botón «Preguntar», y debajo, siempre visible, la pista: «No escribas datos de huéspedes: nombres, documentos, correos ni teléfonos.» Contador cuando pase de 900 caracteres (máximo 1000).

### 2. S5AsistenteInicio (panel abierto, sin conversación)
Saludo corto («¿En qué te ayudo?») y tres preguntas sugeridas como botones, según el rol:
- Recepción: «¿Cómo registro una llegada?», «¿Qué hago si el huésped no llegó?», «¿Cómo corrijo un pago mal registrado?».
- Servicio: «¿Por cuál apartamento empiezo?», «Encontré un daño, ¿qué hago?», «¿Cómo marco un apartamento como preparado?».
- Administración: «¿Cómo pongo un apartamento a la venta?», «¿Cómo subo los precios de una temporada?», «¿Cómo autorizo una salida con saldo?».
Tocar una la envía como si se escribiera. Debajo, una línea: «Respondo con el manual del PMS. Solo explico: no hago cambios en el sistema.»

### 3. S5AsistenteEstados (hoja con los estados de una respuesta)
- **Esperando**: la pregunta enviada y, en el lugar de la respuesta, «Buscando en el manual…» discreto; «Preguntar» deshabilitado mientras tanto.
- **Respondió el manual**: la respuesta con el aviso arriba (tono informativo, no de error), por ejemplo «Llegaste al límite de preguntas por hora: mientras tanto te respondo con el manual.».
- **Sin respuesta**: «No encontré eso en el manual del PMS. Pregúntale al administrador.», sin fuentes.
- **Error de conexión** (no hay red o el servidor no responde): la pregunta queda con «No se pudo enviar. Revisa tu conexión.» y «Reintentar».
- **Pregunta vacía o de más de 1000 caracteres**: el mensaje junto al campo.

### 4. S5PorQue (el «¿Por qué?» de un rechazo)
En un aviso rojo de rechazo del PMS (como el de la llegada del adjunto), un enlace al final: «¿Por qué?». Abre el panel y envía sola la pregunta «¿Por qué el sistema dice: «<el mensaje del aviso>»?». Dibuja el aviso con el enlace (por ejemplo «No se registró la llegada · El apartamento todavía no está preparado.») y el panel ya respondiendo.

---

## Para Claude Code (no va a Claude Design)
- Endpoint: `POST /api/asistente/preguntas` con `PreguntaRequest` (`pregunta` hasta 1000, `pantalla` = la ruta actual, `historial` = hasta 20 `{autor: PERSONA | ASISTENTE, texto}`), y responde `PreguntaResponse` (`respuesta`, `fuentes[{id, titulo, pantalla}]`, `origen` IA | LOCAL, `motivo`, `aviso`). **Siempre 200** salvo 400 (formato), 401 y 403 (huésped). DEC-127 y «Sprint 5 adelantado: asistente de IA» en `pms/DISENO.md`.
- La `pantalla` de una fuente puede traer `{codigo}`: si la ruta actual tiene un código, se reemplaza por él; si no, la fuente va como texto, sin enlace.
- Mandar los últimos mensajes en `historial` (el backend usa los 10 más recientes). Nada se guarda en el navegador: ni `localStorage` ni `sessionStorage`, porque la conversación puede traer datos de la operación.
- El panel va en su propio componente (`pms/src/asistente/`) con una sola línea en el marco (`Panel.tsx`), para no chocar con otras pantallas («Dónde se pueden cruzar»). El «¿Por qué?» va en el componente que ya muestra los rechazos.
- Markdown: solo párrafos, listas y negritas, renderizados sin `dangerouslySetInnerHTML`.
