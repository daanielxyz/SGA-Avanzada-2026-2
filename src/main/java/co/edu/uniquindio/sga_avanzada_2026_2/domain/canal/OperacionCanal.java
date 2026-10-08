package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

/**
 * Operaciones del contrato con un canal externo que quedan en la bitácora (CAN-03 · BIT-01).
 */
public enum OperacionCanal {
    CONSULTAR_DISPONIBILIDAD,
    CREAR_RESERVA,
    CANCELAR_RESERVA,
    PUBLICAR_DISPONIBILIDAD // saliente: el sistema avisa al canal
}
