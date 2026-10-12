package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarValorServicioCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code PUT /api/alojamiento/servicios/{id}/valor}: el precio nuevo del servicio (SERV-02).
 */
public record ValorServicioRequest(
        @NotNull(message = "es obligatorio")
        @PositiveOrZero(message = "no puede ser negativo")
        @Digits(integer = 12, fraction = 0, message = "debe ser un valor en pesos sin decimales")
        BigDecimal valor) {

    CambiarValorServicioCommand aCommand(String alojamientoId, String servicioId) {
        return new CambiarValorServicioCommand(alojamientoId, servicioId, valor);
    }
}
