package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

/**
 * Por qué se rechazó una operación, para que quien la pidió sepa qué puede hacer sin leer el texto del mensaje. Solo
 * se clasifican las reglas ante las que una pantalla reacciona distinto; las demás no llevan motivo.
 */
public enum MotivoRechazo {
    /** RN-01 · RN-07 · RN-20: otra reserva, un bloqueo o la preparación ocupan esas noches. */
    NO_DISPONIBLE,
    /** RN-03 · RN-04: las fechas de la estancia no son válidas. */
    FECHAS_INVALIDAS,
    /** RN-02: el grupo no cabe en el apartamento. */
    CAPACIDAD_EXCEDIDA,
    /** RP-01: la estancia es más corta que el mínimo de la temporada. */
    ESTANCIA_MINIMA,
    /** TIT-01: el titular no cumple lo que se le exige. */
    TITULAR_INVALIDO,
    /** RP-02: el parqueadero no tiene cupo alguna de las noches. */
    SIN_CUPO_PARQUEADERO,
    /** RP-03: el alojamiento o el apartamento no admite esas mascotas. */
    MASCOTAS_NO_ADMITIDAS,
    /** RN-05 · TAR-03: al apartamento le falta la tarifa de una temporada. */
    SIN_TARIFA,
    /** RN-09: falta la hora estimada de llegada. */
    FALTA_HORA_LLEGADA,
    /** RES-15: lo pagado no cubre el anticipo. */
    ANTICIPO_INSUFICIENTE,
    /** RN-08: el estado actual de la reserva no permite la operación; pudo cambiarla otro turno. */
    ESTADO_NO_PERMITIDO,
    /** RN-10 · RES-16: todavía no llega la fecha o la hora desde la que se puede hacer. */
    ANTES_DE_TIEMPO,
    /** RN-11: el apartamento no está preparado para entregarse. */
    APARTAMENTO_NO_PREPARADO,
    /** RN-17: el folio no está en cero y no hay autorización para cerrarlo. */
    SALDO_PENDIENTE,
    /** CAR-07: el folio está cerrado y no admite movimientos. */
    FOLIO_CERRADO,
    /** RN-16: ese cargo o ese pago ya fue revertido; pudo hacerlo otro turno. */
    YA_REVERTIDO,
    /** BL-07: a algún ocupante le faltan datos del TRA; la salida espera a que se completen. */
    DATOS_TRA_INCOMPLETOS,
    /** DEC-97: falta el huésped principal, hay más de uno o no es mayor de edad. */
    HUESPED_PRINCIPAL_INVALIDO,
    /** DEC-98: recepción ya validó la identidad del titular; su nombre solo lo cambia el hotel. */
    IDENTIDAD_VALIDADA,
    /** BL-22: la estancia y sus extras no están pagados y no se registró por qué entra sin pagar. */
    ESTANCIA_SIN_PAGAR,
    /** BL-08: la nota ya se marcó como atendida; pudo hacerlo otro turno. */
    YA_ATENDIDA,
    /** Pruebas del sprint 3: la reserva pendiente no se confirmó antes de la hora límite de llegada de su entrada. */
    LLEGADA_VENCIDA
}
