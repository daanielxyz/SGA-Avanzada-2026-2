package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarPagoCommand;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuerpo de {@code POST /api/reservas/{codigo}/folio/pagos} y de {@code …/folio/devoluciones}. Valida solo el
 * formato (400); que el medio esté habilitado y la fecha no sea futura lo valida el dominio (409).
 *
 * @param medio nombre del medio de pago del catálogo del alojamiento
 * @param fecha cuándo se recibió o se devolvió el dinero
 */
public record RegistrarPagoRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 40, message = "admite máximo 40 caracteres")
        String medio,

        @NotNull(message = "es obligatorio")
        @Positive(message = "debe ser mayor que cero")
        @Digits(integer = 12, fraction = 0, message = "debe ser un valor en pesos sin decimales")
        BigDecimal monto,

        @NotNull(message = "es obligatoria")
        LocalDate fecha) {

    RegistrarPagoCommand aCommand(String codigoReserva) {
        return new RegistrarPagoCommand(codigoReserva, medio, monto, fecha);
    }
}
