package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ActualizarTitularCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/titulares/{id}}: nombre y contacto como quedan. Lo usa el hotel, también con la
 * identidad validada (DEC-98). El documento no se puede cambiar (TIT-05). El nombre llega en {@code nombres} y los
 * apellidos aparte o, como antes, todo junto en {@code nombre}.
 */
public record ActualizarTitularRequest(
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @Email(message = "no tiene formato de correo")
        @Size(max = 150, message = "admite máximo 150 caracteres")
        String correo,

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

    ActualizarTitularCommand aCommand(String id) {
        return new ActualizarTitularCommand(id, separado() ? nombres : nombre, separado() ? primerApellido : null,
                separado() ? segundoApellido : null, correo, telefono);
    }
}
