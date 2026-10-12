package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ActualizarTitularCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/titulares/{id}}: nombre y contacto como quedan. El documento no se puede cambiar
 * (TIT-01).
 */
public record ActualizarTitularRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @Email(message = "no tiene formato de correo")
        @Size(max = 150, message = "admite máximo 150 caracteres")
        String correo,

        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono) {

    ActualizarTitularCommand aCommand(String id) {
        return new ActualizarTitularCommand(id, nombre, correo, telefono);
    }
}
