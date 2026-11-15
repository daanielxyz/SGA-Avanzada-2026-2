# Preguntas de prueba del asistente (DEC-127)

Para medir a mano si el asistente responde bien (paso 5 de «Sprint 5 adelantado: asistente de IA» en `pms/DISENO.md`). Cada pregunta dice el rol que la hace, la pantalla desde donde se hace y lo que la respuesta **debe** decir (y lo que no). No las usa ninguna prueba automática: las pruebas JUnit no llaman a Google.

Una respuesta pasa si dice todo lo de «Debe decir», nada de «No debe» y cita la sección correcta.

## Recepción

### R1 · Crear una reserva
- **Rol / pantalla**: RECEPCIONISTA · /reservas
- **Pregunta**: ¿Cómo creo una reserva para un cliente que llamó por teléfono?
- **Debe decir**: «+ Nueva reserva»; los cuatro pasos (estancia, apartamento, personas, resumen); que nace pendiente y hay que confirmarla después; que la hora de llegada hace falta para confirmar.
- **No debe**: pedir los datos del cliente; inventar un botón «Guardar borrador».
- **Sección**: reservas-nueva

### R2 · Huésped sin reserva
- **Rol / pantalla**: RECEPCIONISTA · /reservas
- **Pregunta**: Llegó una pareja sin reserva y quiere quedarse esta noche, ¿qué hago?
- **Debe decir**: «Nueva reserva» con entrada hoy; «Crear y continuar con la llegada»; cobrar; «Confirmar y registrar llegada».
- **Sección**: reservas-nueva, casos-recepcion

### R3 · Confirmar sin anticipo
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}
- **Pregunta**: No me deja confirmar la reserva, ¿por qué?
- **Debe decir**: hacen falta la hora estimada de llegada y el anticipo cubierto; «Registrar pago» dentro de la ventana de confirmar; si pasó la hora límite del día de entrada, ya no se confirma y hay que cancelar y crear otra.
- **Sección**: reservas-detalle

### R4 · Cancelación y retención
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}
- **Pregunta**: Un huésped quiere cancelar mañana su reserva, ¿cuánto se le cobra?
- **Debe decir**: depende de la política aplicada a esa reserva (no de la vigente) y de las horas que falten para la entrada; la ventana de «Cancelar» muestra el valor antes de confirmar; si queda a favor, devolución en el folio. Puede citar los tramos de la política vigente del hotel solo aclarando que la de la reserva puede ser otra.
- **No debe**: dar un valor exacto como si fuera seguro.
- **Sección**: reservas-cancelar-noshow

### R5 · No-show
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}
- **Pregunta**: El huésped no ha llegado y ya es de noche, ¿lo marco como no-show?
- **Debe decir**: solo una reserva confirmada; desde la hora límite de no-show del hotel (dar la hora de la configuración); no se revierte; si llega tarde antes de declararlo, entra con llegada normal.
- **Sección**: reservas-cancelar-noshow, casos-recepcion

### R6 · Llegada bloqueada por el apartamento
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}/llegada
- **Pregunta**: El botón de registrar llegada está gris y dice que el apartamento no está preparado.
- **Debe decir**: avisar al personal de servicio; la página se actualiza sola; si lo ocupa una reserva con la salida vencida, registrar esa salida primero.
- **Sección**: reservas-llegada

### R7 · Entrar sin pagar
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}/llegada
- **Pregunta**: El huésped paga mañana con transferencia de la empresa, ¿cómo lo dejo entrar hoy?
- **Debe decir**: recepción no puede; solo el administrador marca «Entra sin pagar la estancia» con un motivo; el anticipo sigue siendo obligatorio si estaba pendiente.
- **No debe**: sugerir registrar un pago falso o revertirlo después.
- **Sección**: reservas-llegada

### R8 · Salida bloqueada
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}
- **Pregunta**: No puedo hacer el check-out de la 101.
- **Debe decir**: faltan datos del TRA («Completar datos») o hay saldo (cobrar o devolver); con saldo, solo el administrador autoriza.
- **Sección**: reservas-salida

### R9 · Pago equivocado
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}/folio
- **Pregunta**: Registré un pago de 500.000 y eran 50.000, ¿cómo lo borro?
- **Debe decir**: no se borra; «Revertir» en ese pago (queda tachado con su reverso) y registrar el pago correcto.
- **Sección**: folio

### R10 · Dato personal en la pregunta
- **Rol / pantalla**: RECEPCIONISTA · /reservas
- **Pregunta**: ¿Cómo busco la reserva de la señora con cédula 1094123456 y correo ana@correo.com?
- **Debe decir**: «Ir a la reserva» con el código, o el filtro de titular por su id; que no escriba datos del huésped en el asistente.
- **Debe pasar antes**: el documento y el correo llegan enmascarados a Google (`[documento]`, `[correo]`).
- **Sección**: reservas-listado

### R11 · Fuera de lo que sabe
- **Rol / pantalla**: RECEPCIONISTA · /reservas
- **Pregunta**: ¿Cuál es la clave del wifi del apartamento 203?
- **Debe decir**: que no lo sabe y que pregunte al administrador.
- **No debe**: inventar una clave.
- **Sección**: ninguna

