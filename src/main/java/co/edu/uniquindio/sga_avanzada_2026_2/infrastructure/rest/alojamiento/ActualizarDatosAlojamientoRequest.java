package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.ActualizarDatosAlojamientoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/alojamiento/datos} (CU-30): reemplaza los datos del establecimiento y sus normas.
 */
public record ActualizarDatosAlojamientoRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatoria")
        String descripcion,

        @NotBlank(message = "es obligatoria")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String ciudad,

        @NotBlank(message = "es obligatoria")
        @Size(max = 200, message = "admite máximo 200 caracteres")
        String direccion,

        String normas) {

    ActualizarDatosAlojamientoCommand aCommand(String alojamientoId) {
        return new ActualizarDatosAlojamientoCommand(alojamientoId, nombre, descripcion, ciudad, direccion, normas);
    }
}
