---
id: reservas-conceptos
titulo: Conceptos de las reservas
pantalla: /reservas
roles: RECEPCIONISTA, ADMINISTRADOR
---

# Conceptos de las reservas

## Estados de una reserva
- **Pendiente**: recién creada. Aparta las noches, pero vence si no se confirma dentro del plazo de confirmación del hotel; al vencer queda cancelada sin retención y lo que se haya pagado queda a favor del huésped para devolverlo (RN-21).
- **Confirmada**: ya no vence. Para confirmar hacen falta la hora estimada de llegada y el anticipo cubierto, si el hotel lo exige (RN-09 · RES-15).
- **En curso**: el huésped llegó y la llegada está registrada; el apartamento queda ocupado.
- **Finalizada**: se registró la salida.
- **Cancelada**: se canceló a mano o venció sin confirmar.
- **No se presentó (no-show)**: el huésped no llegó y recepción lo declaró.

Solo se pasa de un estado a otro así: pendiente → confirmada o cancelada; confirmada → en curso, cancelada o no se presentó; en curso → finalizada. Finalizada, cancelada y no se presentó son definitivas: no se deshacen (RN-08).

## Fechas y noches
- La estancia va desde la fecha de entrada hasta la de salida, y **la noche de salida no se ocupa ni se cobra**. Del 10 al 13 son 3 noches: la del 10, la del 11 y la del 12.
- El valor de cada noche sale de la tarifa de la temporada, y se ve en «Desglose por noche».
- El valor se **congela** al crear la reserva: si después cambian las tarifas, la reserva conserva su valor.

## Personas
- **Titular**: quien responde por la reserva y por su folio (la cuenta). Puede no alojarse, por ejemplo si reserva para otra persona.
- **Ocupantes o huéspedes**: quienes se alojan. Todos cuentan para la capacidad del apartamento, pero solo pagan los que a la fecha de entrada tienen la edad de cobro del hotel o más (los niños menores de esa edad no pagan). La edad se calcula a la fecha de entrada.
- **Huésped principal**: el adulto (18 años o más el día de entrada) que responde por el grupo en el reporte a Migración (TRA).

## Dinero
- **Folio**: la cuenta de la reserva, con sus cargos (hospedaje, parqueadero, mascotas, consumos, retenciones) y sus pagos.
- **Saldo** = cargos − pagos. «Pendiente» si el huésped debe, «A favor» si pagó de más, «Al día» si está en cero.
- **Anticipo**: el porcentaje del total que el hotel exige pagar para confirmar. Incluye el parqueadero y las mascotas. Lo dice «Anticipo exigido» en el bloque Pago.
- **Retención**: lo que el hotel se queda al cancelar o por el no-show, según la política de cancelación de la reserva.

## Canales
Una reserva puede venir de la recepción (Directo), de la web del hotel (Portal) o de una plataforma externa (Externo). Las que crea recepción en el PMS son siempre directas.

## TRA
Es el reporte de huéspedes a Migración. Por cada huésped se piden documento, apellidos, nacionalidad, género, país y ciudad de residencia, procedencia, destino y motivo del viaje. Faltar datos no impide registrar la llegada, pero **sí impide registrar la salida** (DEC-96).
