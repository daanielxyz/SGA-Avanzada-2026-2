package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/auth/huesped/cambio-correo}: el documento del titular y el PIN que llegó al teléfono o al
 * correo actual.
 */
public record ConfirmarCambioCorreoRequest(
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "CC|CE|PASAPORTE|TI", message = "debe ser CC, CE, PASAPORTE o TI")
        String tipoDocumento,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String numeroDocumento,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "debe tener 6 dígitos")
        String pin) {

    /** Sin el PIN, por si el Request termina en un log. */
    @Override
    public String toString() {
        return "ConfirmarCambioCorreoRequest[documento=" + tipoDocumento + " " + numeroDocumento + "]";
    }
}
