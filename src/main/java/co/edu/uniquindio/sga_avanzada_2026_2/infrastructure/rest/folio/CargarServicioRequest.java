package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CargarServicioAdicionalCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de {@code POST /api/reservas/{codigo}/folio/servicios}: el servicio del catálogo que consumió el grupo
 * (CAR-05). El valor no viene en el cuerpo: es el del catálogo en este momento.
 */
public record CargarServicioRequest(
        @NotBlank(message = "es obligatorio")
        String servicioId) {

    CargarServicioAdicionalCommand aCommand(String codigoReserva) {
        return new CargarServicioAdicionalCommand(codigoReserva, servicioId);
    }
}
