package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CargarServicioAdicionalCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

/**
 * Cuerpo de {@code POST /api/reservas/{codigo}/folio/servicios}: el servicio del catálogo que consumió el grupo
 * (CAR-05). El valor no viene en el cuerpo: es el del catálogo en este momento, por la cantidad (BL-12).
 */
public record CargarServicioRequest(
        @NotBlank(message = "es obligatorio")
        String servicioId,

        // Opcional (BL-12): sin ella, una unidad
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 99, message = "admite máximo 99")
        Integer cantidad) {

    CargarServicioAdicionalCommand aCommand(String codigoReserva) {
        return new CargarServicioAdicionalCommand(codigoReserva, servicioId, Optional.ofNullable(cantidad).orElse(1));
    }
}
