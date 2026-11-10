# Backlog del producto

Todo lo que el cliente pide y todavía no tiene sprint: los requisitos de la entrevista, lo que sale de cada cierre y lo que quedó para el MVP2. Se ordena con el cliente en cada cierre (`docs/SPRINTS.md`). Para que un ítem entre a un sprint necesita producto (web o pms), una prioridad y cómo se sabrá que funcionó.

| Id | Requisito | Origen | Producto | Prioridad | Cómo sabremos que funcionó | Sprint |
|---|---|---|---|---|---|---|
| BL-01 | Pasarela de pago real (Wompi: tarjeta, PSE, Nequi) | DEC-74 | web | MVP2 | — | — |
| BL-02 | Facturación electrónica DIAN (candidato Factus) | DEC-74 | pms | MVP2 | — | — |
| BL-03 | Proveedor de SMS | DEC-74 | núcleo | Por definir | — | — |
| BL-04 | Varios alojamientos en el backend: un despliegue por hotel o un solo backend para todos (y si `web/` pasa a su propio repositorio) | DEC-83 | núcleo | MVP2 (con el segundo cliente) | Dos alojamientos operan a la vez sin ver los datos del otro | — |
| BL-05 | Marca de cada alojamiento en la web: logo y colores como configuración que llega por la API | DEC-83 | web (y núcleo) | Por definir | Se cambian el logo y los colores de un hotel sin desplegar código | Hecho en el sprint 1 (DEC-87) |
| BL-06 | Publicar los frontends (Cloudflare Pages, un proyecto por frontend y alojamiento) y el backend | DEC-83 | núcleo | Por definir | `web/` y `pms/` abren desde su dominio contra la API publicada | — |

| BL-07 | Datos del TRA de cada huésped en el registro de llegada: tipo y número de documento, nombres, primer y segundo apellido, fecha de nacimiento, nacionalidad, género, país y ciudad de residencia, procedencia, destino y motivo del viaje (catálogo de 10 motivos). La llegada se puede hacer con datos incompletos; **la salida queda bloqueada** (botón deshabilitado y 409 en el backend) mientras falte uno. Recepción solo pregunta de dónde viene y con eso propone residencia y destino. Dirección y ocupación no se piden | Entrevista | pms (y núcleo: `Ocupante`) | Alta | La recepcionista registra la llegada de un huésped sin escribir dos veces el mismo dato, y una salida con datos incompletos no pasa | 3 (el envío del reporte sigue en el 5, B5) |
| BL-08 | Notas de la reserva para el siguiente turno (pagó la mitad, sale temprano, pidió cobijas, falta la transferencia), visibles en el detalle y en la llegada | Entrevista | pms (y núcleo: `NotaReserva`, DEC-105) | Alta | El turno siguiente se entera de lo pendiente sin preguntar | 3 |
| BL-09 | Tablero coloreado por la cuenta: ya salió, al día, con saldo, debe todo | Entrevista | pms (y núcleo: saldo en `OcupacionResponse`, junto con el #2 de `pms/DISENO.md`) | Alta | Recepción ve en el tablero quién debe sin abrir cada reserva | 3 |
| BL-10 | La llegada muestra el desglose por noche y el saldo, y permite registrar el pago antes de entregar el apartamento | Entrevista | pms | Alta | Se cobra y se registra la llegada desde una sola pantalla | 3 |
| BL-11 | Reserva de mostrador (el huésped llega sin reserva): al crear una reserva directa que entra hoy, seguir con el pago, la confirmación y la llegada | Entrevista | pms | Media | Un huésped sin reserva queda alojado sin volver a buscar su reserva | 3 |
| BL-12 | Cantidad al cargar un consumo al folio («dos aguas») | Entrevista | pms (y núcleo: `cantidad` en el cargo, hecho en DEC-107) | Media | Un consumo repetido se carga en un solo paso | 3, dentro del folio |
| BL-13 | IVA según el documento del huésped (cédula colombiana paga el 19 %; pasaporte y cédula de extranjería no), tarifas de plataformas con IVA incluido, y retención en la fuente y reteIVA para empresas. Debe ser configuración del alojamiento | Entrevista | núcleo | Alta, por confirmar | — | Se pregunta al cliente en el cierre 3 |
| BL-14 | Facturar a una empresa distinta del titular, y consumos que no salen en la factura de la empresa (los paga el huésped aparte) | Entrevista | pms | MVP2, con BL-02 | — | — |
| BL-15 | Cambiar de apartamento durante la estancia (EN_CURSO), con el traslado visible en el tablero | Entrevista | núcleo y pms | Media (es esporádico) | — | — |
| BL-16 | Persona adicional que llega después del registro de llegada | Entrevista | núcleo y pms | Media | — | — |
| BL-17 | Placa del vehículo y número de cupo del parqueadero (hoy van en una planilla aparte) | Entrevista | pms | Baja | — | 4 o 5 |
| BL-18 | Foto del documento y soporte del pago adjuntos a la reserva | Entrevista | pms | Media | — | 5, con el TRA |
| BL-19 | Reporte SIRE de Migración para los extranjeros (mismos datos que el TRA más el tipo de movimiento) | Entrevista | pms | Media | — | 5, con el TRA |
| BL-20 | Check-in en línea: el huésped llena sus datos antes de llegar | Entrevista | web | Media | — | — |
| BL-21 | Imprimir la tarjeta de registro | Entrevista | pms | Baja | — | — |
| BL-22 | **La estancia se cobra en la llegada**: el registro de llegada se rechaza (409) si la estancia y sus extras de la reserva (parqueadero y mascotas, DEC-89) no están pagados, salvo una autorización registrada con autor y motivo (por ejemplo, cuenta por cobrar a una empresa), como el cierre del folio (RN-17). Los consumos se pagan en cualquier momento hasta la salida, que ya exige el folio en cero o autorizado | Entrevista | núcleo y pms | Alta | Ninguna llegada queda sin cobrar sin que alguien lo haya autorizado y dicho por qué | 3 |
| BL-23 | Modificar la reserva antes de la llegada también en carros, motos y mascotas (por ejemplo, llegó sin la mascota y se quita del total); la diferencia entra al folio como ajuste | Entrevista | pms (y núcleo: `ModificarReservaRequest`, hecho en DEC-106) | Alta | Recepción corrige lo que de verdad llegó sin cancelar ni crear otra reserva | 3 |
| BL-24 | Reincorporar un no-show cuando el huésped llega después de declararlo (`NO_SHOW` → `CONFIRMADA`). **Por ahora no se construye**: si recepción no lo declara, el huésped retrasado entra con un check-in normal, y volver a tomar noches ya liberadas toca el riesgo de vender dos veces. Se haría solo si el hotel confirma que le ocurre: mismo apartamento, autorizado por el administrador, con motivo y cobrando el valor congelado completo. Cambia una regla compartida: acordarlo antes con el equipo web | Equipo pms | núcleo y pms | Baja | Un huésped retrasado recupera su reserva sin crear otra | — |
| BL-25 | Guardar quién declaró el no-show y cuándo; hoy solo queda el estado de la reserva | Equipo pms | núcleo y pms | Media | Recepción ve en el detalle quién lo declaró y a qué hora | 3 |

**Descartado en la entrevista** (no lo usan o está fuera del alcance del SGA): visitantes, equipaje, el campo «plan», la caja de efectivo, gastos, proveedores y contabilidad, y escribir la tarifa a mano al reservar (choca con el valor congelado; un descuento se hace con un ajuste en el folio).

<!-- Requisitos nuevos de la entrevista o de un cierre: agregarlos aquí con el siguiente BL libre. -->
