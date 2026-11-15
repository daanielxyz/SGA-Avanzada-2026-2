---
id: folio
titulo: El folio (la cuenta de la reserva)
pantalla: /reservas/{codigo}/folio
roles: RECEPCIONISTA, ADMINISTRADOR
---

# El folio

Desde el detalle de la reserva: «Ver folio» en el bloque Pago. Arriba están el saldo, los cargos y el total pagado; debajo, la tabla de cargos y la de pagos.

## Reglas
- Saldo = cargos − pagos. «Pendiente» si el huésped debe, «A favor» si pagó de más, «Al día» en cero.
- Ningún cargo ni pago se borra ni se edita: se corrige con un movimiento contrario, que queda tachado junto al original con su motivo (RN-16).
- Un folio cerrado ya no admite movimientos.

## Registrar un pago
«Registrar pago»: medio de pago (los habilitados por el hotel; el pago en línea es solo de la web), monto en pesos y fecha (hoy por defecto, nunca una fecha futura). En una reserva terminada solo se reciben pagos si todavía debe algo.

## Registrar una devolución
Cuando el saldo está a favor (por ejemplo, después de cancelar). Primero se le entrega el dinero al huésped por fuera del sistema y luego se registra con «Registrar devolución». Nunca se devuelve más de lo pagado.

## Revertir un pago mal registrado
En la tabla de pagos, «Revertir» en ese abono. El pago no se borra: queda tachado junto a su reverso. Es para errores de registro; si le entregas dinero al huésped, es una devolución.

## Cargar un consumo
Con la reserva **en curso**: «Cargar consumo», se elige el servicio del catálogo del hotel y la cantidad («dos aguas» = cantidad 2). Se carga con la fecha de hoy.

## Corregir o revertir un cargo
- **Corregir consumo** (reserva en curso): dos pasos. Primero se revierte el cargo con un motivo; después se carga el correcto, ya propuesto con el mismo servicio y cantidad. Si sales después del primer paso, el cargo queda revertido y no se carga el nuevo.
- **Revertir**: cualquier cargo, con motivo obligatorio. El hospedaje, el parqueadero, las mascotas y la retención no se vuelven a cargar desde el folio: su valor cambia modificando la reserva.

## Cerrar el folio
- Al registrar la salida, el folio se cierra solo.
- En una reserva **cancelada o con no-show**, se cierra con «Cerrar folio» cuando el saldo está al día.
- Con saldo distinto de cero, **solo el administrador** puede cerrarlo, con «Autorizar y cerrar» y el motivo de su autorización; queda a la vista como «Cerrado con autorización» (RN-17). Recepción debe cobrar o devolver primero, o pedir la autorización.