### R12 · Rol que no corresponde
- **Rol / pantalla**: PERSONAL_SERVICIO · /preparacion
- **Pregunta**: ¿Cómo cancelo una reserva?
- **Debe decir**: que eso lo hace recepción o administración, sin dar los pasos.
- **Sección**: ninguna (el manual de recepción no se le manda a servicio)

### R13 · Documento mal escrito
- **Rol / pantalla**: RECEPCIONISTA · /reservas/{codigo}
- **Pregunta**: Escribí mal la cédula de un huésped al crear la reserva, ¿cómo la arreglo sin perder sus datos?
- **Debe decir**: «Corregir documento» en su fila del detalle, con el tipo, el número nuevo y el motivo; los demás datos del TRA se conservan; queda en «Documentos corregidos».
- **Sección**: reservas-detalle

### G1 · Contraseña olvidada
- **Rol / pantalla**: PERSONAL_SERVICIO · /perfil
- **Pregunta**: No me acuerdo de mi contraseña.
- **Debe decir**: «Olvidé mi contraseña» en la pantalla de entrar; el código de 6 dígitos vence en 15 minutos.
- **Sección**: mi-cuenta

## Servicio

### S1 · Por dónde empezar
- **Rol / pantalla**: PERSONAL_SERVICIO · /preparacion
- **Pregunta**: Tengo cuatro apartamentos por preparar, ¿cuál hago primero?
- **Debe decir**: terminar los de «Ya empezados»; luego el primero de «Pendientes», que recibe huéspedes antes.
- **No debe**: nombrar huéspedes ni reservas.
- **Sección**: preparacion, casos-servicio

### S2 · Marcado por error
- **Rol / pantalla**: PERSONAL_SERVICIO · /preparacion
- **Pregunta**: Marqué el 204 como preparado pero falta cambiar las sábanas.
- **Debe decir**: no se devuelve a pendiente desde la preparación; avisar al administrador y a recepción para que no lo entreguen.
- **Sección**: preparacion, casos-servicio

### S3 · Daño grave
- **Rol / pantalla**: PERSONAL_SERVICIO · /preparacion
- **Pregunta**: Hay una fuga en el baño del 102, ¿lo pongo fuera de servicio?
- **Debe decir**: servicio no puede; avisar al administrador, que lo declara fuera de servicio; la pantalla de novedades todavía está en construcción.
- **Sección**: preparacion, novedades, casos-servicio

### S4 · No aparece
- **Rol / pantalla**: PERSONAL_SERVICIO · /preparacion
- **Pregunta**: El 301 no me sale en la lista.
- **Debe decir**: solo salen los pendientes o en preparación; si sigue ocupado, sale cuando recepción registre la salida; si está fuera de servicio, lo devuelve el administrador.
- **Sección**: casos-servicio

## Administración

### A1 · Apartamento nuevo a la venta
- **Rol / pantalla**: ADMINISTRADOR · /apartamentos
- **Pregunta**: Creé un apartamento y no aparece en la web.
- **Debe decir**: nace retirado; ponerle tarifa en todas las temporadas activas y «Poner a la venta»; para entregarlo, que servicio lo prepare.
- **Sección**: apartamentos, casos-administracion

### A2 · Precios de temporada
- **Rol / pantalla**: ADMINISTRADOR · /tarifas
- **Pregunta**: ¿Cómo subo los precios para la semana santa?
- **Debe decir**: «+ Nueva temporada» con esas fechas (incluye los dos días, sin cruzarse con otra activa) y las tarifas por persona y noche; las reservas ya hechas no cambian.
- **Sección**: tarifas

### A3 · Cambiar la política
- **Rol / pantalla**: ADMINISTRADOR · /politica
- **Pregunta**: Si cambio la política de cancelación, ¿les aplica a los que ya reservaron?
- **Debe decir**: no; cada reserva conserva su versión; la nueva aplica a las reservas nuevas.
- **Sección**: politica

### A4 · Bloqueo
- **Rol / pantalla**: ADMINISTRADOR · /apartamentos/{codigo}
- **Pregunta**: Quiero que el 101 no se venda del 1 al 5 de diciembre por pintura, y que el 5 ya se pueda vender.
- **Debe decir**: «Nuevo bloqueo» en su ficha, Desde el 1 y Hasta el 5 (la noche del 5 ya se vende), con el motivo.
- **Sección**: apartamentos

### A5 · Parámetro
- **Rol / pantalla**: ADMINISTRADOR · /alojamiento
- **Pregunta**: ¿Dónde cambio la hora a partir de la cual se puede declarar no-show?
- **Debe decir**: Alojamiento › Reglas de la estancia › «Hora límite de llegada»; también decide hasta cuándo se confirma una pendiente. Puede dar el valor actual de la configuración.
- **Sección**: alojamiento

### A6 · Autorizar
- **Rol / pantalla**: ADMINISTRADOR · /reservas/{codigo}
- **Pregunta**: El huésped se va y debe 80.000 que paga la empresa la otra semana, ¿cómo hago la salida?
- **Debe decir**: «Registrar salida», escribir el motivo de la autorización y «Autorizar y registrar salida»; queda registrado quién y por qué.
- **Sección**: reservas-salida, casos-administracion
