package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.PagarEnLineaCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/portal/reservas/{codigo}/pago}: el token que la pasarela le dio al cliente. Nunca datos
 * de la tarjeta (DEC-69).
 */
public record PagoEnLineaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 200, message = "admite máximo 200 caracteres")
        String tokenPago) {

    PagarEnLineaCommand aCommand(String codigo) {
        return new PagarEnLineaCommand(codigo, tokenPago);
    }
}
