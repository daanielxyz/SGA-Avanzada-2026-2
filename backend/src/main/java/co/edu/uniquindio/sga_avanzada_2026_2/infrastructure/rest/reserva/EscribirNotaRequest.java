package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.EscribirNotaCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/reservas/{codigo}/notas}: lo que el turno le deja dicho al siguiente (BL-08 · DEC-105).
 */
public record EscribirNotaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 500, message = "admite máximo 500 caracteres")
        String texto) {

    EscribirNotaCommand aCommand(String codigoReserva, String autor) {
        return new EscribirNotaCommand(codigoReserva, texto, autor);
    }
}
