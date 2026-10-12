package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/politicas}: la versión nueva de la política de cancelación (CU-35 · DEC-41). Valida
 * solo el formato (400); que los tramos sean coherentes lo valida el dominio (409).
 */
public record PublicarPoliticaRequest(
        @NotEmpty(message = "debe tener al menos un tramo")
        List<@Valid @NotNull(message = "no puede ser nulo") TramoRequest> tramos,

        @NotNull(message = "es obligatoria")
        @Valid
        PenalizacionRequest penalizacionNoShow) {

    /**
     * @param antelacionMinHoras horas antes de la entrada desde las que aplica el tramo
     */
    public record TramoRequest(
            @NotNull(message = "es obligatoria")
            @Min(value = 0, message = "no puede ser negativa")
            Integer antelacionMinHoras,

            @NotNull(message = "es obligatoria")
            @Valid
            PenalizacionRequest penalizacion) {
    }

    /**
     * Porcentaje de la base o monto fijo; que venga exactamente uno lo valida el dominio.
     */
    public record PenalizacionRequest(
            @NotBlank(message = "es obligatoria")
            @Pattern(regexp = "VALOR_TOTAL|PAGADO|ANTICIPO_EXIGIDO",
                    message = "debe ser VALOR_TOTAL, PAGADO o ANTICIPO_EXIGIDO")
            String base,

            @Min(value = 0, message = "debe estar entre 0 y 100")
            @Max(value = 100, message = "debe estar entre 0 y 100")
            Integer porcentaje,

            @Positive(message = "debe ser mayor que cero")
            @Digits(integer = 12, fraction = 0, message = "debe ser un valor en pesos sin decimales")
            BigDecimal montoFijo) {

        PublicarPoliticaCommand.PenalizacionCommand aCommand() {
            return new PublicarPoliticaCommand.PenalizacionCommand(base, porcentaje, montoFijo);
        }
    }

    PublicarPoliticaCommand aCommand(String alojamientoId) {
        return new PublicarPoliticaCommand(alojamientoId,
                tramos.stream()
                        .map(t -> new PublicarPoliticaCommand.TramoCommand(t.antelacionMinHoras(),
                                t.penalizacion().aCommand()))
                        .toList(),
                penalizacionNoShow.aCommand());
    }
}
