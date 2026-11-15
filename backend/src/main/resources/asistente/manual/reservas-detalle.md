---
id: reservas-detalle
titulo: El detalle de una reserva y confirmarla
pantalla: /reservas/{codigo}
roles: RECEPCIONISTA, ADMINISTRADOR
---

# El detalle de una reserva

Se abre tocando una reserva en «Reservas» o con «Ir a la reserva». Arriba van el código, el estado y los botones de lo que se puede hacer según el estado.

## Qué botones salen según el estado
- **Pendiente**: «Confirmar» y «Cancelar». Si la entrada es hoy, también «Continuar llegada»; si es mañana, «Preparar llegada».
- **Confirmada**: «Registrar llegada» (desde el día de entrada), «Declarar no-show» (desde la hora límite de no-show del día de entrada) y «Cancelar». Si la entrada es mañana, también «Preparar llegada».
- **En curso**: «Registrar salida». El administrador ve además «Corregir registro».
- **Finalizada, cancelada o no se presentó**: ninguno; la reserva solo se consulta.

Un botón gris tiene debajo desde cuándo estará disponible.

## Confirmar una reserva
1. En la reserva pendiente, pulsa «Confirmar».
2. Escribe o revisa la **hora estimada de llegada** (obligatoria).
3. Si falta cubrir el anticipo, aparece «Falta cubrir el anticipo» con el valor: pulsa «Registrar pago» ahí mismo, registra el pago y vuelve con «Volver a confirmar».
4. Pulsa «Confirmar reserva». Desde ese momento ya no vence.

Una reserva pendiente que no se confirmó antes de la hora límite de su día de entrada ya no se puede confirmar: el sistema solo ofrece «Cancelar». Si el huésped sí llegó, cancélala y crea una nueva (DEC-110).

## Bloques del detalle
- **Datos de la reserva**: apartamento, titular, canal, entrada, salida, noches, hora estimada de llegada, la llegada registrada (si ya entró), fecha de creación y la **política aplicada** (la versión de la política de cancelación con la que se creó la reserva).
- **Vehículos y mascotas**: con «Cambiar», mientras la reserva esté pendiente o confirmada. Solo cambian esos cargos con los precios de hoy; el valor de las noches no se recalcula, y la diferencia queda como ajuste en el folio (DEC-116). Si un vehículo o una mascota llega durante la estancia, se carga como un consumo en el folio.
- **Pago**: estancia, extras, anticipo exigido, total pagado y saldo, con «Registrar pago» y «Ver folio».
- **Notas del turno**: lo que un turno le deja dicho al siguiente («pidieron cobijas», «paga la mitad al llegar»). Se escribe en «Agregar nota»; cuando se resuelve, se pulsa «Marcar como atendida» y pasa a «Atendidas» con quién y cuándo. En una reserva terminada solo se leen.
- **Ocupantes**: nombre, documento, nacionalidad, fecha de nacimiento y el estado de sus datos del TRA, con «Completar datos» o «Editar datos».
- **Desglose por noche**: el valor de cada noche y su temporada.

## Corregir un documento mal escrito
Si el documento de un huésped o del titular quedó mal escrito, en una reserva activa:
1. Pulsa «Corregir documento»: en la fila del huésped, en «Ocupantes», o junto al titular, en «Datos de la reserva».
2. Elige el tipo nuevo, escribe el número nuevo y el **motivo** (obligatorio, por ejemplo «se escribió mal un número al hacer la reserva»).
3. Guarda. Solo cambia el documento: los demás datos del TRA, el huésped principal y las reservas del titular se conservan (DEC-120).

Cada corrección queda en «Documentos corregidos», con cómo estaba, cómo quedó, el motivo, quién y cuándo; las del titular van marcadas. El sistema la rechaza si el documento nuevo es igual, si ya es de otra persona del grupo o, para el titular, si ya es de otro titular del hotel o es un registro civil.

## Lo que todavía no se hace desde el PMS
Cambiar las fechas, el apartamento o las personas de una reserva ya creada no tiene pantalla todavía: pregúntale al administrador.
