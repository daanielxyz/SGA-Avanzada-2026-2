# Prompt para Claude Design: alojamiento (sprint 4, Daniel)

**Adjuntos**: los mismos de `apartamentos.md`, más la captura `portada-web.png` (la portada de la web de reservas, que usa la marca).

Copia desde aquí:

---

Continúa el diseño de Meridian, el PMS de un hotel. Sigue exactamente los mockups adjuntos: mismo tema oscuro, mismos componentes y mismo `base.css`. Las capturas son del PMS ya programado y mandan sobre los mockups viejos. Agrega componentes nuevos solo si no existe uno equivalente y lístalos al final.

Entrega: un `.html` por pantalla o por hoja de estados, su `png/` a tamaño real, `base.css` con lo nuevo al final bajo «Sprint 4 · alojamiento» y un `LEEME.md` con pantallas, componentes nuevos y «Lo que falta o quedó dudoso». Nombres con prefijo `S4`. Cada pantalla a 1280 px y, con sufijo `Celular`, a 390 px.

## Reglas
- No inventes datos ni acciones: usa solo lo que se describe aquí. Si crees que falta algo, anótalo en «Lo que falta o quedó dudoso» y no lo dibujes.
- Quien usa esto es el **administrador**, en el computador. Español de Colombia, sin términos técnicos.
- Oliva y Terracota no se usan como color de texto. Ningún estado depende solo del color.
- Cada bloque se guarda por separado, con su propio botón y su aviso de guardado. Si se sale con cambios sin guardar, se pregunta.
- Lo ya reservado no cambia: los parámetros nuevos (anticipo, cargos, horas) aplican a las reservas nuevas.

## Datos de ejemplo
Hoy es 17 nov 2026. Alojamiento «Puerta al Sol», Salento (Quindío), «Vereda Palestina km 2». Administración: Andrés Felipe Gómez.

## Pantalla S4Alojamiento
Ruta `/alojamiento`, grupo «Configuración», título con el nombre del alojamiento. Pestañas (en el celular, una lista que abre cada bloque):

### 1. Datos
Nombre (hasta 120), descripción, ciudad (hasta 120), dirección (hasta 200) y normas de la casa (texto largo, opcional). Debajo, **Ubicación**: latitud y longitud con la pista «Cópialas de Google Maps: clic derecho sobre el lugar». Sin mapa (no hay un servicio de mapas conectado). La ubicación se guarda aparte.

### 2. Reglas de la estancia
Agrupadas, cada campo con una línea que explica su efecto:
- **Horarios**: hora de entrada (15:00), hora de salida (12:00), hora límite de llegada (22:00: después, una pendiente ya no se confirma y una confirmada puede declararse no-show), tiempo de preparación entre reservas (3 horas).
- **Reservas y pagos**: anticipo para confirmar (30 %), plazo para confirmar una pendiente (24 horas), pago en línea (anticipo o total) y su plazo (30 minutos), edad desde la que se cobra (12 años).
- **Parqueadero**: cupos (8; 0 = sin parqueadero), cobro por noche o por estadía, valor por carro ($ 15.000) y por moto ($ 8.000).
- **Mascotas**: acepta mascotas (sí o no), máximo por reserva (2) y cargo por estancia ($ 30.000). Si no acepta, los demás campos se ocultan.
- **Mínimos del hotel** (plegado, «Avanzado»): medios de pago, servicios adicionales, temporadas, tramos de la política y capacidades distintas que exige el hotel. Pista: «Valores de la ficha del curso; un hotel real puede bajarlos».

Un solo «Guardar reglas» al final, porque la API las recibe todas juntas.

### 3. Servicios adicionales
Tabla: nombre, si genera cargo («Se cobra» o «Incluido»), valor y estado. Acciones: «Cambiar valor» y «Desactivar» (confirmación: «Deja de ofrecerse en los consumos nuevos. Lo ya cargado no cambia. No se puede volver a activar»). Botón «Nuevo servicio»: nombre, se cobra (sí o no) y valor. Ejemplo: Desayuno $ 25.000, Agua $ 3.000, Lavandería $ 18.000, Wifi incluido.

### 4. Medios de pago
Lista de los habilitados (Efectivo, Tarjeta, Transferencia) con «Deshabilitar». Campo «Agregar medio de pago» con su botón. Rechazo al deshabilitar por debajo del mínimo: «El alojamiento debe tener al menos 2 medios de pago».

### 5. Marca
Lo que usa la web de reservas para verse como el hotel: logo y foto de portada (dirección de una imagen ya publicada), los 10 colores (fondo, superficie, texto, texto suave, borde, borde de campo, acento, texto sobre acento, peligro, éxito; cada uno con su muestra y el código `#RRGGBB`), las dos fuentes (títulos y texto) y los tres radios de las esquinas (pequeño, mediano, grande, en px). A la derecha, una **vista previa** pequeña de la portada de la web con esos valores. Junto a cada par texto/fondo, el contraste medido («9,2:1 · se lee bien» o «2,7:1 · difícil de leer»), porque la API no lo revisa. Un solo «Guardar marca».

Hoja **S4AlojamientoEstados**: cargando; error con «Reintentar»; un bloque enviando; guardado; errores por campo (400); rechazo de negocio (409) junto al bloque; salir con cambios sin guardar.

---

## Para Claude Code (no va a Claude Design)
- Endpoints: `GET /api/alojamiento`; `PUT /api/alojamiento/datos`, `…/ubicacion`, `…/parametros` (**reemplaza todos**: hay que enviar los 21 campos, incluidos parqueadero y mascotas), `…/marca`; `POST /api/alojamiento/servicios`, `PUT …/servicios/{id}/valor` y `…/desactivar`; `PUT …/medios-pago/{medio}/habilitar` y `…/deshabilitar` (el medio es texto libre).
- Ya existe `src/api/alojamiento.ts`: amplíalo.
- La marca la usa la web (`web/`), que es del otro equipo: probar que la portada siga leyéndose después de guardar.
