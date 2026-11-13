package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo opcional de {@code PUT /api/reservas/{codigo}/registrar-llegada}.
 *
 * @param motivoSinPago solo para entrar sin haber pagado la estancia y sus extras, por ejemplo una cuenta por cobrar
 *                      a una empresa (BL-22). Queda con el autor del registro. Solo el administrador puede
 *                      enviarlo (AUTC-01 · DEC-112)
 */
public record RegistrarLlegadaRequest(
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String motivoSinPago) {
}
