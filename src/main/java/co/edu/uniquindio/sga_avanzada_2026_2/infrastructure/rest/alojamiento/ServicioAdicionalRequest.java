package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.AgregarServicioAdicionalCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST /api/alojamiento/servicios}: un servicio nuevo del catálogo (SERV-01).
 */
public record ServicioAdicionalRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "admite máximo 120 caracteres")
        String nombre,

        @NotNull(message = "es obligatorio")
        Boolean generaCargo,

        @NotNull(message = "es obligatorio")
        @PositiveOrZero(message = "no puede ser negativo")
        @Digits(integer = 12, fraction = 0, message = "debe ser un valor en pesos sin decimales")
        BigDecimal valor) {

    AgregarServicioAdicionalCommand aCommand(String alojamientoId) {
        return new AgregarServicioAdicionalCommand(alojamientoId, nombre, generaCargo, valor);
    }
}
