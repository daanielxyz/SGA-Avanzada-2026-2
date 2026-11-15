---
id: canales
titulo: Canales de venta, conflictos y bitácora
pantalla: /canales
roles: ADMINISTRADOR
---

# Canales de venta

Pantalla «Canales de venta» del menú (grupo «Configuración»), solo para el administrador. Un canal es por dónde entra una reserva: «Portal web» (la web de reservas del hotel), «Directo» (lo que crea recepción: teléfono, mostrador, WhatsApp) o «Externo» (una plataforma como Booking o Airbnb, que se conecta sola al sistema con una clave).

## Conflictos pendientes
Arriba de la pantalla. Un conflicto es una reserva que mandó un canal externo y que el sistema **rechazó para no vender dos veces la misma noche** (noche ocupada, apartamento bloqueado o fuera de servicio, más personas que la capacidad, o sin tiempo para prepararlo). La reserva que ya estaba no se toca. El huésped cree que tiene reserva, así que hay que contactarlo.

Cada tarjeta dice el canal, la reserva externa, el apartamento, las fechas, el motivo, cuándo llegó y a quién llamar (nombre y teléfono, o el correo si el canal no mandó teléfono). Los conflictos de antes pueden decir «Sin datos de contacto»: los datos del huésped están en la bitácora del canal, en «Ver mensaje del canal».

Para resolverlo:
1. Llamar o escribirle al huésped y decidir qué hacer: reubicarlo en otro apartamento, cambiar las fechas o cancelar en la plataforma.
2. Si hay que crear o mover una reserva, hacerlo en Reservas. **Resolver el conflicto no cambia ninguna reserva.**
3. «Resolver» en la tarjeta, escribir en «¿Qué se decidió?» lo que se hizo (obligatorio, hasta 500 caracteres) y «Marcar como resuelto». Queda registrado quién lo revisó, cuándo y qué se decidió, y el conflicto sale de la lista.

Si al resolver el sistema dice que ya lo resolvió otra persona, la lista se pone al día sola.

## Canales
Debajo, la lista de canales con su tipo, su estado («Activo» o «Inactivo») y, en los externos, la referencia de la clave.

- **Registrar canal**: nombre y tipo. Si es externo, además la **referencia de la clave**: el nombre con el que el equipo técnico guardó la clave en el servidor (por ejemplo `BOOKING_API_KEY`), **nunca la clave misma**. El canal nace activo.
- **Desactivar** (solo en los externos activos): el canal deja de crear reservas nuevas; las que ya trajo no cambian. **No se puede volver a activar desde el PMS**: hay que pedírselo al equipo técnico.
- El portal web y recepción no se desactivan desde aquí.

## Bitácora de un canal externo
«Ver bitácora» en la fila del canal. Muestra cada mensaje con el canal, también los rechazados, del más reciente al más antiguo y de 10 en 10. Al abrir, los últimos 7 días; se cambian las fechas «Desde» y «Hasta» (las dos se incluyen) y «Consultar».

Cada mensaje dice qué hizo el canal («Consultó disponibilidad», «Envió una reserva», «Canceló una reserva»), el resultado y el detalle (el código de la reserva o el motivo del rechazo):
- «Hecho»: se hizo.
- «Repetido»: esa reserva ya había llegado; no se creó otra.
- «Rechazado por disponibilidad»: no cabía; quedó un conflicto pendiente.
- «Rechazado»: otra regla lo impidió (por ejemplo, el canal está inactivo o falta una tarifa).
- «Error»: algo falló al procesarlo.

«Ver mensaje del canal» muestra lo que mandó la plataforma tal cual. Trae datos del huésped: no se copian fuera del sistema.
