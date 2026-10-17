package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El titular que envía un canal externo. A diferencia del portal y de recepción, el correo y el teléfono no son
 * obligatorios: algunas plataformas no los comparten. Sin correo, el titular no tiene cuenta y sus cambios llegan por
 * la API del canal (DEC-68).
 */
public record TitularExternoRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @Email(message = "no tiene formato de correo")
        @Size(max = 150, message = "admite máximo 150 caracteres")
        String correo,

        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono) {

    TitularCommand aCommand() {
        return new TitularCommand(nombre, tipoDocumento, numeroDocumento, correo, telefono);
    }
}
