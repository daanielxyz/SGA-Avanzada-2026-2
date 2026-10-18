package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import java.util.List;

/**
 * Cuerpo uniforme de todo error HTTP (DEC-33).
 *
 * @param codigo   VALIDACION, CREDENCIALES_INVALIDAS, NO_AUTENTICADO, CANAL_NO_AUTENTICADO, SIN_PERMISO,
 *                 NO_ENCONTRADO, DUPLICADO, REGLA_NEGOCIO, PAGO_RECHAZADO o CONCURRENCIA
 * @param detalles errores por campo cuando la validación del Request falla; vacío en los demás casos
 */
public record ErrorResponse(int estado, String codigo, String mensaje, List<String> detalles) {
}
