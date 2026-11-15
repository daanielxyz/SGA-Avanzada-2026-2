---
id: alojamiento
titulo: Configurar el alojamiento
pantalla: /alojamiento
roles: ADMINISTRADOR
---

# Configurar el alojamiento

Pantalla «Alojamiento» del menú, solo para el administrador. Tiene cinco bloques (pestañas en el computador, una lista en el celular). **Cada bloque se guarda por separado** con su botón; si cambias de bloque o sales con cambios sin guardar, el sistema pregunta.

## Datos
Nombre, ciudad, dirección, descripción (obligatoria), normas (opcional) y ubicación (latitud de −90 a 90 y longitud de −180 a 180). Se puede pegar «4.6372, -75.5701» tal como lo copia Google Maps y se reparte en los dos campos. Al guardar otro nombre, la cabecera del PMS se actualiza.

## Registro Nacional de Turismo (RNT)
En la pestaña «Datos», el bloque «Registro Nacional de Turismo»: el número del RNT del hotel, solo números, y «Guardar RNT». Con él se envía al Ministerio la tarjeta de registro (TRA) de cada huésped que llega. Si está vacío, los envíos del TRA quedan con error y no sale nada.

## Reglas de la estancia
Son los valores que usa todo el sistema; cambiarlos afecta lo que viene, no lo ya reservado.
- **Horarios**: hora de entrada, hora de salida, **hora límite de llegada** (después de ella una reserva pendiente ya no se confirma y una confirmada puede declararse no-show) y tiempo de preparación entre reservas (horas).
- **Reservas y pagos**: anticipo para confirmar (% del total; 0 = no se exige), plazo para confirmar una pendiente (horas; si pasa, la reserva se cancela sola), plazo del pago en línea de la web (minutos) y **edad desde la que se cobra** (los menores no pagan como ocupantes).
- **Parqueadero**: cupos (carros y motos a la vez; 0 = no hay), cobro por noche o por estadía, y valor por carro y por moto.
- **Mascotas**: si se aceptan, el máximo por reserva y el cargo por mascota (una vez por estancia). Cada apartamento puede tener su propio límite en su ficha.
- **Avanzado**: mínimos que el sistema hace cumplir (medios de pago habilitados, servicios activos, temporadas, tramos de la política, capacidades distintas de apartamentos). Se abre solo si uno tiene error.

## Servicios adicionales
Lo que recepción carga como consumo en el folio (minibar, lavandería…). «Nuevo servicio» con nombre y valor; un servicio «Incluido» va con valor 0 y no genera cobro. «Cambiar valor» rige desde ese momento. «Desactivar» lo quita del catálogo de recepción; puede rechazarse si quedaría por debajo del mínimo de servicios activos. Los desactivados aparecen al final, marcados.

## Medios de pago
Con qué registra recepción los pagos. «Agregar medio de pago» con su nombre (hasta 40 caracteres) y «Deshabilitar» para quitarlo; uno deshabilitado se vuelve a habilitar escribiéndolo de nuevo en «Agregar». No se puede bajar del mínimo de medios de pago. El pago en línea es solo de la web.

## Marca
Cómo se ve la web de reservas: colores (#RRGGBB), fuentes, radios de las esquinas, logo y portada (direcciones https://). La pantalla mide el contraste de los colores; desde 4,5:1 «se lee bien».
