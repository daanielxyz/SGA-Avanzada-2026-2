package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.cuenta;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ActualizarCuentaTitularCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/mi-cuenta}: nombre y teléfono. El correo cambia con PIN y el documento nunca (DEC-69).
 * Con la identidad validada, un nombre distinto responde 409 {@code IDENTIDAD_VALIDADA} (DEC-98). El nombre llega en
 * {@code nombres} y los apellidos aparte o, como antes, todo junto en {@code nombre}.
 */
public record ActualizarCuentaRequest(
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Size(max = 30, message = "admite máximo 30 caracteres")
        String telefono,

        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombres,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String primerApellido,

        @Size(max = 60, message = "admite máximo 60 caracteres")
        String segundoApellido) {

    /** DEC-98: los nombres llegan aparte de los apellidos o, como antes, todo junto en {@code nombre}. */
    @JsonIgnore
    @AssertTrue(message = "hace falta nombres o nombre")
    public boolean isNombreRecibido() {
        return separado() || (nombre != null && !nombre.isBlank());
    }

    private boolean separado() {
        return nombres != null && !nombres.isBlank();
    }

    ActualizarCuentaTitularCommand aCommand(String titularId) {
        return new ActualizarCuentaTitularCommand(titularId, separado() ? nombres : nombre,
                separado() ? primerApellido : null, separado() ? segundoApellido : null, telefono);
    }
}
