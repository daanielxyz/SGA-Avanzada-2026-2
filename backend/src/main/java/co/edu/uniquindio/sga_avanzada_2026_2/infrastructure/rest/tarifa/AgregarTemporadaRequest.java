package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cuerpo de {@code POST /api/temporadas} (CU-33): la temporada con la tarifa de cada apartamento activo (TAR-03).
 *
 * @param fechaFin             inclusiva (TEM-01)
 * @param estanciaMinimaNoches opcional; sin ella no se exige mínimo (TEM-07)
 * @param tarifas              opcional mientras no haya apartamentos activos
 */
public record AgregarTemporadaRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 80, message = "admite máximo 80 caracteres")
        String nombre,

        @NotNull(message = "es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "es obligatoria")
        LocalDate fechaFin,

        @Min(value = 0, message = "no puede ser negativa")
        Integer estanciaMinimaNoches,

        List<@Valid @NotNull(message = "no puede ser nula") TarifaRequest> tarifas) {

    public record TarifaRequest(
            @NotBlank(message = "es obligatorio")
            String apartamento,

            @NotNull(message = "es obligatorio")
            @Positive(message = "debe ser mayor que cero")
            @Digits(integer = 12, fraction = 0, message = "debe ser un valor en pesos sin decimales")
            BigDecimal valorPorOcupante) {
    }

    AgregarTemporadaCommand aCommand(String alojamientoId) {
        return new AgregarTemporadaCommand(alojamientoId, nombre, fechaInicio, fechaFin,
                Optional.ofNullable(estanciaMinimaNoches).orElse(0),
                Optional.ofNullable(tarifas).orElse(List.of()).stream()
                        .map(t -> new AgregarTemporadaCommand.TarifaCommand(t.apartamento(), t.valorPorOcupante()))
                        .toList());
    }
}
