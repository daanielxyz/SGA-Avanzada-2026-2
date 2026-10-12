package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RevertirCargoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/reservas/{codigo}/folio/cargos/{id}/revertir}: por qué se corrige el cargo (CAR-03).
 */
public record RevertirCargoRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 170, message = "admite máximo 170 caracteres")
        String motivo) {

    RevertirCargoCommand aCommand(String codigoReserva, String cargoId) {
        return new RevertirCargoCommand(codigoReserva, cargoId, motivo);
    }
}
