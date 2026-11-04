package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarUbicacionAlojamientoCommand;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code PUT /api/alojamiento/ubicacion}. El rango de las coordenadas lo valida el dominio (409).
 */
public record UbicacionRequest(
        @NotNull(message = "es obligatoria")
        Double latitud,

        @NotNull(message = "es obligatoria")
        Double longitud) {

    CambiarUbicacionAlojamientoCommand aCommand(String alojamientoId) {
        return new CambiarUbicacionAlojamientoCommand(alojamientoId, latitud, longitud);
    }
}
